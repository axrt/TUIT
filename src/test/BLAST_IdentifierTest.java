package test;

import blast.BLAST_Identifier;
import db.mysql.MySQL_Connector;
import db.tables.LookupNames;
import helper.Ranks;
import org.junit.Test;
import taxonomy.TaxonomicNode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/11/13
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class BLAST_IdentifierTest {

    @Test
    public void attachChildrenForTaxonomicNodeTest() throws SQLException, ClassNotFoundException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();


        TaxonomicNode taxonomicNode=TaxonomicNode.newDefaultInstance(137, Ranks.valueOf("family"),"Spirochaetaceae");
        taxonomicNode=attachChildrenForTaxonomicNode(connection,taxonomicNode);
        System.out.print("success");
    }
    private static TaxonomicNode attachChildrenForTaxonomicNode(Connection connection, TaxonomicNode parentNode) throws SQLException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + "=?");
            preparedStatement.setInt(1, parentNode.getTaxid());
            resultSet = preparedStatement.executeQuery();

        TaxonomicNode taxonomicNode;
        while (resultSet.next()) {
            taxonomicNode = TaxonomicNode.newDefaultInstance(
                    resultSet.getInt(2),
                    Ranks.values()[resultSet.getInt(5)-1],
                    resultSet.getString(3));
            taxonomicNode.setParent(parentNode);
            parentNode.addChild(attachChildrenForTaxonomicNode(connection, taxonomicNode));
        }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }

        return parentNode;
    }
}

