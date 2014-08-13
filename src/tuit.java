import blast.specification.BLASTIdentifier;
import blast.specification.TUITBLASTIdentifierDB;
import blast.specification.TUITBLASTIdentifierRAM;
import blast.specification.cutoff.TUITCutoffSet;
import db.mysql.MySQL_Connector;
import db.ram.RamDb;
import exception.TUITPropertyBadFormatException;
import format.fasta.nucleotide.NucleotideFasta;
import helper.NCBITablesDeployer;
import io.file.NucleotideFastaTUITFileOperator;
import io.file.TUITFileOperator;
import io.file.TUITFileOperatorHelper;
import io.properties.jaxb.Database;
import io.properties.jaxb.SpecificationParameters;
import io.properties.jaxb.TUITProperties;
import io.properties.load.TUITPropertiesLoader;
import logger.Log;
import org.apache.commons.cli.*;
import org.xml.sax.SAXException;
import taxonomy.Ranks;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
 * A main class for the tuit module implementation
 */

public class tuit {

    private static String licence = "\n" +
            "\nTHANK YOU FOR USING THE TAXONOMIC UNIT IDENTIFICATION TOOL\n\n" +
            "Taxonomic Unit Identification Tool  Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov\n" +
            "    This program comes with ABSOLUTELY NO WARRANTY; for details see LICENCE.\n" +
            "    This is free software, and you are welcome to redistribute it\n" +
            "    under certain conditions; see LICENCE for details.";

    /**
     * The -in flag for the input file
     */
    private final static String IN = "i";
    /**
     * The -out flag for the output file
     */
    private final static String OUT = "o";
    /**
     * The -p flag for the properties file
     */
    private final static String P = "p";
    /**
     * The -b flag for the pre-blasted BLAST output file
     */
    private final static String B = "b";
    /**
     * The -v flag for the verbose output file
     */
    private final static String V = "v";
    /**
     * The -deploy flag for the verbose output file
     */
    private final static String DEPLOY = "deploy";
    /**
     * The -update flag for the verbose output file
     */
    private final static String UPDATE = "update";
    /**
     * tuit output file extension
     */
    private final static String TUIT_EXT = ".tuit";
    /**
     * Determines whether tuit should use the ram-mapped taxonomic database, or connect to a slower mysql version
     */
    private final static String USE_DB = "usedb";
    /**
     * A serialized object for the taxonomic database
     */
    private final static String RAM_DB = "ramdb.obj";

