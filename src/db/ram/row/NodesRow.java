package db.ram.row;

import taxonomy.Ranks;

/**
 * Created by alext on 2/12/14.
 */

//TODO: document
public class NodesRow extends RamRow<Integer,Integer> {

    protected final Ranks rank;

    protected NodesRow(Integer taxid, Integer partentTaxid, Ranks rank) {
        super(taxid, partentTaxid);
        this.rank = rank;
    }

    public Ranks getRank() {
        return rank;
    }

    public static NodesRow newInstance(Integer taxid, Integer partentTaxid, Ranks rank){
        return new NodesRow(taxid,partentTaxid,rank);
    }
}
