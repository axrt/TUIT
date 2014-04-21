package blast.normal.hit;

import blast.ncbi.output.Hit;
import format.BadFormatException;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;
import util.BlastOutputUtil;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
/**
 * This class represents a convenience wrapper for a {@link Hit},
 * it creates a distinction from the contained {@link Hit} by using
 * numeric and thereby useful and comparable values for pIdent,
 * E-value and Query coverage. Moreover, it stores the {@link Hit}'s
 * taxonomy in an appropriate field.
 */

public class NormalizedHit<H extends Hit> {

    /**
     * A minimal E-value at which the E-value is rounded to 0.0
     */
    @SuppressWarnings("WeakerAccess")
    public static final double MINIMAL_EVLAUE = 2.225074e-308;
    /**
     * An ancestor hit that was normalized to have convenient parameters
     */
    @SuppressWarnings("WeakerAccess")
    protected final H hit;
    /**
     * An int representation of the GI
     */
    @SuppressWarnings("WeakerAccess")
    protected final int GI;
    /**
     * Double representation of the pIdent
     */
    @SuppressWarnings("WeakerAccess")
    protected final double pIdent;
    /**
     * Double representation of the Query coverage
     */
    @SuppressWarnings("WeakerAccess")
    protected final double hitQueryCoverage;
    /**
     * Double representation of the E-value
     */
    @SuppressWarnings("WeakerAccess")
    protected final double hitEvalue;
    /**
     * A taxonomic tree reassembled back form the gi_taxid pair
     */
    @SuppressWarnings("WeakerAccess")
    protected TaxonomicNode taxonomy;
    /**
     * A node that is currently being in focus (refers to taxonomic rank)
     */
    @SuppressWarnings("WeakerAccess")
    protected TaxonomicNode focusNode;

    /**
     * A protected constructor from a given set of parameters
     *
     * @param hit         {@link Hit} that need normalization. If a given {@link Hit} has the E-value as 0.0 -
     *                    the minimal value of 2.225074e-308 will be assigned
     * @param queryLength {@code int} of the initial query length (to derive Query coverage from)
     */
    @SuppressWarnings("WeakerAccess")
    protected NormalizedHit(final H hit, final int queryLength) throws BadFormatException {
        super();
        this.hit = hit;
        this.pIdent = BlastOutputUtil.calculatePIdent(hit);
        this.hitQueryCoverage = BlastOutputUtil.calculateQueryCoverage(queryLength, hit);
        double eval = BlastOutputUtil.getEvalueFromHit(hit);
        //If the E-value was rounded to 0.0, change it to the minimal value so that the
        //E-value with any other hit could be told by division
        if (eval == 0) {
            this.hitEvalue = NormalizedHit.MINIMAL_EVLAUE;
        } else {
            this.hitEvalue = eval;
        }
        this.GI = Integer.parseInt(BlastOutputUtil.extractGIFromHitID(hit.getHitId()));
    }

    /**
     * Assigned rank getter
     *
     * @return {@link Ranks} assigned rank getter (rank is assigned in accordance to the GI)
     */
    public Ranks getAssignedRank() {
        return this.focusNode.getRank();
    }

    /**
     * A getter for the current hit taxid (at the current rank)
     *
     * @return {@code int} taxid
     */
    public int getAssignedTaxid() {
        return this.focusNode.getTaxid();
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
     */
    public void setFocusNode(TaxonomicNode focusNode) {
        this.focusNode = focusNode;
    }

    /**
     * A getter for GI
     *
     * @return {@code  int} the GI for the current hit
     */
    public int getGI() {
        return GI;
    }

    /**
     * A getter for the pIdent
     *
     * @return {@code double} pident
     */
    public double getpIdent() {
        return pIdent;
    }

    /**
     * A getter of the query coverage
     *
     * @return {@code double} query coverage
     */
    public double getHitQueryCoverage() {
        return hitQueryCoverage;
    }

    /**
     * A getter for the E-value
     *
     * @return {@code double} E-value
     */
    public double getHitEvalue() {
        return hitEvalue;
    }

    /**
     * Getter for {@code H}
     * @return {@code H}
     */
    public H getHit() {
        return hit;
    }

    /**
     * This method is needed to check whether this hit may point to a taxid,
     * that is parent to the {@link NormalizedHit} candidate's in test taxid.
     * If it does point to a parental taxid, then the {@link NormalizedHit} candidate
     * is considered to be supported by a hit of a higher taxonomic rank.
     *
     * @param candidate {@link NormalizedHit} that is being tested
     * @return {@code true} if the {@link NormalizedHit} this hit points to a different
     *         taxonomic group, {@code false} if this hit point to a parental node.
     */
    public boolean refusesParenthood(NormalizedHit candidate) {
        return !this.taxonomy.isParentOf(candidate.getFocusNode().getTaxid());
    }

    /**
     * A static factory from a given set of parameters
     *
     * @param hit         {@link Hit} that need normalization
     * @param queryLength {@code int} of the initial query length (to derive Query coverage from)
     * @return a new instance of {@link NormalizedHit} form a given set of parameters
     * @throws BadFormatException in case formatting the {@link Hit} GI fails
     */
    public static NormalizedHit newDefaultInstanceFromHit(final Hit hit, final int queryLength) throws BadFormatException {
        return new NormalizedHit<Hit>(hit, queryLength);

    }


}
