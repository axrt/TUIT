package toolkit.tools;

import db.ram.RamDb;
import format.fasta.Fasta;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by alext on 9/19/14.
 * TODO document class
 */
public class ExtractPCRSeqsNonRedundant {

    private ExtractPCRSeqsNonRedundant() {
        throw new AssertionError("Non-Instantiable.");
    }

    public static File reduce(Path toFile, Path toOutFile, Path toTaxDB) throws IOException, ClassNotFoundException {

        final File out = toOutFile.toFile();
        final Map<String,Set<Integer>> sequenceSet = new HashMap<>();

        final RamDb ramDb=RamDb.loadSelfFromFile(toTaxDB.toFile());

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(out))) {
            String line;
            String ac = "";
            Integer taxid=null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(Fasta.fastaStart)) {
                    ac = line;
                    taxid=ramDb.getTaxIdByGi(Integer.valueOf(ac.split("\\|")[1]));
                } else {
                    line = line.replaceAll("\\.", "").replaceAll("\\-", "");
                    if (sequenceSet.keySet().contains(line)) {
                        final Set<Integer>set= sequenceSet.get(line);
                        if(!set.contains(taxid)){
                            bufferedWriter.write(ac);
                            bufferedWriter.newLine();
                            bufferedWriter.write(line);
                            bufferedWriter.newLine();
                            set.add(taxid);
                        }
                    }else{
                        bufferedWriter.write(ac);
                        bufferedWriter.newLine();
                        bufferedWriter.write(line);
                        bufferedWriter.newLine();
                        sequenceSet.put(line,new HashSet<>(taxid));
                    }
                }
            }
        }

        return out;
    }

}
