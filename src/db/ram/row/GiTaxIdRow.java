package db.ram.row;

/**
 * Created by alext on 2/12/14.
 */
//TODO: document
public class GiTaxIdRow extends RamRow<Integer,Integer> {

    public GiTaxIdRow(Integer integer, Integer integer2) {
        super(integer, integer2);
    }

    public static GiTaxIdRow newInstance(Integer gi, Integer taxid){
        return new GiTaxIdRow(gi,taxid);
    }
}
