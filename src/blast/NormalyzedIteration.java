package blast;

import BLAST.NCBI.output.Hit;
import BLAST.NCBI.output.Iteration;
import db.connect.DatabaseOperator;
import helper.Ranks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * //TODO: document
 */
public class NormalyzedIteration {

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

    public NormalyzedIteration(Iteration iteration) {
        this.iteration = iteration;
        this.queryLength = Integer.parseInt(this.iteration.getIterationQueryLen());
    }

    protected void normalyzeHits(DatabaseOperator operator) throws SQLException {

        if (this.normalizedHits == null) {

            this.normalizedHits = new ArrayList<NormalizedHit>(this.iteration.getIterationHits().getHit().size());

            for (Hit hit : this.iteration.getIterationHits().getHit()) {
                NormalizedHit normalizedHit = operator.normalyzeHit(hit, this.queryLength);
                if (normalizedHit != null) {
                    this.normalizedHits.add(normalizedHit);
                }
            }
        }
    }

    protected Ranks findLowestRank(){

        Ranks lowestRank=Ranks.no_rank;
        for(NormalizedHit normalizedHit:this.normalizedHits){
            if(normalizedHit.getAssignedRank().ordinal()>lowestRank.ordinal()){
                lowestRank= normalizedHit.getAssignedRank();
            }
        }
        return lowestRank;
    }

}
