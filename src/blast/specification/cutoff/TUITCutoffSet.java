package blast.specification.cutoff;

import blast.normal.hit.NormalizedHit;

/**
 * A representation of a set of cutoffs. As long as every {@link blast.normal.hit.NormalizedHit} has to meet some certain requirements to qualify
 * as a pivotal hit for the taxonomic identification, this class helps to check the hits pIdent, query coverage and the E-value
 * ratio between a hit and any other hit within the specification list.
 */

public class TUITCutoffSet {
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
    protected final double evalueDifferenceCutoff;

    /**
     * A protected constructor for the use via factories
     *
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param queryCoverageCutoff   {@code double} A cutoff for query coverage
     * @param evalueDifferenceCutoff {@code double} A cutoff for E-value ratio
     */
    @SuppressWarnings("WeakerAccess")
    protected TUITCutoffSet(final double pIdentCutoff, final double queryCoverageCutoff, final double evalueDifferenceCutoff) {
        this.pIdentCutoff = pIdentCutoff;
        this.queryCoverageCutoff = queryCoverageCutoff;
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
        return !(normalizedHit == null
                || normalizedHit.getpIdent() < this.pIdentCutoff
                || normalizedHit.getHitQueryCoverage() < this.queryCoverageCutoff);
    }

    /**
     * Checks whether the given {@link NormalizedHit}s E-value ratio is higher than the cutoff value
     * @param oneNormalizedHit {@link NormalizedHit} (assuming the hit with a worse (higher) e-value)
     * @param anotherNormalizedHit {@link NormalizedHit} (assuming the hit with a better (lower) e-value)
     * @return {@code true} if the E-value ratio is high enough, {@code false} otherwise or if either of the pointers
     *         point to {@code null}
     */
    public boolean hitsAreFarEnoughByEvalue(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit) {
        return !(oneNormalizedHit == null || anotherNormalizedHit == null) && oneNormalizedHit.getHitEvalue() / anotherNormalizedHit.getHitEvalue() >= this.evalueDifferenceCutoff;
    }

    /**
     * A static factory that returns a new instance of the {@link TUITCutoffSet}
     * @param pIdentCutoff           {@code double} A cutoff for pIdent
     * @param queryCoverageCutoff   {@code double} A cutoff for query coverage
     * @param evalueDifferenceCutoff {@code double} A cutoff for E-value ratio
     * @return a new instance of {@link TUITCutoffSet} from the given parameters
     */
    public static TUITCutoffSet newDefaultInstance(final double pIdentCutoff, final double queryCoverageCutoff, final double evalueDifferenceCutoff) {
        return new TUITCutoffSet(pIdentCutoff, queryCoverageCutoff, evalueDifferenceCutoff);
    }
}

