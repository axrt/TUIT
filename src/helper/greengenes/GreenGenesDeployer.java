package helper.greengenes;

import helper.NCBITablesDeployer;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

//TODO document
public class GreenGenesDeployer {

    public static String FTPMIRROR = "ftp://greengenes.microbio.me";
    public static String GRENEGENES_RELEASE = "greengenes_release";
    public static String TAXDUMP = "";

    /**
     * Private constructor, prevents instantiation
     */
    private GreenGenesDeployer() {
        throw new AssertionError("Non-instantiable!");
    }

    public static NCBITablesDeployer.TaxonomyFiles convertToTaxonomyFiles(final Path taxonomy) {


        return null;
    }

    private static final class TaxonomicConverter extends ArrayList<Map<String,TaxonomicNode>> {

        public static final List<Ranks> GG_RANKS = Arrays.asList(
                new Ranks[]{
                        Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus, Ranks.subspecies
                }
        );

        public final static Map<Ranks,Integer> GG_RANK_MAP = new HashMap<>();
        static{

            GG_RANKS.stream().forEach((rank)->  GG_RANK_MAP.put(rank, GG_RANKS.indexOf(rank)));
        }

        private int taxId;
        private final TaxonomicNode root;
        private final Map<TaxonomicNode,Integer> giTaxonomyMap;
        private TaxonomicConverter() {
            this.taxId=0;
            this.giTaxonomyMap=new HashMap<>();
            this.root=TaxonomicNode.newDefaultInstance(taxId++,Ranks.root_of_life,Ranks.root_of_life.getName());
            IntStream.rangeClosed(0, 7).forEach((i) -> this.add(i, new HashMap<>()));
        }

        private void addRank(final String taxon, final Ranks rank){
            final Map<String,TaxonomicNode> map=this.getRankMap(rank);
            if(!map.keySet().contains(taxon)){
                map.put(taxon, TaxonomicNode.newDefaultInstance(this.taxId++, rank, taxon));
            }
        }
        private void attachToRoot(final String top){
            final TaxonomicNode topNode=this.get(0).get(top);
            topNode.setParent(root);
            root.addChild(topNode);
        }

        private void connectNodes(final String parent, final String child, final Ranks parentRank){
            final Ranks childRank=GG_RANKS.get(GG_RANK_MAP.get(parentRank)+1);
            final TaxonomicNode childNode=this.getRankMap(childRank).get(child);
            final TaxonomicNode parentNode=this.getRankMap(parentRank).get(parent);
            parentNode.addChild(childNode);
            childNode.setParent(parentNode);
        }

        private Map<String,TaxonomicNode> getRankMap(final Ranks rank) {
            return this.get(GG_RANK_MAP.get(rank));
        }

        private void addGGLine(final List<String> line) {
             IntStream.range(1,line.size()-1).forEach((i)->{
                 final String parent=line.get(i);
                 final String child=line.get(i+1);
                 final Ranks parentRank=GG_RANKS.get(i-1);
                 final Ranks childRank=GG_RANKS.get(i);
                 this.addRank(parent,parentRank);
                 this.addRank(child,childRank);
                 this.connectNodes(parent,child,parentRank);
             });
            this.attachToRoot(line.get(1));
        }

        public void addGGLine(String line) {
            final String[] GGIsplit = line.split("\t");
            final String[] taxaSplit = GGIsplit[1].split("; ");
            final List<String> listRepresentation = Arrays.asList(taxaSplit).stream().map(taxa -> taxa.substring(2)).collect(Collectors.toList());
            this.addGGLine(listRepresentation);
        }
        private void checkLine(final String line){
            throw new NotImplementedException();
        }
    }
}
