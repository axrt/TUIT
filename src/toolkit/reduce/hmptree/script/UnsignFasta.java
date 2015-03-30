

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

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
                    line = UUID.randomUUID().toString();
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
