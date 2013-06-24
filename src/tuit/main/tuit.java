package tuit.main;

import blast.specification.BLASTIdentifier;
import blast.specification.TUITBLASTIdentifier;
import blast.specification.cutoff.TUITCutoffSet;
import db.mysql.MySQL_Connector;
import exception.TUITPropertyBadFormatException;
import helper.NCBITablesDeployer;
import taxonomy.Ranks;
import io.file.NucleotideFastaTUITFileOperator;
import io.properties.jaxb.Database;
import io.properties.jaxb.SpecificationParameters;
import io.properties.jaxb.TUITProperties;
import io.properties.load.TUITPropertiesLoader;
import org.apache.commons.cli.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * A main class for the tuit module implementation
 */

public class tuit {

    /**
     * The -in flag for the input file
     */
    public final static String IN = "in";
    /**
     * The -out flag for the output file
     */
    public final static String OUT = "out";
    /**
     * the -p flag for the properties file
     */
    public final static String P = "p";
    /**
     * tuit output file extension
     */
    public final static String TUIT_EXT = ".tuit";

    public static void main(String[] args) {

        //Declare variables
        File inputFile;
        File outputFile;
        File tmpDir;
        File blastnExecutable;
        File properties;
        //
        TUITPropertiesLoader tuitPropertiesLoader;
        TUITProperties tuitProperties;
        //
        String[] parameters;
        //
        Connection connection;
        MySQL_Connector mySQL_connector;
        //
        Map<Ranks, TUITCutoffSet> cutoffMap;
        //
        BLASTIdentifier blast_identifier;

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(tuit.IN, "input<file>", true, "Input file (currently fasta-formatted only)");
        options.addOption(tuit.OUT, "output<file>", true, "Output file (in " + tuit.TUIT_EXT + " format)");
        options.addOption(tuit.P, "prop<file>", true, "Properties file (XML formatted)");
        HelpFormatter formatter = new HelpFormatter();
        try {
            //Read command line
            CommandLine commandLine = parser.parse(options, args, true);
            //Check vital parameters
            if (!commandLine.hasOption(tuit.IN)) {
                throw new ParseException("No input file option found, exiting.");
            } else {
                inputFile = new File(commandLine.getOptionValue(tuit.IN));
            }
            if (!commandLine.hasOption(tuit.P)) {
                throw new ParseException("No io.properties file option found, exiting.");
            } else {
                properties = new File(commandLine.getOptionValue(tuit.P));
            }
            //Correct the output file option if needed
            if (!commandLine.hasOption(tuit.OUT)) {
                outputFile = new File((inputFile.getPath()).split("\\.")[0] + tuit.TUIT_EXT);
            } else {
                outputFile = new File(commandLine.getOptionValue(tuit.OUT));
            }
            //Try all files
            if (!inputFile.exists() || !inputFile.canRead()) {
                throw new Exception("Input file either does not exist, or is not readable.");
            } else if (inputFile.isDirectory()) {
                throw new Exception("Input file points to a directory.");
            }
            if (!properties.exists() || !properties.canRead()) {
                throw new Exception("Properties file either does not exist, or is not readable.");
            } else if (inputFile.isDirectory()) {
                throw new Exception("Properties file points to a directory.");
            }
            /*if (!outputFile.canWrite()) {
                throw new Exception("Cannot write the output file "+outputFile.getPath()+", please check file system permissions.");
            }*/
            //Load io.properties
            tuitPropertiesLoader = TUITPropertiesLoader.newInstanceFromFile(properties);
            tuitProperties = tuitPropertiesLoader.getTuitProperties();
            //Create tmp directory and blastn executable
            tmpDir = new File(tuitProperties.getTMPDir().getPath());
            blastnExecutable = new File(tuitProperties.getBLASTNPath().getPath());
            //Create blast parameters
            StringBuilder stringBuilder = new StringBuilder();
            //stringBuilder.append("\"");
            for (Database database : tuitProperties.getBLASTNParameters().getDatabase()) {
                stringBuilder.append(database.getUse());
                stringBuilder.append(" ");//Gonna insert an unessessary space for the last database
            }
            //stringBuilder.append("\"");
            String remote;
            String entrez_query;
            if (tuitProperties.getBLASTNParameters().getRemote().getDeligate().equals("yes")) {
                remote = "-remote";
                entrez_query = "-entrez_query";
                parameters = new String[]{
                        "-db", stringBuilder.toString(),
                        remote,
                        entrez_query, tuitProperties.getBLASTNParameters().getEntrezQuery().getValue(),
                        "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue()
                };
            } else {
                parameters = new String[]{
                        "-db", stringBuilder.toString(),
                        "-evalue", tuitProperties.getBLASTNParameters().getExpect().getValue()
                };
            }

            //Connect to the database
            mySQL_connector = MySQL_Connector.newDefaultInstance(
                    "jdbc:mysql://" + tuitProperties.getDBConnection().getUrl().trim() + "/",
                    tuitProperties.getDBConnection().getLogin().trim(),
                    tuitProperties.getDBConnection().getPassword().trim());
            mySQL_connector.connectToDatabase();
            connection = mySQL_connector.getConnection();
            //Prepare a cutoff Map
            if (tuitProperties.getSpecificationParameters() != null && tuitProperties.getSpecificationParameters().size() > 0) {
                cutoffMap = new HashMap<Ranks, TUITCutoffSet>(tuitProperties.getSpecificationParameters().size());
                for(SpecificationParameters specificationParameters:tuitProperties.getSpecificationParameters()){
                    cutoffMap.put(Ranks.valueOf(specificationParameters.getCutoffSet().getRank()),
                            TUITCutoffSet.newDefaultInstance(
                                    Double.parseDouble(specificationParameters.getCutoffSet().getPIdentCutoff().getValue()),
                                    Double.parseDouble(specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue()),
                                    Double.parseDouble(specificationParameters.getCutoffSet().getEvalueRatioCutoff().getValue())));
                }
            } else {
                cutoffMap = new HashMap<Ranks, TUITCutoffSet>();
            }
            NucleotideFastaTUITFileOperator.getInstance().setInputFile(inputFile);
            NucleotideFastaTUITFileOperator.getInstance().setOutputFile(outputFile);
            //Create blast identifier
            blast_identifier = TUITBLASTIdentifier.newInstanceFromFileOperator(
                    tmpDir, blastnExecutable, parameters,
                    NucleotideFastaTUITFileOperator.getInstance(), connection,
                    cutoffMap, Integer.parseInt(tuitProperties.getBLASTNParameters().getMaxFilesInBatch().getValue()));


            Future<?> runnableFuture= Executors.newSingleThreadExecutor().submit(blast_identifier);
            runnableFuture.get();


        } catch (ParseException pe) {
            System.err.println(pe.getMessage());
            formatter.printHelp( "tuit", options );
            //pe.printStackTrace();
        } catch (SAXException saxe) {
            System.err.println(saxe.getMessage());
            //saxe.printStackTrace();
        } catch (FileNotFoundException fnfe) {
            System.err.println(fnfe.getMessage());
            //fnfe.printStackTrace();
        } catch (TUITPropertyBadFormatException tpbfe) {
            System.err.println(tpbfe.getMessage());
            //tpbfe.printStackTrace();
        } catch (JAXBException jaxbee) {
            System.err.println("The io.properties file is not well formatted. Please ensure that the XML is consistent with the io.properties.dtd schema.");
            //jaxbee.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            //Probably won't happen unless the library deleted from the .jar
            System.err.println(cnfe.getMessage());
            //cnfe.printStackTrace();
        } catch (SQLException sqle) {
            System.err.println("A database communication error occurred with the following message:\n" +
                    sqle.getMessage());
            if(sqle.getMessage().contains("Access denied for user")){
                System.err.println("Please use standard database login: "+ NCBITablesDeployer.login+" and password: "+ NCBITablesDeployer.password);
            }
            //sqle.printStackTrace();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } finally {
            System.err.println("Exiting..");
            System.exit(1);
        }
    }
}