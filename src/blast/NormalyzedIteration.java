package blast;

import BLAST.NCBI.output.Hit;
import BLAST.NCBI.output.Iteration;
import helper.Ranks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * //TODO: document
 */
public class NormalyzedIteration {

    protected final BLAST_Identifier blastIdentifier;
    protected final Iteration iteration;
    protected List<NormalyzedHit> normalizedHits;
    protected final int queryLength;

    /**
     * Current level of specification
     */
    protected Ranks currentRank;
    /**
     * A current candidate NormalyzedHit that may become the pivotal one
     */
    protected NormalyzedHit pivotalHit;

    public NormalyzedIteration(Iteration iteration, BLAST_Identifier blastIdentifier) {
        this.iteration = iteration;
        this.blastIdentifier = blastIdentifier;
        this.queryLength = Integer.parseInt(this.iteration.getIterationQueryLen());
    }

    protected void normalyzeHits() throws SQLException {

        if (this.normalizedHits == null) {

            this.normalizedHits = new ArrayList<NormalyzedHit>(this.iteration.getIterationHits().getHit().size());

            for (Hit hit : this.iteration.getIterationHits().getHit()) {
                NormalyzedHit normalizedHit = this.blastIdentifier.normalyzeHit(hit, this.queryLength);
                if (normalizedHit != null) {
                    this.normalizedHits.add(normalizedHit);
                }
            }
        }
    }

    protected void findLowestRank() {

        Ranks lowestRank = Ranks.no_rank;
        for (NormalyzedHit normalizedHit : this.normalizedHits) {
            if (normalizedHit.getAssignedRank().ordinal() > lowestRank.ordinal()) {
                this.currentRank = normalizedHit.getAssignedRank();
            }
        }
    }

    protected boolean couldLiftCurrentRank() {
        if (this.currentRank != Ranks.root_of_life) {
            this.currentRank = Ranks.previous(this.currentRank);
            return true;
        } else {
            return false;
        }
    }

    protected List<NormalyzedHit> gatherHitsAtCurrentRank() {
        int numberOfHitsThatQualify = 0;
        for (NormalyzedHit normalyzedHit : this.normalizedHits) {
            if (normalyzedHit.getAssignedRank().equals(this.currentRank)) {
                numberOfHitsThatQualify++;
            }
        }
        if (numberOfHitsThatQualify > 0) {
            List<NormalyzedHit> normalyzedHitsAtCurrentRank = new ArrayList<NormalyzedHit>(numberOfHitsThatQualify);
            for (NormalyzedHit normalyzedHit : this.normalizedHits) {
                if (normalyzedHit.getAssignedRank().equals(this.currentRank)) {
                    normalyzedHitsAtCurrentRank.add(normalyzedHit);
                }
            }
            return normalyzedHitsAtCurrentRank;
        } else {
            return null;
        }
    }

    protected List<NormalyzedHit> ensureNormalyzedHitsPassCutoffsAtCurrentRank(List<NormalyzedHit> normalizedHitsUnderTest) throws SQLException {
        if (normalizedHitsUnderTest != null && normalizedHitsUnderTest.size() > 0) {
            List<NormalyzedHit> esuredNormalyzedHits = new ArrayList<NormalyzedHit>(normalizedHitsUnderTest.size());
            for (NormalyzedHit normalyzedHit : normalizedHitsUnderTest) {
                if (this.blastIdentifier.normalyzedHitChecksAgainstParametersForRank(normalyzedHit, this.currentRank)) {
                    esuredNormalyzedHits.add(normalyzedHit);
                } else {
                    this.blastIdentifier.liftRankForNormalyzedHit(normalyzedHit);
                }
            }
            if (esuredNormalyzedHits.size() > 0) {
                return esuredNormalyzedHits;
            } else {
                return null;
            }

        } else {
            return null;
        }
    }

    protected List<NormalyzedHit> getNormalyzedHitsWithBetterEvalue() {
        if (this.pivotalHit != null) {
            List<NormalyzedHit> normalyzedHitsWithBetterEvalue = new ArrayList<NormalyzedHit>();

            for (NormalyzedHit normalyzedHit : this.normalizedHits) {
                if (!normalyzedHit.equals(this.pivotalHit)) {
                    normalyzedHitsWithBetterEvalue.add(normalyzedHit);
                } else {
                    break;
                }
            }
            if (normalyzedHitsWithBetterEvalue.size() > 0) {
                return normalyzedHitsWithBetterEvalue;
            } else {
                return null;
            }

        } else {
            return null;
        }

    }

    protected boolean normalyzedHitsWithBetterEvalueAllowPivotal() throws SQLException {
        List<NormalyzedHit> normalyzedHitsWithBetterEvalue;
        if ((normalyzedHitsWithBetterEvalue = this.getNormalyzedHitsWithBetterEvalue()) != null) {
            for (NormalyzedHit normalyzedHit : normalyzedHitsWithBetterEvalue) {
                if (normalyzedHit.deniesParenthood(this.pivotalHit)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    protected boolean couldSetPivotalHitAtCurrentRank() throws SQLException {

        List<NormalyzedHit> normalyzedHitsAtCurrentRank = this.ensureNormalyzedHitsPassCutoffsAtCurrentRank(this.gatherHitsAtCurrentRank());
        if (normalyzedHitsAtCurrentRank != null) {
            this.pivotalHit = normalyzedHitsAtCurrentRank.get(0);
            return true;
        } else {
            if (this.couldLiftCurrentRank()) {
                return this.couldSetPivotalHitAtCurrentRank();
            }
            return false;
        }

    }

    protected boolean normalyzedHitsWithWorseEvalueAllowPivotal() {
        return false;
    }

    protected void specify() throws SQLException {

        this.normalyzeHits();
        this.findLowestRank();

        while (couldSetPivotalHitAtCurrentRank()) {
            if (normalyzedHitsWithBetterEvalueAllowPivotal() && normalyzedHitsWithWorseEvalueAllowPivotal()) {
                //success
            }
        }
    }

}
