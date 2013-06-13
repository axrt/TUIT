package blast;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Iteration;
import db.connect.DatabaseOperator;
import db.tables.LookupNames;
import format.BadFromatException;
import format.fasta.nucleotide.NucleotideFasta;
import helper.Ranks;
import org.xml.sax.SAXException;
import taxonomy.TaxonomicNode;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * TODO: document
 */
public class BLAST_Identifier extends NCBI_EX_BLASTN implements DatabaseOperator {

    protected final Connection connection;
    protected final Map<Ranks, TUITCutoffSet> cutoffSetMap;
    protected List<NormalyzedIteration> normalyzedIterations;

    protected BLAST_Identifier(List<? extends NucleotideFasta> query, List<String> query_IDs,
                               File tempDir, File executive, String[] parameterList,
                               Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        super(query, query_IDs, tempDir, executive, parameterList);
        this.connection = connection;
        this.cutoffSetMap = cutoffSetMap;
    }

    @Override
    public void run() {

        try {
            this.BLAST();
            this.normalyzedIterations=new ArrayList<NormalyzedIteration>(this.blastOutput.getBlastOutputIterations().getIteration().size());
            this.BLASTed=true;
            this.normalizeIterations();
            for(NormalyzedIteration normalyzedIteration:this.normalyzedIterations){
                normalyzedIteration.specify();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BadFromatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }


    }

    protected boolean normalyzedHitChecksAgainstParametersForRank(final NormalizedHit normalizedHit, final Ranks rank) {
          return true;
        //TODO: commented for testing
        /*TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet= this.cutoffSetMap.get(rank)) == null||normalizedHit==null||rank==null) {
            return true;
        } else {
            return tuitCutoffSet.normalizedHitPassesCheck(normalizedHit);
        }*/
    }
    protected boolean hitsAreFarEnoughByEvalueAtRank(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit, Ranks rank){
        if((oneNormalizedHit.getHitEvalue()/anotherNormalizedHit.getHitEvalue())>=100){
            return true;
        } else{
            return false;
        }
        //TODO: commented for testing
        /*TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet= this.cutoffSetMap.get(rank)) == null||oneNormalizedHit==null||anotherNormalizedHit==null) {
            return true;
        } else {
            return tuitCutoffSet.hitsAreFarEnoughByEvalue(oneNormalizedHit,anotherNormalizedHit);
        }*/
    }

    @Override
    public NormalizedHit assignTaxonomy(final NormalizedHit normalizedHit) throws SQLException{

        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.taxon_by_gi.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name()
                            + "=?");
            preparedStatement.setInt(1, normalizedHit.getGI());
            resultSet = preparedStatement.executeQuery();

            int taxid;
            Ranks rank;
            String scientificName;
            if (resultSet.next()) {
                taxid = resultSet.getInt(2);
                scientificName = resultSet.getString(3);
                rank = Ranks.values()[resultSet.getInt(5) - 1];
                TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(taxid, rank, scientificName);
                normalizedHit.setTaxonomy(taxonomicNode);
                normalizedHit.setFocusNode(taxonomicNode);
            } else {
                return null;
            }
        } finally {
            //Close and cleanup
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }

        //TODO: remove all resultsets.close() from the finally blocks
        //Set the nodes to the hits taxonomy field

        return normalizedHit;
    }

    @Override
    public NormalizedHit liftRankForNormalyzedHit(final NormalizedHit normalizedHit) throws SQLException {
        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name()
                            + "=(SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.nodes.name
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name() + "=?)");
            preparedStatement.setInt(1, normalizedHit.getAssignedTaxid());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(resultSet.getInt(2),
                        Ranks.values()[resultSet.getInt(5) - 1],
                        resultSet.getString(3));
                taxonomicNode.addChild(normalizedHit.getFocusNode());
                normalizedHit.setTaxonomy(taxonomicNode);
                normalizedHit.setFocusNode(taxonomicNode);
            } else {
                return null;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
            if (resultSet != null) {
                resultSet.close();
            }
        }
        return normalizedHit;
    }

    protected TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode parentNode) throws SQLException {

        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + "=?");
            preparedStatement.setInt(1, parentNode.getTaxid());
            resultSet = preparedStatement.executeQuery();

            TaxonomicNode taxonomicNode;
            parentNode.getChildren().clear();
            while (resultSet.next()) {
                taxonomicNode = TaxonomicNode.newDefaultInstance(
                        resultSet.getInt(2),
                        Ranks.values()[resultSet.getInt(5) - 1],
                        resultSet.getString(3));
                taxonomicNode.setParent(parentNode);
                parentNode.addChild(this.attachChildrenForTaxonomicNode(taxonomicNode));
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

    protected void normalizeIterations(){
        //Normalize each iteration
        for(Iteration iteration:this.blastOutput.getBlastOutputIterations().getIteration()){
            this.normalyzedIterations.add(NormalyzedIteration.newDefaultInstance(iteration,this));
        }
    }

    public static BLAST_Identifier newDefaultInstance(List<? extends NucleotideFasta> query, List<String> query_IDs,
                                                      File tempDir, File executive, String[] parameterList,
                                                      Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        return new BLAST_Identifier(query, query_IDs, tempDir, executive, parameterList, connection, cutoffSetMap);
    }

    protected class TUITCutoffSet {
        protected final double pIdentCutoff;
        protected final double querryCoverageCutoff;
        protected final double evalueDifferenceCutoff;

        protected TUITCutoffSet(final double pIdentCutoff, final double querryCoverageCutoff, final double evalueDifferenceCutoff) {
            this.pIdentCutoff = pIdentCutoff;
            this.querryCoverageCutoff = querryCoverageCutoff;
            this.evalueDifferenceCutoff = evalueDifferenceCutoff;
        }

        public boolean normalizedHitPassesCheck(final NormalizedHit normalizedHit) {
            if (normalizedHit.getpIdent() < this.pIdentCutoff) {
                return false;
            }
            if (normalizedHit.getHitQueryCoverage() < this.querryCoverageCutoff) {
                return false;
            }
            return true;
        }

        public boolean hitsAreFarEnoughByEvalue(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit) {
            if (oneNormalizedHit.getHitEvalue() / anotherNormalizedHit.getHitEvalue() >= this.evalueDifferenceCutoff) {
                return true;
            } else {
                return false;
            }
        }
    }
}
