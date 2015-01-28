package toolkit.split;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by alext on 1/23/15.
 */
public class SplitFasta {

    public static void main (String[]args){
        final Path toFile= Paths.get(args[0]);
        try {
            simpleSplit(toFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void simpleSplit(Path toFastaFile) throws Exception {

        final String[] split;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFastaFile.toFile()))) {
            final StringBuilder stringBuilder=new StringBuilder();
            String line;
            while((line=bufferedReader.readLine())!=null){
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            split=stringBuilder.toString().split(">");
            //1.8 option
            //split = bufferedReader.lines().collect(Collectors.joining("\n")).split(CommonFormats.Fasta.FASTA_START);
            int counter = 0;
            int filesWritten = 1;
            final List<String> splitList=Arrays.asList(split);
            final Queue<String> queue = new LinkedList<>(splitList.subList(1,splitList.size()));
            while (!queue.isEmpty()) {
                try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toFastaFile.resolveSibling(toFastaFile.getFileName() + "." + filesWritten + ".part").toFile()))) {
                    while (!queue.isEmpty() && counter < 5000) {
                        bufferedWriter.write(">");
                        bufferedWriter.write(queue.poll());
                        bufferedWriter.newLine();
                        counter++;
                    }
                    counter=0;
                    filesWritten++;
                }
            }

        }

    }

}
