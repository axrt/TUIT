package toolkit.weld;

import format.fasta.Fasta;

import java.io.*;
import java.nio.file.Path;

/**
 * Created by alext on 1/28/15.
 */
public class SelectFasta {

    public static Path select(Path input, Path output, String select) throws IOException {


        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(input.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output.toFile()))) {
            String line;
            boolean save = false;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(Fasta.fastaStart)) {
                    save=false;
                    if (line.toLowerCase().contains(select.toLowerCase())){
                        save=true;
                    }
                }
                if(save){
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
            }
        }
        return output;
    }

    public static boolean selectIsFine(String select) {
        return true;//todo implement some real checks
    }

}
