package toolkit.livingtree;

import format.fasta.Fasta;
import toolkit.greengenes.GreenGenesDeployer;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alext on 1/29/15.
 */
public class LivingTreeToGGFormatConverter {

    public static final String[] BADNODES = {"Actinobacteridae;", "Nitriliruptoridae;", "Rubrobacteridae;", "Chloroflexineae;", "Roseiflexineae;",
            "Sphaerobacteridae;", "Cystobacterineae;", "Nannocystineae;", "Sorangiineae;"};

    public static Map<String, String> convert(Path toLTDTaxFile, Path toOutputFile) throws IOException {

        final Map<String, String> map;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toLTDTaxFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toOutputFile.toFile()))) {
            map = new HashMap<>();
            String line;
            int fakeID = 100000;
            while ((line = bufferedReader.readLine()) != null) {

                //Getting rid of the suborders, that spoil the whole thing
                for(String s:BADNODES){
                    line=line.replaceAll(s,"");
                }

                final String[] superSplit = line.split("\t");
                final String[] subSplit = superSplit[9].concat(";").concat(superSplit[4]).concat(";").split(";");

                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(fakeID);
                map.put(superSplit[0], String.valueOf(fakeID));
                fakeID++;
                stringBuilder.append('\t');

                for (int i = 0; i < GreenGenesDeployer.TAX_ROW_MARKER.length; i++) {
                    stringBuilder.append(GreenGenesDeployer.TAX_ROW_MARKER[i]);
                    if (i < subSplit.length) {
                        stringBuilder.append(subSplit[i]);
                    }
                    stringBuilder.append(';');
                }

                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.newLine();

            }
        }
        return map;
    }


    public static void recodeFasta(Path toFastaFile, Path toOutputFile, Map<String, String> encodes) throws Exception {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFastaFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toOutputFile.toFile()))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                if (line.startsWith(Fasta.fastaStart)) {
                    bufferedWriter.write(">");
                    final String encodedeAC = encodes.get(line.substring(1).split("\t")[0]);
                    if (encodedeAC == null) {
                        throw new IllegalArgumentException(line);
                    } else {
                        bufferedWriter.write(encodedeAC);
                    }
                } else {
                    bufferedWriter.write(line);
                }
                bufferedWriter.newLine();
            }
        }
    }

}
