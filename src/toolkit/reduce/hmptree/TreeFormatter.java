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
    protected TreeFormatterFormat format;

    protected TreeFormatter(TreeFormatterFormat format) {
        this.root = new CountingTaxonomicNode(0, Ranks.no_rank, "root", 0);
        this.root.setParent(this.root);
        this.format = format;
    }

    public String toHMPTree(boolean normalize) {
        return this.format.toHMPTree(this.root, normalize);
    }

    public void loadFromPath(final Path path) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.loadFromInputStream(inputStream);
        }
    }

    public void loadFromPath(final Path path, int cutoff) throws IOException {
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            this.loadFromInputStream(inputStream, cutoff);
        }
    }

    public void loadFromInputStream(final InputStream inputStream) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.root.join(this.format.toNode(line));
            }
        }
    }

    public void loadFromInputStream(final InputStream inputStream, int cutoff) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                this.root.join(this.format.toNodeWithCutoff(line, cutoff));
            }
        }
    }

    public void loadFromBufferedReader(final BufferedReader bufferedReader, int cutoff) throws IOException {

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            this.root.join(this.format.toNodeWithCutoff(line, cutoff));
        }
    }

    public static TreeFormatter newInstance(TreeFormatterFormat format) {
        return new TreeFormatter(format);
    }

    public abstract static class TreeFormatterFormat {
        public static Set<Ranks> HMPTREES_ALLOWED_RANKS = new HashSet<>(
                Arrays.asList(new Ranks[]{
                        Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus
                })
        );

        public abstract CountingTaxonomicNode toNode(final String line);

        public abstract CountingTaxonomicNode toNodeWithCutoff(final String line, int cutoff);

        public String toHMPTree(final CountingTaxonomicNode taxonomicNode, final boolean normalize) {
            if (!normalize) {
                return this.toHMPTreeHelper(taxonomicNode, "").trim();
            }
            final String hmpTreelines = toHMPTreeHelper(taxonomicNode, "").trim();
            final String[] lines = hmpTreelines.split("\n");
            final StringBuilder stringBuilder = new StringBuilder();
            final String normalizingLine = lines[0];
            final String[] normalizationBlocks = normalizingLine.split("\t");
            final int[] normalizationConstants = new int[normalizationBlocks.length - 1];
            for (int i = 0; i < normalizationConstants.length; i++) {
                normalizationConstants[i] = Integer.parseInt(normalizationBlocks[i + 1]);
            }
            for (String s : lines) {
                final String[] line = s.split("\t");
                stringBuilder.append(line[0]);
                stringBuilder.append('\t');
                for (int i = 1; i < line.length; i++) {
                    stringBuilder.append((double) Integer.parseInt(line[i]) / normalizationConstants[i - 1] * 100);
                    if (i < line.length - 1) {
                        stringBuilder.append('\t');
                    }
                }
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
            if (!allowed && taxonomicNode.getChildren().isEmpty()) {
                return "";
            }
            if (allowed && taxonomicNode.getChildren().isEmpty()) {
                stringBuilder.append('\n');
                stringBuilder.append(prefix.substring(0, prefix.length() - 1));
                stringBuilder.append('\t');
                for (int i = 0; i < taxonomicNode.getCount().length; i++) {
                    stringBuilder.append(taxonomicNode.getCount()[i]);
                    if (i < taxonomicNode.getCount().length - 1) {
                        stringBuilder.append('\t');
                    }
                }
            }
            if (allowed) {
                if (taxonomicNode.getChildren().isEmpty()) {
                    return stringBuilder.toString();
                }
                stringBuilder.append('\n');
                stringBuilder.append(prefix.substring(0, prefix.length() - 1));
                stringBuilder.append('\t');
                for (int i = 0; i < taxonomicNode.getCount().length; i++) {
                    stringBuilder.append(taxonomicNode.getCount()[i]);
                    if (i < taxonomicNode.getCount().length - 1) {
                        stringBuilder.append('\t');
                    }
                }
            }
            for (TaxonomicNode child : taxonomicNode.getChildren()) {

                stringBuilder.append(toHMPTreeHelper((CountingTaxonomicNode) child, prefix));
            }
            return stringBuilder.toString();
        }

        public String mergeDatasets(final List<HMPTreesOutput> hmptreeDatasets) {
            final Set<String> masterTaxaList = new TreeSet<>();
            //Combine taxonomy
            for (HMPTreesOutput h : hmptreeDatasets) {
                masterTaxaList.addAll(h.getTaxa());
            }
            //Find out the number of columns in the resulting table
            final int colDim=hmptreeDatasets.stream().mapToInt(hpmt->{return hpmt.getLength();}).sum();
            //Arrange names in column headers
            final String[][] table = new String[masterTaxaList.size() + 1][colDim + 1];
            table[0][0] = "taxonomy";
            for (int i = 1; i < hmptreeDatasets.size() + 1; i++) {
                table[0][i] = hmptreeDatasets.get(i - 1).getName();
            }
            //Insert the sorted master list of taxonomic paths
            int t = 0;
            for (String s : masterTaxaList) {
                table[++t][0] = s;
            }
            //Lookup the master table paths and search through all of the samples
            int positioner=1;
            for (int i = 0; i < hmptreeDatasets.size(); i++) {
                int j = 0;
                for (String s : masterTaxaList) {
                    final List<Double> count = hmptreeDatasets.get(i).getReads().get(s);

                    if (count == null) {
                        for(int k=0;k<hmptreeDatasets.get(i).getLength();k++) {
                            table[++j][positioner+k] = String.valueOf(0);
                        }
                    } else {
                        for(int k=0;k<hmptreeDatasets.get(i).getLength();k++) {
                            table[++j][positioner+k] = String.valueOf(count.get(k));
                        }
                    }
                }
                positioner+=hmptreeDatasets.get(i).getLength();
            }
            final StringBuilder stringBuilder = new StringBuilder();
            for (int i = 0; i < table.length; i++) {
                for (int j = 0; j < table[0].length; j++) {
                    stringBuilder.append(table[i][j]);
                    if (j != table[0].length - 1) {
                        stringBuilder.append("\t");
                    }
                }
                if (i != table.length - 1) {
                    stringBuilder.append('\n');
                }
            }
            return stringBuilder.toString();
        }

        protected void formatComplain(final String line) {
            throw new IllegalArgumentException("The line \"".concat(line).concat("...\" is not properly formatted!"));
        }

        public static class HMPTreesOutput {
            protected final List<String> taxa;
            protected final Map<String, List<Double>> reads;
            protected final String name;
            protected final int length;

            protected HMPTreesOutput(List<String> taxa, Map<String, List<Double>> reads, String name,int length) {
                this.taxa = taxa;
                this.reads = reads;
                this.name = name;
                this.length=length;
            }

            public List<String> getTaxa() {
                return taxa;
            }

            public Map<String, List<Double>> getReads() {
                return reads;
            }

            public String getName() {
                return name;
            }

            public int getLength() {
                return length;
            }

            public static HMPTreesOutput newInstance(final String hmpOutput, final String name) {
                final String[] lines = hmpOutput.split("\n");
                final List<String> taxa = new ArrayList<>();
                final Map<String, List<Double>> counts = new HashMap<>();
                for (String line : lines) {
                    final String[] subsplit = line.split("\t");
                    if (subsplit.length < 2 || !Character.isLetter(subsplit[0].charAt(0)) || !Character.isDigit(subsplit[1].charAt(0))) {
                        throw new IllegalArgumentException("HMPTrees output is not properly formatted!");
                    }
                    taxa.add(subsplit[0]);
                    final List<Double> tableCounts = new ArrayList<>();
                    for (int i = 1; i < subsplit.length; i++) {
                        tableCounts.add(Double.valueOf(subsplit[i]));
                    }
                    counts.put(subsplit[0], tableCounts);
                }
                final int length=lines[0].split("\t").length-1;
                return new HMPTreesOutput(taxa, counts, name,length);
            }
        }
    }

    public static class MothurLineTreeFormatterFormat extends TreeFormatterFormat {

        public static final List<Ranks> rankSequence = Arrays.asList(Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus);

        @Override
        public CountingTaxonomicNode toNodeWithCutoff(String line, int cutoff) {
            line = line.replaceAll("\"", "");
            final String[] split = line.split("\t");
            if (split.length < 2) {
                this.formatComplain(line);
            }
            final String[] taxSplit = split[1].split("\\)\\;");
            final int[] counts = new int[split.length - 2];
            for (int i = 2; i < split.length; i++) {
                counts[i - 2] = Integer.parseInt(split[i]);
            }
            final List<TaxonomicNode> taxonomicNodes = this.taxonomicNodes(taxSplit, counts, cutoff);
            return (CountingTaxonomicNode) taxonomicNodes.get(0);
        }

        @Override
        public CountingTaxonomicNode toNode(String line) {
            return this.toNodeWithCutoff(line, 0);
        }

        private List<TaxonomicNode> taxonomicNodes(final String[] taxa, final int[] counts, int cutoff) {
            final List<TaxonomicNode> taxonomicNodes = new ArrayList<>();
            taxonomicNodes.add(new CountingTaxonomicNode(0, Ranks.no_rank, "root", 0)); //add a pseudoroot
            taxonomicNodes.add(new CountingTaxonomicNode(0, Ranks.no_rank, "cellular organisms", 0));
            int i = 0;
            for (String s : taxa) {
                if (s.startsWith("unknown")) {
                    break;
                }
                final String[] subSplit = s.split("\\(");

                if (!s.contains("unclassified") && Integer.parseInt(subSplit[1]) >= cutoff) {
                    taxonomicNodes.add(new CountingTaxonomicNode(0, rankSequence.get(i++), subSplit[0], Arrays.copyOf(counts, counts.length)));
                } else {
                    break;
                }
            }

            for (i = 0; i < taxonomicNodes.size() - 1; i++) {
                taxonomicNodes.get(i).addChild(taxonomicNodes.get(i + 1));
                taxonomicNodes.get(i + 1).setParent(taxonomicNodes.get(0));
            }
            taxonomicNodes.get(0).setParent(taxonomicNodes.get(0));
            return taxonomicNodes;
        }

        public static File combineTaxonomyAndReadTable(final Path taxonomy, final Path readTable, final Path outFile) throws IOException {

            final Map<String, String> taxSeqMap = new HashMap<>();
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(taxonomy.toFile()))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    final String[] split = line.split("\t");
                    taxSeqMap.put(split[0], split[1]);
                }
            }
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(readTable.toFile()));
                 BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile.toFile()))) {


                String line = bufferedReader.readLine();
                final String[] headerSplit = line.split("\t");
                StringBuilder sb = new StringBuilder();
                sb.append(headerSplit[0]);
                sb.append('\t');
                sb.append("taxonomy\t");
                for (int i = 1; i < headerSplit.length; i++) {
                    sb.append(headerSplit[i]);
                    if (i < headerSplit.length - 1) {
                        sb.append('\t');
                    }
                }
                bufferedWriter.write(sb.toString());
                bufferedWriter.newLine();

                while ((line = bufferedReader.readLine()) != null) {

                    final String[] split = line.split("\t");
                    final String tax = taxSeqMap.get(split[0]);
                    if (tax == null) {

                        continue;
                    }
                    sb = new StringBuilder();
                    sb.append(split[0]);
                    sb.append('\t');
                    sb.append(tax);
                    sb.append('\t');
                    for (int i = 1; i < split.length; i++) {
                        sb.append(split[i]);
                        if (i < split.length - 1) {
                            sb.append('\t');
                        }
                    }
                    bufferedWriter.write(sb.toString());
                    bufferedWriter.newLine();
                }
            }

            return outFile.toFile();
        }
    }

    public static class TuitLineTreeFormatterFormat extends TreeFormatterFormat {

        @Override
        public CountingTaxonomicNode toNodeWithCutoff(String line, int cutoff) {
            return this.toNode(line);
        }

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
            final List<TaxonomicNode> taxonomicNodes = this.taxonomicNodes(taxSplit, Integer.parseInt(acSplit[1]));
            return (CountingTaxonomicNode) taxonomicNodes.get(0);
        }

        private List<TaxonomicNode> taxonomicNodes(final String[] taxa, final int count) {
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


    }

    public static class CountingTaxonomicNode extends TaxonomicNode {

        private int[] count;

        public CountingTaxonomicNode(int taxid, Ranks rank, String scientificName, int count) {
            super(taxid, rank, scientificName);
            this.count = new int[1];
            this.count[0] = count;
        }

        public CountingTaxonomicNode(int taxid, Ranks rank, String scientificName, int[] counts) {
            super(taxid, rank, scientificName);
            this.count = counts;
        }

        public int[] getCount() {
            return count;
        }

        @Override
        public boolean join(TaxonomicNode otherNode) {
            if (!(otherNode instanceof CountingTaxonomicNode)) {
                return false;
            } else {
                if (this.rank.equals(otherNode.getRank()) && this.scientificName.equals(otherNode.getScientificName())) {
                    for (int i = 0; i < this.count.length; i++) {
                        if (i < ((CountingTaxonomicNode) otherNode).count.length) {
                            this.count[i] += ((CountingTaxonomicNode) otherNode).count[i];
                        }
                    }
                    final List<TaxonomicNode> combinedChildren = new ArrayList<>();
                    final List<TaxonomicNode> toRemove = new ArrayList<>();
                    combinedChildren.addAll(this.children);
                    combinedChildren.addAll(otherNode.getChildren());
                    for (TaxonomicNode tn1 : combinedChildren) {
                        for (TaxonomicNode tn2 : combinedChildren) {
                            if (!tn1.equals(tn2) && !toRemove.contains(tn1) && tn1.join(tn2)) {
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
