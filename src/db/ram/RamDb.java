package db.ram;

import db.ram.row.GiTaxIdRow;
import db.ram.row.NamesRow;
import db.ram.row.NodesRow;
import taxonomy.Ranks;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by alext on 2/12/14.
 */
//TODO: document
public class RamDb implements Serializable {

    protected final Map<Integer, GiTaxIdRow> giByTaxIdMap;
    protected final Map<Integer, NamesRow> nameByTaxIdMap;
    protected final Map<Integer, NodesRow> nodeByTaxidMap;

    protected RamDb() {
        this.giByTaxIdMap = new HashMap<>();
        this.nameByTaxIdMap = new HashMap<>();
        this.nodeByTaxidMap = new HashMap<>();
    }

    public static RamDb loadSelfFromFile(File objDb) throws IOException, ClassNotFoundException {
        try (
                final ObjectInputStream objectInputStream = new ObjectInputStream(new FileInputStream(objDb));
        ) {
            final RamDb ramDb = (RamDb) objectInputStream.readObject();
            return ramDb;
        } catch (IOException | ClassCastException e) {
            throw e;
        }
    }

    public static RamDb loadSelfFromFilteredNcbiFiles(File gi_taxid_dmp, File names_dmp, File nodes_dmp) throws IOException {

        try (
                final BufferedReader giTaxidReader = new BufferedReader(new FileReader(gi_taxid_dmp));
                final BufferedReader namesReader = new BufferedReader(new FileReader(names_dmp));
                final BufferedReader nodesReader = new BufferedReader(new FileReader(nodes_dmp));
        ) {
            //Create a new instance of the DB
            final RamDb ramDb = new RamDb();

            //Read in the files
            String line;
            while ((line = namesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final NamesRow namesRow = NamesRow.newInstance(Integer.parseInt(split[0]), split[1]);
                ramDb.nameByTaxIdMap.put(namesRow.getK(), namesRow);
            }
            while ((line = giTaxidReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final Integer taxid = Integer.parseInt(split[1].trim());
                if (ramDb.nameByTaxIdMap.containsKey(taxid)) {
                    final GiTaxIdRow giTaxIdRow = GiTaxIdRow.newInstance(Integer.parseInt(split[0]), taxid);
                    ramDb.giByTaxIdMap.put(giTaxIdRow.getK(), giTaxIdRow);
                }
            }
            final Map<Integer, Ranks> ranks_ids = new HashMap<>();
            for (Ranks r : Ranks.values()) {
                ranks_ids.put(r.ordinal() + 1, r);
            }
            while ((line = nodesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final Integer taxid = Integer.parseInt(split[0]);
                final NodesRow nodesRow = NodesRow.newInstance(taxid, Integer.parseInt(split[1]), ranks_ids.get(Integer.parseInt(split[2])));
                ramDb.nodeByTaxidMap.put(nodesRow.getK(), nodesRow);
            }

            return ramDb;

        } catch (IOException e) {
            throw e;
        }

    }

    public static File serialize(RamDb ramDb, File out) throws IOException {
        try {
            try (
                    final ObjectOutputStream objectOutputStream = new ObjectOutputStream(new FileOutputStream(out));
            ) {
                objectOutputStream.writeObject(ramDb);
                return out;
            }
        } catch (IOException e) {
            throw e;
        }
    }
}
