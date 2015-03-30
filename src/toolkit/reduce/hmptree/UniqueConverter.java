package toolkit.reduce.hmptree;

import javafx.util.Pair;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by alext on 2/4/15.
 */
public class UniqueConverter {
    public static int counter = 0;

    public static void main(String[] args) {


        final Path toUniqueTaxFile = Paths.get(args[0]);
        final Path toNamesFile = Paths.get(args[1]);
        final Path toGroupsFile = Paths.get(args[2]);

        try {
            final Map<String, List<String>> names = collectNames(toNamesFile);
            final Map<String, String> groups = collectGroups(toGroupsFile);
            final List<Pair<String, String>> taxonomy = getTaxonomy(toUniqueTaxFile);
            //System.out.println(groups.size());
            final Map<String, StringBuilder> groupMap = new HashMap<>();
            final Set<String> groupNames = new HashSet<>(groups.values());
            for (String s : groupNames) {
                groupMap.put(s, new StringBuilder());
            }


            for (Pair<String, String> p : taxonomy) {
                final Map<String, Integer> countsForGroups = new HashMap<>();
                final List<String> namesAcs = names.get(p.getKey());
                if(namesAcs==null) {
                    System.out.println(p.getKey());
                }
                for (String s : namesAcs) {
                    final String groupName = groups.get(s);
                    if (countsForGroups.containsKey(groupName)) {
                        countsForGroups.put(groupName, countsForGroups.get(groupName) + 1);
                    } else {
                        countsForGroups.put(groupName, 1);
                    }
                }
                for (String s : countsForGroups.keySet()) {

                    final StringBuilder groupOutput = groupMap.get(s);
                    System.out.println(p.getKey()+" "+p.getValue());
                    final int count = countsForGroups.get(s);
                    counter+=count;
                    if(groupOutput==null){
                        System.out.println(s);
                    }
                    groupOutput.append("0@");
                    groupOutput.append(count);
                    groupOutput.append(":\t");
                    groupOutput.append(p.getValue());
                    groupOutput.append('\n');
                }
            }

            for (Map.Entry<String, StringBuilder> e : groupMap.entrySet()) {

                final Path toFile = toUniqueTaxFile.resolveSibling(e.getKey().concat(".rdc.tuit"));
                saveGroup(toFile, e.getValue().toString());

            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println(counter);
    }

    public static Path saveGroup(final Path toFile, String group) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(toFile.toFile()))) {
            bufferedWriter.write(group);
        }
        return toFile;
    }

    public static Map<String, List<String>> collectNames(final Path toNamesFile) throws IOException {

        final Map<String, List<String>> namesMap = new HashMap<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toNamesFile.toFile()))) {
            bufferedReader.lines().filter(line -> line.length() != 0).forEach(line -> {
                final String[] split = line.split("\t");
                final String[] subsplit = split[1].split("\\,");
                namesMap.put(split[0], Arrays.asList(subsplit));
            });
        }

        return namesMap;
    }

    public static Map<String, String> collectGroups(final Path toGroupsFile) throws IOException {
        final Map<String, String> map = new HashMap<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toGroupsFile.toFile()))) {
            bufferedReader.lines().filter(line -> line.length() != 0).forEach(line -> {
                final String[] split = line.split("\t");
                map.put(split[0], split[1]);
            });
        }
        return map;
    }

    public static List<Pair<String, String>> getTaxonomy(final Path toTaxonomyFile) throws IOException {
        final List<Pair<String, String>> list;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toTaxonomyFile.toFile()))) {
            list = bufferedReader.lines().filter(line -> line.length() != 0).map(line -> {
                final int firstTab = line.indexOf('\t');
                final String ac = line.substring(0, firstTab - 1);
                final String taxonomy = line.substring(firstTab + 1, line.length());
                return new Pair<>(ac.toLowerCase(), taxonomy);
            }).collect(Collectors.toList());
        }
        return list;
    }

}
