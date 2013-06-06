package helper;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A master class that contains functions that deploy all databases form a given set of dmp files
 */
public class NCBITablesDeployer {

    /**
     * Constructor grants non-instantiability
     */
    private NCBITablesDeployer() {
        throw new AssertionError();
    }

    //TODO: implement the sql script that deploys the full schema from MySQL Workbench

    /**
     * Deploys the full set of files for the NCBI taxonomic database
     * <b>Deprecated due to the use of deprecated inefficient database deployment methods.</b>
     *
     * @param connection      {@link Connection} to the database
     * @param gi_taxidDmpFile {@link File} gi_taxid.dmp
     * @param namesDmpFile    {@link File} names.dmp
     * @param nodesDmpFile    {@link File} nodes.dmp
     * @throws IOException  in case something goes wrong during file read
     * @throws SQLException in case something goes wrong upon database communication
     */
    @Deprecated
    public static void deployNCBIDatabasesFromFiles(Connection connection, File gi_taxidDmpFile, File namesDmpFile, File nodesDmpFile) throws IOException, SQLException {

        //Deploy the names table
        System.out.println("Deploying Names Database..");
        NamesDeployer.deployNamesTable(connection, namesDmpFile);
        System.out.println("Names Database deployed");
        //Deploy the gi_taxid table
        System.out.println("Deploying GI_TaxID Database..");
        GI_TaxIDDeployer.deployGI_TaxIDTable(connection, gi_taxidDmpFile);
        System.out.println("GI_TaxID Database deployed");
        //Read and create a validation table for the nodes.dmp
        System.out.println("Preparing Rank-validation table..");
        NodesDBDeployer.deployRanksValidataionTable(connection, NodesDBDeployer.calculateASetOfRanksFromFile(nodesDmpFile));
        System.out.println("Rank-validation table deployed");
        //Deploy the nodes table
        System.out.println("Deploying Nodes Database..");
        NodesDBDeployer.deployNodesDatabase(connection, nodesDmpFile);
        System.out.println("Nodes Database deployed");
        System.out.println("NCBI database tables are ready.");
    }

    /**
     * Deploys the full set of files for the NCBI taxonomic database (faster implementation)
     *
     * @param connection      {@link Connection} to the database
     * @param gi_taxidDmpFile {@link File} gi_taxid.dmp
     * @param namesDmpFile    {@link File} names.dmp
     * @param nodesDmpFile    {@link File} nodes.dmp
     * @throws IOException  in case something goes wrong during file read
     * @throws SQLException in case something goes wrong upon database communication
     */
    public static void fastDeployNCBIDatabasesFromFiles(Connection connection, File gi_taxidDmpFile, File namesDmpFile, File nodesDmpFile) throws IOException, SQLException {

        //Deploy the names table
        System.out.println("Deploying Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNodesDmpFile(namesDmpFile));
        System.out.println("Names Database deployed");
        //Deploy the gi_taxid table
        System.out.println("Deploying GI_TaxID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, gi_taxidDmpFile));
        System.out.println("GI_TaxID Database deployed");
        //Read and create a validation table for the nodes.dmp
        System.out.println("Preparing Rank-validation table..");
        NodesDBDeployer.deployRanksValidataionTable(connection, NodesDBDeployer.calculateASetOfRanksFromFile(nodesDmpFile));
        System.out.println("Rank-validation table deployed");
        //Deploy the nodes table
        System.out.println("Deploying Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, nodesDmpFile));
        System.out.println("Nodes Database deployed");
        System.out.println("NCBI database tables are ready.");

    }
}
