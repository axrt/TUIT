package db.ram;

import db.ram.row.NamesRow;
import db.ram.row.NodesRow;
import db.ram.row.RamRow;
import logger.Log;
import taxonomy.Ranks;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */


/**
 * A serializable object, that is used as a taxonomic database. Implements {@link java.io.Serializable}, thereby needs to be deployed only once
 * and can be further used by loading into RAM. Currently the amount of RAM necessary to load the object is around 4GB.
 * Usage of this object dramatically increases the speed of classification comparing to RDBMS (even well optimized), moreover, the deployment
 * process is way faster.
 */
public class RamDb implements Serializable {
    /**
     * Serial version. TUIT will complain if this version goes obsolete and requests to redeploy the database.
     */
    static final long serialVersionUID = 2L;
    /**
     * Gi-taxId map. A use of a plain int[] array increases the amount of ram, that the object takes, however,
     * the dramatic benefit is the speed of access. GI in this case is the index, and the taxId is the corresponding int[i];
     * As long as the NCBI database issues new GIs and may delete the old ones, this array may have zeros (int[i]==0 is true) and this means that the
     * NCBI database does not have a GI (the GI most likely had been there, but was deleted). This approach is way faster than using {@link java.util.HashMap}
     * especially in terms of deployment speed.
     */
    protected int[] giByTaxIdMap;
    /**
     * Allows for a fast search of a row of GI-scientific name pair, mapped by the given GI
     */
    protected final Map<Integer, RamRow<Integer, String>> nameByTaxIdMap;
    /**
     * Allows for a fast search of a row of taxId-parentTaxId-taxnomic rank, mapped by a given taxId
     */
    protected final Map<Integer, NodesRow> nodeByTaxidMap;

    /**
     * Protected constructor
     */
    protected RamDb() {
        //Initialize both maps
        this.nameByTaxIdMap = new HashMap<>();
        this.nodeByTaxidMap = new HashMap<>();
    }

    /**
     * A getter of a taxId by a given GI
     *
     * @param gi {@link java.lang.Integer} GI
     * @return {@link java.lang.Integer} corresponding taxID, {@code null} case null is passed, or the given GI is out of range.
     * A returned null in the latter case indicates that the database should be updated.
     */
    public Integer getTaxIdByGi(final Integer gi) {
        //Check if the given GI is in the database.
        if (gi == null) {
            return null;
        }
        if (gi >= this.giByTaxIdMap.length) {
            //A returned null must indicate that the database should be updated
            return null;
        }
        //Safely return a corresponding taxId
        return this.giByTaxIdMap[gi.intValue()];
    }

    /**
     * A getter of a scientific name by a given taxId
     *
     * @param taxid {@link java.lang.Integer} taxId
     * @return {@link java.lang.String} scientific name, {@code null} if {@code null} passed
     */
    public String getNameByTaxId(final Integer taxid) {
        final RamRow<Integer, String> nr = this.nameByTaxIdMap.get(taxid);
        if (nr != null) {
            return nr.getV();
        } else {
            return null;
        }
    }

    /**
     * A getter for a taxonomic rank by a given taxId
     *
     * @param taxid {@link java.lang.Integer} taxId
     * @return {@link taxonomy.Ranks} taxonomic rank, or {@code null} if no rank was found by a given taxId, {@code null} if {@code null} passed
     */
    public Ranks getRankByTaxId(final Integer taxid) {
        final NodesRow nodesRow = this.nodeByTaxidMap.get(taxid);
        if (nodesRow.getRank() != null) {
            return nodesRow.getRank();
        } else {
            return null;
        }
    }

    /**
     * A getter for a parent taxId by a given taxId
     *
     * @param taxid {@link java.lang.Integer} of a node that need to be evaluated for its parent
     * @return {@link java.lang.Integer} parent's taxId or {@code null} if no parent's taxId was found by a given taxId, {@code null} if {@code null} passed
     */
    public Integer getParetTaxIdByTaxId(final Integer taxid) { //Not used in this implementation of TUIT
        final NodesRow nodesRow = this.getNodeByTaxId(taxid);
        if (nodesRow != null) {
            return nodesRow.getV();
        } else {
            return null;
        }
    }

    /**
     * A getter for a {@link db.ram.row.NodesRow} by the given taxId
     *
     * @param taxid {@link java.lang.Integer} given taxId
     * @return Corresponding {@link db.ram.row.NodesRow} or {@code null} if non found for a given taxId
     */
    public NodesRow getNodeByTaxId(final Integer taxid) {
        return this.nodeByTaxidMap.get(taxid);
    }

