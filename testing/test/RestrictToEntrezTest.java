package tuit.main;

import io.file.TUITFileOperatorHelper;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 8/2/13
 * Time: 2:09 PM
 * To change this template use File | Settings | File Templates.
 */
public class RestrictToEntrezTest {
    @Test
    public void restrictToEntrezTest(){

        File tmpDir=new File("/home/alext/Downloads/tmp");
        try {
            TUITFileOperatorHelper.restrictToEntrez(tmpDir, "not hybrid not other not uncultured not enrichment not unclassified not uncultivated not unspecified not environmental not metagenomes");
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
