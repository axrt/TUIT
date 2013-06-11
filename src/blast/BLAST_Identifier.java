package blast;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Hit;
import db.connect.DatabaseOperator;
import db.tables.LookupNames;
import format.BadFromatException;
import format.fasta.nucleotide.NculeotideFasta;
import helper.Ranks;
import taxonomy.TaxonomicNode;

import java.io.File;
import java.sql.*;
import java.util.List;
import java.util.Map;

/**
 * TODO: document
 */
public class BLAST_Identifier extends NCBI_EX_BLASTN implements DatabaseOperator {

    protected final Connection connection;
    protected final Map<Ranks, TUITCutoffSet> cutoffSetMap;

    protected BLAST_Identifier(List<? extends NculeotideFasta> query, List<String> query_IDs,
                               File tempDir, File executive, String[] parameterList,
                               Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        super(query, query_IDs, tempDir, executive, parameterList);
        this.connection = connection;
        this.cutoffSetMap=cutoffSetMap;
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean normalyzedHitChecksAgainstParametersForRank(NormalizedHit normalizedHit, Ranks ranks) {


        return false;
    }

    @Override
    public NormalizedHit normalyzeHit(Hit hit, int queryLength) throws SQLException, BadFromatException {

        //Create a raw NormalizedHit
        NormalizedHit normalizedHit = NormalizedHit.newDefaultInstance(hit, queryLength);
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
        } finally {
            //Close and cleanup
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        int taxid;
        Ranks rank;
        String scientificName;
        if (resultSet.next()) {
            taxid = resultSet.getInt(2);
            scientificName = resultSet.getString(3);
            rank = Ranks.values()[resultSet.getInt(5) - 1];
            TaxonomicNode initialNode = TaxonomicNode.newDefaultInstance(taxid, rank, scientificName);
            normalizedHit.setTaxonomy(initialNode);

        } else {
            return null;
        }
        if (resultSet != null) {
            resultSet.close();
        }

        //Set the nodes to the hits taxonomy field

        return normalizedHit;
    }

    @Override
    public NormalizedHit liftRankForNormalyzedHit(NormalizedHit hit) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
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

    public static BLAST_Identifier newDefaultInstance(List<? extends NculeotideFasta> query, List<String> query_IDs,
                                                      File tempDir, File executive, String[] parameterList,
                                                      Connection connection,Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        return new BLAST_Identifier(query, query_IDs, tempDir, executive, parameterList, connection,cutoffSetMap);
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