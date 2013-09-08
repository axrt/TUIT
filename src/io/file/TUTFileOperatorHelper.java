package io.file;

import db.tables.LookupNames;
import format.EncodedFasta;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception;
import io.properties.jaxb.TUITProperties;
import logger.Log;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.net.URL;
import java.net.URLEncoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * A helper class that allows to load TUIT io.properties from an XML formatted input
 */
public class TUTFileOperatorHelper {

    private TUTFileOperatorHelper() {
        throw new AssertionError();
    }

    /**
     * Return a {@link TUITProperties} from an {@code InputStream}. Used by
     * {@link TUITProperties} to get the output. Being produced in such a form, it
     * allows to store the schemas in the same package as the
     * {@link TUITProperties}, thereby allowing to make it obscure from the user
     * within the package
     *
     * @param in :{@link java.io.InputStream } from a URL or other type of connection
     * @return {@link TUITProperties}
     * @throws SAXException                 upon SAX parsing error
     * @throws JAXBException                upon unmarshalling
     */
    public static TUITProperties catchProperties(InputStream in)
            throws SAXException, JAXBException {
        JAXBContext jc = JAXBContext.newInstance(TUITProperties.class);
        Unmarshaller u = jc.createUnmarshaller();
        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        xmlreader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlreader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                true);
        xmlreader.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException, IOException {
                String file;
                if (systemId.contains("properties.dtd")) {
                    file = "properties.dtd";
                } else {
                    throw new SAXException("Wrong name for the schema dtd, please correct to \n" +
                            "<!DOCTYPE TUITProperties PUBLIC \"-//TUIT//TUITProperties/EN\" \"properties.dtd\">");
                }
                return new InputSource(TUITProperties.class
                        .getResourceAsStream(file));
            }
        });
        InputSource input = new InputSource(in);
        Source source = new SAXSource(xmlreader, input);
        return (TUITProperties) u.unmarshal(source);
    }

    /**
     * @param file {@link java.io.File} a file that contains the a list of fasta records (may be represented by a single record
     * @return {@link java.util.List < format.EncodedFasta >} of fasta records
     * @throws IOException in case opening and reading the file fails
     * @throws format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception
     *                     in case of a single line format or none at all
     */
    public static List<EncodedFasta> loadOTURecords(File file) throws IOException,
            NucleotideFasta_BadFormat_Exception {
        //Open file and check whether it is even Fasta at all
        List<EncodedFasta> encodedFastas = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String recordAC = file.getName().split(".")[0];//Get the file name that is supposed to be the AC without ".fasta" extension or whatever the extension is being used
            String line;
            //Read the first line to see if it is fromatted properly
            line = bufferedReader.readLine().trim();//trim() is needed in case there had been white traces
            if (line.startsWith(Fasta.fastaStart)) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            } else {
                bufferedReader.close();
                throw new NucleotideFasta_BadFormat_Exception("Nucleotide Fasta record: bad format; record does not start with " + Fasta.fastaStart + " identifier ");
            }
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            //Try splitting the file by > if it is possible
            String[] splitter = stringBuilder.toString().split(Fasta.fastaStart);
            //Prepare a list of a split size to store the records
            encodedFastas = new ArrayList<EncodedFasta>(splitter.length);
            //Parse every record and then store it in the list
            for (String s : splitter) {
                encodedFastas.add(EncodedFasta.newInstanceFromFormattedText(recordAC, s));
            }
        } finally {
            //Finally return the prepared list of records
            if (bufferedReader != null) {
                bufferedReader.close();
            }
        }
        return encodedFastas;
    }

    /**
     * Uploads a given entrez query to the NCBI server in order to obtain a list of GI numbers in order to restrict the to a given entrez query.
     * @param tmpDir {@link File} temporary directory that will be used to store the GI list
     * @param entrez_query {@link String} that represents the entrez query
     * @return {@link File} that points to the GI list file
     * @throws IOException in case an error rw file occurs
     * @throws NoSuchAlgorithmException may never happen, is caused by MessageDigest.getInstance("MD5"), that creates a md5 hash for the entrez query file name.
     */
    public static File restrictToEntrez(File tmpDir, String entrez_query) throws IOException, NoSuchAlgorithmException {
        String encodedEntrezQuery=URLEncoder.encode(entrez_query,"UTF-8");
        URL eutilsCount = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=nuccore&rettype=count&term="+encodedEntrezQuery);
        BufferedReader bufferedReader = null;
        BufferedWriter bufferedWriter = null;
        byte[]entrezQueryByte=entrez_query.getBytes("UTF-8");
        MessageDigest messageDigest=MessageDigest.getInstance("MD5");
        byte[]entrezQueryByteMD5=messageDigest.digest(entrezQueryByte);
        StringBuilder sb = new StringBuilder();
        for (byte anEntrezQueryByteMD5 : entrezQueryByteMD5) {
            sb.append(Integer.toString((anEntrezQueryByteMD5 & 0xff) + 0x100, 16).substring(1));
        }
        File restrictedGIs=new File(tmpDir,sb.toString()+".gil");
        if(restrictedGIs.exists()){
            Log.getInstance().log(Level.INFO, "The GI restrictions file " + restrictedGIs.getAbsolutePath() + " already exists, proceeding.");
            return restrictedGIs;
        }
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(eutilsCount.openConnection().getInputStream()));
            String line;
            String countString = null;
            Log.getInstance().log(Level.INFO,"Entrez query restrictions have not been created yet. Preparing a restricting GI file. This may take some 5-10 minutes, please wait.");
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("<Count>")) {
                    countString = line.substring("<Count>".length()+1, line.indexOf("</Count>"));
                    Log.getInstance().log(Level.INFO,"Number of GIs in set: "+countString+", downloading from NCBI.");
                }
            }
            bufferedReader.close();
            if (countString == null) {
                throw new IOException("Could not determine a count for the entrez_query '" + entrez_query + "'");
            }
            eutilsCount = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=nuccore&retmax=" + countString + "&term=" + encodedEntrezQuery);
            bufferedReader = new BufferedReader(new InputStreamReader(eutilsCount.openConnection().getInputStream()));

            bufferedWriter=new BufferedWriter(new FileWriter(restrictedGIs));
            while ((line = bufferedReader.readLine()) != null) {
                 if(line.contains("<Id>")){
                     bufferedWriter.write(line.substring("<Id>".length()+1,line.indexOf("</Id>")));
                     bufferedWriter.newLine();
                 }
            }
            bufferedWriter.flush();
            bufferedReader.close();
            bufferedWriter.close();
        } finally {
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.close();
            }
        }

        return restrictedGIs;
    }

    /**
     * As long as there is no efficient way to apply entrez query to a local BLAST, a file of the GIs, to which the search
     * should be restricted has to be created and added to the BLASTN command line as "-l restricted_gis.gil".
     * In order to create such a file, the taxonomic database looks for all the entrez query clauses, links those to their GIs
     * and lists out the GIs which were not affected by the entrez query restrictions. The file has a unique name based on the
     * entrez query clauses and is created only once for each entrez query in order to ensure maximum performance.
     *
     * @param connection   {@link Connection} to the database
     * @param tempDir      {@link File} which points to the temporary directory, where the gi restrictiong file will be stored
     * @param entrez_query "not smth not smth" formatted {@link String}
     * @return {@link File} that points to the GI restrictions file, which gets created in the temporary folder
     * @throws java.sql.SQLException in case a database communication error occurs
     */
    @Deprecated
    public static File restrictLocalBLASTDatabaseToEntrez(Connection connection, File tempDir, String entrez_query) throws SQLException, IOException {
        //Prepare a file name
        //Currently understands only "not" cases
        String[] split = entrez_query.split("not ");
        for (int i = 1; i < split.length; i++) {
            split[i] = split[i].trim();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 1; i < split.length; i++) {
            stringBuilder.append(split[i]);
            if (i + 1 < split.length) {
                stringBuilder.append("_");
            }
        }
        //Append the .gil-format file extension
        stringBuilder.append(".gil");
        File restrictedGIs = new File(tempDir, stringBuilder.toString());
        //Check if the file already exists
        if (restrictedGIs.exists()) {
            Log.getInstance().log(Level.INFO,"The GI restrictions file " + restrictedGIs.getAbsolutePath() + " already exists, proceeding.");
            return restrictedGIs;
        }
        //Prepare a sql part of the entrez
        stringBuilder = new StringBuilder();
        String nameNotLike = "l1." + LookupNames.dbs.NCBI.names.columns.name.name() + " like ";
        for (int i = 1; i < split.length; i++) {
            stringBuilder.append(nameNotLike);
            stringBuilder.append("\"%");
            stringBuilder.append(split[i]);
            stringBuilder.append("%\"");
            if (i + 1 < split.length) {
                stringBuilder.append(" or ");
            }
        }
        Statement statement = null;
        BufferedWriter bufferedWriter = null;
        try {
            statement = connection.createStatement();
            //Notify that it's gonna take some time
            Log.getInstance().log(Level.INFO,"Entrez query restrictions have not been created yet. Preparing a restricting GI file. This may take some 5-10 minutes, please wait.");
            //Process the file
            statement.execute("USE " + LookupNames.dbs.NCBI.name);
            statement.execute("SELECT "
                    + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name()
                    + " FROM "
                    + LookupNames.dbs.NCBI.views.taxon_by_gi.name()
                    + " AS l1 "
                    + " WHERE "
                    + stringBuilder.toString()
                    + " INTO OUTFILE \""
                    + restrictedGIs.getAbsolutePath()
                    + "\"");
            ResultSet resultSet = statement.executeQuery(
                    "SELECT "
                            + "l2."
                            + LookupNames.dbs.NCBI.nodes.columns.taxid
                            + " FROM  "
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent
                            + " AS l1 JOIN "
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent
                            + " AS l2 ON l2."
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid
                            + "=l1."
                            + LookupNames.dbs.NCBI.nodes.columns.taxid
                            + " WHERE "
                            + stringBuilder.toString()
            );
            List<Integer> taxids = new ArrayList<Integer>();
            while (resultSet.next()) {
                taxids.add(resultSet.getInt(1));
            }
            Log.getInstance().log(Level.INFO,String.valueOf(taxids.size()) + " non-reliable nodes have been identified");
            if (!taxids.isEmpty()) {
                bufferedWriter = new BufferedWriter(new FileWriter(restrictedGIs, true));
                for (Integer i : taxids) {
                    for (Integer leaf : TUTFileOperatorHelper.leavesByTaxid(connection, i, new ArrayList<Integer>())) {
                        ResultSet giSet = statement.executeQuery(
                                "SELECT "
                                        + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name()
                                        + " FROM "
                                        + LookupNames.dbs.NCBI.gi_taxid.name
                                        + " WHERE "
                                        + LookupNames.dbs.NCBI.gi_taxid.columns.taxid
                                        + "="
                                        + leaf);
                        if (giSet.next()) {
                            bufferedWriter.write(String.valueOf(giSet.getInt(1)));
                            bufferedWriter.newLine();
                            bufferedWriter.flush();
                        }
                    }
                }
            }
        } finally {
            if (statement != null) {
                statement.close();
            }
            if (bufferedWriter != null) {
                bufferedWriter.flush();
                bufferedWriter.close();
            }
        }
        //Report success
        Log.getInstance().log(Level.INFO,"Entrez query restrictions have been created successfully");
        //Return the restricting file
        return restrictedGIs;
    }

    /**
     * Returns a list of leaves for the given taxid
     *
     * @param connection {@link Connection} to the database
     * @param taxid      which identifies the branch, which contains the leaves that need to be found
     * @param leaves     {@link List} that will append the taxids of the leaves
     * @return {@link List} with appended leave's taxids
     * @throws SQLException in case a database communication error occurs
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Integer> leavesByTaxid(Connection connection, int taxid, List<Integer> leaves) throws SQLException {
        Statement statement = null;
        List<Integer> taxids;
        try {
            statement = connection.createStatement();
            statement.execute("USE " + LookupNames.dbs.NCBI.name);
            ResultSet resultSet = statement.executeQuery(
                    "SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent
                            + " WHERE "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + "=" + String.valueOf(taxid)
            );
            taxids = new ArrayList<Integer>();
            while (resultSet.next()) {
                taxids.add(resultSet.getInt(1));
            }
            if (taxids.isEmpty()) {
                leaves.add(taxid);
            } else {
                for (Integer i : taxids) {
                    leaves = TUTFileOperatorHelper.leavesByTaxid(connection, i, leaves);
                }
            }

        } finally {
            if (statement != null) {
                statement.close();
            }
        }
        return leaves;
    }
}