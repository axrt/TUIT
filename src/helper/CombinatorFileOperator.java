package helper;

import toolkit.reduce.hmptree.TreeFormatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Created by alext on 8/14/14.
 * TODO document class
 */
public class CombinatorFileOperator {

    private CombinatorFileOperator(){
        throw new AssertionError("Non-instantiable!");
    }

    public static File save(List<TreeFormatter.TreeFormatterFormat.HMPTreesOutput> hmpTreesOutputs,TreeFormatter treeFormatter, Path destination)throws IOException{

        final File dest=destination.toFile();
        try(BufferedWriter bufferedWriter =new BufferedWriter(new FileWriter(dest))){
           bufferedWriter.write(treeFormatter.mergeDataSets(hmpTreesOutputs));
        }
        return dest;

    }
}
