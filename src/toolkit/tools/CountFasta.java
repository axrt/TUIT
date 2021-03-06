package toolkit.tools;

import format.fasta.Fasta;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Created by alext on 9/18/14.
 * TODO document class
 */
public class CountFasta {

    private CountFasta() {
        throw new AssertionError("Non-Instantiable.");
    }

    public static int count(Path toFile) throws IOException {

        final int count;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFile.toFile()))) {
             count=bufferedReader.lines().filter(line->line.startsWith(Fasta.fastaStart)).mapToInt(line->{return 1;}).sum();
        }

        return count;
    }


}
