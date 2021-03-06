package helper.nodes;

import db.tables.LookupNames;
import logger.Log;
import taxonomy.Ranks;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 * Contains utility methods for nodes database deployment from nodes.dmp file from NCBI
 */
public class NodesDBDeployer {

    /**
     * Constructor grants non-instantiability
     */
    private NodesDBDeployer() {
        throw new AssertionError();
    }

    /**
     * Creates a set of ranks from the nodes.dmp file. Used to create a validation table,
     * (with the help of {@code NodesDBDeployer assignIDsToRanks(Set ranks)} otherwise the NCBI database contains too much redundancy.
     *
     * @param nodesDmpFile an NCBI nodes.dmp {@link File} that contains the full set of ranks in a redundant format
     * @return a {@link Set} of all possible ranks within the file
     * @throws IOException in case smth goes wrong during file read and parsing
     */
    @Deprecated
    public static Set<String> calculateASetOfRanksFromFile(final File nodesDmpFile) throws IOException {
        //Prepare a new set to store the ranks
        Set<String> ranks = new HashSet<String>();
        //Open the file
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));) {
            //Read line by line, splitting the line by the separator
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                //Put every rank value into the set
                ranks.add(line.split("\t|\t")[4]);
            }
        }
        return ranks;
    }

    /**
     * <b>Deprecated due to that the database needs strongly a strongly ordered set of ranks.</b>
     * Generates a {@link Map} that helps create a validation table for the nodes NCBI taxonomic database.
     * <b>Deprecated due to redundancy</b>
     *
     * @param ranks {@link Set} of ranks
     * @return a new {@link  Map} where each ranks has been assigned a unique ID
     */
    @Deprecated
    public static Map<String, Integer> assignIDsToRanks(final Set<String> ranks) {
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
     * @throws SQLException in case an error occurs during database communication
     */
    public static void deployRanksValidationTable(final Connection connection) throws SQLException {
        //Switch to a correct table
        try (Statement statement = connection.createStatement();) {
            statement.execute("use " + LookupNames.dbs.NCBI.name);
        }

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "insert into " + LookupNames.dbs.NCBI.ranks.name
                        + " (" + LookupNames.dbs.NCBI.ranks.columns.rank.name() + ")" + " values(?) "
        );) {
            //Prepare a statement

            for (Ranks r : Ranks.values()) {
                //Populate the batch
                preparedStatement.setString(1, r.getName());
                preparedStatement.addBatch();
            }
            //Execute the batch
            preparedStatement.executeBatch();
        }
    }

    /**
     * Collects a lookup map that will further allow easy lookup during the nodes.dmp insert
     *
     * @param connection {@link Connection} to the database
     * @return a {@link Map} lookup from the database id_ranks, rank validation table columns
     * @throws SQLException in case an error occurs during database communication
     */
    public static Map<String, Integer> collectRanksValidationLookup(final Connection connection) throws SQLException {
        //Switch to a correct table
        Map<String, Integer> ranks_ids = new HashMap<String, Integer>();
        try (Statement statement = connection.createStatement();) {
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            //Create a statement and execute
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            final ResultSet resultSet = statement.executeQuery(
                    "select * from " + LookupNames.dbs.NCBI.ranks.name
            );
            while (resultSet.next()) {
                ranks_ids.put(resultSet.getString(2), resultSet.getInt(1));
            }
        }
        return ranks_ids;
    }

    /**
     * Deploys the Nodes database table. Depends of the existence of the NCBI schema and an empty Nodes table existence.
     * <b>Deprecated due to low efficiency. See..</b>
     *
     * @param connection   {@link Connection} to the database
     * @param nodesDmpFile {@link File} nodes.dmp
     * @throws SQLException in case something goes wrong upon database communication
     * @throws IOException  in case something goes wrong during file read
     */
    @Deprecated
    public static void deployNodesDatabase(final Connection connection, final File nodesDmpFile) throws SQLException, IOException {
        //Switch to a correct table
        try (Statement statement = connection.createStatement();) {

            statement.execute("use " + LookupNames.dbs.NCBI.name);
        }
        //Prepare a validation lookup
        Map<String, Integer> ranks_ids = NodesDBDeployer.collectRanksValidationLookup(connection);
        //Read the input file line by line
        try (PreparedStatement preparedStatement = connection.prepareStatement("insert into " + LookupNames.dbs.NCBI.nodes.name
                + " ("
                + LookupNames.dbs.NCBI.nodes.columns.taxid
                + LookupNames.dbs.NCBI.nodes.columns.parent_taxid
                + LookupNames.dbs.NCBI.nodes.columns.id_ranks
                + ")" + " values(?,?,?) ");
             BufferedReader bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
        ) {
            String line;
            int counter = 0;
            while ((line = bufferedReader.readLine()) != null) {
                String[] split = line.split("\t\\|\t");
                preparedStatement.setInt(1, Integer.parseInt(split[0]));
                preparedStatement.setInt(2, Integer.parseInt(split[1]));
                preparedStatement.setInt(3, ranks_ids.get(split[2]));
                preparedStatement.addBatch();
                counter++;
                /*
      A size for batch inserts
     */
                int BATCH_SIZE = 10000;
                if (counter == BATCH_SIZE) {
                    preparedStatement.executeBatch();
                    counter = 0;
                }
            }
            //Flush the batch
            preparedStatement.executeBatch();
        }
    }

    /**
     * Re-parses the nodes.dmp file. Extracts the taxid, parent_taxid fields as well as the rank, but upon extraction - goes through the validation table and assigns the id
     * instead of the redundant usage of character representation.
     *
     * @param connection   {@link Connection} to the database
     * @param nodesDmpFile {@link File} nodes.dmp
     * @return a new {@link File} that points to the newly filtered file
     * @throws java.io.IOException in case a file read/write fails
     * @throws java.sql.SQLException in case of a db communication error occurs
     */
    public static File filterNodesDmpFile(final Connection connection, final File nodesDmpFile) throws IOException, SQLException {
        //Read the input file line by line
        final File filteredNodesDmpFile = new File(nodesDmpFile.getAbsoluteFile().toString() + ".mod");
        try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
                FileWriter fileWriter = new FileWriter(filteredNodesDmpFile);
        ) {
            //Prepare a validation lookup
            final Map<String, Integer> ranks_ids = NodesDBDeployer.collectRanksValidationLookup(connection);
            String line;
            String empty = "";
            while ((line = bufferedReader.readLine()) != null) {
                String[] splitter = line.split("\t\\|\t");
                if (splitter.length > 3 && !splitter[0].equals(empty) && !splitter[1].equals(empty)) {

                    if (ranks_ids.containsKey(splitter[2])) {
                        fileWriter.write(
                                splitter[0] + '\t'
                                        + splitter[1] + '\t'
                                        + ranks_ids.get(splitter[2])
                                        + '\n'
                        );
                        fileWriter.flush();
                    }
                }
            }
        }
        return filteredNodesDmpFile;
    }

    /**
     * Accepts a pointer to the "nodes.dmp" file from the NCBI, which contains the taxonomic nodes info.
     * See <a href="ftp://ftp-trace.ncbi.nlm.nih.gov/pub/taxonomy/">taxdump</a>.
     * @param nodesDmpFile {@link java.io.File} that points to the input file "nodes.dmp"
     * @return a {@link java.io.File} pointer to the newly created .mod file
     * @throws IOException a file read/write error occurs
     * @throws SQLException in case a RDBMS communication error occurs
     */
    public static File filterNodesDmpFileRam(final File nodesDmpFile) throws IOException, SQLException {
        //Read the input file line by line
        final File filteredNodesDmpFile = new File(nodesDmpFile.getAbsoluteFile().toString() + ".mod");
        try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(nodesDmpFile));
                FileWriter fileWriter = new FileWriter(filteredNodesDmpFile);
        ){
            //Prepare a validation lookup
            String line;
            String empty = "";
            int count = 0;
            while ((line = bufferedReader.readLine()) != null) {

                final String[] splitter = line.split("\t\\|\t");
                if (splitter.length != 13) {
                    Log.getInstance().log(Level.WARNING, "Row number " + count + " in " + nodesDmpFile.getName() + " is inconsistent:");
                    Log.getInstance().log(Level.WARNING, nodesDmpFile.getName() + count + ": " + line);
                    Log.getInstance().log(Level.WARNING, "Skipping inconsistent line.");
                    continue;
                }
                if (!splitter[0].equals(empty) && !splitter[0].startsWith("|") && !splitter[1].equals(empty)) {
                    fileWriter.write(
                            splitter[0] + '\t'
                                    + splitter[1] + '\t'
                                    + Ranks.convertValue(splitter[2]).ordinal()
                                    + '\n'
                    );
                    count++;
                    fileWriter.flush();
                }
            }
        }
        return filteredNodesDmpFile;
    }

    /**
     * Injects the nodes.dmp.mod prefiltered file into the Nodes table of the NCBI schema.
     *
     * @param connection        {@link Connection} to the database
     * @param nodesFilteredFile {@link File} nodes.dmp
     * @throws SQLException in case something goes wrong upon database communication
     */
    public static void injectProcessedNodesDmpFile(final Connection connection, final File nodesFilteredFile) throws SQLException {
        try (Statement statement = connection.createStatement();){
            //Switch to a correct schema
            statement.execute("use " + LookupNames.dbs.NCBI.name);
            statement.execute("SET foreign_key_checks = 0;");
            final boolean execute = statement.execute(
                    "LOAD DATA INFILE '"
                            + nodesFilteredFile.getPath().replaceAll("\\\\", "/")
                            + "' REPLACE INTO TABLE "
                            + LookupNames.dbs.NCBI.nodes.name
                            + " FIELDS TERMINATED BY '\t' LINES TERMINATED BY '\n'" +
                            " ("
                            + LookupNames.dbs.NCBI.nodes.columns.taxid + ", "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid + ", "
                            + LookupNames.dbs.NCBI.nodes.columns.id_ranks
                            + ")"
            );
            statement.execute("SET foreign_key_checks = 1;");
        }
    }
}
