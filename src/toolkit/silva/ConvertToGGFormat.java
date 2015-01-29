package toolkit.silva;

import toolkit.greengenes.GreenGenesDeployer;

import java.io.*;
import java.nio.file.Path;

/**
 * Created by alext on 1/29/15.
 */
public class ConvertToGGFormat {


    public static void convert(Path toSilvaTaxFile, Path toOutputFile) throws IOException {


        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toSilvaTaxFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toOutputFile.toFile()))) {
             String line;
            while ((line=bufferedReader.readLine())!=null){


                final String[] superSplit= line.split("\t");
                final String[] subSplit= superSplit[1].split(";");

                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(superSplit[0]);
                stringBuilder.append('\t');

                for(int i=0;i< GreenGenesDeployer.TAX_ROW_MARKER.length;i++){
                    stringBuilder.append(GreenGenesDeployer.TAX_ROW_MARKER[i]);
                    if(i<subSplit.length){
                        stringBuilder.append(subSplit[i]);
                    }
                    stringBuilder.append(';');
                }

                bufferedWriter.write(stringBuilder.toString());
                bufferedWriter.newLine();

            }
        }

    }

}
