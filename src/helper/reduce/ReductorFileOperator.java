package helper.reduce;

import format.fasta.Fasta;
import toolkit.reduce.SequenceReductor;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

/**
 * Created by alext on 8/14/14.
 * TODO document class
 */
public class ReductorFileOperator {

    private ReductorFileOperator(){
        throw new AssertionError("Non-instantiable!");
    }

    public static<F extends Fasta> File save(SequenceReductor<F> reductor, Path toSave) throws IOException{

        final File toSaveFile=toSave.toFile();
        try(BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(toSaveFile))){
            bufferedWriter.write(reductor.toString());
        }
        return toSaveFile;
    }
}
