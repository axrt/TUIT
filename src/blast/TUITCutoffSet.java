package blast;

import blast.normal.hit.NormalizedHit;

/**
 * A representation of a set of cutoffs. As long as every {@link blast.normal.hit.NormalizedHit} has to meet some certain requirments to qualify
 * as a pivotal hit for the taxonomic identification, this class helps to check the hits pIdent, query coverage and the E-value
 * ratio between a hit and any other hit within the specification list.
 */

public class TUITCutoffSet {
    /**
     * A cutoff for pIdent
     */
    protected final double pIdentCutoff;
    /**
     * A cutoff for query coverage
     */
    protected final double querryCoverageCutoff;
    /**
     * A cutoff for the E-value ratio
     */
    protected final double evalueDifferenceCutoff;

    /**
     * A protected construcotor for the use via factories
     *
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param querryCoverageCutoff   {@code double} A cutoff for query coverage
     * @param evalueDifferenceCutoff {@code double} A cutoff for E-value ratio
     */
    protected TUITCutoffSet(final double pIdentCutoff, final double querryCoverageCutoff, final double evalueDifferenceCutoff) {
        this.pIdentCutoff = pIdentCutoff;
        this.querryCoverageCutoff = querryCoverageCutoff;
        this.evalueDifferenceCutoff = evalueDifferenceCutoff;
    }

    /**
     * Checks whether the given {@link blast.normal.hit.NormalizedHit} passes the cutoffs for pIdent and query coverage
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} that is being checked
     * @return {@code true} if the {@link blast.normal.hit.NormalizedHit} passes checks, {@code false} otherwise or if a pointer
     *         to {@code null} was given
     */
    public boolean normalizedHitPassesCheck(final NormalizedHit normalizedHit) {
        if (normalizedHit == null
                || normalizedHit.getpIdent() < this.pIdentCutoff
                || normalizedHit.getHitQueryCoverage() < this.querryCoverageCutoff) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Checks whether the given {@link NormalizedHit}s E-value ratio is higher than the cutoff value
     * @param oneNormalizedHit {@link NormalizedHit} (assuming the hit with a worse (higher) e-value)
     * @param anotherNormalizedHit {@link NormalizedHit} (assuming the hit with a better (lower) e-value)
     * @return {@code true} if the E-value ratio is high enough, {@code false} otherwise or if either of the pointers
     *         point to {@code null}
     */
    public boolean hitsAreFarEnoughByEvalue(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit) {
        if (oneNormalizedHit==null||anotherNormalizedHit==null) {
            return false;
        } else if(oneNormalizedHit.getHitEvalue() / anotherNormalizedHit.getHitEvalue() >= this.evalueDifferenceCutoff){
            return true;
        } else {
            return false;
        }
    }

    /**
     * A static factory that returns a new instance of the {@link TUITCutoffSet}
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param querryCoverageCutoff   {@code double} A cutoff for query coverage
     * @param evalueDifferenceCutoff {@code double} A cutoff for E-value ratio
     * @return a new instance of {@link TUITCutoffSet} from the given parameters
     */
    public static TUITCutoffSet newDefaultInstance(final double pIdentCutoff, final double querryCoverageCutoff, final double evalueDifferenceCutoff) {
        return new TUITCutoffSet(pIdentCutoff, querryCoverageCutoff, evalueDifferenceCutoff);
    }
}

