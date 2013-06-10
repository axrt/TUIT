package blast;

import BLAST.NCBI.output.Hit;
import helper.Ranks;
import taxonomy.TaxonomicNode;
import util.BlastOutputUtil;

/**
 */
public class NormalyzedHit {

    protected final Hit hit;
    protected final double pIdent;
    protected final double hitQueryCoverage;
    protected final double hitEvalue;
    protected TaxonomicNode taxonomy;
    protected TaxonomicNode focusNode;

    protected NormalyzedHit(final Hit hit, final int queryLength) {
        super();
        this.hit = hit;
        this.pIdent = BlastOutputUtil.calculatePIdent(hit);
        this.hitQueryCoverage = BlastOutputUtil.calculateQueryCoverage(queryLength, hit);
        this.hitEvalue = BlastOutputUtil.getEvalueFromHit(hit);
    }

   /* public static NormalyzedHit newDefaultInstance(Connection connection, final Hit hit, final int queryLength){



        return new NormalyzedHit(hit,queryLength);
    }*/

    public Ranks getAssignedRank() {
        return this.focusNode.getRank();
    }
    public void setTaxonomy(TaxonomicNode taxonomy){
        this.taxonomy=taxonomy;
    }

    public TaxonomicNode getFocusNode() {
        return focusNode;
    }
    public void setFocusNode(TaxonomicNode focusNode){
        this.focusNode=focusNode;
    }

    public boolean deniesParenthood(NormalyzedHit candidate){

        if(this.taxonomy.isParentOf(candidate.getFocusNode().getTaxid())){

        }


        return false;
    }
}
