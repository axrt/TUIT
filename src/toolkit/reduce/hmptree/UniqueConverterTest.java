package toolkit.reduce.hmptree;

import org.junit.Test;

/**
 * Created by alext on 2/4/15.
 */
public class UniqueConverterTest {

    @Test
    public void test() {

        final String[] args = {"/home/alext/Documents/Research/Ocular/HCE/reprocessing2/cornea.unique.full.tuit.rest",
                "/home/alext/Documents/Research/Ocular/HCE/reprocessing2/cornea.names",
                "/home/alext/Documents/Research/Ocular/HCE/reprocessing2/cornea.groups"};

        UniqueConverter.main(args);


    }


}
