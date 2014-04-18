package db.ram;

import org.junit.Test;

import java.io.*;
import java.util.Date;

/**
 * Created by alext on 2/12/14.
 */
//TODO: remove
public class RamDBTest {
    //@Test
    public void test(){

        final File gi_taxid_dmp=new File("/home/alext/Downloads/tmp/gi_taxid_nucl_diff/gi_taxid_nucl_diff.dmp.mod");
        final File names_dmp=new File("/home/alext/Downloads/tmp/taxdump/names.dmp.mod");
        final File nodes_dmp=new File("/home/alext/Downloads/tmp/taxdump/nodes.dmp.mod");
        System.out.println(new Date());
        System.out.println(Runtime.getRuntime().totalMemory());
        try {
            final RamDb ramDb=RamDb.loadSelfFromFilteredNcbiFiles(gi_taxid_dmp,names_dmp,nodes_dmp);
            RamDb.serialize(ramDb,new File("/home/alext/Downloads/tmp/taxdump/ramdb.obj"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(new Date());
        System.out.println(Runtime.getRuntime().totalMemory());

    }

    @Test
    public void testGiTaxIdNumeration(){
        final File gi_taxid_dmp=new File("/home/alext/Downloads/tmp/gi_taxid_nucl/gi_taxid_nucl.dmp");
        String line;
        try(
                final BufferedReader bufferedReader=new BufferedReader(new FileReader(gi_taxid_dmp));
                ) {
            int count=0;
            System.out.println("Count: "+count);
            while((line=bufferedReader.readLine())!=null){
                //final String[]split=line.split("\t");
                count++;
            }
            System.out.println("Count: "+count);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
