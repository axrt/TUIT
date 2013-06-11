package blast;

import BLAST.NCBI.output.Hit;
import format.BadFromatException;
import helper.Ranks;
import taxonomy.TaxonomicNode;
import util.BlastOutputUtil;

/**
 * This class represents a convenience wrapper for a {@link Hit},
 * it creates a distinction from the contained {@link Hit} by using
 * numeric and thereby useful and comparable values for pIdent,
 * E-value and Query coverage. Moreover, it stores the {@link Hit}'s
 * taxonomy in an appropriate field.
 */
//Todo: document

public class NormalizedHit {

    /**
     * An ancestor hit that was normalized to have convenient parameters
     */
    protected final Hit hit;
    /**
     * An int representation of the GI
     */
    protected final int GI;
    /**
     * Double representation of the pIdent
     */
    protected final double pIdent;
    /**
     * Double representation of the Quer coverage
     */
    protected final double hitQueryCoverage;
    /**
     * Double representation of the E-value
     */
    protected final double hitEvalue;
    /**
     * A taxonomic tree reassembled back form the gi_taxid pair
     */
    protected TaxonomicNode taxonomy;
    /**
     * A node that is currently being in focus (refers to taxonomic rank)
     */
    protected TaxonomicNode focusNode;

    /**
     * A constructor from
     *
     * @param hit         {@link Hit} that need normalization
     * @param queryLength {@link int} of the initial query length (to derive Query coverage from)
     */
    protected NormalizedHit(final Hit hit, final int queryLength) throws BadFromatException {
        super();
        this.hit = hit;
        this.pIdent = BlastOutputUtil.calculatePIdent(hit);
        this.hitQueryCoverage = BlastOutputUtil.calculateQueryCoverage(queryLength, hit);
        this.hitEvalue = BlastOutputUtil.getEvalueFromHit(hit);
        this.GI=Integer.parseInt(BlastOutputUtil.extractGIFromHitID(hit.getHitId()));
    }

    /**
     * Assigned rank getter
     *
     * @return <{@link Ranks} assigned rank getter (rank is assigned in accordance to the GI)
     */
    public Ranks getAssignedRank() {
        return this.focusNode.getRank();
    }

    /**
     * Taxonomy setter
     *
     * @param taxonomy {@link TaxonomicNode} that will become the current taxonomy of the node
     */
    public void setTaxonomy(TaxonomicNode taxonomy) {
        this.taxonomy = taxonomy;
    }

    /**
     * Focus node getter
     *
     * @return {@link TaxonomicNode} getter for the node in focus
     */
    public TaxonomicNode getFocusNode() {
        return focusNode;
    }

    /**
     * Focus node setter
     *
     */
    public void setFocusNode(TaxonomicNode focusNode) {
        this.focusNode = focusNode;
    }

    /**
     * A getter for GI
     * @return {@link  int} the GI for the current hit
     */
    public int getGI() {
        return GI;
    }

    public double getpIdent() {
        return pIdent;
    }

    public double getHitQueryCoverage() {
        return hitQueryCoverage;
    }

    public double getHitEvalue() {
        return hitEvalue;
    }
    /**
     * This method is needed to check whether this hit may point to a taxid,
     * that is parent to the {@link NormalizedHit} candidate's in test taxid.
     * If it does point to a parental taxid, then the {@link NormalizedHit} candidate
     * is considered to be supported by a hit of a higher taxonomic rank.
     *
     * @param candidate {@link NormalizedHit} that is being tested
     * @return {@link true} if the {@link NormalizedHit} this hit points to a different
     *         taxonomic group, {@link false} if this hit point to a parental node.
     */
    public boolean refusesParenthood(NormalizedHit candidate) {
        if (this.taxonomy.isParentOf(candidate.getFocusNode().getTaxid())) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * A static factory from
     * @param hit         {@link Hit} that need normalization
     * @param queryLength {@link int} of the initial query length (to derive Query coverage from)
     * @return
     */
    public static NormalizedHit newDefaultInstance(final Hit hit, final int queryLength) throws BadFromatException {
        return new NormalizedHit(hit, queryLength);
    }


}
