package toolkit.reduce.hmptree;

import org.junit.Test;
import taxonomy.node.TaxonomicNode;

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
        final Path fastaFile= Paths.get("/home/alext/Documents/tuit/final testing/reductor.test.tuit");
        final TreeFormatter treeFormatter=new TreeFormatter(1,new TreeFormatter.TuitLineTreeFormatterFormat());
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
                "82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> PesudoBullsit {family} -> TotalBullCrap {genus}";
        final TreeFormatter treeFormatter=new TreeFormatter(1,new TreeFormatter.TuitLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()));
            System.out.println(treeFormatter.fromat.toHMPTree(treeFormatter.root,true));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    @Test
    public void testMergeDatasets(){
        final String line="82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> Pseudomonadaceae {family} -> Pseudomonas {genus} -> Pseudomonas aeruginosa group {species group} -> Pseudomonas aeruginosa {species}\n" +
                "82@2117:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Bullshit {subgenus} -> Gammaproteobacteria {class} -> Pseudomonadales {order} -> PesudoBullsit {family} -> TotalBullCrap {genus}";
        final TreeFormatter treeFormatter=new TreeFormatter(1,new TreeFormatter.TuitLineTreeFormatterFormat());
        try {
            treeFormatter.loadFromInputStream(new ByteArrayInputStream(line.getBytes()));
            final TreeFormatter.TreeFormatterFormat.HMPTreesOutput output=
                    TreeFormatter.TreeFormatterFormat.HMPTreesOutput.newInstance(
                    treeFormatter.fromat.toHMPTree(treeFormatter.root, true),"test"
            );
            final List<TreeFormatter.TreeFormatterFormat.HMPTreesOutput> testList=new ArrayList<>();
            testList.add(output);
            System.out.println(treeFormatter.fromat.mergeDatasets(testList));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
