package test;
import db.mysql.MySQL_Connector;
import helper.names.NamesDeployer;
import helper.nodes.NodesDBDeployer;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 5/31/13
 * Time: 9:55 PM
 * To change this template use File | Settings | File Templates.
 */
public class NamesDeployerTest {

    //@Test
    public void testDeployNamesTable () throws SQLException, ClassNotFoundException, IOException {


        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();
        try{
        NamesDeployer.deployNamesTable(connection,new File("/home/alext/Downloads/NCBI/taxdump/names.dmp"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }
    //@Test
    public void filterNodesDmpFileTest() throws IOException {
        System.out.println(NamesDeployer.filterNamesDmpFile(new File("/home/alext/Downloads/NCBI/taxdump/names.dmp")));
    }

    @Test
    public void printOutFullRanksSet() throws SQLException, ClassNotFoundException, IOException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();

        Set<String> ranks=NodesDBDeployer.calculateASetOfRanksFromFile(new File("/home/alext/Downloads/NCBI/taxdump/nodes.dmp"));
        for (String s:ranks){
            System.out.println(s);
        }
    }
}
