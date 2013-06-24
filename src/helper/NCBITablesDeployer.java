package helper;


import helper.gitaxid.GI_TaxIDDeployer;
import helper.names.NamesDeployer;
import helper.nodes.NodesDBDeployer;
import util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A master class that contains functions that deploy all databases form a given set of dmp files.
 * Standard login is "tuit", password "tuit"
 */
public class NCBITablesDeployer {
    /**
     * Database login
     */
    public static final String login="tuit";
    /**
     * Database password
     */
    public static final String password="tuit";

    /**
     * Constructor grants non-instantiability
     */
    private NCBITablesDeployer() {
        throw new AssertionError();
    }

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
    public static void deployNCBIDatabasesFromFiles(final Connection connection, final File gi_taxidDmpFile, final File namesDmpFile, final File nodesDmpFile) throws IOException, SQLException {

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
        NodesDBDeployer.deployRanksValidataionTable(connection);
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
    public static void fastDeployNCBIDatabasesFromFiles(final Connection connection, final File gi_taxidDmpFile, final File namesDmpFile, final File nodesDmpFile) throws IOException, SQLException {

        //Deploy the names table
        System.out.println("Deploying Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(namesDmpFile));
        System.out.println("Names Database deployed");
        //Deploy the gi_taxid table
        System.out.println("Deploying GI_TaxID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, gi_taxidDmpFile));
        System.out.println("GI_TaxID Database deployed");
        //Read and create a validation table for the nodes.dmp
        System.out.println("Preparing Rank-validation table..");
        NodesDBDeployer.deployRanksValidataionTable(connection);
        System.out.println("Rank-validation table deployed");
        //Deploy the nodes table
        System.out.println("Deploying Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, nodesDmpFile));
        System.out.println("Nodes Database deployed");
        System.out.println("NCBI database tables are ready.");

    }

    /**
     * Fully deploys the NCBI taxonomic database directly from the NCBI FTP server
     * @param connection  {@link Connection} to the database
     * @param tmpDir a {@link File} directory that the temporary update files will be downloaded to
     * @throws IOException upon file read/write errors
     * @throws SQLException in case a database communication error occurs
     */
    public static void fastDeployNCBIDatabasesFromNCBI(final Connection connection, final File tmpDir)throws IOException, SQLException{
        //Downloading files
        System.out.println("Downloading files..");
        System.out.println("Downloading " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_tar_gz=SystemUtil.downloadFileFromNCBIFTP(tmpDir, new File(SystemUtil.NCBI_TAXONOMY),new File(SystemUtil.TAXDUMP_ARCH));
        System.out.println("Downloading "+SystemUtil.GI_TAXID_DMP_ARCH+" updates..");
        File gi_taxid_dmp= SystemUtil.downloadFileFromNCBIFTP(tmpDir, new File(SystemUtil.NCBI_TAXONOMY),new File(SystemUtil.GI_TAXID_DMP_ARCH));
        //Extracting files
        System.out.println("Extracting "+SystemUtil.TAXDUMP_ARCH);
        File taxdump_dir=SystemUtil.unArchiveTarGZFile(taxdump_tar_gz,tmpDir);
        System.out.println("Extracting "+SystemUtil.GI_TAXID_DMP_ARCH);
        File gi_taxid_deploy_dir=SystemUtil.unArchiveGZFile(gi_taxid_dmp,tmpDir);
        //Deploying the database
        System.out.println("Deploying Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(new File(taxdump_dir, SystemUtil.NAMES_FILE)));
        System.out.println("Deploying GI_TAXID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, new File(gi_taxid_deploy_dir, SystemUtil.GI_TAXID_UPD_FILE)));
        System.out.println("Deploying Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, new File(taxdump_dir,SystemUtil.NODES_FILE)));
        //Reporting
        System.out.println("Database deployed successfully..");

    }
    /**
     * Fully updates the NCBI taxonomic database directly from the NCBI FTP server
     * @param connection  {@link Connection} to the database
     * @param tmpDir a {@link File} directory that the temporary update files will be downloaded to
     * @throws IOException upon file read/write errors
     * @throws SQLException in case a database communication error occurs
     */
    public static void updateDatabasesFromNCBI(final Connection connection, final File tmpDir) throws IOException, SQLException {
        //Downloading files
        System.out.println("Downloading updates..");
        System.out.println("Downloading "+SystemUtil.TAXDUMP_ARCH);
        File taxdump_tar_gz=SystemUtil.downloadFileFromNCBIFTP(tmpDir, new File(SystemUtil.NCBI_TAXONOMY),new File(SystemUtil.TAXDUMP_ARCH));
        System.out.println("Downloading "+SystemUtil.GI_TAXID_UPD_FILE_ARCH+" updates..");
        File gi_taxid_update= SystemUtil.downloadFileFromNCBIFTP(tmpDir, new File(SystemUtil.NCBI_TAXONOMY),new File(SystemUtil.GI_TAXID_UPD_FILE_ARCH));
        //Extracting files
        System.out.println("Extracting "+SystemUtil.TAXDUMP_ARCH);
        File taxdump_dir=SystemUtil.unArchiveTarGZFile(taxdump_tar_gz,tmpDir);
        System.out.println("Extracting "+SystemUtil.GI_TAXID_UPD_FILE_ARCH);
        File gi_taxid_update_dir=SystemUtil.unArchiveGZFile(gi_taxid_update,tmpDir);
        //Updating the database
        System.out.println("Updating Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(new File(taxdump_dir,SystemUtil.NAMES_FILE)));
        System.out.println("Updating GI_TAXID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, new File(gi_taxid_update_dir,SystemUtil.GI_TAXID_UPD_FILE)));
        System.out.println("Updating Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, new File(taxdump_dir,SystemUtil.NODES_FILE)));
        //Reporting
        System.out.println("Database update completed successfully..");
    }

}
