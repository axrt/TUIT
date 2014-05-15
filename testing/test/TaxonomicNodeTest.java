package taxonomy.node;

import org.junit.Test;
import toolkit.reduce.hmptree.TreeFormatter;

/**
 * Created by alext on 5/15/14.
 */
public class TaxonomicNodeTest {

    @Test
    public void testJoin(){

        TreeFormatter.TuitLineTreeFormatterFormat tuitLineTreeFormatterFormat=new TreeFormatter.TuitLineTreeFormatterFormat();
        final TaxonomicNode node1=tuitLineTreeFormatterFormat.toNode(
                "GR749QQ02GEWQ6:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Betaproteobacteria {class} -> Burkholderiales {order} -> Comamonadaceae {family} -> Acidovorax {genus}"
        );
        final TaxonomicNode node2=tuitLineTreeFormatterFormat.toNode("GR749QQ02GEWQ6:\troot {no rank} -> cellular organisms {no rank} -> Bacteria {superkingdom} -> Proteobacteria {phylum} -> Betaproteobacteria {class} -> Burkholderiales {order} -> Comamonadaceae {family} -> Bullshit {subfamily} -> Bulshitovorax {genus}"
        );
        node1.join(node2);
        System.out.println();
    }
}
