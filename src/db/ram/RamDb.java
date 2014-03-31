package db.ram;

import db.ram.row.NamesRow;
import db.ram.row.NodesRow;
import logger.Log;
import taxonomy.Ranks;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by alext on 2/12/14.
 */
//TODO: document
public class RamDb implements Serializable {
    static final long serialVersionUID = 2L;

    protected int[] giByTaxIdMap;
    protected final Map<Integer, NamesRow> nameByTaxIdMap;
    protected final Map<Integer, NodesRow> nodeByTaxidMap;

    protected RamDb() {
        this.nameByTaxIdMap = new HashMap<>();
        this.nodeByTaxidMap = new HashMap<>();
    }

    public Integer getTaxIdByGi(final Integer gi) {
        if(gi>=this.giByTaxIdMap.length){
            return null;
        }
        return this.giByTaxIdMap[gi.intValue()];
    }

    public String getNameByTaxId(final Integer taxid) {
        final NamesRow nr=this.nameByTaxIdMap.get(taxid);
        if(nr!=null){
            return nr.getV();
        }else{
            return null;
        }
    }

    public Ranks getRankByTaxId(final Integer taxid) {
        final NodesRow nodesRow=this.nodeByTaxidMap.get(taxid);
        if(nodesRow.getRank()!=null){
           return nodesRow.getRank();
        }else{
            return null;
        }
    }

    public Integer getParetTaxIdByTaxId(final Integer taxid) {
        final NodesRow nodesRow=this.getNodeByTaxId(taxid);
        if(nodesRow!=null){
            return nodesRow.getV();
        }else{
            return null;
        }
    }

    public NodesRow getNodeByTaxId(final Integer taxid) {
        return this.nodeByTaxidMap.get(taxid);
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

    public static RamDb loadSelfFromFilteredNcbiFiles(File gi_taxid_dmp, File names_dmp, File nodes_dmp) throws Exception {

        try (
                final BufferedReader giTaxidReader = new BufferedReader(new FileReader(gi_taxid_dmp));
                final BufferedReader namesReader = new BufferedReader(new FileReader(names_dmp));
                final BufferedReader nodesReader = new BufferedReader(new FileReader(nodes_dmp));
        ) {
            //Create a new instance of the DB
            final RamDb ramDb = new RamDb();

            //Read in the files
            Log.getInstance().log(Level.INFO, "Mapping names...");
            String line;
            while ((line = namesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final NamesRow namesRow = NamesRow.newInstance(Integer.valueOf(split[0]), split[1]);
                ramDb.nameByTaxIdMap.put(namesRow.getK(), namesRow);
            }
            Log.getInstance().log(Level.INFO, "Deploying GIs...");
            //Performance
            int max=0;
            try (final BufferedReader giCount = new BufferedReader(new FileReader(gi_taxid_dmp));) {
                while ((line = giCount.readLine()) != null) {
                    final String[]split=line.split("\t");
                    final int gi=Integer.parseInt(split[0].trim());
                    //TODO: rethink this
                    if(max<gi){
                       max=gi;
                    }else{
                        throw new Exception("Inconsistency in gi_taxid.dmp!");
                    }
                }
            }
            Log.getInstance().log(Level.INFO, "Maximum GI: "+max);
            ramDb.giByTaxIdMap = new int[max+1];
            //End Performance
            int count = 0;
            while ((line = giTaxidReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final int gi=Integer.parseInt(split[0].trim());
                final Integer taxid = Integer.parseInt(split[1].trim());
                while(gi!=count&&count<ramDb.giByTaxIdMap.length){
                    ramDb.giByTaxIdMap[count]=0;
                    count++;
                }
                ramDb.giByTaxIdMap[count]=taxid;
                count++;
            }
            Log.getInstance().log(Level.INFO, "Mapping Nodes...");
            final Map<Integer, Ranks> ranks_ids = new HashMap<>();
            for (Ranks r : Ranks.values()) {
                ranks_ids.put(r.ordinal(), r);
            }
            while ((line = nodesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final Integer taxid = Integer.valueOf(split[0]);
                final NodesRow nodesRow = NodesRow.newInstance(taxid, Integer.valueOf(split[1]), ranks_ids.get(Integer.valueOf(split[2])));
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
