package blast;

/**
 * //Todo: document
 */

public class TUITCutoffSet {
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
    public static TUITCutoffSet newDefaultInstance(final double pIdentCutoff, final double querryCoverageCutoff, final double evalueDifferenceCutoff){
        return new TUITCutoffSet(pIdentCutoff,querryCoverageCutoff,evalueDifferenceCutoff);
    }
}

