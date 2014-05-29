package toolkit.reduce.hmptree;

import io.file.NucleotideFastaTUITFileOperator;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;
import toolkit.reduce.NucleotideFastaSequenceReductor;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

//TODO document
public class TreeFormatter {

    protected final CountingTaxonomicNode root;
    protected double cutoff;
    protected TreeFormatterFormat fromat;

    protected TreeFormatter(double cutoff, TreeFormatterFormat fromat) {
        this.cutoff = cutoff;
        this.root = new CountingTaxonomicNode(0, Ranks.no_rank, "root", 0);
        this.root.setParent(this.root);
        this.fromat = fromat;
    }

    public void loadFromPath(final Path path) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.loadFromInputStream(inputStream);
        }
    }

    public void loadFromInputStream(final InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.root.join(this.fromat.toNode(line));
            }
        }
    }

    public abstract static class TreeFormatterFormat {
        public static Set<Ranks> HMPTREES_ALLOWED_RANKS = new HashSet<>(
                Arrays.asList(new Ranks[]{
                        Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus
                })
        );

        public abstract CountingTaxonomicNode toNode(final String line);

        public abstract String toHMPTree(final CountingTaxonomicNode taxonomicNode, final boolean normalize);

        public abstract String mergeDatasets(final List<HMPTreesOutput> hmptreeDatasets);

        public static class HMPTreesOutput {
            protected final List<String> taxa;
            protected final Map<String, Double> reads;
            protected final String name;

            protected HMPTreesOutput(List<String> taxa, Map<String, Double> reads, String name) {
                this.taxa = taxa;
                this.reads = reads;
                this.name = name;
            }

            public List<String> getTaxa() {
                return taxa;
            }

            public Map<String, Double> getReads() {
                return reads;
            }

            public String getName() {
                return name;
            }

            public static HMPTreesOutput newInstance(final String hmpOutput, final String name) {
                final String[] lines = hmpOutput.split("\n");
                final List<String> taxa = new ArrayList<>();
                final Map<String, Double> counts = new HashMap<>();
                for (String line : lines) {
                    final String[] subsplit = line.split("\t");
                    if (subsplit.length != 2 || !Character.isLetter(subsplit[0].charAt(0)) || !Character.isDigit(subsplit[1].charAt(0))) {
                        throw new IllegalArgumentException("HMPTrees output is not properly formatted!");
                    }
                    taxa.add(subsplit[0]);
                    counts.put(subsplit[0], Double.valueOf(subsplit[1]));
                }
                return new HMPTreesOutput(taxa, counts, name);
            }
        }
    }

    public static class TuitLineTreeFormatterFormat extends TreeFormatterFormat {
        @Override
        public CountingTaxonomicNode toNode(final String line) {
            final String[] split = line.substring(0, line.length() - 1).split(":\t");
            if (split.length != 2) {
                this.formatComplain(line);
            }
            final String[] acSplit = split[0].split(NucleotideFastaSequenceReductor.COUNT_MARKER);
            if (acSplit.length != 2) {
                this.formatComplain(split[0]);
            }
            if (!split[1].contains("->")) {
                this.formatComplain(line);
            }
            if (split[1].equals(NucleotideFastaTUITFileOperator.NOT_IDENTIFIED)) {
                return null;
            }
            final String[] taxSplit = split[1].split("} -> ");
            final List<TaxonomicNode>taxonomicNodes=this.taxonomicNodes(taxSplit,Integer.parseInt(acSplit[1]));
            return (CountingTaxonomicNode) taxonomicNodes.get(0);
        }

        private List<TaxonomicNode>taxonomicNodes(final String[]taxa, final int count){
            final List<TaxonomicNode> taxonomicNodes = new ArrayList<>();
            for (String s : taxa) {
                final String[] subSplit = s.split(" \\{");
                if (subSplit.length != 2) {
                    formatComplain(s);
                }
                taxonomicNodes.add(new CountingTaxonomicNode(0, Ranks.convertValue(subSplit[1]), subSplit[0], count));
            }

            for (int i = 0; i < taxonomicNodes.size() - 1; i++) {
                taxonomicNodes.get(i).addChild(taxonomicNodes.get(i + 1));
                taxonomicNodes.get(i + 1).setParent(taxonomicNodes.get(0));
            }
            taxonomicNodes.get(0).setParent(taxonomicNodes.get(0));
            return taxonomicNodes;
        }

        @Override
        public String toHMPTree(final CountingTaxonomicNode taxonomicNode, final boolean normalize) {
            if (!normalize) {
                return this.toHMPTreeHelper(taxonomicNode, "").trim();
            }
            final String[] lines = toHMPTreeHelper(taxonomicNode, "").trim().split("\n");
            final StringBuilder stringBuilder = new StringBuilder();
            final int normalizationConstant = Integer.parseInt(lines[0].split("\t")[1]);
            for (String s : lines) {
                final String[] line = s.split("\t");
                stringBuilder.append(line[0]);
                stringBuilder.append('\t');
                stringBuilder.append((double) Integer.parseInt(line[1]) / normalizationConstant * 100);
                stringBuilder.append('\n');
            }
            return stringBuilder.toString().trim();
        }

        private String toHMPTreeHelper(final CountingTaxonomicNode taxonomicNode, String prefix) {
            final StringBuilder stringBuilder = new StringBuilder();
            final boolean allowed = HMPTREES_ALLOWED_RANKS.contains(taxonomicNode.getRank());
            if (allowed) {
                prefix = prefix.concat(taxonomicNode.getScientificName()).concat("{").concat(taxonomicNode.getRank().getName()).concat("}.");
            }
            if (allowed && taxonomicNode.getChildren().isEmpty()) {
                stringBuilder.append('\n');
                stringBuilder.append(prefix.substring(0, prefix.length() - 1));
                stringBuilder.append('\t');
                stringBuilder.append(taxonomicNode.getCount());
            }
            for (TaxonomicNode child : taxonomicNode.getChildren()) {
                if (allowed) {
                    stringBuilder.append('\n');
                    stringBuilder.append(prefix.substring(0, prefix.length() - 1));
                    stringBuilder.append('\t');
                    stringBuilder.append(taxonomicNode.getCount());
                }
                stringBuilder.append(toHMPTreeHelper((CountingTaxonomicNode) child, prefix));
            }
            return stringBuilder.toString();
        }

        @Override
        public String mergeDatasets(final List<HMPTreesOutput> hmptreeDatasets) {
            final Set<String> masterTaxaList = new TreeSet<>();
            for (HMPTreesOutput h : hmptreeDatasets) {
                masterTaxaList.addAll(h.getTaxa());
            }
            final String[][] table = new String[masterTaxaList.size() + 1][hmptreeDatasets.size() + 1];
            table[0][0] = "taxonomy";
            for (int i = 1; i < hmptreeDatasets.size() + 1; i++) {
                table[0][i] = hmptreeDatasets.get(i - 1).getName();
            }
            int t = 0;
            for (String s : masterTaxaList) {
                table[++t][0] = s;
            }
            for (int i = 0; i < hmptreeDatasets.size(); i++) {
                table[0][i + 1] = hmptreeDatasets.get(i).getName();
                int j = 1;
                for (String s : masterTaxaList) {
                    final Double count = hmptreeDatasets.get(i).getReads().get(s);
                    if (count == null) {
                        table[++j][i+1] = String.valueOf(0);
                    } else {
                        table[j++][i+1] = String.valueOf(count);
                    }
                }
            }
            final StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < table.length; i++) {
                for (int j = 0; j < table[0].length; j++) {
                    stringBuilder.append(table[i][j]);
                    if (j != table[0].length - 1) {
                        stringBuilder.append("\t");
                    }
                }
                if(i!=table.length-1){
                    stringBuilder.append('\n');
                }
            }
            return stringBuilder.toString();
        }

        protected void formatComplain(final String line) {
            throw new IllegalArgumentException("The line \"".concat(line).concat("...\" is not properly formatted!"));
        }
    }

    public static class CountingTaxonomicNode extends TaxonomicNode {

        private int count;

        public CountingTaxonomicNode(int taxid, Ranks rank, String scientificName, int count) {
            super(taxid, rank, scientificName);
            this.count = count;
        }

        public int getCount() {
            return count;
        }

        @Override
        public boolean join(TaxonomicNode otherNode) {
            if (!(otherNode instanceof CountingTaxonomicNode)) {
                return false;
            } else {
                if (this.rank.equals(otherNode.getRank()) && this.scientificName.equals(otherNode.getScientificName())) {
                    this.count += ((CountingTaxonomicNode) otherNode).count;
                    final List<TaxonomicNode> combinedChildren = new ArrayList<>();
                    final List<TaxonomicNode> toRemove = new ArrayList<>();
                    combinedChildren.addAll(this.children);
                    combinedChildren.addAll(otherNode.getChildren());
                    for (TaxonomicNode tn1 : combinedChildren) {
                        for (TaxonomicNode tn2 : combinedChildren) {
                            if (!tn1.equals(tn2) & !toRemove.contains(tn1) && tn1.join(tn2)) {
                                toRemove.add(tn2);
                                break;
                            }
                        }
                    }
                    combinedChildren.removeAll(toRemove);
                    this.children.clear();
                    this.children.addAll(combinedChildren);
                    return true;
                }
                return false;
            }
        }
    }
}
