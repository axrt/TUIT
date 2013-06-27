package test;

import db.mysql.MySQL_Connector;
import helper.NCBITablesDeployer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/6/13
 * Time: 2:22 PM
 * To change this template use File | Settings | File Templates.
 */
public class NCBITablesDeployerTest {

    //@Test
    public  void deployDatabasesTest(){

        try {
            MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
            mySQL_connector.connectToDatabase();
            final Connection connection = mySQL_connector.getConnection();

            final File gi_taxidDmpFile=new File ("/home/alext/Downloads/NCBI/gi_taxid_nucl.dmp");
            final File namesDmpFile=new File ("/home/alext/Downloads/NCBI/taxdump/names.dmp");
            final File nodesDmpFile=new File ("/home/alext/Downloads/NCBI/taxdump/nodes.dmp");

            NCBITablesDeployer.fastDeployNCBIDatabasesFromFiles(connection,gi_taxidDmpFile,namesDmpFile,nodesDmpFile);

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }
    //@Test
    public  void updateDatabasesFromNCBITest(){


        try {
            MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
            mySQL_connector.connectToDatabase();
            Connection connection = mySQL_connector.getConnection();

            NCBITablesDeployer.updateDatabasesFromNCBI(connection, new File("/home/alext/Downloads/tmp"));

        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }


}
