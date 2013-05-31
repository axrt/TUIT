package test;
import db.mysqlwb.tables.LookupNames;
import org.junit.Test;
/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 5/31/13
 * Time: 12:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class LookupNamesTest {

    @Test
    public void testNameSpaces(){
       System.out.println(String.valueOf(LookupNames.dbs.NCBI.ranks.columns.id_ranks.getDeclaringClass()));
    }
}
