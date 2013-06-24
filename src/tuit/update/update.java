package tuit.update;

import db.mysql.MySQL_Connector;
import helper.NCBITablesDeployer;
import io.properties.jaxb.TUITProperties;
import io.properties.load.TUITPropertiesLoader;
import org.apache.commons.cli.*;

import java.io.File;
import java.sql.Connection;

public class update {
    /**
     * the -p flag for the properties file
     */
    public final static String P = "p";

    public static void main(String[] args) {
        //Declare variables
        File properties;
        File tmpDir;
        //
        //
        TUITPropertiesLoader tuitPropertiesLoader;
        TUITProperties tuitProperties;

        Connection connection;
        MySQL_Connector mySQL_connector;
        CommandLineParser parser = new GnuParser();
        Options options = new Options();
        options.addOption(update.P, "TMP<file>", true, "Temporary directory file (XML formatted)");
        HelpFormatter formatter = new HelpFormatter();
        try{
            //Read command line
            CommandLine commandLine = parser.parse(options, args, true);
            //Check vital parameters
            if (!commandLine.hasOption(update.P)) {
                throw new ParseException("No input file option found, exiting.");
            } else {
                properties = new File(commandLine.getOptionValue(update.P));
            }
            //Load io.properties
            tuitPropertiesLoader = TUITPropertiesLoader.newInstanceFromFile(properties);
            tuitProperties = tuitPropertiesLoader.getTuitProperties();
            //Create tmp directory and blastn executable
            tmpDir = new File(tuitProperties.getTMPDir().getPath());
            //Connect to the database
            mySQL_connector = MySQL_Connector.newDefaultInstance(
                    "jdbc:mysql://" + tuitProperties.getDBConnection().getUrl().trim() + "/",
                    tuitProperties.getDBConnection().getLogin().trim(),
                    tuitProperties.getDBConnection().getPassword().trim());
            mySQL_connector.connectToDatabase();
            connection = mySQL_connector.getConnection();
            //Update the databases
            NCBITablesDeployer.updateDatabasesFromNCBI(connection,tmpDir);
            //
        } catch (ParseException e) {
            e.printStackTrace();  //TODO: improve

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
          //
        }
    }
}
