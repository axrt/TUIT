

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by alext on 2/5/15.
 */
public class RestrictTaxa {

    public static void main(String[] args) {

        final Path toTuitOutput = Paths.get(args[0]);
        final Path toListFile = Paths.get(args[1]);

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toTuitOutput.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toTuitOutput.resolveSibling(toTuitOutput.toFile().getName() + ".rest").toFile()))) {

            final List<String> restrictedTaxa = loadTaxaList(toListFile);
            bufferedReader.lines().filter(line -> line.length() != 0).filter(line -> {
                return !hasTaxa(restrictedTaxa, line);
            }).forEach(line -> {
                try {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            e.getCause().printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<String> loadTaxaList(final Path toListFile) throws IOException {
        final List<String> restrictedTaxa;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toListFile.toFile()))) {
            restrictedTaxa = bufferedReader.lines().filter(line -> line.length() != 0).collect(Collectors.toList());
        }
        return restrictedTaxa;
    }

    public static boolean hasTaxa(final List<String> taxaToRestrict, final String line) {
        for (String s : taxaToRestrict) {
            if (line.contains(s)) {
                return true;
            }
        }
        return false;
    }

}
