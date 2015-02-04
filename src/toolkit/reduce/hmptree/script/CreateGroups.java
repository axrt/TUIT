
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by alext on 2/2/15.
 */
public class CreateGroups {

    public static void main(String[] args) {

        final Path toDriverFile = Paths.get(args[0]);
        final Map<Path, String> groupMap;

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(toDriverFile.toFile()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("groups.groups"))) {

            groupMap = new HashMap<>();
            bufferedReader.lines().filter(line -> line.length() != 0).forEach(line -> {
                final String[] split = line.split("\t");
                groupMap.put(Paths.get(split[0].trim()), split[1].trim());

            });

            for (Path p : groupMap.keySet()) {
                final List<String> acs = getACsFromFile(p);
                for(String s:acs){
                    bufferedWriter.write(s);
                    bufferedWriter.write('\t');
                    bufferedWriter.write(groupMap.get(p));//inefficient
                    bufferedWriter.newLine();
                }
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }


    public static List<String> getACsFromFile(final Path toFile) throws IOException {
        final List<String>acs;
        try(BufferedReader bufferedReader=new BufferedReader(new FileReader(toFile.toFile()))){
            acs=bufferedReader.lines().filter(line->line.startsWith(">")).map(line->{
                return line.substring(1).trim();
            }).collect(Collectors.toList());
        }
        return acs;
    }

}
