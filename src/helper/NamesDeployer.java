package helper;

import db.tables.LookupNames;

import java.io.*;
import java.sql.*;

/**
 * Contains utility methods for nodes database deployment from names.dmp file from NCBI
 */
public class NamesDeployer {
    /**
     * A size for batch inserts
     */
    private static int BATCH_SIZE = 10000;
    /**
     * The "scientific name" that indicates that the taxid points to a valid name for a taxonomic group
     */
    private static String SCIENTIFIC_NAME = "scientific name";

    /**
     * Constructor grants non-instantiability
     */
    private NamesDeployer() {
        throw new AssertionError();
    }

    /**
     * Deploys the Names database table. Depends of the existence of the NCBI schema and an empty Names table existence.
     * <b>Deprecated due to low efficiency, See injectProcessedNamesDmpFile(Connection connection, File nodesFilteredFile)
     * and filterNodesDmpFile(File namesDmpFile) as a faster way of implementation. </b>
     *
     * @param connection
     * @param namesFile
     * @throws SQLException in case something goes wrong upon database communication
     * @throws IOException  in case something goes wrong during file read
     */
    @Deprecated
    public static void deployNamesTable(Connection connection, File namesFile) throws SQLException, IOException {
        BufferedReader bufferedReader = null;
        PreparedStatement preparedStatement = null;
        //Switch to correct schema
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute("use " + LookupNames.dbs.NCBI.name);
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if(statement!=null){
                statement.close();
            }  
        }

        try {
            //Prepare a statement to insert
            preparedStatement = connection.prepareStatement(
                    "insert into " + LookupNames.dbs.NCBI.names.name
                            + " (" + LookupNames.dbs.NCBI.names.columns.taxid.name() + ","
                            + LookupNames.dbs.NCBI.names.columns.name.name() + ")"
                            + " values(?,?) "
            );

            //Load the file and read line by line
            bufferedReader = new BufferedReader(new FileReader(namesFile));
            String line;
            int counter = 0;
            while ((line = bufferedReader.readLine()) != null) {

                String[] split = line.split("\t");//The dmp file has a broken format, can't use "\t\\|\t"
                if (split[6].equals("scientific name")) {
                    preparedStatement.setInt(1, Integer.valueOf(split[0]));
                    preparedStatement.setString(2, split[2]);
                    preparedStatement.addBatch();
                    counter++;
                }
                //Execute batch every time the batch buffer gets full
                if (counter == NamesDeployer.BATCH_SIZE) {
                    preparedStatement.executeBatch();
                    System.out.println("Another batch inserted into names, the last gi was: " + split[0]);
                    counter = 0;
                }
            }

            //Execute batch for the trace in the buffer
            preparedStatement.executeBatch();

        } catch (SQLException sqle) {
            throw sqle;
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            //Close and cleanup
            if(bufferedReader!=null){
                bufferedReader.close(); 
            }
            if(preparedStatement!=null){
                preparedStatement.close();
            }
        }
    }

    /**
     * Re-parses the nodes.dmp file. Extracts the taxid and the "scientific name" marked fields
     *
     * @param namesDmpFile {@link File} names.dmp
     * @return a new {@link File} that points to the newly filtered file
     * @throws IOException
     * @throws SQLException
     */
    public static File filterNamesDmpFile(File namesDmpFile) throws IOException {
        //Read the input file line by line
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        File filteredNamesDmpFile = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(namesDmpFile));
            filteredNamesDmpFile = new File(namesDmpFile.getAbsoluteFile().toString() + ".mod");
            fileWriter = new FileWriter(filteredNamesDmpFile);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitter = line.split("\t");
                String[] split = line.split("\t");//The dmp file has a broken format, can't use "\t\\|\t"
                if (split[6].equals(NamesDeployer.SCIENTIFIC_NAME)) {
                    //TODO: input another check which filters out environmental and other crap
                    fileWriter.write(split[0] + '\t' + split[2] + '\n');
                }
            }
            fileWriter.flush();

        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            bufferedReader.close();
            if(fileWriter!=null){
                fileWriter.close();
            }
            return filteredNamesDmpFile;
        }
    }

    /**
     * Injects the names.dmp.mod prefiltered file into the Nodes table of the NCBI schema.
     *
     * @param connection        {@link Connection} to the database
     * @param nodesFilteredFile {@link File} names.dmp
     * @throws SQLException in case something goes wrong upon database communication
     */
    public static void injectProcessedNamesDmpFile(Connection connection, File nodesFilteredFile) throws SQLException {

        Statement statement = null;

        try {
            statement = connection.createStatement();
            //Switch to a correct schema
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            statement.execute(
                    "LOAD DATA INFILE '"
                            + nodesFilteredFile.toString()
                            + "' REPLACE INTO TABLE "
                            + LookupNames.dbs.NCBI.names.name
                            + " FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n'" +
                            " ("
                            + LookupNames.dbs.NCBI.names.columns.taxid + ", "
                            + LookupNames.dbs.NCBI.names.columns.name
                            + ")");

        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            if(statement!=null){
                statement.close(); 
            }      
        }
    }
}
