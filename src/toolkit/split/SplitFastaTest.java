package toolkit.split;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 1/23/15.
 */
public class SplitFastaTest {
    @Test
    public void test(){
        final Path toFile= Paths.get("/home/alext/Documents/Research/brain_rnaseq/SRP005169/single/taxonomy/16/16.nongenome.fasta.rdc");
        try {
            SplitFasta.simpleSplit(toFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
