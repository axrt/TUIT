package blast.specification.cutoff;

import org.apache.commons.math3.stat.inference.TestUtils;
import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 11/1/13
 * Time: 4:35 PM
 * To change this template use File | Settings | File Templates.
 */
public class TUITCutoffSetTest {
    //TODO move this to tests outside
    @Test
    public void test(){
       System.out.println(

               TestUtils.chiSquare(new long[][]{{1430,1514},{87,0}})

       );
    }
}
