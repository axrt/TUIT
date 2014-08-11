package toolkit.reduce.hmptree.script;

import toolkit.reduce.hmptree.TreeFormatter;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 5/30/14.
 * TODO document class
 */
public class DesScript {

    public static void main(String[]args){

        System.out.println("Process Started>>>");
        System.out.println("Concatinating tables>>>\n");

        final Path toInputTableFile= Paths.get("/home/alext/Documents/Ocular Project/sequencing/DES/miseq/my_processing/stability.trim.contigs.good.unique.good.filter.uchime.pick.count_table");
        final Path toInputTaxonomyFile= toInputTableFile.resolveSibling("stability.trim.contigs.good.unique.good.filter.unique.pick.nodash.pds.wang.taxonomy");
        File combinedTable=null;
        try {
            combinedTable=TreeFormatter.MothurLineTreeFormatterFormat.combineTaxonomyAndReadTable(toInputTaxonomyFile,toInputTableFile,toInputTableFile.resolveSibling("hmp.combined.table"));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Creating an HMPTrees compatible table>>>\n");

        final TreeFormatter.MothurLineTreeFormatterFormat mothurLineTreeFormatterFormat=new TreeFormatter.MothurLineTreeFormatterFormat();
        final TreeFormatter treeFormatter=TreeFormatter.newInstance(mothurLineTreeFormatterFormat);

        final int cutoff = 80;

        final Path toOutputFile= toInputTableFile.resolveSibling("hmptree.out");

        try(BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(toOutputFile.toFile()));
            InputStream inputStream=new FileInputStream(combinedTable);
        BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream))) {
            final String headerLine=bufferedReader.readLine();
            bufferedWriter.write(headerLine.replaceAll("Representative_Sequence\t",""));
            bufferedWriter.newLine();
            treeFormatter.loadFromBufferedReader(bufferedReader,cutoff);
           bufferedWriter.write(treeFormatter.toHMPTree(true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();


    }

}
