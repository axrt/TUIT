package fastblast;

import blast.output.Iteration;
import format.fasta.Fasta;

import java.io.*;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by alext on 1/6/15.
 */
public class FileDataHandler implements DataHandler<Iteration> {

    protected final Set<String> allowedAcs;

    protected FileDataHandler() {
        allowedAcs = new HashSet<>();

    }

    @Override
    public void handle(Iteration iteration) throws Exception {
        this.allowedAcs.add(iteration.getIterationQueryDef());
    }

    public void restrict(Path toUnrestrictedFile, Path toRestrictedFile) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toUnrestrictedFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toRestrictedFile.toFile()))) {

            String line;
            while((line=bufferedReader.readLine())!=null){
                if(line.startsWith(Fasta.fastaStart)){
                    if(this.allowedAcs.contains(line.substring(1))){
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                        bufferedWriter.write(line=bufferedReader.readLine());
                        bufferedWriter.newLine();
                    }
                }
            }
        }
    }
    public static FileDataHandler get(){
        return new FileDataHandler();
    }
}
