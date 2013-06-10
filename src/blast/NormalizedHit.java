package blast;

import BLAST.NCBI.output.Hit;
import helper.Ranks;
import util.BlastOutputUtil;

import java.sql.Connection;

/**
 */
public class NormalizedHit {

    protected final Hit hit;
    protected final double pIdent;
    protected final double hitQueryCoverage;
    protected final Ranks assignedRank;
    protected final double hitEvalue;

    protected NormalizedHit(final Hit hit, final int queryLength, final Ranks assignedRank) {
        super();
        this.hit = hit;
        this.pIdent = BlastOutputUtil.calculatePIdent(hit);
        this.hitQueryCoverage = BlastOutputUtil.calculateQueryCoverage(queryLength, hit);
        this.assignedRank = assignedRank;
        this.hitEvalue = BlastOutputUtil.getEvalueFromHit(hit);
    }

   /* public static NormalizedHit newDefaultInstance(Connection connection, final Hit hit, final int queryLength){



        return new NormalizedHit(hit,queryLength);
    }*/

    public Ranks getAssignedRank() {
        return assignedRank;
    }
}
