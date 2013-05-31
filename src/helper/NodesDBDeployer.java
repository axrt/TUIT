package helper;

import db.mysqlwb.tables.LookupNames;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Contains utility methods for nodes database deployment from nodes.dmp file from NCBI
 */
public class NodesDBDeployer {

    /**
     * Creates a set of ranks from the nodes.dmp file. Used to create a validation table,
     * (with the help of {@code NodesDBDeployerassingIDsToRanks(Set<String> ranks)} otherwise the NCBI database contains too much redundancy.
     *
     * @param dmpFile an NCBI nodes.dmp {@link File} that contains the full set of ranks in a redundant format
     * @return a {@link Set<String>} of all possible ranks within the file
     * @throws IOException in case smth goes wrong during file read and parsing
     */
    public static Set<String> calculateASetOfRanksFromFile(File dmpFile) throws IOException {
        //Prepare a new set to store the ranks
        Set<String> ranks = new HashSet<String>();
        //Open the file
        BufferedReader bufferedReader = new BufferedReader(new FileReader(dmpFile));
        //Read line by line, splitting the line by the separator
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            //Put every rank value into the set
            ranks.add(line.split("\t|\t")[4]);
        }
        //Close everything and return the set
        bufferedReader.close();
        return ranks;
    }

    /**
     * Generates a {@link Map<String, Integer>} that helps create a validation table for the nodes NCBI taxonomic database.
     *
     * @param ranks {@link Set<String>} of ranks
     * @return a new {@link  Map<String, Integer>} where each ranks has been assigned a unique ID
     */
    public static Map<String, Integer> assingIDsToRanks(Set<String> ranks) {
        //Create a new HashMap<String, Integer>()
        Map<String, Integer> ranks_ids = new HashMap<String, Integer>();
        //Populate it from the set of ranks and assign an incremented id on the fly
        int i = 0;
        for (String s : ranks) {
            ranks_ids.put(s, ++i);
        }
        //Return the HashMap
        return ranks_ids;
    }

    /**
     * Deploys a validation table for the rank values
     *
     * @param connection {@link Connection} to the database
     * @param ranks      a {@link Set<String>} of rank names
     * @throws SQLException in case an error occurs during database communication
     */
    public static void deployRanksValidataionTable(Connection connection, Set<String> ranks) throws SQLException {
        //Switch to a nessessary table
        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.execute("use " + LookupNames.dbs.NCBI.name);
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            statement.close();
        }

        PreparedStatement preparedStatement = null;
        try {
            //Prepare a statement
            preparedStatement = connection.prepareStatement(
                    "insert into " + LookupNames.dbs.NCBI.ranks.name
                            + " (" + LookupNames.dbs.NCBI.ranks.columns.rank.name() + ")" + " values(?) ");
            for (String s : ranks) {
                //Populate the batch
                preparedStatement.setString(1, s);
                preparedStatement.addBatch();
            }
            //Execute the batch
            preparedStatement.executeBatch();
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            //Close and cleanup
            preparedStatement.close();
        }
    }

    /**
     *
     * @param connection
     * @return
     * @throws SQLException
     */
    public static Map<String, Integer> collectRanksValidationLookup(Connection connection) throws SQLException {
        //Switch to a nessessary table
        Statement statement = null;
        Map<String, Integer> ranks_ids = new HashMap<String, Integer>();
        try {
            statement = connection.createStatement();
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            //Create a statement and execute
            ResultSet resultSet = statement.executeQuery(
                    "select * from " + LookupNames.dbs.NCBI.ranks.name
            );

            while (resultSet.next()) {
                ranks_ids.put(resultSet.getString(2), resultSet.getInt(1));
            }

        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            statement.close();
            return ranks_ids;
        }
    }
}
