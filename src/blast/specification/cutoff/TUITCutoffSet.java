package blast.specification.cutoff;

import blast.ncbi.output.Hsp;
import blast.normal.hit.NormalizedHit;
import logger.Log;
import org.apache.commons.math3.stat.inference.TestUtils;

import java.util.logging.Level;


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
 * A representation of a set of cutoffs. As long as every {@link blast.normal.hit.NormalizedHit} has to meet some certain requirements to qualify
 * as a pivotal hit for the taxonomic identification, this class helps to check the hits pIdent, query coverage and the E-value
 * ratio between a hit and any other hit within the specification list.
 */

public class TUITCutoffSet {
    /**
     * A symbol that BLAST uses to mark a gap
     */
    private static final char gap = '-';
    /**
     * A cutoff for pIdent
     */
    @SuppressWarnings("WeakerAccess")
    protected final double pIdentCutoff;
    /**
     * A cutoff for query coverage
     */
    @SuppressWarnings("WeakerAccess")
    protected final double queryCoverageCutoff;
    /**
     * A cutoff for the E-value ratio
     */
    @SuppressWarnings("WeakerAccess")
    protected final double alpha;

    /**
     * A protected constructor for the use via factories
     *
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param queryCoverageCutoff    {@code double} A cutoff for query coverage
     * @param alpha {@code double} alpha cutoff for the p-value
     */
    @SuppressWarnings("WeakerAccess")
    protected TUITCutoffSet(final double pIdentCutoff, final double queryCoverageCutoff, final double alpha) {
        this.pIdentCutoff = pIdentCutoff;
        this.queryCoverageCutoff = queryCoverageCutoff;
        this.alpha = alpha;
    }

    /**
     * A getter for the percent Identity cutoff
     * @return {@code double} percent identity cutoff
     */
    public double getpIdentCutoff() {
        return pIdentCutoff;
    }

    /**
     * A getter for the query coverage cutoff
     * @return {@code double} query coverage cutoff
     */
    public double getQueryCoverageCutoff() {
        return queryCoverageCutoff;
    }

    /**
     * A getter for the alpha level cutoff
     * @return {@code double} alpha level of the p-value cutoff
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * As long as BLAST outputs treats every gap as an independent event, this method allows to correct for this by calculating gapopen.
     * A standard BLAST XMP output reports only gaps, but not gapopens. Note: gaps>=gapopens
     *
     * @param seq {@link String} Nucleotide sequence with gaps, marked as '-'
     * @return {@code int} number of gapopens
     */
    public static int calculateNumberOfGapOpens(final String seq) {

        int numGapOpen = 0;
        for (int i = 1; i < seq.length(); i++) {
            if (seq.charAt(i) == gap) {
                if (seq.charAt(i - 1) != gap) {
                    numGapOpen++;
                }
            }
        }
        return numGapOpen;
    }

    /**
     * A static factory that returns a new instance of the {@link TUITCutoffSet}
     *
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param queryCoverageCutoff    {@code double} A cutoff for query coverage
     * @param evalueDifferenceCutoff {@code double} A cutoff for E-value ratio
     * @return a new instance of {@link TUITCutoffSet} from the given parameters
     */
    public static TUITCutoffSet newDefaultInstance(final double pIdentCutoff, final double queryCoverageCutoff, final double evalueDifferenceCutoff) {
        return new TUITCutoffSet(pIdentCutoff, queryCoverageCutoff, evalueDifferenceCutoff);
    }

    /**
     * Checks whether the given {@link blast.normal.hit.NormalizedHit} passes the cutoffs for pIdent and query coverage
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} that is being checked
     * @return {@code true} if the {@link blast.normal.hit.NormalizedHit} passes checks, {@code false} otherwise or if a pointer
     *         to {@code null} was given
     */
    public boolean normalizedHitPassesCheck(final NormalizedHit normalizedHit) {
        return !(normalizedHit == null
                || normalizedHit.getpIdent() < this.pIdentCutoff
                || normalizedHit.getHitQueryCoverage() < this.queryCoverageCutoff);
    }

