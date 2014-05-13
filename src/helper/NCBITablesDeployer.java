package helper;


import db.ram.RamDb;
import helper.gitaxid.GI_TaxIDDeployer;
import helper.names.NamesDeployer;
import helper.nodes.NodesDBDeployer;
import logger.Log;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import util.SystemUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
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
 * A master class that contains functions that deploy all databases form a given set of dmp files.
 * Standard login is "tuit", password "tuit"
 */
public class NCBITablesDeployer {
    /**
     * Database login
     */
    public static final String login = "tuit";
    /**
     * Database password
     */
    public static final String password = "tuit";

    /**
     * Constructor grants non-instantiability
     */
    private NCBITablesDeployer() {
        throw new AssertionError();
    }

    /**
     * Deploys the full set of files for the NCBI taxonomic database (faster implementation)
     *
     * @param connection      {@link Connection} to the database
     * @param gi_taxidDmpFile {@link File} gi_taxid.dmp
     * @param namesDmpFile    {@link File} names.dmp
     * @param nodesDmpFile    {@link File} nodes.dmp
     * @throws SQLException in case something goes wrong upon database communication
     * @throws IOException  in case something goes wrong during file read
     */
    public static void fastDeployNCBIDatabasesFromFiles(final Connection connection, final File gi_taxidDmpFile, final File namesDmpFile, final File nodesDmpFile) throws IOException, SQLException {

        //Deploy the names table
        Log.getInstance().log(Level.INFO, "Deploying Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(namesDmpFile));
        Log.getInstance().log(Level.INFO, "Names Database deployed");
        //Deploy the gi_taxid table
        Log.getInstance().log(Level.INFO, "Deploying GI_TaxID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, gi_taxidDmpFile));
        Log.getInstance().log(Level.INFO, "GI_TaxID Database deployed");
        //Read and create a validation table for the nodes.dmp
        Log.getInstance().log(Level.INFO, "Preparing Rank-validation table..");
        NodesDBDeployer.deployRanksValidationTable(connection);
        Log.getInstance().log(Level.INFO, "Rank-validation table deployed");
        //Deploy the nodes table
        Log.getInstance().log(Level.INFO, "Deploying Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, nodesDmpFile));
        Log.getInstance().log(Level.INFO, "Nodes Database deployed");
        Log.getInstance().log(Level.INFO, "NCBI database tables are ready.");

    }

    public static void fastDeployNCBIDatabasesFromFiles(final Connection connection, final TaxonomyFiles taxFiles) throws IOException, SQLException {
        //TODO implement
        throw new NotImplementedException();
    }

    /**
     * Fully deploys the NCBI taxonomic database directly from the NCBI FTP server
     *
     * @param connection {@link Connection} to the database
     * @param tmpDir     a {@link File} directory that the temporary update files will be downloaded to
     * @throws SQLException in case a database communication error occurs
     * @throws IOException  upon file read/write errors
     */
    public static void fastDeployNCBIDatabasesFromNCBI(final Connection connection, final File tmpDir) throws IOException, SQLException {
        //Downloading files
        Log.getInstance().log(Level.INFO, "Downloading files..");
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_tar_gz = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.TAXDUMP_ARCH);
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.GI_TAXID_DMP_ARCH + " updates..");
        File gi_taxid_dmp = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.GI_TAXID_DMP_ARCH);
        //Extracting files
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_dir = SystemUtil.unArchiveTarGZFile(taxdump_tar_gz, tmpDir);
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.GI_TAXID_DMP_ARCH);
        File gi_taxid_deploy_dir = SystemUtil.unArchiveGZFile(gi_taxid_dmp, tmpDir);
        //Deploying the database
        Log.getInstance().log(Level.INFO, "Deploying Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(new File(taxdump_dir, SystemUtil.NAMES_FILE)));
        Log.getInstance().log(Level.INFO, "Deploying GI_TAXID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, new File(gi_taxid_deploy_dir, SystemUtil.GI_TAXID_NUCL)));
        Log.getInstance().log(Level.INFO, "Deploying Nodes Database..");
        NodesDBDeployer.deployRanksValidationTable(connection);
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, new File(taxdump_dir, SystemUtil.NODES_FILE)));
        //Reporting
        Log.getInstance().log(Level.INFO, "Database deployed successfully..");

    }

    public static void fastDeployNCBIDatabasesFromNCBI(final Connection connection, final TaxonomyFiles taxFiles) throws IOException, SQLException {
        //TODO implement
        throw new NotImplementedException();
    }

    /**
     * Fully deploys a RAM-bases taxonomic database to a {@link db.ram.RamDb} object from the NCBI taxonomy files
     *
     * @param tmpDir      requires a {@link java.io.File} that points to a temporary directory that is needed to download the files form the
     *                    <a href="ftp://ftp-trace.ncbi.nlm.nih.gov/pub/taxonomy/">NCBI ftp</a>
     * @param ramDbObject {@link java.io.File} that points to a file that the {@link db.ram.RamDb} object can be serialized to
     * @throws Exception in case a connection of a serialization error occurs
     */
    public static void fastDeployNCBIRamDatabaseFromNCBI(final File tmpDir, final File ramDbObject) throws Exception {
        //Downloading files
        Log.getInstance().log(Level.INFO, "Downloading files..");
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_tar_gz = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.TAXDUMP_ARCH);
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.GI_TAXID_DMP_ARCH + " updates..");
        File gi_taxid_dmp = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.GI_TAXID_DMP_ARCH);
        //Extracting files
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_dir = SystemUtil.unArchiveTarGZFile(taxdump_tar_gz, tmpDir);
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.GI_TAXID_DMP_ARCH);
        File gi_taxid_deploy_dir = SystemUtil.unArchiveGZFile(gi_taxid_dmp, tmpDir);
        //Deploying the database
        Log.getInstance().log(Level.INFO, "Deploying Names Database..");
        final File names_dmp = NamesDeployer.filterNamesDmpFile(new File(taxdump_dir, SystemUtil.NAMES_FILE));
        Log.getInstance().log(Level.INFO, "Deploying Nodes Database..");
        final File nodes_dmp = NodesDBDeployer.filterNodesDmpFileRam(new File(taxdump_dir, SystemUtil.NODES_FILE));
        Log.getInstance().log(Level.INFO, "Assembling RAM database object..");
        fastDeployRamDatabaseFromFiles(
                new TaxonomyFiles.TaxonomyFilesBuilder().giTaxidDmp(gi_taxid_dmp.toPath()).namesDmp(names_dmp.toPath()).nodesDmp(nodes_dmp.toPath()).build()
                , ramDbObject);
    }

    public static void fastDeployRamDatabaseFromFiles(final TaxonomyFiles taxFiles, final File ramDbObject) throws Exception {
        //Deploying
        final RamDb ramDb = RamDb.loadSelfFromFilteredNcbiFiles(taxFiles.getGiTaxidDmp(), taxFiles.getNamesDmp(), taxFiles.nodesDmp);
        Log.getInstance().log(Level.INFO, "Serializing the database for future use..");
        RamDb.serialize(ramDb, ramDbObject);
        //Reporting
        Log.getInstance().log(Level.INFO, "Database deployed successfully..");
    }

    /**
     * Fully updates the NCBI taxonomic database directly from the NCBI FTP server
     *
     * @param connection {@link Connection} to the database
     * @param tmpDir     a {@link File} directory that the temporary update files will be downloaded to
     * @throws SQLException in case a database communication error occurs
     * @throws IOException  upon file read/write errors
     */
    public static void updateDatabasesFromNCBI(final Connection connection, final File tmpDir) throws IOException, SQLException {
        //Downloading files
        Log.getInstance().log(Level.INFO, "Downloading updates..");
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_tar_gz = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.TAXDUMP_ARCH);
        Log.getInstance().log(Level.INFO, "Downloading " + SystemUtil.GI_TAXID_UPD_FILE_ARCH + " updates..");
        File gi_taxid_update = SystemUtil.downloadFileFromNCBIFTP(tmpDir, SystemUtil.NCBI_TAXONOMY, SystemUtil.GI_TAXID_UPD_FILE_ARCH);
        //Extracting files
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.TAXDUMP_ARCH);
        File taxdump_dir = SystemUtil.unArchiveTarGZFile(taxdump_tar_gz, tmpDir);
        Log.getInstance().log(Level.INFO, "Extracting " + SystemUtil.GI_TAXID_UPD_FILE_ARCH);
        File gi_taxid_update_dir = SystemUtil.unArchiveGZFile(gi_taxid_update, tmpDir);
        //Updating the database
        Log.getInstance().log(Level.INFO, "Updating Names Database..");
        NamesDeployer.injectProcessedNamesDmpFile(connection, NamesDeployer.filterNamesDmpFile(new File(taxdump_dir, SystemUtil.NAMES_FILE)));
        Log.getInstance().log(Level.INFO, "Updating GI_TAXID Database..");
        GI_TaxIDDeployer.injectProcessedGI_TaxIDDmpFile(connection, GI_TaxIDDeployer.filterGI_TaxIDDmp(connection, new File(gi_taxid_update_dir, SystemUtil.GI_TAXID_UPD_FILE)));
        Log.getInstance().log(Level.INFO, "Updating Nodes Database..");
        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection, new File(taxdump_dir, SystemUtil.NODES_FILE)));
        //Reporting
        Log.getInstance().log(Level.INFO, "Database update completed successfully..");
    }

    public static void updateDatabasesFromNCBI(final Connection connection, final TaxonomyFiles taxFiles) throws IOException, SQLException {
        //TODO implement
        throw new NotImplementedException();
    }

    //TODO document
    public static class TaxonomyFiles {
        private final File giTaxidDmp;
        private final File nodesDmp;
        private final File namesDmp;

        public TaxonomyFiles(TaxonomyFilesBuilder builder) {
            this.giTaxidDmp = builder.giTaxidDmp.toFile();
            this.nodesDmp = builder.nodesDmp.toFile();
            this.namesDmp = builder.namesDmp.toFile();
        }

        public File getGiTaxidDmp() {
            return giTaxidDmp;
        }

        public File getNamesDmp() {
            return namesDmp;
        }

        public File getNodesDmp() {
            return nodesDmp;
        }

        public static class TaxonomyFilesBuilder {
            private Path giTaxidDmp;
            private Path nodesDmp;
            private Path namesDmp;

            public TaxonomyFilesBuilder giTaxidDmp(Path giTaxidDmp) {
                this.giTaxidDmp = giTaxidDmp;
                return this;
            }

            public TaxonomyFilesBuilder nodesDmp(Path nodesDmp) {
                this.nodesDmp = nodesDmp;
                return this;
            }

            public TaxonomyFilesBuilder namesDmp(Path namesDmp) {
                this.namesDmp = namesDmp;
                return this;
            }

            //Constructor omitted
            public TaxonomyFiles build() {
                if (this.giTaxidDmp == null || this.nodesDmp == null || this.namesDmp == null) {
                    throw new IllegalStateException("Please provide file paths to gi_taxid, nodes and names dump files!");
                }
                return new TaxonomyFiles(this);
            }
        }
    }
}
