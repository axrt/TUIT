package blast;

import BLAST.NCBI.output.Hit;
import BLAST.NCBI.output.Iteration;
import format.BadFromatException;
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
    protected List<NormalizedHit> normalizedHits;
    protected final int queryLength;

    /**
     * Current level of specification
     */
    protected Ranks currentRank;
    /**
     * A current candidate NormalizedHit that may become the pivotal one
     */
    protected NormalizedHit pivotalHit;

    public NormalyzedIteration(Iteration iteration, BLAST_Identifier blastIdentifier) {
        this.iteration = iteration;
        this.blastIdentifier = blastIdentifier;
        this.queryLength = Integer.parseInt(this.iteration.getIterationQueryLen());
    }

    protected void normalyzeHits() throws SQLException, BadFromatException {
        //Check if the costly procedure of Hits normalization has already been performed
        if (this.normalizedHits == null) {
            //If not yet - create a new list of the size of the list of hits
            this.normalizedHits = new ArrayList<NormalizedHit>(this.iteration.getIterationHits().getHit().size());
            //For every hit on the hit list
            for (Hit hit : this.iteration.getIterationHits().getHit()) {
                //Create a normalized version and store in the newly created list
                NormalizedHit normalizedHit = this.blastIdentifier.normalyzeHit(hit, this.queryLength);
                //The hit may be returned as null upon errors and inability of the blastIdentifier module to process the request
                if (normalizedHit != null) {
                    this.normalizedHits.add(normalizedHit);
                }
            }
        }
    }

    protected void findLowestRank() {

        Ranks lowestRank = Ranks.root_of_life;
        for (NormalizedHit normalizedHit : this.normalizedHits) {
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

    /**
     * Creates a list of hits that display GIs that point to the current taxonomic rank
     * @return {@link List<NormalizedHit>} of normalized hits at current taxonomic rank
     */
    protected List<NormalizedHit> gatherHitsAtCurrentRank() {
        //First - count how many hits with this rank exist
        int numberOfHitsThatQualify = 0;
        for (NormalizedHit normalizedHit : this.normalizedHits) {
            if (normalizedHit.getAssignedRank().equals(this.currentRank)) {
                numberOfHitsThatQualify++;
            }
        }
        //Knowing the exact number - create a list of exactly the needed size
        if (numberOfHitsThatQualify > 0) {
            List<NormalizedHit> normalizedHitsAtCurrentRank = new ArrayList<NormalizedHit>(numberOfHitsThatQualify);
            for (NormalizedHit normalizedHit : this.normalizedHits) {
                //and then put all matching normalized hits into the list
                if (normalizedHit.getAssignedRank().equals(this.currentRank)) {
                    normalizedHitsAtCurrentRank.add(normalizedHit);
                }
            }
            return normalizedHitsAtCurrentRank;
        } else {
            return null;
        }
    }

    /**
     * Ensures that all of the hits from a given {@link List<NormalizedHit>} pass the cutoffs threshold at the current level of taxonomic identification.
     * @param normalizedHitsUnderTest{@link List<NormalizedHit>} of normalized hits to test
     * @return {@link List<NormalizedHit>} of only those normalized hits that have passed
     * @throws SQLException in case a communication error occurs during the database interaction
     */
    protected List<NormalizedHit> ensureNormalyzedHitsPassCutoffsAtCurrentRank(List<NormalizedHit> normalizedHitsUnderTest) throws SQLException {
        //First check the incoming list for null and for that at least one normalized hit exists there
        if (normalizedHitsUnderTest != null && normalizedHitsUnderTest.size() > 0) {
            //Create a new list to hold those hits that have passed the cutoffs assuming in an optimistic way that all of the
            //hits will pass and a list of the same size will be needed
            List<NormalizedHit> esuredNormalizedHits = new ArrayList<NormalizedHit>(normalizedHitsUnderTest.size());
            for (NormalizedHit normalizedHit : normalizedHitsUnderTest) {
                if (this.blastIdentifier.normalyzedHitChecksAgainstParametersForRank(normalizedHit, this.currentRank)) {
                    esuredNormalizedHits.add(normalizedHit);
                } else {
                    this.blastIdentifier.liftRankForNormalyzedHit(normalizedHit);
                }
            }
            if (esuredNormalizedHits.size() > 0) {
                return esuredNormalizedHits;
            } else {
                return null;
            }

        } else {
            return null;
        }
    }

    protected List<NormalizedHit> getNormalyzedHitsWithBetterEvalue() {
        if (this.pivotalHit != null) {
            List<NormalizedHit> normalizedHitsWithBetterEvalue = new ArrayList<NormalizedHit>();

            for (NormalizedHit normalizedHit : this.normalizedHits) {
                if (!normalizedHit.equals(this.pivotalHit)) {
                    normalizedHitsWithBetterEvalue.add(normalizedHit);
                } else {
                    break;
                }
            }
            if (normalizedHitsWithBetterEvalue.size() > 0) {
                return normalizedHitsWithBetterEvalue;
            } else {
                return null;
            }

        } else {
            return null;
        }

    }

    protected boolean normalyzedHitsWithBetterEvalueAllowPivotal() throws SQLException {
        List<NormalizedHit> normalizedHitsWithBetterEvalue;
        if ((normalizedHitsWithBetterEvalue = this.getNormalyzedHitsWithBetterEvalue()) != null) {
            for (NormalizedHit normalizedHit : normalizedHitsWithBetterEvalue) {
                if (normalizedHit.refusesParenthood(this.pivotalHit)) {
                    return false;
                }
            }
        } else {
            return false;
        }
        return true;
    }

    protected boolean couldSetPivotalHitAtCurrentRank() throws SQLException {

        List<NormalizedHit> normalizedHitsAtCurrentRank = this.ensureNormalyzedHitsPassCutoffsAtCurrentRank(this.gatherHitsAtCurrentRank());
        if (normalizedHitsAtCurrentRank != null) {
            this.pivotalHit = normalizedHitsAtCurrentRank.get(0);
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

    protected void specify() throws SQLException, BadFromatException {

        this.normalyzeHits();
        this.findLowestRank();

        while (couldSetPivotalHitAtCurrentRank()) {
            if (normalyzedHitsWithBetterEvalueAllowPivotal() && normalyzedHitsWithWorseEvalueAllowPivotal()) {
                //success
            }
        }
    }

}