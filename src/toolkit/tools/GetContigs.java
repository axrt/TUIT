package toolkit.tools;

import format.fasta.Fasta;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by alext on 12/12/14.
 */
public class GetContigs {

    public static void main(String[] args) {

        final Path toOriginalFasta = Paths.get(args[0]);
        final Path toRDCFile = Paths.get(args[1]);
        final Path toTUITClassification = Paths.get(args[2]);
        final Path toOutFile = Paths.get(args[3]);

        final List<String> RDCSequences;
        final List<String> taxonomy;
        final List<String> fastaACs;
        final List<String> fastaSequences;
        try (BufferedReader rdcReader = new BufferedReader(new FileReader(toRDCFile.toFile()));
             BufferedReader taxonomyReader = new BufferedReader(new FileReader(toTUITClassification.toFile()));
             BufferedReader fastaReader = new BufferedReader(new FileReader(toOriginalFasta.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toOutFile.toFile()))) {

            final String[] rdc = rdcReader.lines().collect(Collectors.joining("\n")).split(">");
            RDCSequences = Arrays.asList(rdc).stream().skip(1).map(elem -> {

                return elem.substring(elem.indexOf('\n')).replaceAll("\n", "");

            }).collect(Collectors.toList());

            taxonomy = Arrays.asList(taxonomyReader.lines().collect(Collectors.joining("\n")).split("\n")).stream().map(elem -> {
                final String[] out = elem.split(":\t");
                if (out[1].equals("")) {
                    return "not identified";
                } else {
                    return out[1];
                }
            }).collect(Collectors.toList());

            final Map<String, Integer> sequenceMap = new HashMap<>();
            int i = 0;
            for (String s : RDCSequences) {
                sequenceMap.put(s, i);
                i++;
            }

            final String[]fastaRrecords=fastaReader.lines().collect(Collectors.joining("\n")).split(">");
            fastaACs=new ArrayList<>();
            fastaSequences=new ArrayList<>();
            for(String fasta:fastaRrecords){
                if(fasta.equals("")) continue;
                final int position=fasta.indexOf('\n');
                final String ac=fasta.substring(0,position);
                final String sequnce=fasta.substring(position).replaceAll("\n","");
                fastaACs.add(ac);
                fastaSequences.add(sequnce);
            }

            bufferedWriter.write("Record\tSequence\tTaxonomy");
            bufferedWriter.newLine();

            i=0;
            for(String ac:fastaACs){
                bufferedWriter.write(ac);
                bufferedWriter.write('\t');
                bufferedWriter.write(fastaSequences.get(i));
                bufferedWriter.write('\t');
                bufferedWriter.write(taxonomy.get(sequenceMap.get(fastaSequences.get(i))));
                bufferedWriter.newLine();
                i++;
            }


        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

