package helper.greengenes;

import format.fasta.Fasta;
import helper.NCBITablesDeployer;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;
import util.SystemUtil;

import java.io.*;
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

    public static File reformatSequenceDatabase(final Path pathToSequenceDatabase, final Path toReformattedSequenceDatabase) throws IOException {
        final File reformattedSequenceDatabaseFile = toReformattedSequenceDatabase.toFile();
        try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(pathToSequenceDatabase.toFile()));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(reformattedSequenceDatabaseFile))
        ) {
            bufferedReader.lines().forEach(line -> {
                try {
                    if (line.startsWith(Fasta.fastaStart)) {

                        bufferedWriter.write(Fasta.fastaStart.concat("ggid|").concat(line.substring(1)));

                    } else {
                        bufferedWriter.write(line);
                    }
                    bufferedWriter.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
        return reformattedSequenceDatabaseFile;
    }


    public static NCBITablesDeployer.TaxonomyFiles convertToTaxonomyFiles(final Path taxonomyFile, final Path outputFolder) throws IOException {
        final TaxonomicConverter tc = new TaxonomicConverter();
        try (
                InputStream inputStream = new FileInputStream(taxonomyFile.toFile())) {

            tc.fillIn(inputStream);

        }
        final TaxonomicNode taxonomy = tc.root;
        final Map<Integer, TaxonomicNode> giTaxonomyMap = tc.giTaxonomyMap;

        final Path nodesFilePath = outputFolder.resolve(SystemUtil.NODES_FILE);
        final Path namesFilePath = outputFolder.resolve(SystemUtil.NAMES_FILE);

        try (
                BufferedWriter nodesWriter = new BufferedWriter(new FileWriter(nodesFilePath.toFile()));
                BufferedWriter namesWriter = new BufferedWriter(new FileWriter(namesFilePath.toFile()));
        ) {
            tc.stream().flatMap(map -> map.entrySet().stream()).forEach(entry -> {
                try {
                    nodesWriter.write(formatNode(entry.getValue()));
                    nodesWriter.newLine();
                    namesWriter.write(formatName(entry.getValue()));
                    namesWriter.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        final Path giTaxidFilePath = outputFolder.resolve(SystemUtil.GI_TAXID_FILE);
        try (
                BufferedWriter giTaxidWriter = new BufferedWriter(new FileWriter(giTaxidFilePath.toFile()))) {
            tc.giTaxonomyMap.entrySet().stream().sorted(Comparator.comparing(Map.Entry::getKey)).forEach(entry -> {
                try {
                    giTaxidWriter.write(formatGiTaxid(entry.getKey(), entry.getValue()));
                    giTaxidWriter.newLine();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (UncheckedIOException e) {
            throw e.getCause();
        }

        return new NCBITablesDeployer.TaxonomyFiles.TaxonomyFilesBuilder().giTaxidDmp(giTaxidFilePath).namesDmp(namesFilePath).nodesDmp(nodesFilePath).build();
    }

    private static String formatGiTaxid(final Integer gi, final TaxonomicNode taxonomicNode) {
        return String.valueOf(gi).concat("\t").concat(String.valueOf(taxonomicNode.getTaxid()));
    }

    private static String formatNode(final TaxonomicNode taxonomicNode) {
        return String.valueOf(taxonomicNode.getTaxid())
                .concat("\t")
                .concat(String.valueOf(taxonomicNode.getParent().getTaxid()))
                .concat("\t")
                .concat(String.valueOf(taxonomicNode.getRank().ordinal()));
    }

    private static String formatName(final TaxonomicNode taxonomicNode) {
        return String.valueOf(taxonomicNode.getTaxid()).concat("\t").concat(taxonomicNode.getScientificName());
    }

    private static final class TaxonomicConverter extends ArrayList<Map<String, TaxonomicNode>> {

        private static final List<Ranks> GG_RANKS = Arrays.asList(
                new Ranks[]{
                        Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus, Ranks.species
                }
        );

        private final static Map<Ranks, Integer> GG_RANK_MAP = new HashMap<>();

        static {
            GG_RANKS.stream().forEach((rank) -> GG_RANK_MAP.put(rank, GG_RANKS.indexOf(rank)));
        }

        private int taxId;
        private final TaxonomicNode root;
        private final Map<Integer, TaxonomicNode> giTaxonomyMap;

        private TaxonomicConverter() {
            this.taxId = 1;
            this.giTaxonomyMap = new HashMap<>();
            this.root = TaxonomicNode.newDefaultInstance(taxId++, Ranks.root_of_life, Ranks.root_of_life.getName());
            this.root.setParent(this.root);
            IntStream.range(0, 7).forEach((i) -> this.add(i, new HashMap<>()));
        }

        private void addRank(final String taxon, final Ranks rank) {
            final Map<String, TaxonomicNode> map = this.getRankMap(rank);
            if (!map.keySet().contains(taxon)) {
                map.put(taxon, TaxonomicNode.newDefaultInstance(this.taxId++, rank, taxon));
            }
        }

        private void attachToRoot() {

            this.get(0).entrySet().stream().forEach(entry -> {
                entry.getValue().setParent(root);
                root.addChild(entry.getValue());
            });

        }

        private void connectNodes(final String parent, final String child, final Ranks parentRank) {
            if (child.equals("")) return;

            final Ranks childRank = GG_RANKS.get(GG_RANK_MAP.get(parentRank) + 1);
            final TaxonomicNode childNode = this.getRankMap(childRank).get(child);
            final TaxonomicNode parentNode = this.getRankMap(parentRank).get(parent);
            parentNode.addChild(childNode);
            childNode.setParent(parentNode);
        }

        private Map<String, TaxonomicNode> getRankMap(final Ranks rank) {
            return this.get(GG_RANK_MAP.get(rank));
        }

        private void addGGLine(final List<String> line) {
            IntStream.range(1, line.size() - 1).forEach((i) -> {
                final String parent = line.get(i);
                final String child = line.get(i + 1);
                if (child.equals("")) {
                    return;
                }
                final Ranks parentRank = GG_RANKS.get(i - 1);
                final Ranks childRank = GG_RANKS.get(i);
                this.addRank(parent, parentRank);
                this.addRank(child, childRank);
                this.connectNodes(parent, child, parentRank);
            });
            final Integer gi = Integer.parseInt(line.get(0));
            this.giTaxonomyMap.put(gi, this.find(line));
        }

        private TaxonomicNode find(final List<String> line) {
            for (int i = this.size(); i > 0; i--) {
                if (this.get(i - 1).containsKey(line.get(i))) {
                    return this.get(i - 1).get(line.get(i));
                }
            }
            throw new IllegalArgumentException();
        }

        public void fillIn(final InputStream inputStream) throws IOException {
            try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                bufferedReader.lines().forEach(line -> this.addGGLine(line));
            }
            this.attachToRoot();
        }

        private void addGGLine(String line) {
            final String[] GGIsplit = line.split("\t");
            final String[] taxaSplit = GGIsplit[1].split("; ");
            //TODO think of additional checks
            if (GGIsplit.length != 2 || taxaSplit.length != 7) {
                this.dieOnMissformat(line);
            }
            final List<String> listRepresentation = Arrays.asList(taxaSplit).stream().map(taxa -> taxa.substring(3)).collect(Collectors.toList());
            listRepresentation.add(0, GGIsplit[0]);
            this.addGGLine(listRepresentation);
        }

        private void dieOnMissformat(final String line) {
            throw new IllegalArgumentException("Please check the input format, a given line: \"".concat(line).concat("\" does not seem to be in GreenGenes format."));
        }

        public TaxonomicNode getTaxonomy() {
            return this.root;
        }
    }
}
