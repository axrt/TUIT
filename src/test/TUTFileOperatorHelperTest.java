package test;

import io.file.TUTFileOperatorHelper;
import io.properties.jaxb.TUITProperties;
import org.junit.Test;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/17/13
 * Time: 12:44 PM
 * To change this template use File | Settings | File Templates.
 */
public class TUTFileOperatorHelperTest {


    @Test
    public void testPropertiesLoading(){

        try {
           InputStream inputStream=new FileInputStream(new File("/home/alext/Developer/IdeaProjects/TUIT/src/test/io.properties.xml"));
           TUITProperties tuitProperties= TUTFileOperatorHelper.catchProperties(inputStream);
            System.out.println("hit");
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
