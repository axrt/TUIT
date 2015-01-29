package toolkit.weld;

import format.fasta.Fasta;
import logger.Log;
import toolkit.tools.CountFasta;

import java.io.*;
import java.nio.file.Path;
import java.util.logging.Level;

/**
 * Created by alext on 1/28/15.
 */
public class WeldTaxonomy {

    public static Path weldToFile(Path toSeqFile, Path toTaxFile, Path output) throws IOException, IllegalArgumentException {

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output.toFile()))) {
            if (numOfSeqsMatchNumOfTax(toSeqFile, toTaxFile)) {

                try (BufferedReader taxReader = new BufferedReader(new FileReader(toTaxFile.toFile()));
                     BufferedReader seqReader = new BufferedReader(new FileReader(toSeqFile.toFile()))) {

                    String seqLine;
                    StringBuilder seqBuilder;
                    while ((seqLine = seqReader.readLine()) != null) {
                        bufferedWriter.write(seqLine);
                        if (seqLine.startsWith(Fasta.fastaStart)) {
                            bufferedWriter.write("::");
                            String taxLine;
                            while((taxLine=taxReader.readLine())!=null)
                                if(taxLine.contains("->")||taxLine.contains("{")){

                                    bufferedWriter.write(taxLine.substring(taxLine.indexOf("\t")+1));
                                    break;
                                }
                        }
                        bufferedWriter.newLine();
                    }
                }

            } else {
                throw new IllegalArgumentException("Files differ in numbers of records and corresponding taxonomies! ");
            }
        }
        return output;
    }

    public static boolean numOfSeqsMatchNumOfTax(Path toSeqFile, Path toTaxFile) throws IOException {
        final int countTax;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toTaxFile.toFile()))) {
            countTax = bufferedReader.lines().filter(line -> line.contains("->")||line.contains("{")).mapToInt(line -> {
                return 1;
            }).sum();
            Log.getInstance().log(Level.INFO, "Number of taxonomy lines: "+countTax);
        }
        final int countSeq = CountFasta.count(toSeqFile);
        return countSeq == countTax;
    }

    public static <F extends Fasta> Fasta weld(F query, String taxonomy) {
        return new Fasta(query.getAC() + "::" + taxonomy, query.getSequence()) {
        };
    }

}
