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
     * A size for batch inserts
     */
    private static int batchSize = 10000;

    /**
     * Constructor grants non-instantiability
     */
    private NodesDBDeployer() {
        throw new AssertionError();
    }

    /**
     * Creates a set of ranks from the nodes.dmp file. Used to create a validation table,
     * (with the help of {@code NodesDBDeployerassingIDsToRanks(Set<String> ranks)} otherwise the NCBI database contains too much redundancy.
     *
     * @param nodesDmpFile an NCBI nodes.dmp {@link File} that contains the full set of ranks in a redundant format
     * @return a {@link Set<String>} of all possible ranks within the file
     * @throws IOException in case smth goes wrong during file read and parsing
     */
    public static Set<String> calculateASetOfRanksFromFile(File nodesDmpFile) throws IOException {
        //Prepare a new set to store the ranks
        Set<String> ranks = new HashSet<String>();
        //Open the file
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
            //Read line by line, splitting the line by the separator
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                //Put every rank value into the set
                ranks.add(line.split("\t|\t")[4]);
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            //Close everything and return the set
            bufferedReader.close();
            return ranks;
        }

    }

    /**
     * Generates a {@link Map<String, Integer>} that helps create a validation table for the nodes NCBI taxonomic database.
     * <b>Deprecated due to redundancy</b>
     *
     * @param ranks {@link Set<String>} of ranks
     * @return a new {@link  Map<String, Integer>} where each ranks has been assigned a unique ID
     */
    @Deprecated
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
     * Deploys a validation table for the rank values. Depends on the preexistence of the NCBI schema and an
     * empty Ranks validation table.
     *
     * @param connection {@link Connection} to the database
     * @param ranks      a {@link Set<String>} of rank names
     * @throws SQLException in case an error occurs during database communication
     */
    public static void deployRanksValidataionTable(Connection connection, Set<String> ranks) throws SQLException {
        //Switch to a correct table
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
     * Collects a lookup map that will further allow easy lookup during the nodes.dmp insert
     *
     * @param connection {@link Connection} to the database
     * @return a {@link Map<String, Integer>} lookup from the database id_ranks, rank validation table columns
     * @throws SQLException in case an error occurs during database communication
     */
    public static Map<String, Integer> collectRanksValidationLookup(Connection connection) throws SQLException {
        //Switch to a correct table
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

    /**
     * Deploys the Nodes database table. Depends of the existence of the NCBI schema and an empty Nodes table existence.
     * <b>Deprecated due to low efficiency. See..</b>
     *
     * @param connection   {@link Connection} to the database
     * @param nodesDmpFile {@link File} nodes.dmp
     * @throws SQLException SQLException in case something goes wrong upon database communication
     * @throws IOException  IOException in case something goes wrong during file read
     */
    @Deprecated
    public static void deployNodesDatabase(Connection connection, File nodesDmpFile) throws SQLException, IOException {

        //Switch to a correct table
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

        //Prepare a validation lookup
        Map<String, Integer> ranks_ids = NodesDBDeployer.collectRanksValidationLookup(connection);

        //Read the input file line by line
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
            String line;
            int counter = 0;
            while ((line = bufferedReader.readLine()) != null) {

                String[] split = line.split("\t\\|\t");

                preparedStatement = connection.prepareStatement("insert into " + LookupNames.dbs.NCBI.nodes.name
                        + " ("
                        + LookupNames.dbs.NCBI.nodes.columns.taxid
                        + LookupNames.dbs.NCBI.nodes.columns.parent_taxid
                        + LookupNames.dbs.NCBI.nodes.columns.id_ranks
                        + ")" + " values(?,?,?) ");
                preparedStatement.setInt(1, Integer.parseInt(split[0]));
                preparedStatement.setInt(2, Integer.parseInt(split[1]));
                preparedStatement.setInt(3, ranks_ids.get(split[2]));
                preparedStatement.addBatch();
                counter++;
                if (counter == NodesDBDeployer.batchSize) {
                    preparedStatement.executeBatch();
                    counter = 0;
                }

            }
            //Flush the batch
            preparedStatement.executeBatch();

        } catch (IOException ioe) {
            throw ioe;
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            //Close and cleanup
            bufferedReader.close();
            preparedStatement.close();
        }
    }

    /**
     * Re-parses the nodes.dmp file. Extracts the taxid, parent_taxid fields as well as the rank, but upon extraction - goes throught the validation table and assigns the id
     * instead of the redundant usage of character representation.
     *
     * @param connection   {@link Connection} to the database
     * @param nodesDmpFile {@link File} nodes.dmp
     * @return a new {@link File} that points to the newly filtered file
     */
    public static File filterNodesDmpFile(Connection connection, File nodesDmpFile) throws IOException, SQLException {
        //Read the input file line by line
        BufferedReader bufferedReader = null;
        FileWriter fileWriter = null;
        File filteredNodesDmpFile = null;
        try {
            //Prepare a validation lookup
            Map<String, Integer> ranks_ids = NodesDBDeployer.collectRanksValidationLookup(connection);
            bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
            filteredNodesDmpFile = new File(nodesDmpFile.getAbsoluteFile().toString() + ".mod");
            fileWriter = new FileWriter(filteredNodesDmpFile);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitter = line.split("\t\\|\t");
                fileWriter.write(
                        splitter[0] + '\t'
                                + splitter[1] + '\t'
                                + ranks_ids.get(splitter[2])
                                + '\n'
                );
            }
            fileWriter.flush();

        } catch (FileNotFoundException fnfe) {
            throw fnfe;
        } catch (IOException ioe) {
            throw ioe;
        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            bufferedReader.close();
            fileWriter.close();
        }
        return filteredNodesDmpFile;
    }

     /**
      * Injects the nodes.dmp.mod prefiltered file into the Nodes table of the NCBI schema.
     * @param connection  {@link Connection} to the database
     * @param nodesFilteredFile {@link File} nodes.dmp
     * @throws SQLException in case something goes wrong upon database communication
     */
    public static void injectProcessedNodesDmpFile(Connection connection, File nodesFilteredFile) throws SQLException {

        Statement statement = null;

        try {
            statement = connection.createStatement();
            //Switch to a correct schema
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            statement.execute(
                    "LOAD DATA INFILE '"
                            + nodesFilteredFile.toString()
                            + "' REPLACE INTO TABLE "
                            + LookupNames.dbs.NCBI.nodes.name
                            + " FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n'" +
                            " ("
                            + LookupNames.dbs.NCBI.nodes.columns.taxid +", "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid +", "
                            + LookupNames.dbs.NCBI.nodes.columns.id_ranks
                            + ")");

        } catch (SQLException sqle) {
            throw sqle;
        } finally {
            statement.close();
        }
    }
}
