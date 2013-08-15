package helper.gitaxid;

import db.tables.LookupNames;
import logger.Log;

import java.io.*;
import java.sql.*;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;

/**
 * Contains utility methods for nodes database deployment from gi_taxid.dmp file from NCBI
 */

public class GI_TaxIDDeployer {
    /**
     * Constructor grants non-insatiability
     */
    private GI_TaxIDDeployer() {
        throw new AssertionError();
    }

    /**
     * Deploys a GI_TAXID database table for the NCBI table gi_taxid.dmp, downloaded form the NCBI FTP.
     * Depends of the existence of the NCBI schema and an empty GI_TAXID table existence.
     * <b>Deprecated due to low efficiency.
     * See GI_TaxIDDeployer.filterGI_TaxIDDmp(Connection connection, File gi_taxidFile) for a preferred method.</b>
     *
     * @param connection   {@link Connection} to the database
     * @param gi_taxidFile {@link File} gi_taxid.dmp
     * @throws SQLException in case something goes wrong upon database communication
     * @throws IOException  in case something goes wrong during file read
     */
    @SuppressWarnings("ConstantConditions")
    @Deprecated
    public static void deployGI_TaxIDTable(Connection connection, File gi_taxidFile) throws SQLException, IOException {
        BufferedReader bufferedReader = null;
        PreparedStatement preparedStatement = null;

        Statement statement = null;
        ResultSet resultSet;
        //As long as the NCBI database is not fully consistent and not all of the GIs have assigned "scientific names" through the
        //"names" database, it is assumed that the names database has been successfully deployed and the existing taxids already
        //exist within the database.
        //Create a set of existing taxids
        Set<Integer> existingTaxIDs = null;
        try {
            statement = connection.createStatement();
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            resultSet = statement.executeQuery(
                    "select count("
                            + LookupNames.dbs.NCBI.names.columns.taxid + ") from "
                            + LookupNames.dbs.NCBI.names.name);
            int numberOfTaxIDs;
            if (resultSet.next()) {
                numberOfTaxIDs = resultSet.getInt(1);
                Log.getInstance().log(Level.FINE, "TaxIDs in database: " + numberOfTaxIDs);
                existingTaxIDs = new HashSet<Integer>(numberOfTaxIDs);
            }

            resultSet = statement.executeQuery(
                    "select " + LookupNames.dbs.NCBI.names.columns.taxid
                            + " from " + LookupNames.dbs.NCBI.names.name);
            while (resultSet.next()) {
                existingTaxIDs.add(resultSet.getInt(1));
            }

        } finally {
            if (statement != null) {
                statement.close();
            }
        }

        try {
            //Prepare a statement to insert
            preparedStatement = connection.prepareStatement(
                    "insert into " + LookupNames.dbs.NCBI.gi_taxid.name
                            + " (" + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name() + ","
                            + LookupNames.dbs.NCBI.gi_taxid.columns.taxid.name() + ")"
                            + " values(?,?) "
            );

            //Load the file and read line by line
            bufferedReader = new BufferedReader(new FileReader(gi_taxidFile));
            String line;
            int counter = 0;
            while ((line = bufferedReader.readLine()) != null) {

                String[] split = line.split("\t");

                Integer taxid = Integer.valueOf(split[1]);
                if (existingTaxIDs.contains(taxid)) {
                    Integer gi = Integer.valueOf(split[0]);
                    preparedStatement.setInt(1, gi);
                    preparedStatement.setInt(2, taxid);
                    preparedStatement.addBatch();
                    counter++;
                } else {
                    Log.getInstance().log(Level.INFO,"TaxID " + taxid + " was not included;");
                }
                //Execute batch every time the batch buffer gets full
                if (counter == 10000) {
                    preparedStatement.executeBatch();
                    Log.getInstance().log(Level.INFO,"Another batch inserted into gi_taxid, the last gi was: " + split[0]);
                    counter = 0;
                }
            }

            //Execute batch for the trace in the buffer
            preparedStatement.executeBatch();

        } finally {
            //Close and cleanup
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
    }

    /**
     * Makes a filtered copy of the gi_taxid.dmo database dump file, downloaded from the NCBI FTP.
     * As long as the taxid field of the GI_TAXID database has a foreign key of taxid form the Names database,
     * it first extracts all existing taxids form the Names table and generates a {@link HashSet}.
     * It then reads the .dmp file line by line and writes to a .mod file only those lines, that contain a valid taxid.
     *
     * @param connection   {@link Connection} to the database
     * @param gi_taxidFile {@link File} gi_taxid.dmp
     * @return {@link File} .mod file that contains the filtered database table .dmp file
     * @throws SQLException in case something goes wrong upon database communication
     * @throws IOException  IOException in case something goes wrong during file read/write
     */
    public static File filterGI_TaxIDDmp(Connection connection, File gi_taxidFile) throws SQLException, IOException {

        Statement statement = null;
        ResultSet resultSet = null;


        //Create a set of existing taxids
        Set<Integer> existingTaxIDs = null;

        try {
            statement = connection.createStatement();
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            resultSet = statement.executeQuery(
                    "select count("
                            + LookupNames.dbs.NCBI.names.columns.taxid + ") from "
                            + LookupNames.dbs.NCBI.names.name);
            int numberOfTaxIDs;
            if (resultSet.next()) {
                numberOfTaxIDs = resultSet.getInt(1);
                Log.getInstance().log(Level.INFO,"TaxIDs in database: " + numberOfTaxIDs);
                existingTaxIDs = new HashSet<Integer>(numberOfTaxIDs);
            }

            resultSet = statement.executeQuery(
                    "select " + LookupNames.dbs.NCBI.names.columns.taxid
                            + " from " + LookupNames.dbs.NCBI.names.name);
            while (resultSet.next()) {
                //noinspection ConstantConditions
                existingTaxIDs.add(resultSet.getInt(1));
            }

        }finally {
            if (statement != null) {
                statement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        //Open the gi_taxid.dmp file and read line by line
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        File outFile = new File(gi_taxidFile.getAbsoluteFile().toString() + ".mod");
        try {
            bufferedReader = new BufferedReader(new FileReader(gi_taxidFile));
            fileWriter = new FileWriter(outFile);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (existingTaxIDs != null && existingTaxIDs.contains(Integer.parseInt(line.split("\t")[1]))) {
                    fileWriter.write(line + '\n');
                }
            }
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
        return outFile;
    }

    /**
     * Injects the gi_taxid.dmp.mod prefiltered file into the GI_TAXID table of the NCBI schema.
     *
     * @param connection           {@link Connection} to the database
     * @param gi_taxidFilteredFile {@link File} gi_taxid.dmp
     * @throws SQLException in case something goes wrong upon database communication
     */
    public static void injectProcessedGI_TaxIDDmpFile(Connection connection, File gi_taxidFilteredFile) throws SQLException {

        Statement statement = null;

        try {
            statement = connection.createStatement();
            //Switch to a correct schema
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            final boolean execute=statement.execute(
                    "LOAD DATA INFILE '"
                            + gi_taxidFilteredFile.getPath().replaceAll(String.valueOf('\\'), "/")
                            + "' REPLACE INTO TABLE "
                            + LookupNames.dbs.NCBI.gi_taxid.name
                            + " FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n'");

        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
