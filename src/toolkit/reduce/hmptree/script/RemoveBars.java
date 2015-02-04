package toolkit.reduce.hmptree.script;

import org.junit.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 2/2/15.
 */
public class RemoveBars {

    @Test
    public void remove() {
        final Path toFile = Paths.get("/home/alext/Documents/Research/Ocular/HCE/reprocessing2/cornea.unique.filter.unique.fasta");
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toFile.resolveSibling(toFile.toFile().getName() + ".nob").toFile()))) {

            String line;
            while((line=bufferedReader.readLine())!=null){
                if(line.startsWith(">")){
                    bufferedWriter.write(line);
                }else{
                    bufferedWriter.write(line.replaceAll("\\-",""));
                }
                bufferedWriter.newLine();
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
