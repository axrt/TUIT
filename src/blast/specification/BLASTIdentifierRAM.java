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
 * An abstraction that uses a RAM-based taxonomic database.
 */
public abstract class BLASTIdentifierRAM extends BLASTIdentifier<NucleotideFasta> {

    /**
     * A pointer to the RAM-based taxonoimic database taxonomic database
     */
    protected final RamDb ramDb;
    /**
     * A protected constructor.
     *
     * @param query                  {@link List} a list of query
     *                               fasta-formatted records
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String}[] A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     *
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     */
    protected BLASTIdentifierRAM(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList,
                                 TUITFileOperator identifierFileOperator, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize, boolean cleanup, final RamDb ramDb) {
        super(query, tempDir, executive, parameterList, identifierFileOperator, cutoffSetMap, batchSize, cleanup);
        this.ramDb = ramDb;
    }

    /**
     * Based on the RAM-based taxonomic database and the {@link taxonomy.Ranks} of the given {@link taxonomy.node.TaxonomicNode}, assigned the taxonomy (taxid, scientific name)
     * **does not assign children down to the leaves, that is done by the {@code attachChildrenForTaxonomicNode(TaxonomicNode parentNode)}
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} which needs to know its taxonomy
     * @return {@link blast.normal.hit.NormalizedHit} which points to the same object as the given {@link blast.normal.hit.NormalizedHit} parameter, but with a newly attached
     * {@link taxonomy.node.TaxonomicNode}
     */
    @Override
    public NormalizedHit assignTaxonomy(NormalizedHit normalizedHit){
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

    /**
     * Based on the RAM taxonomic database and the {@link Ranks} of the given {@link NormalizedHit}
     * rises its rank one step higher (say, for subspecies raised for species)
     *
     * @param normalizedHit {@link NormalizedHit}
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter,
     * but with one step risen {@code Ranks}
     */
    @Override
    public NormalizedHit liftRankForNormalizedHit(NormalizedHit normalizedHit) {

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

    /**
     * This method is no longer used, however, was supposed to assign a full subtree for a given taxonomic node
     * @param taxonomicNode
     * @return {@code null}
     */
    @Override
    public TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode taxonomicNode){
        //Currently not used
        return null;
    }

    /**
     * Checks whether a given parent taxid is indeed a parent taxid for the given one, as well as it checks
     * whether the parent taxid may be a sibling taxid for the given. Used to check for if the normalized hits with
     * better E-value restrict the chosen pivotal hit.
     *
     * @param parentTaxid a taxid of a {@link TaxonomicNode} that should be a parent to the given taxid in order to
     *                    support the choice of the pivotal normalized hit
     * @param taxid       of the {@link TaxonomicNode} of the pivotal taxid
     * @return {@code true} if the parent taxid is indeed parent (direct parent or grand parent within the lineage) of
     * the given taxid, {@code false} otherwise.
     * @throws java.sql.SQLException in case a database communication error occurs
     */
    @Override
    public boolean isParentOf(int parentTaxid, int taxid) {
        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(taxid);
        if(nodesRow==null){
            return false;
        }
        return parentTaxid == nodesRow.getV() || nodesRow.getV()!= 1 && this.isParentOf(parentTaxid, nodesRow.getV());
    }

    /**
     * For a given {@link TaxonomicNode} attaches its parent and higher lineage structure. Originally used to
     * save results in a form of a taxonomic branch.
     *
     * @param taxonomicNode {@link TaxonomicNode} that needs to get its full lineage structure
     * @return a pointer to the same {@link TaxonomicNode} object, but with attached pointers to its taxonomic lineage
     */
    @Override
    public TaxonomicNode attachFullDirectLineage(TaxonomicNode taxonomicNode) {
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

    /**
     * Allows to reduce those hits, which have a no_rank parent (such as unclassified Bacteria)
     *
     * @param normalizedHit {@link NormalizedHit}
     * @return {@code true} if a given Hit has a no_rank parent, {@code false} otherwise
     */
    @Override
    public boolean hitHasANoRankParent(NormalizedHit normalizedHit) {
        final NodesRow nodesRow=this.ramDb.getNodeByTaxId(normalizedHit.getAssignedTaxid());
        if(nodesRow==null){
            return false;
        }
        return nodesRow.getRank()==Ranks.no_rank;
    }
}