    /**
     * A static helper method that allows the class to load an instance of self for HDD and deserialize it.
     *
     * @param objDb {@link java.io.File} pointing to the serialized object
     * @return {@link db.ram.RamDb}, that was loaded from HDD
     * @throws IOException            in case and IO error occurs
     * @throws ClassNotFoundException in case the objDb points to an object of an incorrect class
     */
    public static RamDb loadSelfFromFile(final File objDb) throws IOException, ClassNotFoundException {
        try (
                ObjectInputStream objectInputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(objDb)));
        ) {
            final RamDb ramDb = (RamDb) objectInputStream.readObject();
            return ramDb;
        } catch (IOException | ClassCastException e) {
            throw e;
        }
    }

    /**
     * A static method that allows the class to create and return an instance of self using the corresponding files
     * from the <a href="ftp://ftp-trace.ncbi.nlm.nih.gov/">NCBI FTP server</a>;
     * The files used are
     *
     * <a href="ftp://ftp-trace.ncbi.nlm.nih.gov/pub/taxonomy/gi_taxid_nucl.dmp.gz">GI-TaxID dump</a>;
     * <a href="ftp://ftp-trace.ncbi.nlm.nih.gov/pub/taxonomy/taxdump.tar.gz">Combined taxonomy dump</a>, which contains a number of files as nodes.dmp and names.dmp, that contain taxId-parentTaxid and taxId-scientific name pairs accordingly.
     *
     * After those files are downloaded (see {@link helper.NCBITablesDeployer}, the method can use modified (with a ".mod" extension) files.
     *
     * @param gi_taxid_dmp {@link java.io.File} that point to a modified gi_taxid.dmp.mod
     * @param names_dmp    {@link java.io.File} that point to a modified names.dmp.mod
     * @param nodes_dmp    {@link java.io.File} that point to a modified nodes.dmp.mod
     * @return a newly created {@link db.ram.RamDb} database
     * @throws Exception {@link java.io.IOException} in case an IO problem arises upon file read, {@link java.lang.Exception} in case any of the given files were misformatted
     */
    public static RamDb loadSelfFromFilteredNcbiFiles(File gi_taxid_dmp, File names_dmp, File nodes_dmp) throws Exception {

        try (
                BufferedReader giTaxidReader = new BufferedReader(new FileReader(gi_taxid_dmp));
                BufferedReader namesReader = new BufferedReader(new FileReader(names_dmp));
                BufferedReader nodesReader = new BufferedReader(new FileReader(nodes_dmp));
        ) {
            //Create a new instance of the DB
            final RamDb ramDb = new RamDb();

            //Read in the files
            Log.getInstance().log(Level.INFO, "Mapping names...");
            String line;
            //Read names line by line and create row objects for each line, put them into map
            while ((line = namesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final RamRow<Integer, String> namesRow = NamesRow.newInstance(Integer.valueOf(split[0]), split[1]);
                ramDb.nameByTaxIdMap.put(namesRow.getK(), namesRow);
            }
            Log.getInstance().log(Level.INFO, "Deploying GIs...");
            //Performance, described in giByTaxIdMap comment
            int max = 0;
            int i=0;
            try (BufferedReader giCount = new BufferedReader(new FileReader(gi_taxid_dmp));) {
                System.out.println(gi_taxid_dmp);
                while ((line = giCount.readLine()) != null) {
                    final String[] split = line.split("\t");
                    int gi=0;
                    try {
                        gi = Integer.parseInt(split[0].trim());
                    }catch (NumberFormatException e){
                        System.out.println(split[0]);
                        System.out.println(i);
                    }
                    //Check for consistent increasing sorting of the NCBI database
                    //Seems like it, but not tested and not guaranteed by the NCBI
                    if (max < gi) {
                        max = gi;
                        i++;
                    } else {
                        throw new Exception("Inconsistency in gi_taxid.dmp!");
                    }
                }
            }
            Log.getInstance().log(Level.INFO, "Maximum GI: " + max);
            //Create a Gi-taxId map and start filling it in
            ramDb.giByTaxIdMap = new int[max + 1];
            //End Performance
            int count = 0;
            while ((line = giTaxidReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final int gi = Integer.parseInt(split[0].trim());
                final Integer taxid = Integer.parseInt(split[1].trim());
                while (gi != count && count < ramDb.giByTaxIdMap.length) {
                    ramDb.giByTaxIdMap[count] = 0;
                    count++;
                }
                ramDb.giByTaxIdMap[count] = taxid;
                count++;
            }
            Log.getInstance().log(Level.INFO, "Mapping Nodes...");
            final Map<Integer, Ranks> ranks_ids = new HashMap<>();
            for (Ranks r : Ranks.values()) {
                ranks_ids.put(r.ordinal(), r);
            }
            //Take care of the nodes in a similar fashion:
            //read line by line and create a corresponding row object, put a newly created object in a map
            while ((line = nodesReader.readLine()) != null) {
                final String[] split = line.split("\t");
                final Integer taxid = Integer.valueOf(split[0]);
                final NodesRow nodesRow = NodesRow.newInstance(taxid, Integer.valueOf(split[1]), ranks_ids.get(Integer.valueOf(split[2])));
                ramDb.nodeByTaxidMap.put(nodesRow.getK(), nodesRow);
            }
            //Finally return the database object
            return ramDb;
        }
    }

    /**
     * A static method that allows the class to serialize and instance of self to the hard drive
     *
     * @param ramDb {@link db.ram.RamDb} an instance to serialize
     * @param out   {@link java.io.File} the same file, that was passed for serialization (see @return)
     * @return {@link java.io.File} that can be further used as a pointer to this serialized object,
     * @throws IOException but throws in case the object was not serialized and the file does not point to a correct object
     */
    public static File serialize(RamDb ramDb, File out) throws IOException {
        try (
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(out)));
        ) {
            objectOutputStream.writeObject(ramDb);
            return out;
        }
    }
}
