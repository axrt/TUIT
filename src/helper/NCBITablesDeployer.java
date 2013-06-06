package helper;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import util.SystemUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * A master class that contains functions that deploy all databases form a given set of dmp files
 */
public class NCBITablesDeployer {
    /**
     * NCBI FTP server address
     */
    private static String NCBI_FTP = "ftp.ncbi.nlm.nih.gov";
    /**
     * Anonymous ftp login/password
     */
    private static String ANONYMOUS = "anonymous";
    /**
     * NCBI FTP server taxonomy subfolder
     */
    private static String NCBI_TAXONOMY = "/pub/taxonomy/";
    /**
     * NCBI FTP server address for taxonomy files
     */
    private static String NCBI_TAXONOMY_FTP = NCBITablesDeployer.NCBI_FTP + NCBITablesDeployer.NCBI_TAXONOMY;
    /**
     * Dump file .dmp extension
     */
    private static String DMP_SUF = ".dmp";
    /**
     * Archive format suffix
     */
    private static String ARCH_SUF = ".gz";
    /**
     * Perfix for the gi_taxid database dump
     */
    private static String GI_TAXID = "gi_taxid";
    /**
     * GI_TAXID dump file archive name
     */
    public static String GI_TAXID_ARCH = NCBITablesDeployer.GI_TAXID + "_nucl_diff" + NCBITablesDeployer.DMP_SUF + NCBITablesDeployer.ARCH_SUF;
    /**
     * GI_TAXID dump file name
     */
    private static String GI_TAXID_FILE = "gi_taxid" + NCBITablesDeployer.DMP_SUF;
    /**
     * Perfix for the nodes database dump
     */
    private static String NODES = "nodes";
    /**
     * Nodes dump file name
     */
    private static String NODES_FILE = "nodes" + NCBITablesDeployer.DMP_SUF;
    /**
     * Perfix for the nodes database dump
     */
    private static String NAMES = "names";
    /**
     * Nodes dump file name
     */
    private static String NAMES_FILE = "names" + NCBITablesDeployer.DMP_SUF;

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

    private static File downloadGI_TAXIDUpdate() {


        return null;
    }

    //TODO: test and document as soon as works
    /**
     *
     * @param tmpDownloadDir
     * @param fileName
     * @return
     * @throws IOException
     */
    public static File downloadFileFromNCBIFTP(File tmpDownloadDir, String fileName) throws IOException {

        //Prepare an FTP client
        FTPClient ftpClient = new FTPClient();
        FileOutputStream fileOutputStream = null;
        File outputFile = null;

        try {
            //Connect to the server
            ftpClient.connect(NCBITablesDeployer.NCBI_FTP);
            //Login
            ftpClient.login(NCBITablesDeployer.ANONYMOUS, NCBITablesDeployer.ANONYMOUS);
            //Set binary mode
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            //Change the directory to the one that contains taxonomic infomation
            ftpClient.cwd(NCBITablesDeployer.NCBI_TAXONOMY);
            //Prepare the outputstream to save the file
            fileOutputStream = new FileOutputStream(outputFile = new File(tmpDownloadDir.getAbsoluteFile() + SystemUtil.SysFS + fileName));
            //Download the required file
            ftpClient.retrieveFile(fileName, fileOutputStream);

        } catch (IOException ioe) {
            throw ioe;
        } finally {
            if(fileOutputStream!=null){
                fileOutputStream.close();
            }
            ftpClient.disconnect();
        }
        return outputFile;
    }
}
