package db.ram.row;

/**
 * Created by alext on 2/12/14.
 */
//TODO: document
public class NamesRow extends RamRow<Integer,String> {

    protected NamesRow(Integer integer, String s) {
        super(integer, s);
    }

    public static NamesRow newInstance(Integer taxid, String scientificName){
        return new NamesRow(taxid,scientificName);
    }
}
