
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by alext on 2/5/15.
 */
public class PullOutFasta {

    public static void main(String[] args) {

        final Path toFastaFile = Paths.get(args[0]);
        final Path toPullList = Paths.get(args[1]);

        try {
            final Set<String> pullSet = new HashSet<>(loadPullList(toPullList));

            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFastaFile.toFile()));
                 BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toFastaFile.resolveSibling(toPullList.toFile().getName() + ".pull").toFile()))) {
                String line;
                while((line=bufferedReader.readLine())!=null){
                    if(line.startsWith(">")){
                        if(pullSet.contains(line.substring(1).toUpperCase())){
                            bufferedWriter.write(line);
                            bufferedWriter.newLine();
                            bufferedWriter.write(bufferedReader.readLine());
                            bufferedWriter.newLine();
                        }
                    }
                }

            }

        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static List<String> loadPullList(final Path toListFile) throws IOException {
        final List<String> restrictedTaxa;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toListFile.toFile()))) {
            restrictedTaxa = bufferedReader.lines().filter(line -> line.length() != 0).collect(Collectors.toList());
        }
        return restrictedTaxa;
    }


}