    @SuppressWarnings("ConstantConditions")
    public static void main(String[] args) {
        System.out.println(licence);
        //Declare variables
        File inputFile;
        File outputFile;
        File tmpDir;
        File blastnExecutable;
        File properties;
        File blastOutputFile = null;
        //
        TUITPropertiesLoader tuitPropertiesLoader;
        TUITProperties tuitProperties;
        //
        String[] parameters = null;
        //
        Connection connection = null;
        MySQL_Connector mySQL_connector;
        //
        Map<Ranks, TUITCutoffSet> cutoffMap;
        //
        BLASTIdentifier blastIdentifier = null;
        //
        RamDb ramDb = null;

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(tuit.IN, "input<file>", true, "Input file (currently fasta-formatted only)");
        options.addOption(tuit.OUT, "output<file>", true, "Output file (in " + tuit.TUIT_EXT + " format)");
        options.addOption(tuit.P, "prop<file>", true, "Properties file (XML formatted)");
        options.addOption(tuit.V, "verbose", false, "Enable verbose output");
        options.addOption(tuit.B, "blast_output<file>", true, "Perform on a pre-BLASTed output");
        options.addOption(tuit.DEPLOY, "deploy", false, "Deploy the taxonomic databases");
        options.addOption(tuit.UPDATE, "update", false, "Update the taxonomic databases");
        options.addOption(tuit.USE_DB, "usedb", false, "Use RDBMS instead of RAM-based taxonomy");
        HelpFormatter formatter = new HelpFormatter();

        try {
            //Get TUIT directory
            final File tuitDir = new File(new File(tuit.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParent());
            final File ramDbFile = new File(tuitDir, tuit.RAM_DB);
            //Setup logger
            Log.getInstance().setLogName("tuit.log");
            //Read command line
            final CommandLine commandLine = parser.parse(options, args, true);
            if (!commandLine.hasOption(tuit.P)) {
                throw new ParseException("No properties file option found, exiting.");
            } else {
                properties = new File(commandLine.getOptionValue(tuit.P));
            }
            //Load properties
            tuitPropertiesLoader = TUITPropertiesLoader.newInstanceFromFile(properties);
            tuitProperties = tuitPropertiesLoader.getTuitProperties();


            //Create tmp directory and blastn executable
            tmpDir = new File(tuitProperties.getTMPDir().getPath());
            blastnExecutable = new File(tuitProperties.getBLASTNPath().getPath());

            //Check for deploy
            if (commandLine.hasOption(tuit.DEPLOY)) {
                if (commandLine.hasOption(tuit.USE_DB)) {
                    NCBITablesDeployer.fastDeployNCBIDatabasesFromNCBI(connection, tmpDir);
                } else {
                    NCBITablesDeployer.fastDeployNCBIRamDatabaseFromNCBI(tmpDir, ramDbFile);
                }

                Log.getInstance().log(Level.FINE, "Task done, exiting...");
                return;
            }
            //Check for update
            if (commandLine.hasOption(tuit.UPDATE)) {
                if (commandLine.hasOption(tuit.USE_DB)) {
                    NCBITablesDeployer.updateDatabasesFromNCBI(connection, tmpDir);
                } else {
                    //No need to specify a different way to update the database other than just deploy in case of the RAM database
                    NCBITablesDeployer.fastDeployNCBIRamDatabaseFromNCBI(tmpDir, ramDbFile);
                }
                Log.getInstance().log(Level.FINE, "Task done, exiting...");
                return;
            }

            //Connect to the database
            if (commandLine.hasOption(tuit.USE_DB)) {
                mySQL_connector = MySQL_Connector.newDefaultInstance(
                        "jdbc:mysql://" + tuitProperties.getDBConnection().getUrl().trim() + "/",
                        tuitProperties.getDBConnection().getLogin().trim(),
                        tuitProperties.getDBConnection().getPassword().trim());
                mySQL_connector.connectToDatabase();
                connection = mySQL_connector.getConnection();
            } else {
                //Probe for ram database

                if (ramDbFile.exists() && ramDbFile.canRead()) {
                    Log.getInstance().log(Level.INFO, "Loading RAM taxonomic map...");
                    try {
                        ramDb = RamDb.loadSelfFromFile(ramDbFile);
                    } catch (IOException ie) {
                        if (ie instanceof java.io.InvalidClassException)
                            throw new IOException("The RAM-based taxonomic database needs to be updated.");
                    }

                } else {
                    Log.getInstance().log(Level.SEVERE, "The RAM database either has not been deployed, or is not accessible." +
                            "Please use the --deploy option and check permissions on the TUIT directory. " +
                            "If you were looking to use the RDBMS as a taxonomic reference, plese use the -usedb option.");
                    return;
                }
            }

            if (commandLine.hasOption(tuit.B)) {
                blastOutputFile = new File(commandLine.getOptionValue(tuit.B));
                if (!blastOutputFile.exists() || !blastOutputFile.canRead()) {
                    throw new Exception("BLAST output file either does not exist, or is not readable.");
                } else if (blastOutputFile.isDirectory()) {
                    throw new Exception("BLAST output file points to a directory.");
                }
            }
            //Check vital parameters
            if (!commandLine.hasOption(tuit.IN)) {
                throw new ParseException("No input file option found, exiting.");
            } else {
                inputFile = new File(commandLine.getOptionValue(tuit.IN));
                Log.getInstance().setLogName(inputFile.getName().split("\\.")[0] + ".tuit.log");
            }
            //Correct the output file option if needed
            if (!commandLine.hasOption(tuit.OUT)) {
                outputFile = new File((inputFile.getPath()).split("\\.")[0] + tuit.TUIT_EXT);
            } else {
                outputFile = new File(commandLine.getOptionValue(tuit.OUT));
            }

            //Adjust the output level
            if (commandLine.hasOption(tuit.V)) {
                Log.getInstance().setLevel(Level.FINE);
                Log.getInstance().log(Level.INFO, "Using verbose output for the log");
            } else {
                Log.getInstance().setLevel(Level.INFO);
            }
            //Try all files
            if (inputFile != null) {
                if (!inputFile.exists() || !inputFile.canRead()) {
                    throw new Exception("Input file either does not exist, or is not readable.");
                } else if (inputFile.isDirectory()) {
                    throw new Exception("Input file points to a directory.");
                }
            }

            if (!properties.exists() || !properties.canRead()) {
                throw new Exception("Properties file either does not exist, or is not readable.");
            } else if (properties.isDirectory()) {
                throw new Exception("Properties file points to a directory.");
            }

            //Create blast parameters
            final StringBuilder stringBuilder = new StringBuilder();
            for (Database database : tuitProperties.getBLASTNParameters().getDatabase()) {
                stringBuilder.append(database.getUse());
                stringBuilder.append(" ");//Gonna insert an extra space for the last database
            }
            String remote;
            String entrez_query;
            if (tuitProperties.getBLASTNParameters().getRemote().getDelegate().equals("yes")) {
                remote = "-remote";
                entrez_query = "-entrez_query";
                parameters = new String[]{
                        "-db", stringBuilder.toString(),
                        remote,
                        entrez_query, tuitProperties.getBLASTNParameters().getEntrezQuery().getValue(),
                        "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue()
                };
            } else {
                if (!commandLine.hasOption(tuit.B)) {
                    if (tuitProperties.getBLASTNParameters().getEntrezQuery().getValue().toUpperCase().startsWith("NOT") || tuitProperties.getBLASTNParameters().getEntrezQuery().getValue().toUpperCase().startsWith("ALL")) {
                        parameters = new String[]{
                                "-db", stringBuilder.toString(),
                                "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue(),
                                "-negative_gilist", TUITFileOperatorHelper.restrictToEntrez(
                                tmpDir, tuitProperties.getBLASTNParameters().getEntrezQuery().getValue().toUpperCase().replace("NOT", "OR")).getAbsolutePath(),
                                "-num_threads", tuitProperties.getBLASTNParameters().getNumThreads().getValue()
                        };
                    }else if(tuitProperties.getBLASTNParameters().getEntrezQuery().getValue().toUpperCase().equals("")){
                        parameters = new String[]{
                                "-db", stringBuilder.toString(),
                                "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue(),
                                "-num_threads", tuitProperties.getBLASTNParameters().getNumThreads().getValue()
                        };
                    }else {
                        parameters = new String[]{
                                "-db", stringBuilder.toString(),
                                "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue(),
                                /*"-gilist", TUITFileOperatorHelper.restrictToEntrez(
                                tmpDir, tuitProperties.getBLASTNParameters().getEntrezQuery().getValue()).getAbsolutePath(),*/ //TODO remove comment!!!!!
                                "-num_threads", tuitProperties.getBLASTNParameters().getNumThreads().getValue()
                        };
                    }
                }
            }
            //Prepare a cutoff Map
            if (tuitProperties.getSpecificationParameters() != null && tuitProperties.getSpecificationParameters().size() > 0) {
                cutoffMap = new HashMap<Ranks, TUITCutoffSet>(tuitProperties.getSpecificationParameters().size());
                for (SpecificationParameters specificationParameters : tuitProperties.getSpecificationParameters()) {
                    cutoffMap.put(Ranks.valueOf(specificationParameters.getCutoffSet().getRank()),
                            TUITCutoffSet.newDefaultInstance(
                                    Double.parseDouble(specificationParameters.getCutoffSet().getPIdentCutoff().getValue()),
                                    Double.parseDouble(specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue()),
                                    Double.parseDouble(specificationParameters.getCutoffSet().getAlpha().getValue())));
                }
            } else {
                cutoffMap = new HashMap<Ranks, TUITCutoffSet>();
            }
            final TUITFileOperatorHelper.OutputFormat format;
            if(tuitProperties.getBLASTNParameters().getOutputFormat().getFormat().equals("rdp")){
                format= TUITFileOperatorHelper.OutputFormat.RDP_FIXRANK;
            }else{
                format= TUITFileOperatorHelper.OutputFormat.TUIT;
            }

            try (TUITFileOperator<NucleotideFasta> nucleotideFastaTUITFileOperator = NucleotideFastaTUITFileOperator.newInstance(format,cutoffMap);) {
                nucleotideFastaTUITFileOperator.setInputFile(inputFile);
                nucleotideFastaTUITFileOperator.setOutputFile(outputFile);
                final String cleanupString = tuitProperties.getBLASTNParameters().getKeepBLASTOuts().getKeep();
                final boolean cleanup;
                if (cleanupString.equals("no")) {
                    Log.getInstance().log(Level.INFO, "Temporary BLAST files will be deleted.");
                    cleanup = true;
                } else {
                    Log.getInstance().log(Level.INFO, "Temporary BLAST files will be kept.");
                    cleanup = false;
                }
                //Create blast identifier
                ExecutorService executorService = Executors.newSingleThreadExecutor();
                if (commandLine.hasOption(tuit.USE_DB)) {

                    if (blastOutputFile == null) {
                        blastIdentifier = TUITBLASTIdentifierDB.newInstanceFromFileOperator(
                                tmpDir, blastnExecutable, parameters,
                                nucleotideFastaTUITFileOperator, connection,
                                cutoffMap,
                                Integer.parseInt(tuitProperties.getBLASTNParameters().getMaxFilesInBatch().getValue())
                                , cleanup);

                    } else {
                        try {
                            blastIdentifier = TUITBLASTIdentifierDB.newInstanceFromBLASTOutput(nucleotideFastaTUITFileOperator, connection,
                                    cutoffMap, blastOutputFile,
                                    Integer.parseInt(tuitProperties.getBLASTNParameters().getMaxFilesInBatch().getValue()), cleanup);

                        } catch (JAXBException e) {
                            Log.getInstance().log(Level.SEVERE, "Error reading " + blastOutputFile.getName() + ", please check input. The file must be XML formatted.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                } else {
                    if (blastOutputFile == null) {
                        blastIdentifier = TUITBLASTIdentifierRAM.newInstanceFromFileOperator(
                                tmpDir, blastnExecutable, parameters,
                                nucleotideFastaTUITFileOperator,
                                cutoffMap,
                                Integer.parseInt(tuitProperties.getBLASTNParameters().getMaxFilesInBatch().getValue())
                                , cleanup, ramDb);

                    } else {
                        try {
                            blastIdentifier = TUITBLASTIdentifierRAM.newInstanceFromBLASTOutput(nucleotideFastaTUITFileOperator,
                                    cutoffMap, blastOutputFile,
                                    Integer.parseInt(tuitProperties.getBLASTNParameters().getMaxFilesInBatch().getValue()), cleanup, ramDb);

                        } catch (JAXBException e) {
                            Log.getInstance().log(Level.SEVERE, "Error reading " + blastOutputFile.getName() + ", please check input. The file must be XML formatted.");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                Future<?> runnableFuture = executorService.submit(blastIdentifier);
                runnableFuture.get();
                executorService.shutdown();
            }
        } catch (ParseException pe) {
            Log.getInstance().log(Level.SEVERE, (pe.getMessage()));
            formatter.printHelp("tuit", options);
        } catch (SAXException saxe) {
            Log.getInstance().log(Level.SEVERE, saxe.getMessage());
        } catch (FileNotFoundException fnfe) {
            Log.getInstance().log(Level.SEVERE, fnfe.getMessage());
        } catch (TUITPropertyBadFormatException tpbfe) {
            Log.getInstance().log(Level.SEVERE, tpbfe.getMessage());
        } catch (ClassCastException cce) {
            Log.getInstance().log(Level.SEVERE, cce.getMessage());
        } catch (JAXBException jaxbee) {
            Log.getInstance().log(Level.SEVERE, "The properties file is not well formatted. Please ensure that the XML is consistent with the io.properties.dtd schema.");
        } catch (ClassNotFoundException cnfe) {
            //Probably won't happen unless the library deleted from the .jar
            Log.getInstance().log(Level.SEVERE, cnfe.getMessage());
            //cnfe.printStackTrace();
        } catch (SQLException sqle) {
            Log.getInstance().log(Level.SEVERE, "A database communication error occurred with the following message:\n" +
                    sqle.getMessage());
            //sqle.printStackTrace();
            if (sqle.getMessage().contains("Access denied for user")) {
                Log.getInstance().log(Level.SEVERE, "Please use standard database login: " + NCBITablesDeployer.login + " and password: " + NCBITablesDeployer.password);
            }
        } catch (Exception e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (SQLException sqle) {
                    Log.getInstance().log(Level.SEVERE, "Problem closing the database connection: " + sqle);
                }
            }
            Log.getInstance().log(Level.FINE, "Task done, exiting...");
        }
    }
}
