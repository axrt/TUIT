package com.company;

import blast.BLAST_Identifier;
import blast.TUITCutoffSet;
import db.mysql.MySQL_Connector;
import exception.TUITPropertyBadFormatException;
import helper.Ranks;
import io.file.properties.jaxb.Database;
import io.file.properties.jaxb.SpecificationParameters;
import io.file.properties.jaxb.TUITProperties;
import io.file.properties.jaxb.TUITPropertiesLoader;
import org.apache.commons.cli.*;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.*;

//TODO: comment as soon as works
public class Main {

    public final static String IN = "in";
    public final static String OUT = "out";
    public final static String P = "p";
    public final static String TUIT_EXT = ".tuit";
    public final static String USAGE = "";//TODO: give a correct usage explanation

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

        //
        BLAST_Identifier blast_identifier;

        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(Main.IN, "input", true, "Input file (currently fasta-formatted only)");
        options.addOption(Main.OUT, "input", true, "Output file (in " + Main.TUIT_EXT + " format)");
        options.addOption(Main.P, "input", true, "Properties file (XML formatted)");

        try {
            //Read command line
            CommandLine commandLine = parser.parse(options, args, true);
            //Check vital parameters
            if (!commandLine.hasOption(Main.IN)) {
                throw new ParseException("No input file option found, exiting.");
            } else {
                inputFile = new File(commandLine.getOptionValue(Main.IN));
            }
            if (!commandLine.hasOption(Main.P)) {
                throw new ParseException("No properties file option found, exiting.");
            } else {
                properties = new File(commandLine.getOptionValue(Main.P));
            }
            //Correct the output file option if needed
            if (!commandLine.hasOption(Main.OUT)) {
                outputFile = new File(inputFile.getPath().split(".")[0] + Main.TUIT_EXT);
            } else {
                outputFile = new File(commandLine.getOptionValue(Main.OUT));
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
            if (!outputFile.canWrite()) {
                throw new Exception("Cannot write the output file, please check file system permissions.");
            }
            //Load properties
            tuitPropertiesLoader = TUITPropertiesLoader.newInstanceFromFile(properties);
            tuitProperties = tuitPropertiesLoader.getTuitProperties();
            //Create tmp directory and blastn executable
            tmpDir = new File(tuitProperties.getTMPDir().getPath());
            blastnExecutable = new File(tuitProperties.getBLASTNPath().getPath());
            //Create blast parameters
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("\"");
            for (Database database : tuitProperties.getBLASTNParameters().getDatabase()) {
                stringBuilder.append(database.getUse());
                stringBuilder.append(" ");//Gonna insert an unessessary space for the last database
            }
            stringBuilder.append("\"");
            String remote;
            if (tuitProperties.getBLASTNParameters().getRemote().getDeligate().equals("yes")) {
                remote = "-remote";
            } else {
                remote = "";
            }
            parameters = new String[]{
                    "-db", stringBuilder.toString(),
                    remote,
                    "-entrez_query", "\"" + tuitProperties.getBLASTNParameters().getEntrezQuery() + "\"",
                    "-expect", tuitProperties.getBLASTNParameters().getExpect().getValue()
            };
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
            //Load the query


        } catch (ParseException pe) {
            System.out.println(pe.getMessage());
        } catch (SAXException saxe) {
            System.out.println(saxe.getMessage());
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe.getMessage());
        } catch (TUITPropertyBadFormatException tpbfe) {
            System.out.println(tpbfe.getMessage());
        } catch (JAXBException jaxbee) {
            System.out.println(jaxbee.getMessage());
        } catch (ClassNotFoundException cnfe) {
            System.out.println(cnfe.getMessage());
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
        } finally {
            System.out.println("Exiting");
            System.exit(1);
        }
    }
}
