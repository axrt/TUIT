package toolkit.reduce.hmptree.script;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by alext on 5/30/14.
 * TODO document class
 */
public class DesSplitGroups {

    public static void main(String[] args) {

        //First read the driver
        final List<String> cases = new ArrayList<>();

        final List<String> controls = new ArrayList<>();

        final List<String> excludes = new ArrayList<>();

        final List<String> junk = new ArrayList<>();
        //read the drv
        final Path drvPath = Paths.get("/home/alext/Documents/Ocular Project/sequencing/DES/miseq/my_processing/des.drv");
        try {
            try (BufferedReader bufferedReader = new BufferedReader(new FileReader(drvPath.toFile()))) {

                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    line=line.toUpperCase();
                    final String[] split = line.split("\t");
                    switch (split[1]) {
                        case "CASE":
                            cases.add(split[0]);
                            break;
                        case "CONTROL":
                            controls.add(split[0]);
                            break;
                        case "EXCLUDE":
                            excludes.add(split[0]);
                            break;
                        default:
                            junk.add(split[0]);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        final List<String> casesSkin = splitList(cases, "S");
        final List<String> casesEyes = splitList(cases, "E");
        final List<String> controlsSkin = splitList(controls, "S");
        final List<String> controlsEyes = splitList(controls, "E");
        final List<String> excludesSkin = splitList(excludes, "S");
        final List<String> excludesEyes = splitList(excludes, "E");
        //Read the taxonomy file
        final Path taxoPath = drvPath.resolveSibling("hmptree.out");
        final List<Column> columns = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(taxoPath.toFile()))) {

            String line = bufferedReader.readLine();
            final String[] headerSplit = line.split("\t");
            for (String s : headerSplit) {
                columns.add(new Column(s));
            }

            while ((line = bufferedReader.readLine()) != null) {
                line=line.toUpperCase();
                final String[] split = line.split("\t");
                int i = 0;
                for (String s : split) {
                    columns.get(i++).add(s);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Hash down for faster search
        final Map<String, Column> columnMap = new HashMap<>();
        for (Column c : columns) {
            columnMap.put(c.getName(), c);
        }


        final Path casEpath = taxoPath.resolveSibling("cases.eye.hmptree");
        final Path casSpath = taxoPath.resolveSibling("cases.skin.hmptree");
        final Path contEpath = taxoPath.resolveSibling("control.eye.hmptree");
        final Path contSpath = taxoPath.resolveSibling("control.skin.hmptree");
        final Path exclEpath = taxoPath.resolveSibling("exclude.eye.hmptree");
        final Path exclSpath = taxoPath.resolveSibling("exclude.skin.hmptree");

        final String firstColumn = columns.get(0).getName();
        try {
            writeOutGroup(casEpath, collectGroup(columnMap, casesEyes, firstColumn));
            writeOutGroup(casSpath, collectGroup(columnMap, casesSkin, firstColumn));
            writeOutGroup(contEpath, collectGroup(columnMap, controlsEyes, firstColumn));
            writeOutGroup(contSpath, collectGroup(columnMap, controlsSkin, firstColumn));
            writeOutGroup(exclEpath, collectGroup(columnMap, excludesEyes, firstColumn));
            writeOutGroup(exclSpath, collectGroup(columnMap, excludesSkin, firstColumn));
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private static class Column extends ArrayList<String> {
        private final String name;

        public String getName() {
            return name;
        }

        private Column(String name) {
            super();
            this.name = name;
        }
    }

    private static List<String> splitList(final List<String> strings, final String delim) {
        final List<String> toRetrun = new ArrayList<>(strings.size());
        for (String s : strings) {
            if (s.endsWith(delim)) {
                toRetrun.add(s);
            }
        }
        return toRetrun;
    }

    private static void writeOutGroup(final Path outfile, final List<Column> columns) throws IOException {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outfile.toFile()))) {
            final StringBuilder headerBuilder = new StringBuilder();
            for (Column c : columns) {
                headerBuilder.append(c.getName());
                headerBuilder.append('\t');
            }
            bufferedWriter.write(headerBuilder.toString().trim());
            bufferedWriter.newLine();
            for (int i = 0; i < columns.get(0).size(); i++) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(columns.get(0).get(i));
                stringBuilder.append('\t');
                for (int j=1;j<columns.size();j++) {
                    String writeOut=columns.get(j).get(i);
                    if(writeOut.length()>5){
                        stringBuilder.append(writeOut.substring(0,5));
                    }else {
                        stringBuilder.append(writeOut);
                    }


                    stringBuilder.append('\t');

                }
                bufferedWriter.write(stringBuilder.toString().trim());
                bufferedWriter.newLine();
            }
        }
    }

    private static List<Column> collectGroup(final Map<String, Column> map, final List<String> group, final String firstColumn) {
        final List<Column> collected = new ArrayList<>();
        collected.add(map.get(firstColumn));
        for (String s : group) {
            collected.add(map.get(s));
        }
        return collected;
    }
}
