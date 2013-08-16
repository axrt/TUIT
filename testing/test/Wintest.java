package tuit.main;

import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 8/16/13
 * Time: 9:32 AM
 * To change this template use File | Settings | File Templates.
 */
public class Wintest {


    @Test
    public void test(){
        System.out.println("/");
        System.out.println("\\");
        System.out.println("/".replaceAll("/","\\\\"));
    }

}