    /**
     * Checks whether the given {@link NormalizedHit}s E-value ratio is higher than the cutoff value
     *
     * @param oneNormalizedHit     {@link NormalizedHit} (assuming the hit with a worse (higher) e-value)
     * @param anotherNormalizedHit {@link NormalizedHit} (assuming the hit with a better (lower) e-value)
     * @return {@code true} Performs a Chi Squared test on a contingency table <br>
     *         <table class="tg-table-plain">
     *         <tr>
     *         <th></th>
     *         <th>Better hit</th>
     *         <th>Other hit</th>
     *         <th>Sum</th>
     *         </tr>
     *         <tr class="tg-even">
     *         <td>Corrected Match</td>
     *         <td>Number of identities</td>
     *         <td>Number of identities</td>
     *         <td></td>
     *         </tr>
     *         <tr>
     *         <td>Corrected Mismatch</td>
     *         <td>Align.length-(Total Gaps - Gapopen)</td>
     *         <td>Align.length-(Total Gaps - Gapopen)</td>
     *         <td></td>
     *         </tr>
     *         <tr class="tg-even">
     *         <td>Sum</td>
     *         <td></td>
     *         <td></td>
     *         <td></td>
     *         </tr>
     *         </table>
     *         , returns true if the test has shown the statistical significant prevalence of one alignment over another
     *         , {@code false} otherwise or if either of the pointers
     *         point to {@code null}
     */
    public boolean hitsAreStatisticallyDifferent(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit) {

        int oneAlignLen=0;
        for(Hsp hsp:oneNormalizedHit.getHit().getHitHsps().getHsp()){
            oneAlignLen+=Integer.valueOf(hsp.getHspAlignLen());
        }

        int anotherAlignLen =0;
        for(Hsp hsp:anotherNormalizedHit.getHit().getHitHsps().getHsp()){
            anotherAlignLen+=Integer.valueOf(hsp.getHspAlignLen());
        }

        int oneNumIdents = 0;
        for(Hsp hsp:oneNormalizedHit.getHit().getHitHsps().getHsp()){
            oneNumIdents+=Integer.valueOf(hsp.getHspIdentity());
        }

        int anotherNumIdents = 0;
        for(Hsp hsp:anotherNormalizedHit.getHit().getHitHsps().getHsp()){
            anotherNumIdents+=Integer.valueOf(hsp.getHspIdentity());
        }

        int oneNumGaps = 0;
        for(Hsp hsp:oneNormalizedHit.getHit().getHitHsps().getHsp()){
            oneNumGaps+=Integer.valueOf(hsp.getHspGaps());
        }

        int anotherNumGaps = 0;
        for(Hsp hsp:anotherNormalizedHit.getHit().getHitHsps().getHsp()){
            anotherNumGaps+=Integer.valueOf(hsp.getHspGaps());
        }

        int oneNumGapOpens = 0;
        for(Hsp hsp:oneNormalizedHit.getHit().getHitHsps().getHsp()){
            oneNumGapOpens+=calculateNumberOfGapOpens(hsp.getHspQseq())
                    + calculateNumberOfGapOpens(hsp.getHspHseq());
        }

        int anotherNumGapOpens = 0;
        for(Hsp hsp:anotherNormalizedHit.getHit().getHitHsps().getHsp()){
            anotherNumGapOpens+=calculateNumberOfGapOpens(hsp.getHspQseq())
                    + calculateNumberOfGapOpens(hsp.getHspHseq());
        }

        return TestUtils.chiSquareTest(new long[][]{{oneNumIdents, anotherNumIdents},
                {(oneAlignLen - (oneNumGaps - oneNumGapOpens)) - oneNumIdents,
                        (anotherAlignLen - (anotherNumGaps - anotherNumGapOpens)) - anotherNumIdents}},this.alpha);
    }
}

