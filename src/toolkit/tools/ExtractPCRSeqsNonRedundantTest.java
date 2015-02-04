package toolkit.tools;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 9/19/14.
 * TODO document class
 */
public class ExtractPCRSeqsNonRedundantTest {


    @Test
    public void testReduce(){
        final Path toFile= Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/LTPs119_SSU.compressed.fasta.recode.pcr.align");
        final Path toOutFile=toFile.resolveSibling("ltp.custom");
        final Path toOutDB=Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/ramdb.obj");
        try {
            ExtractPCRSeqsNonRedundant.reduce(toFile,toOutFile,toOutDB);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
