package toolkit.silva;

import db.ram.RamDb;
import logger.Log;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by alext on 8/11/14.
 * TODO document class
 */
public class SilvaHelper {

    private SilvaHelper() {
        throw new AssertionError("Non-instantiable!");
    }


    public static TaxonomicNode createNodesDbFile(Path toNodesFile) throws IOException {

        final Map<String, Integer> names = collectNamesMapFromFile(toNodesFile);
        final Map<String, Ranks> ranks = collectRanksMapFromFile(toNodesFile);

        final TaxonomicNode root = TaxonomicNode.newDefaultInstance(1, Ranks.root_of_life, "root");
        root.setParent(root);
        int counter = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toNodesFile.toFile()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() < 1) {
                    continue;
                }
                final TaxonomicNode sudoRoot = TaxonomicNode.newDefaultInstance(1, Ranks.root_of_life, "root");
                //System.out.println(line);
                final TaxonomicNode firstLevel = convertFromSilvaLine(line, names, ranks);
                sudoRoot.addChild(firstLevel);
                firstLevel.setParent(root);
                counter++;
                System.out.print('\r');
                System.out.print(counter);
                root.join(sudoRoot);
            }
        }
        return root;
    }

    public static Map<String, Integer> collectNamesMapFromFile(Path file) throws IOException {

        final Map<String, Integer> names = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                final String[] split = line.split("\t");
                final Integer taxid = Integer.valueOf(split[1]);
                final String[] taxSplit = split[0].split(";");
                names.put(taxSplit[taxSplit.length - 1], taxid);
            }
        }

        return names;
    }

    public static Map<String, Ranks> collectRanksMapFromFile(Path file) throws IOException {

        final Map<String, Ranks> names = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() < 1) {
                    continue;
                }

                final String[] split = line.split("\t");
                if (split[2].equals("domain")) {
                    split[2] = "superkingdom";
                } else if (split[2].equals("")) {
                    split[2] = "order";
                } else if (split[2].equals("major_clade")) {
                    split[2] = "phylum";
                }
                final Ranks rank = Ranks.convertValue(split[2]);
                final String[] taxSplit = split[0].split(";");
                names.put(taxSplit[taxSplit.length - 1], rank);
            }
        }

        return names;
    }

    public static Map<String, Integer> collectGiTaxidMapFromFile(Path file) throws IOException {

        final Map<String, Integer> gi_taxid = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.length() < 1) {
                    continue;
                }
                final String[] split = line.split("\t");
                gi_taxid.put(split[0], Integer.valueOf(split[1]));
            }
        }

        return gi_taxid;
    }

    public static Map<String, Integer> enrichGiTaxidMapFromSequenceFile(Path toGiTaxidFile, Path toFastaFile, Path toNodesFile, Path toModNodesFile) throws IOException {

        final Map<String, Integer> gi_taxid = collectGiTaxidMapFromFile(toGiTaxidFile);
        int start = findMaxTaxid(toNodesFile);
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toFastaFile.toFile()));
             BufferedReader nodesReader = new BufferedReader(new FileReader(toNodesFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toModNodesFile.toFile()))
        ) {
            String line;
            while ((line = nodesReader.readLine()) != null) {
                bufferedWriter.write(line);
                bufferedWriter.newLine();
            }
            final Map<String, Integer> newSpecies = new HashMap<>();
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(">")) {
                    final String[] split = line.split(";");
                    final String[] acSplit = line.split(" ");
                    final String AC = acSplit[0].substring(1);
                    final String[] speciesSplit = split[split.length - 1].split(" ");
                    if (speciesSplit.length > 1 && speciesSplit[0].equals(split[split.length - 2])) {
                        int taxid = ++start;
                        final String key = line.substring(line.indexOf(" ") + 1);
                        if (newSpecies.containsKey(key)) {
                            taxid = newSpecies.get(key);
                        } else {
                            newSpecies.put(key, taxid);
                            bufferedWriter.write(line.substring(line.indexOf(" ") + 1) + "\t" + taxid + "\tspecies\t\t119+");
                            bufferedWriter.newLine();
                        }
                        gi_taxid.put(AC, taxid);
                    }
                }
            }
        }
        return gi_taxid;
    }


    public static int findMaxTaxid(Path file) throws IOException {
        int max = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()))) {

            String line;
            while ((line = bufferedReader.readLine()) != null) {

                final String[] split = line.split("\t");
                final Integer taxid = Integer.valueOf(split[1]);
                if (taxid > max) {
                    max = taxid;
                }
            }
        }

        return max;
    }

    public static TaxonomicNode convertFromSilvaLine(String line, Map<String, Integer> names, Map<String, Ranks> ranks) {

        final String[] split = line.split("\t");
        final String[] taxSplit = split[0].split(";");
        final List<TaxonomicNode> taxonomicNodes = new ArrayList<>();
        for (String s : taxSplit) {
            final int taxid = names.get(s);
            final Ranks rank = ranks.get(s);
            final TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(taxid, rank, s);
            taxonomicNodes.add(taxonomicNode);
        }
        for (int i = 0; i < taxonomicNodes.size() - 1; i++) {
            taxonomicNodes.get(i).addChild(taxonomicNodes.get(i + 1));
        }

        return taxonomicNodes.get(0);
    }

    public static File replaceUWithT(Path file) throws IOException {
        final Path outFile = file.resolveSibling(file.getFileName().toString().concat(".t"));
        return replaceUWithT(file, outFile);
    }

    public static Map<String, Integer> collectSurrogateACs(Path toModGITaxidFile) throws IOException {

        final Map<String, Integer> map = new HashMap<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toModGITaxidFile.toFile()))) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final Integer surrogateAC = Integer.valueOf(split[0]);
                final String realAC = split[2];
                map.put(realAC, surrogateAC);
            }
        }

        return map;
    }

    public static File appendSurrogatACToSequences(Path toFastaFile, Path toModFastaFile, Map<String, Integer> surrogateMap) throws IOException {

        try (
                BufferedReader bufferedReader = new BufferedReader(new FileReader(toFastaFile.toFile()));
                BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toModFastaFile.toFile()));
        ) {
            String line;
            int counter = 0;
            int outerCounter = 0;
            final Set<String> used=new HashSet<>();
            outer:
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(">")) {
                    outerCounter++;
                    final String[] split = line.split(" ");
                    String realAC = split[0].substring(1);
                    while (!surrogateMap.containsKey(realAC)||used.contains(realAC)) {
                        counter++;
                        outerCounter++;
                        line=scroll(bufferedReader);
                        if(line!=null) {
                            realAC = line.split(" ")[0].substring(1);
                        }else{
                            break outer;
                        }
                    }
                    used.add(realAC);
                    line = ">sgi|"+surrogateMap.get(realAC)+"|ref|" +realAC+"| ";
                }
                if (line != null) {
                    bufferedWriter.write(line);
                    bufferedWriter.newLine();
                }
            }
            System.out.println(counter + " Fasta records excluded out of " + outerCounter);
        }

        return toModFastaFile.toFile();
    }

    private static String scroll(BufferedReader bufferedReader)throws IOException{
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            if (line.startsWith(">")) {
                break;
            }
        }
        return line;
    }


    public static File replaceUWithT(Path file, Path outFile) throws IOException {

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outFile.toFile()));
        ) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                bufferedWriter.write(line.replaceAll("U", "T"));
                bufferedWriter.newLine();
            }
        }
        return outFile.toFile();
    }

    public static File saveNamesToFile(Map<String, Integer> namesMap, Path namesFile) throws IOException {
        final File names = namesFile.toFile();

        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(names))) {
            for (Map.Entry<String, Integer> entry : namesMap.entrySet()) {
                bufferedWriter.write(String.valueOf(entry.getValue()));
                bufferedWriter.write("\t");
                bufferedWriter.write(entry.getKey());
                bufferedWriter.newLine();
            }
        }

        return names;
    }

    public static File saveGiTaxidToFile(Map<String, Integer> giTaxidMap, Path giTaxidFile) throws IOException {
        final File gi_taxid = giTaxidFile.toFile();
        int numerator = 100000;
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(gi_taxid))) {
            for (Map.Entry<String, Integer> entry : giTaxidMap.entrySet()) {
                bufferedWriter.write(String.valueOf(numerator++));
                bufferedWriter.write("\t");
                bufferedWriter.write(String.valueOf(entry.getValue()));
                bufferedWriter.write("\t");
                bufferedWriter.write(entry.getKey());
                bufferedWriter.newLine();
            }
        }

        return gi_taxid;
    }

    public static File saveNodesTableFromTaxonomy(TaxonomicNode taxonomy, Path toNodesFile) throws IOException {
        final File nodes = toNodesFile.toFile();
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(nodes))) {
            bufferedWriter.write(formNodesLine(taxonomy).trim());
        }
        return nodes;
    }

    public static String formNodesLine(TaxonomicNode taxonomicNode) {

        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(taxonomicNode.getTaxid());
        stringBuilder.append('\t');
        stringBuilder.append(taxonomicNode.getParent().getTaxid());
        stringBuilder.append('\t');
        stringBuilder.append(taxonomicNode.getRank().ordinal());
        stringBuilder.append('\n');

        if (taxonomicNode.getChildren().size() > 0) {

            final int parentTaxid = taxonomicNode.getTaxid();
            for (TaxonomicNode child : taxonomicNode.getChildren()) {
                stringBuilder.append(formNodesLine(child));
            }
        }
        return stringBuilder.toString();
    }

    public static void create(Path toNodesFile, Path toEnrichedNodesFile, Path toModNodesFile,
                              Path toGiTaxIdFile, Path toModGITaxidFile,
                              Path toFastaFile, Path toModFastaFile,
                              Path toModNamesFile,
                              Path toRAMDBFile, Path toRenamedFastaFile) throws IOException {

        //Replace all U for T in fasta
        System.out.println("Reformatting fasta sequence file..");
        //replaceUWithT(toFastaFile, toModFastaFile);

        //Enrich the taxonomy
        System.out.println("Enriching taxonomy..");
        final Map<String, Integer> gi_taxid_enrichment = enrichGiTaxidMapFromSequenceFile(toGiTaxIdFile, toFastaFile, toNodesFile, toEnrichedNodesFile);
        System.out.println("Saving GI taxID list to " + toModGITaxidFile.toString() + " ..");
        saveGiTaxidToFile(gi_taxid_enrichment, toModGITaxidFile);

        //Collect names
        System.out.println("Collecting names..");
        final Map<String, Integer> names = collectNamesMapFromFile(toEnrichedNodesFile);
        System.out.println("Saving names to " + toModNamesFile.toString() + " ..");
        saveNamesToFile(names, toModNamesFile);

        //Create taxonomy
        System.out.println("Generating taxonomy nodes..");
        //final TaxonomicNode taxonomy = createNodesDbFile(toEnrichedNodesFile);
        System.out.println("Saving taxonomy nodes file to " + toModNodesFile + " ..");
        //saveNodesTableFromTaxonomy(taxonomy, toModNodesFile);

        //Create RamDB
        System.out.println("Generating RAM DB..");

        try {
            Log.getInstance().setLogName("test");
            final RamDb ramDb = RamDb.loadSelfFromFilteredNcbiFiles(toModGITaxidFile.toFile(), toModNamesFile.toFile(), toModNodesFile.toFile());
            System.out.println("Saving RAM DB..");
            RamDb.serialize(ramDb, toRAMDBFile.toFile());
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Append surrogate ACs to the fasta records
        final Map<String, Integer> surrogateKeyMap = SilvaHelper.collectSurrogateACs(toModGITaxidFile);
        SilvaHelper.appendSurrogatACToSequences(toModFastaFile, toRenamedFastaFile, surrogateKeyMap);

        System.out.println("Done.");
    }

}
