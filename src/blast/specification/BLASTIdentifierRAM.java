package blast.specification;

import blast.normal.hit.NormalizedHit;
import blast.specification.cutoff.TUITCutoffSet;
import com.ice.tar.tar;
import db.ram.RamDb;
import db.ram.row.NodesRow;
import format.fasta.nucleotide.NucleotideFasta;
import io.file.TUITFileOperator;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Created by alext on 2/13/14. TODO: document
 */
public abstract class BLASTIdentifierRAM extends BLASTIdentifier<NucleotideFasta> {

    protected final RamDb ramDb;

    protected BLASTIdentifierRAM(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList,
                                 TUITFileOperator identifierFileOperator, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize, boolean cleanup, final RamDb ramDb) {
        super(query, tempDir, executive, parameterList, identifierFileOperator, cutoffSetMap, batchSize, cleanup);
        this.ramDb = ramDb;
    }

    @Override
    public NormalizedHit assignTaxonomy(NormalizedHit normalizedHit) throws Exception {
        final Integer taxid = this.ramDb.getTaxIdByGi(normalizedHit.getGI());
        if (taxid == null) {
            return null;
        }
        final String scientificName = this.ramDb.getNameByTaxId(taxid);
        if (scientificName == null) {
            return null;
        }
        final Ranks rank = this.ramDb.getRankByTaxId(taxid);
        if (rank == null) {
            return null;
        }

        final TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(taxid, rank, scientificName);
        normalizedHit.setTaxonomy(taxonomicNode);
        normalizedHit.setFocusNode(taxonomicNode);
        return normalizedHit;
    }

    @Override
    public NormalizedHit liftRankForNormalizedHit(NormalizedHit normalizedHit) throws Exception {

        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(normalizedHit.getAssignedTaxid());
        if(nodesRow==null){
            return null;
        }
        final NodesRow parentNodesRow=this.ramDb.getNodeByTaxId(nodesRow.getV());
        if(parentNodesRow==null){
            return null;
        }
        final String scientificName=this.ramDb.getNameByTaxId(parentNodesRow.getK());
        if(scientificName==null){
            return null;
        }
        final TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(parentNodesRow.getK(),parentNodesRow.getRank(),scientificName);
        taxonomicNode.addChild(normalizedHit.getFocusNode());
        normalizedHit.setTaxonomy(taxonomicNode);
        normalizedHit.setFocusNode(taxonomicNode);

        return normalizedHit;
    }

    @Override
    public TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode taxonomicNode) throws Exception {

        //Currently not used
        return null;
    }

    @Override
    public boolean isParentOf(int parentTaxid, int taxid) throws Exception {
        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(taxid);
        if(nodesRow==null){
            return false;
        }
        return parentTaxid == nodesRow.getV()|| nodesRow.getV()!= 1 && this.isParentOf(parentTaxid, nodesRow.getV());
    }

    @Override
    public TaxonomicNode attachFullDirectLineage(TaxonomicNode taxonomicNode) throws Exception {
        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(taxonomicNode.getTaxid());
        final NodesRow parentNodesRow=this.ramDb.getNodeByTaxId(nodesRow.getV());
        if(parentNodesRow==null){
            return null;
        }
        final String scientificName=this.ramDb.getNameByTaxId(parentNodesRow.getK());
        if(scientificName==null){
            return null;
        }
        TaxonomicNode parentTaxonomicNode = TaxonomicNode.newDefaultInstance(parentNodesRow.getK(),parentNodesRow.getRank(), scientificName);
        parentTaxonomicNode.addChild(taxonomicNode);
        taxonomicNode.setParent(parentTaxonomicNode);
        if (parentNodesRow.getK()!=parentNodesRow.getV()) {
            parentTaxonomicNode = this.attachFullDirectLineage(parentTaxonomicNode);
        }
        return taxonomicNode;
    }

    @Override
    public boolean hitHasANoRankParent(NormalizedHit normalizedHit) throws Exception {
        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(normalizedHit.getAssignedTaxid());
        if(nodesRow==null){
            return false;
        }
        return nodesRow.getRank()==Ranks.no_rank;
    }
}
