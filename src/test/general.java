package test;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 5/30/13
 * Time: 6:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class general {

    public static void main (String[]args){
        File file = new File ("/home/alext/Downloads/rdp_output.tar.gz");
        System.out.println(file.getName());
    }
}
