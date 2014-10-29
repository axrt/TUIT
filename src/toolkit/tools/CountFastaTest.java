package toolkit.tools;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 9/18/14.
 * TODO document class
 */
public class CountFastaTest {

    @Test
    public void testCount(){

        final Path toFile= Paths.get("/home/alext/Documents/tuit/greengenes/gg_13_5.fasta");
        try {
            System.out.println(CountFasta.count(toFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
