package helper;

import db.mysqlwb.tables.LookupNames;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

/**
 * Contains utility methods for nodes database deployment from names.dmp file from NCBI
 */
public class NamesDeployer {
    /**
     * A size for batch inserts
     */
    private static int batchSize=10000;

    /**
     * Constructor grants non-instantiability
     */
    private NamesDeployer() {
        throw new AssertionError();
    }
    //TODO: document as soon as working
    /**
     *
     * @param connection
     * @param namesFile
     * @throws SQLException
     * @throws IOException
     */
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
            statement.close();
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

                String[] split = line.split("\t|\t");
                if (split[6].equals("scientific name")) {
                    preparedStatement.setInt(1, Integer.valueOf(split[0]));
                    preparedStatement.setString(2, split[2]);
                    preparedStatement.addBatch();
                    counter++;
                }
                //Execute batch every time the batch buffer gets full
                if (counter == NamesDeployer.batchSize) {
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
            bufferedReader.close();
            preparedStatement.close();
        }
    }
}
