package db.ram;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * Created by alext on 2/12/14.
 */
//TODO: remove
public class RamDBTest {
    @Test
    public void test(){

        final File gi_taxid_dmp=new File("/home/alext/Downloads/tmp/gi_taxid_nucl_diff/gi_taxid_nucl_diff.dmp.mod");
        final File names_dmp=new File("/home/alext/Downloads/tmp/taxdump/names.dmp.mod");
        final File nodes_dmp=new File("/home/alext/Downloads/tmp/taxdump/nodes.dmp.mod");
        System.out.println(new Date());
        System.out.println(Runtime.getRuntime().totalMemory());
        try {
            final RamDb ramDb=RamDb.loadSelfFromFilteredNcbiFiles(gi_taxid_dmp,names_dmp,nodes_dmp);
            RamDb.serialize(ramDb,new File("/home/alext/Downloads/tmp/taxdump/ramdb.obj"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println(new Date());
        System.out.println(Runtime.getRuntime().totalMemory());

    }


}
