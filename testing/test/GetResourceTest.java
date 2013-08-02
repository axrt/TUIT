package tuit.main;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 8/2/13
 * Time: 5:13 PM
 * To change this template use File | Settings | File Templates.
 */
public class GetResourceTest {
    @Test
    public void testGetResuorce(){
        //URL url=getClass().getResource("tuit/db/sql/schema.sql");
        InputStream inputStream=this.getClass().getClassLoader().getResourceAsStream("tuit/db/sql/schema.sql");
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
        String line;
        try {
            while((line=bufferedReader.readLine())!=null){
               System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }
}
