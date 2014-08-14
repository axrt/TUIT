package toolkit.reduce.hmptree;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alext on 5/15/14.
 */
public class TreeFormatterTest {
    //@Test
    public void test(){
        final Path fastaFile= Paths.get("/home/alext/Documents/tuit/final testing/distro 1.0.5/reductor.tuit");
        final TreeFormatter treeFormatter=new TreeFormatter(new TreeFormatter.TuitLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromPath(fastaFile);
            System.out.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void testToString(){
        final String line="82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Testobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Testobacteria {phylum} -> Bullshit {subgenus} -> Testoproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> PesudoBullsit {family} -> TotalBullCrap {genus}";
        final TreeFormatter treeFormatter=new TreeFormatter(new TreeFormatter.TuitLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()));
            System.out.println(treeFormatter.format.toHMPTree(treeFormatter.root, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testMergeDatasets(){
        final String line="82@1:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@2:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> PesudoBullsit {family} -> TotalBullCrap {genus}";

        final String line2="82@1:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@1:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Testobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@1:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Testobacteria {phylum} -> Bullshit {subgenus} -> Testoproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@1:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> PesudoBullsit {family} -> TotalBullCrap {genus}";

        final TreeFormatter treeFormatter=new TreeFormatter(new TreeFormatter.TuitLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()));
            final TreeFormatter.TreeFormatterFormat.HMPTreesOutput output=
                    TreeFormatter.TreeFormatterFormat.HMPTreesOutput.newInstance(
                            treeFormatter.format.toHMPTree(treeFormatter.root, false), "test1"
                    );
            treeFormatter.erase();
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line2.getBytes()));
            final TreeFormatter.TreeFormatterFormat.HMPTreesOutput output2=
                    TreeFormatter.TreeFormatterFormat.HMPTreesOutput.newInstance(
                            treeFormatter.format.toHMPTree(treeFormatter.root, false), "test2"
                    );
            final List<TreeFormatter.TreeFormatterFormat.HMPTreesOutput> testList=new ArrayList<>();
            testList.add(output);
            testList.add(output2);
            System.out.println(treeFormatter.format.mergeDatasets(testList));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void testMothur(){
        System.out.println("NO CUTOFF>>>\n");
        final String line="M01529_30_000000000-A64PD_1_1101_14316_1559\tBacteria(100);\"Actinobacteria\"(100);Actinobacteria(100);Actinomycetales(100);Micrococcaceae(100);Nesterenkonia(100);\t580157\t7067\t14115\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tBacteria(100);Firmicutes(100);Bacilli(100);Bacillales(100);Bacillaceae_1(100);Aeribacillus(100);\t344287\t3441\t4960\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tBacteria(100);Firmicutes(100);Bacilli(100);Bacillales(100);Bacillaceae_1(100);TestoBacillus(100);\t344287\t3441\t4960";
        final TreeFormatter treeFormatter=new TreeFormatter(new TreeFormatter.MothurLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()));
            System.out.println(treeFormatter.format.toHMPTree(treeFormatter.root, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }
    //@Test
    public void testMothurCutoff(){
        System.out.println("CUTOFF>>>\n");
        final String line="M01529_30_000000000-A64PD_1_1101_14316_1559\tBacteria(100);\"Actinobacteria\"(100);Actinobacteria(100);Actinomycetales(100);Micrococcaceae(100);Nesterenkonia(100);\t580157\t7067\t14115\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tBacteria(100);Firmicutes(100);Bacilli(100);Bacillales(70);Bacillaceae_1(90);Aeribacillus(50);\t344287\t3441\t4960\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tBacteria(100);Firmicutes(100);Bacilli(100);unclassified;\t344287\t3441\t4960\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tunknown;unclassified;unclassified;unclassified;\t344287\t3441\t4960\n" +
                "M01529_30_000000000-A64PD_1_1101_17032_1812\tBacteria(100);Firmicutes(100);Bacilli(100);Bacillales(100);Bacillaceae_1(100);TestoBacillus(100);\t344287\t3441\t4960";
        final TreeFormatter treeFormatter=new TreeFormatter(new TreeFormatter.MothurLineTreeFormatterFormat());
        final int cutoff = 80;
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()),cutoff);
            System.out.println(treeFormatter.format.toHMPTree(treeFormatter.root, true));
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println();
    }
    //@Test
    public void combineTaxonomyAndReadTableTest(){
       final Path taxonomy=Paths.get("/home/alext/Documents/Ocular Project/sequencing/DES/miseq/my_processing/test.pds.wang.taxonomy");
        final Path seqMap=taxonomy.resolveSibling("stability.trim.contigs.good.unique.good.filter.uchime.pick.count_table");
        final Path outfile=taxonomy.resolveSibling("out.table.txt");
        try {
            TreeFormatter.MothurLineTreeFormatterFormat.combineTaxonomyAndReadTable(taxonomy,seqMap,outfile);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
