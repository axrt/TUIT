

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 2/2/15.
 */
public class UnsignFasta {

    public static void main(String[] args) {

        final Path toFile = Paths.get(args[0]);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toFile.resolveSibling(toFile.toFile().getName() + ".unsign").toFile()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(">")) {
                    if (line.contains("\t")) {//inefficient
                        line = line.split("\t")[0].substring(1);
                    } else if (line.split(" ").length>1){
                        line = line.split(" ")[1];
                    }else{
                        line=line.substring(1);
                    }
                    bufferedWriter.write(">");
                }
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
