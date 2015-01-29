package toolkit.silva;

import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 1/29/15.
 */
public class ConvertToGGFormatTest {

    @Test
    public void testConvert(){

        final Path toSilvaTaxFile = Paths.get("/home/alext/Documents/Research/Ocular/HCE/silva/Silva.nr_v119/silva.nr_v119.tax");
        final Path toReformattedFile=toSilvaTaxFile.resolveSibling(toSilvaTaxFile.toFile().getName()+".ggtax");

        try {
            ConvertToGGFormat.convert(toSilvaTaxFile,toReformattedFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

}
