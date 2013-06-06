package test;

import db.mysql.MySQL_Connector;
import helper.NodesDBDeployer;
import junit.framework.Assert;
import org.junit.Test;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 5/31/13
 * Time: 10:59 AM
 * To change this template use File | Settings | File Templates.
 */
public class NodesDBDDeployerTest {

    // @Test
    public void calculateASetOfRanksFromFileTest() {

        //Open the nodes.dmp file
        File nodes_dmp_file = new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp");
        try {
            //Send it to the method
            Set<String> ranks = NodesDBDeployer.calculateASetOfRanksFromFile(nodes_dmp_file);
            //Ensure that the Set.size() >0, and print out the components
            assertTrue(ranks.size() > 0);
            for (String s : ranks) {
                System.out.println(s);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //@Test
    public void assingIDsToRanksTest() {
        //Open the nodes.dmp file
        File nodes_dmp_file = new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp");
        try {

            Map<String, Integer> ranks_ids = NodesDBDeployer.assingIDsToRanks(NodesDBDeployer.calculateASetOfRanksFromFile(nodes_dmp_file));
            for (Map.Entry<String, Integer> e : ranks_ids.entrySet()) {
                System.out.println(e.getKey() + "\t>" + e.getValue());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //@Test
    public void deployRanksValidataionTableTest() throws SQLException, ClassNotFoundException, IOException {
        //Open the nodes.dmp file
        File nodes_dmp_file = new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp");
        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();

        NodesDBDeployer.deployRanksValidataionTable(connection, NodesDBDeployer.calculateASetOfRanksFromFile(nodes_dmp_file));

    }

    //@Test
    public void collectRanksValidationLookupTest() throws SQLException, ClassNotFoundException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();

        Map<String, Integer> ranks_ids = NodesDBDeployer.collectRanksValidationLookup(connection);
        for (Map.Entry<String, Integer> e : ranks_ids.entrySet()) {
            System.out.println(e.getKey() + "\t>" + e.getValue());
        }
    }
    //@Test
    public void filterNodesDmpFileTest() throws SQLException, ClassNotFoundException, IOException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();
        NodesDBDeployer.filterNodesDmpFile(connection, new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp"));

    }

    @Test
    public void injectProcessedNodesDmpFileTest()throws SQLException, ClassNotFoundException, IOException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();

        NodesDBDeployer.injectProcessedNodesDmpFile(connection, NodesDBDeployer.filterNodesDmpFile(connection,new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp")));
    }
}
