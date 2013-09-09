package helper.names;

import db.tables.LookupNames;
import logger.Log;

import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Contains utility methods for nodes database deployment from names.dmp file from NCBI
 */
public class NamesDeployer {

    /**
     * Constructor grants non-instatiability
     */
    private NamesDeployer() {
        throw new AssertionError();
    }

    /**
     * Deploys the Names database table. Depends of the existence of the NCBI schema and an empty Names table existence.
     * <b>Deprecated due to low efficiency, See injectProcessedNamesDmpFile(Connection connection, File nodesFilteredFile)
     * and filterNodesDmpFile(File namesDmpFile) as a faster way of implementation. </b>
     *
     * @param connection {@link Connection} to the database
     * @param namesFile  {@link File} that points to the file that contains the names part of the data
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
        } finally {
            if (statement != null) {
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
                if (split[6].trim().equals("scientific name")) {
                    preparedStatement.setInt(1, Integer.valueOf(split[0].trim()));
                    preparedStatement.setString(2, split[2].trim());
                    preparedStatement.addBatch();
                    counter++;
                }
                //Execute batch every time the batch buffer gets full
                /*
      A size for batch inserts
     */
                int BATCH_SIZE = 10000;
                if (counter == BATCH_SIZE) {
                    preparedStatement.executeBatch();
                    Log.getInstance().log(Level.INFO, "Another batch inserted into names, the last gi was: " + split[0]);
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
     * Re-parses the nodes.dmp file. Extracts the taxid and the "scientific name" marked fields
     *
     * @param namesDmpFile {@link File} names.dmp
     * @return a new {@link File} that points to the newly filtered file
     * @throws IOException
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
                String[] split = line.split("\t");//The dmp file has a broken format, can't use "\t\\|\t"
                /*
      The "scientific name" that indicates that the taxid points to a valid name for a taxonomic group
     */
                String SCIENTIFIC_NAME = "scientific name";
                if (split.length >= 7) {
                    if (split[6].trim().equals(SCIENTIFIC_NAME)) {
                        fileWriter.write(split[0].trim() + '\t' + split[2].trim() + '\n');
                    }
                }
            }
            fileWriter.flush();

        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (fileWriter != null) {
                fileWriter.close();
            }
        }
        return filteredNamesDmpFile;
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
            statement.execute("SET foreign_key_checks = 0;");
            statement.execute(
                    "LOAD DATA INFILE '"
                            + nodesFilteredFile.getPath().replaceAll("\\\\","/")
                            + "' REPLACE INTO TABLE "
                            + LookupNames.dbs.NCBI.names.name
                            + " FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n'" +
                            " ("
                            + LookupNames.dbs.NCBI.names.columns.taxid + ", "
                            + LookupNames.dbs.NCBI.names.columns.name
                            + ")");
            statement.execute("SET foreign_key_checks = 1;");
        } finally {
            if (statement != null) {
                statement.close();
            }
        }
    }
}
