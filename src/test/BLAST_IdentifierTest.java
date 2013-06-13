package test;

import BLAST.NCBI.output.Iteration;
import blast.BLAST_Identifier;
import db.mysql.MySQL_Connector;
import db.tables.LookupNames;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;
import helper.Ranks;
import org.junit.Test;
import taxonomy.TaxonomicNode;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/11/13
 * Time: 2:05 PM
 * To change this template use File | Settings | File Templates.
 */
public class BLAST_IdentifierTest {
    @Test
    public void BLAST_IdentifierTest() {
        try {
            //Preparing a query fasta
            String queryFasta = ">GR749QQ02HKHXJ\tv2bBar1031L\t16\n" +
                    "TATCGGAACGTACCCGGAAATGGGGATAACGTAGCGAAAGTTACGCTA" +
                    "ATACCGCATATGCCCTGAGGGGGAAAGCGGGGGATTCGTAAGAACCTCGCG" +
                    "TTTTCGGAGCGGCCGATATCGGATTAGCTAGTAGGTGAGGTAAAGGCTCACC" +
                    "TAGGCGACGATCCGTAGCTGGTCTGAGAGGACGACCAGCCACACTGGAACTGA" +
                    "GACACGGTCCAGACTCCTACGGGAGGCAGCAGTGGGGAATTTTGGACAATGGGCG" +
                    "CAAGCCTGATCCAGCCATGCCGCGTGAGTGAAGAAGGCCTTCGGGTTGTAAAGCTC" +
                    "TTTCAGCCGGAAAGAAAACGCACGGGTTAATACCCTGTGTGGATGACGGTACCGGAA" +
                    "GAAGAAGCACCGGCTAACTACGTG\n";


            NucleotideFasta nculeotideFasta = NucleotideFasta.newInstanceFromFromattedText(queryFasta);

            System.out.println(nculeotideFasta);

            List<NucleotideFasta> nucleotideFastas = new ArrayList<NucleotideFasta>(1);
            nucleotideFastas.add(nculeotideFasta);

            //Prepare files
            File executable = new File("blastn");
            File tmpDir = new File("/home/alext/Downloads/tmp");

            //Prepare parameters
            String[] parameters = new String[]{
                    "-db", "nt", "-remote", "-entrez_query", "not uncultured not enrichment not unclassified not uncultivated not unspecified"
            };

            //Prepare MySQL connection
            MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
            mySQL_connector.connectToDatabase();
            Connection connection = mySQL_connector.getConnection();

            //Prepare the BLAST_Identifier
            BLAST_Identifier blast_identifier = BLAST_Identifier.newDefaultInstance(nucleotideFastas, null, tmpDir, executable, parameters, connection, null);
            blast_identifier.run();

            System.out.println("Finished");
        } catch (NucleotideFasta_BadFromat_Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NucleotideFasta_Sequence_BadFromatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (ClassNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }


    //@Test
    public void attachChildrenForTaxonomicNodeTest() throws SQLException, ClassNotFoundException {

        MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
        mySQL_connector.connectToDatabase();
        Connection connection = mySQL_connector.getConnection();


        TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(137, Ranks.valueOf("family"), "Spirochaetaceae");
        taxonomicNode = attachChildrenForTaxonomicNode(connection, taxonomicNode);
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
                        Ranks.values()[resultSet.getInt(5) - 1],
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

