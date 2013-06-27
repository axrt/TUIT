package test;

import org.junit.Test;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/27/13
 * Time: 1:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class GIListTest {

    public static void main(String[]args){
        File gilist= new File("/home/alext/Downloads/tmp/uncultured_enrichment_unclassified_uncultivated_unspecified_environmental.gil");

        try {
            BufferedReader bufferedReader=new BufferedReader(new FileReader(gilist));
            for (int i=0;i<10;i++){
                System.out.println(bufferedReader.readLine());
            }
            bufferedReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

    }

}
