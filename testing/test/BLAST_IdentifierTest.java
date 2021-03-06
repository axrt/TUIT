package test;

import BLAST.NCBI.output.Iteration;
import blast.specification.BLASTIdentifier;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import db.mysql.MySQL_Connector;
import db.tables.LookupNames;
import format.fasta.nucleotide.NucleotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;
import taxonomy.Ranks;
import io.file.TUITFileOperator;
import org.junit.Test;
import taxonomy.node.TaxonomicNode;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            //nucleotideFastas.add(nculeotideFasta);
            //nucleotideFastas.add(nculeotideFasta);
            //Prepare files
            File executable = new File("blastn");
            File tmpDir = new File("/home/alext/Downloads/tmp");

            //Prepare parameters
            String[] parameters = new String[]{
                    "-db", "nt",  "-entrez_query", "not uncultured not enrichment not unclassified not uncultivated not unspecified","-remote"
            };

            //Prepare MySQL connection
            MySQL_Connector mySQL_connector = MySQL_Connector.newDefaultInstance("jdbc:mysql://localhost/", "ocular", "ocular");
            mySQL_connector.connectToDatabase();
            Connection connection = mySQL_connector.getConnection();

            //Prepare a set of cutoffs
            Map<Ranks, TUITCutoffSet> cutoffSetMap=new HashMap<Ranks, TUITCutoffSet>();
            cutoffSetMap.put(Ranks.species,TUITCutoffSet.newDefaultInstance(97.5,95, 100));
            cutoffSetMap.put(Ranks.species_subgroup,TUITCutoffSet.newDefaultInstance(97.5,95, 100));
            cutoffSetMap.put(Ranks.species_group,TUITCutoffSet.newDefaultInstance(97.5,95, 100));

            cutoffSetMap.put(Ranks.genus,TUITCutoffSet.newDefaultInstance(95,90, 100));
            cutoffSetMap.put(Ranks.subgenus,TUITCutoffSet.newDefaultInstance(95,90, 100));

            cutoffSetMap.put(Ranks.family,TUITCutoffSet.newDefaultInstance(80,90, 100));
            cutoffSetMap.put(Ranks.subfamily,TUITCutoffSet.newDefaultInstance(80,90, 100));
            cutoffSetMap.put(Ranks.superfamily,TUITCutoffSet.newDefaultInstance(80,90, 100));

            //Prepare the BLASTIdentifier
            BLASTIdentifier blast_identifier = BLASTIdentifier.newDefaultInstance(
                    nucleotideFastas, tmpDir, executable,
                    parameters, new TUITFileOperator<NucleotideFasta>() {
                @Override
                protected NucleotideFasta newFastaFromRecord(String record) throws Exception {
                    return null;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                protected boolean inputFileFormattingIsFine() throws Exception {
                    return false;  //To change body of implemented methods use File | Settings | File Templates.
                }

                @Override
                public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) {
                    System.out.println(query.getAC() + ": " + normalizedIteration.getPivotalHit().getFocusNode().getFormattedLineage());
                    return true;  //To change body of implemented methods use File | Settings | File Templates.
                }
            },
                    connection, cutoffSetMap);
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

