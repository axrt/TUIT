package toolkit.silva;

import org.junit.Test;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by alext on 8/11/14.
 * TODO document class
 */
public class SilvaHelperTest {

    //@Test
    public void testReplaceUWithT(){
       final Path toFile= Paths.get("/home/alext/Documents/tuit/silva/SILVA_119_SSURef_tax_silva.fasta");
        try {
            SilvaHelper.replaceUWithT(toFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void collectNamesMapFromFileTest(){
        final Path toFile= Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        try {
            final Map<String,Integer>names=SilvaHelper.collectNamesMapFromFile(toFile);
            System.out.println(names.get("Sphingobium"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void collectRanksMapFromFileTest(){
        final Path toFile= Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        try {
            final Map<String, Ranks>names=SilvaHelper.collectRanksMapFromFile(toFile);
            System.out.println(names.get("Sphingobium"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void findMaxTaxidTest(){
        final Path toFile= Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        try {

            System.out.println(SilvaHelper.findMaxTaxid(toFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testSpeciesLevel(){
        final Path toFile= Paths.get("/home/alext/Documents/tuit/silva/SILVA_119_SSURef_tax_silva.fasta");
        try(BufferedReader bufferedReader=new BufferedReader(new FileReader(toFile.toFile()))){
            String line;
            final Set<String> species=new HashSet<>();
            while((line=bufferedReader.readLine())!=null){
                if(line.startsWith(">")){
                    final String[]split=line.split(";");
                    if(split[split.length-1].split(" ").length<2&&split[split.length-1].split(" ")[0].equals(split[split.length-2])) {
                        System.out.println(line);
                        species.add(split[split.length - 1]);
                    }
                }
            }
            for(String s:species){
                //System.out.println(s);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void enrichGiTaxidMapFromSequenceFileTest(){
        final Path toGiTaxidFile= Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.acs");
        final Path toFastaFile=Paths.get("/home/alext/Documents/tuit/silva/SILVA_119_SSURef_tax_silva.fasta");
        final Path toNodesFile=Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        final Path toModNodesFile=toNodesFile.resolveSibling(toNodesFile.getFileName().toString().concat(".mod"));

        try {
            final Map<String,Integer> gi_taxid=SilvaHelper.enrichGiTaxidMapFromSequenceFile(toGiTaxidFile,toFastaFile,toNodesFile,toModNodesFile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    //@Test
    public void createNodesDbFileTest(){
        final Path toNodesFile=Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        try {
            final TaxonomicNode taxonomy=SilvaHelper.createNodesDbFile(toNodesFile);
            System.out.println("done");
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    //@Test
    public void saveNodesTableFromTaxonomyTest(){
        final Path toNodesFile=Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.enr.txt");
        final Path toNodesModDMP=toNodesFile.resolveSibling("nodes.dmp");
        try {
            final TaxonomicNode taxonomy=SilvaHelper.createNodesDbFile(toNodesFile);
            SilvaHelper.saveNodesTableFromTaxonomy(taxonomy,toNodesModDMP);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    @Test
    public void createTest(){

        final Path toNodesFile=Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.txt");
        final Path toEnrichedNodesFile=toNodesFile.resolveSibling("tax_slv_ssu_nr_119.enr.txt");
        final Path toModNodesFile=toEnrichedNodesFile.resolveSibling("nodes.dmp");

        final Path toGiTaxidFile= Paths.get("/home/alext/Documents/tuit/silva/tax_slv_ssu_nr_119.acs");
        final Path toModGiTaxidFile=toGiTaxidFile.resolveSibling("gi_taxid.dmp");

        final Path toFastaFile=Paths.get("/home/alext/Documents/tuit/silva/SILVA_119_SSURef_tax_silva.fasta");
        final Path toModFastaFile=toFastaFile.resolveSibling("silva.fasta");
        final Path toRenamedFastaFile=toFastaFile.resolveSibling("blastdb.fasta");

        final Path toModNamesFile=toNodesFile.resolveSibling("names.dmp");

        final Path toRAMBDFile=toNodesFile.resolveSibling("ramdb.obj");

        try {
            SilvaHelper.create(toNodesFile,toEnrichedNodesFile,toModNodesFile,toGiTaxidFile,toModGiTaxidFile,toFastaFile,toModFastaFile,toModNamesFile,toRAMBDFile,toRenamedFastaFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
