package io.file;

import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
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
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

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
import java.util.*;
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
 * A helper class that allows to load TUIT io.properties from an XML formatted input
 */
public class TUITFileOperatorHelper {

    /**
     * Private constructor, ensures non-instantiability.
     */
    private TUITFileOperatorHelper() {
        throw new AssertionError();
    }

    /**
     * A utility entity, that collects output format options and allows to create new formats
     */
    public enum OutputFormat {
        TUIT, RDP_FIXRANK, RDP_SPECIES;
        /**
         * A default nodes delimiter for TUIT
         */
        public static final String TUIT_TAXA_DELIM = " -> ";
        /**
         * A default forward (opening) rank delimiter for TUIT
         */
        public static final String TUIT_RANK_DELIM_FW = "{";
        /**
         * A default reverse (closing) rank delimiter for TUIT
         */
        public static final String TUIT_RANK_DELIM_RW = "}";
        /**
         * A default delimiter between query ac and taxonomy for TUIT
         */
        public static final String TUIT_QUERY_DELIM = ":\t";
        /**
         * Marks a result of a zero hits returned by BLAST, or a completely unidentifiable sequence for TUIT format
         */
        public static final String TUIT_NOT_IDENTIFIED = "<-not identified->";
        /**
         * Marks a result of a zero hits returned by BLAST, or a completely unidentifiable sequence for RDP format
         */
        public static final String RDP_NOT_IDENTIFIED = "unclassified";

        /**
         * An output formatter that helps format an {@link blast.ncbi.output.Iteration}
         *
         * @param <I> extends {@link blast.ncbi.output.Iteration} that will be formatted
         */
        public interface OutputFormatter<I extends Iteration> {
            /**
             * Performs full format.
             *
             * @param ac                  {@link java.lang.String} that will be appended to the line (presumably AC)
             * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} to format
             * @return {@link java.lang.String} representation of the lineage in full formatted line(s)
             */
            public String format(final String ac, final NormalizedIteration<I> normalizedIteration);

            /**
             * @param ac                  {@link java.lang.String} that will be appended to the line (presumably AC)
             * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} to format
             * @return {@link String} representation of the query or/and AC
             */
            public String formatQuery(final String ac, final NormalizedIteration<I> normalizedIteration);

            /**
             * Formats (presumably in a recursive manner) the lineage going by taxonomic nodes
             *
             * @param taxonomicNode {@link taxonomy.node.TaxonomicNode} that is presumably the deppes node that TUIT was able to classify
             * @return {@link String} representaion of the lineage
             */
            public String formatFullLineage(final TaxonomicNode taxonomicNode);
        }

        /**
         * A helper method to translate {@link taxonomy.Ranks} names to RDP conventional format
         *
         * @param rank {@link taxonomy.Ranks} rank to get a translation for
         * @return a {@link java.lang.String} representation of the RDP compatible rank name
         */
        public static String translateToRDPConventions(final Ranks rank) {
            switch (rank) {
                case superkingdom: {
                    return "domain";
                }
                case root_of_life: {
                    return "Root";
                }
                default: {
                    return rank.getName();
                }
            }
        }

        /**
         * A default format for TUIT. Formats the output as "taxon {rank} ->" chain.
         */
        public static final OutputFormatter<Iteration> defaultTUITFormatter = new OutputFormatter<Iteration>() {
            /**
             * Formats a given {@link blast.normal.iteration.NormalizedIteration} with a given {@link java.lang.String} AC as row identifier.
             * @param ac {@link String} that identifies the row (AC of the query, presumably)
             * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} that contains the result
             * @return {@link String} representation of the full lineage in TUIT format
             */
            @Override
            public String format(final String ac, final NormalizedIteration<Iteration> normalizedIteration) {
                final StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(formatQuery(ac, normalizedIteration));
                if (normalizedIteration.getPivotalHit() == null) {
                    stringBuilder.append(TUIT_QUERY_DELIM);
                    stringBuilder.append(TUIT_NOT_IDENTIFIED);
                }
                final TaxonomicNode taxonomicNode = normalizedIteration.getPivotalHit().getFocusNode();
                stringBuilder.append(formatFullLineage(taxonomicNode));
                return stringBuilder.toString();
            }

            /**
             * Recursively adds {@link taxonomy.node.TaxonomicNode}s from the lineage to a {@link java.lang.String} representation
             * @param taxonomicNode {@link taxonomy.node.TaxonomicNode} that is presumably the deepest node, that TUIT was able to calssify
             * @return {@link java.lang.String} representation of a given {@link taxonomy.node.TaxonomicNode} lineage
             */
            @Override
            public String formatFullLineage(final TaxonomicNode taxonomicNode) {
                final StringBuilder stringBuilder = new StringBuilder();
                if (taxonomicNode.getParent() != null) {
                    stringBuilder.append(formatFullLineage(taxonomicNode.getParent()));
                    stringBuilder.append(OutputFormat.TUIT_TAXA_DELIM);
                }
                stringBuilder.append(taxonomicNode.getScientificName());
                stringBuilder.append(' ');
                stringBuilder.append(TUIT_RANK_DELIM_FW);
                stringBuilder.append(taxonomicNode.getRank().getName());
                stringBuilder.append(TUIT_RANK_DELIM_RW);
                return stringBuilder.toString();
            }

            /**
             * Formats query sequence by adding a given {@link java.lang.String} ac to the TUIT query delimiter (":\t")
             * @param ac {@link String} that identifies the row (AC of the query, presumably)
             * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} that contains the result (not used by this implementation)
             * @return {@link java.lang.String} representation of the AC and TUIT-specific delimiter
             */
            @Override
            public String formatQuery(final String ac, final NormalizedIteration<Iteration> normalizedIteration) {
                return ac.concat(TUIT_QUERY_DELIM);
            }
        };

        /**
         * Not Thread-safe!
         * A default {@link io.file.TUITFileOperatorHelper.OutputFormat.OutputFormatter} that allows for RDP fixrank formatting. In case of "unclassified", a relevant field gets added
         * with an "unclassified" mark and 0 confidence. For those classified, the confidence is 1-alpha, which comes form the {@link blast.specification.cutoff.TUITCutoffSet}.
         *
         * @param cutoffMap {@link java.util.Map} of cutoffsets, that forms once upon program initiation
         * @return {@link io.file.TUITFileOperatorHelper.OutputFormat.OutputFormatter} that allows for RPD fixrank format of the output
         */
        public static OutputFormatter<Iteration> defaultFixRankRDPFormatter(final Map<Ranks, TUITCutoffSet> cutoffMap) {
            return new OutputFormatter<Iteration>() {
                /**
                 * A list of taxonomic ranks that are commonly used by the RDP fixrank formatting
                 */
                private final Ranks[] rdpFixRankRanks = {Ranks.superkingdom, Ranks.phylum, Ranks.c_lass, Ranks.order, Ranks.family, Ranks.genus, Ranks.species};
                /**
                 * A queue that allows for tracking of which of the mandatory fixrank ranks had already appeared, and which not. Helps greatly in cases, when a hit had less
                 * ranks in its lineage that assumed by the fixrank format
                 */
                private Deque<Ranks> orderOfRankAppearence = new ArrayDeque<>(Arrays.asList(rdpFixRankRanks));
                /**
                 * A set of fixranks for faster search "if-rank-present/allowed-by-the-fixrank-format"
                 */
                private final Set<Ranks> rdpFixRankRanksSet = new HashSet<>(orderOfRankAppearence);

                /**
                 * A helper method, that searches for the rank of the given {@link taxonomy.node.TaxonomicNode} within the {@code rdpFixRankRanks}. The one found retains a corresponding
                 * position within the fixrank ouput.
                 * @param taxonomicNode {@link taxonomy.node.TaxonomicNode} that will be used a the lower-ranked node, within a taxonomy that must be searched
                 * @return -1 in case the rank was not found, {@code int} position in the {@code rdpFixRankRanks} when found.
                 */
                private final int findRank(final TaxonomicNode taxonomicNode) {
                    int count = 0;
                    for (Ranks r : rdpFixRankRanks) {
                        if (r.equals(taxonomicNode.getRank())) {
                            return count;
                        }
                        count++;
                    }
                    if (taxonomicNode.getParent() != null) {
                        return findRank(taxonomicNode.getParent());
                    }
                    return -1;
                }

                /**
                 * Formats a given {@link blast.normal.iteration.NormalizedIteration} with a given {@link java.lang.String} AC as row identifier.
                 * @param ac {@link String} that identifies the row (AC of the query, presumably)
                 * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} that contains the result
                 * @return {@link String} representation of the full lineage in RDP fixrank format
                 */
                @Override
                public String format(String ac, NormalizedIteration<Iteration> normalizedIteration) {
                    final StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append(formatQuery(ac, normalizedIteration));
                    if (normalizedIteration.getPivotalHit() == null) {
                        for (int i = 0; i < rdpFixRankRanks.length; i++) {
                            stringBuilder.append(RDP_NOT_IDENTIFIED);
                            stringBuilder.append('\t');
                            stringBuilder.append(translateToRDPConventions(rdpFixRankRanks[i]));
                            stringBuilder.append('\t');
                            stringBuilder.append(0);
                            stringBuilder.append('\t');
                        }
                        return stringBuilder.toString().trim();
                    }
                    final TaxonomicNode taxonomicNode = normalizedIteration.getPivotalHit().getFocusNode();
                    stringBuilder.append(formatFullLineage(taxonomicNode));
                    //Determine which ranks have been left out

                    final int ranksLeftOut = findRank(taxonomicNode) + 1;

                    for (int i = ranksLeftOut; i < rdpFixRankRanks.length; i++) {
                        stringBuilder.append(RDP_NOT_IDENTIFIED);
                        stringBuilder.append('\t');
                        stringBuilder.append(translateToRDPConventions(rdpFixRankRanks[i]));
                        stringBuilder.append('\t');
                        stringBuilder.append(0);
                        stringBuilder.append('\t');
                    }
                    orderOfRankAppearence = new ArrayDeque<>(Arrays.asList(rdpFixRankRanks));
                    return stringBuilder.toString().trim();
                }

                /**
                 * Formats query sequence by adding a given {@link java.lang.String} ac to the RDP query delimiter ("\t\t")
                 * @param ac {@link String} that identifies the row (AC of the query, presumably)
                 * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} that contains the result (not used by this implementation)
                 * @return {@link java.lang.String} representation of the AC and RDP-specific delimiter
                 */
                @Override
                public String formatQuery(String ac, NormalizedIteration<Iteration> normalizedIteration) {
                    return ac.concat("\t\t");
                }

                /**
                 * Recursively adds {@link taxonomy.node.TaxonomicNode}s from the lineage to a {@link java.lang.String} representation, substitutes the
                 * missing rank with underscore-connected {@link taxonomy.node.TaxonomicNode} rank and name, and substitutes unclassified with "unclassified".
                 * Confidence scores, common for the RDP output are substituted by a logical "confidence" approximation: 1-alpha.
                 * Exmple: say, we have an alpha of 0.05 for a given taxonomic rank. Then we are confident that the classification is a true 95%. Thereby, we put
                 * 0.95 confidence on the RDP fixrank output.
                 * @param taxonomicNode {@link taxonomy.node.TaxonomicNode} that is presumably the deepest node, that TUIT was able to calssify
                 * @return {@link java.lang.String} representation of a given {@link taxonomy.node.TaxonomicNode} lineage
                 */
                @Override
                public String formatFullLineage(TaxonomicNode taxonomicNode) {
                    final StringBuilder stringBuilder = new StringBuilder();
                    if (taxonomicNode.getParent() != null) {
                        stringBuilder.append(formatFullLineage(taxonomicNode.getParent()));
                    }
                    if (rdpFixRankRanksSet.contains(taxonomicNode.getRank())) {
                        if (taxonomicNode.getRank().equals(orderOfRankAppearence.getFirst())) {
                            stringBuilder.append(taxonomicNode.getScientificName());
                            stringBuilder.append('\t');
                            stringBuilder.append(translateToRDPConventions(taxonomicNode.getRank()));
                            stringBuilder.append('\t');
                            stringBuilder.append(1 - cutoffMap.get(taxonomicNode.getRank()).getAlpha());
                            stringBuilder.append('\t');
                        } else {
                            while (orderOfRankAppearence.size() > 1 & !taxonomicNode.getRank().equals(orderOfRankAppearence.getFirst())) {
                                stringBuilder.append(taxonomicNode.getScientificName().concat("_").concat(orderOfRankAppearence.getFirst().toString()));
                                stringBuilder.append('\t');
                                stringBuilder.append(translateToRDPConventions(orderOfRankAppearence.getFirst()));
                                stringBuilder.append('\t');
                                stringBuilder.append(1 - cutoffMap.get(orderOfRankAppearence.getFirst()).getAlpha());
                                stringBuilder.append('\t');
                                orderOfRankAppearence.pollFirst();
                            }
                            stringBuilder.append(taxonomicNode.getScientificName());
                            stringBuilder.append('\t');
                            stringBuilder.append(translateToRDPConventions(taxonomicNode.getRank()));
                            stringBuilder.append('\t');
                            stringBuilder.append(1 - cutoffMap.get(taxonomicNode.getRank()).getAlpha());
                            stringBuilder.append('\t');
                        }
                        orderOfRankAppearence.pollFirst();
                    }
                    return stringBuilder.toString();
                }
            };
        }

        //TODO: implement
        public static OutputFormatter<Iteration> defaultFullRankRDPFormatter(final Map<Ranks, TUITCutoffSet> cutoffMap) {
            return new OutputFormatter<Iteration>() {
                @Override
                public String format(String ac, NormalizedIteration<Iteration> normalizedIteration) {
                    return null;
                }

                @Override
                public String formatQuery(String ac, NormalizedIteration<Iteration> normalizedIteration) {
                    return null;
                }

                @Override
                public String formatFullLineage(TaxonomicNode taxonomicNode) {
                    return null;
                }
            };
        }
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
     * @throws SAXException  upon SAX parsing error
     * @throws JAXBException upon unmarshalling
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
     * @throws IOException                                                 in case opening and reading the file fails
     * @throws format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception in case of a single line format or none at all
     */
    public static List<EncodedFasta> loadOTURecords(File file) throws IOException,
            NucleotideFasta_BadFormat_Exception {
        //Open file and check whether it is even Fasta at all
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            final StringBuilder stringBuilder = new StringBuilder();
            final String recordAC = file.getName().split(".")[0];//Get the file name that is supposed to be the AC without ".fasta" extension or whatever the extension is being used
            String line;
            //Read the first line to see if it is formatted properly
            line = bufferedReader.readLine().trim();//trim() is needed in case there had been white traces
            if (line.startsWith(Fasta.fastaStart)) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            } else {
                throw new NucleotideFasta_BadFormat_Exception("Nucleotide Fasta record: bad format; record does not start with " + Fasta.fastaStart + " identifier ");
            }
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            //Try splitting the file by > if it is possible
            final String[] splitter = stringBuilder.toString().split(Fasta.fastaStart);
            //Prepare a list of a split size to store the records
            final List<EncodedFasta> encodedFastas = new ArrayList<EncodedFasta>(splitter.length);
            //Parse every record and then store it in the list
            for (String s : splitter) {
                encodedFastas.add(EncodedFasta.newInstanceFromFormattedText(recordAC, s));
            }
            return encodedFastas;
        }
    }

    /**
     * Uploads a given entrez query to the NCBI server in order to obtain a list of GI numbers in order to restrict the to a given entrez query.
     *
     * @param tmpDir       {@link File} temporary directory that will be used to store the GI list
     * @param entrez_query {@link String} that represents the entrez query
     * @return {@link File} that points to the GI list file
     * @throws IOException              in case an error rw file occurs
     * @throws NoSuchAlgorithmException may never happen, is caused by MessageDigest.getInstance("MD5"), that creates a md5 hash for the entrez query file name.
     */
    public static File restrictToEntrez(File tmpDir, String entrez_query) throws IOException, NoSuchAlgorithmException {
        //Connect to NCBI eutils esearch
        final String encodedEntrezQuery = URLEncoder.encode(entrez_query, "UTF-8");
        URL eutilsCount = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=nuccore&rettype=count&term=" + encodedEntrezQuery);//Non-final cuz will be repointed
        //Check md5 to see if the file has already been created from the same entrez query.
        byte[] entrezQueryByte = entrez_query.getBytes("UTF-8");
        final MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        byte[] entrezQueryByteMD5 = messageDigest.digest(entrezQueryByte);
        final StringBuilder sb = new StringBuilder();
        for (byte anEntrezQueryByteMD5 : entrezQueryByteMD5) {
            sb.append(Integer.toString((anEntrezQueryByteMD5 & 0xff) + 0x100, 16).substring(1));//Reformat md5
        }
        final File restrictedGIs = new File(tmpDir, sb.toString() + ".gil");
        if (restrictedGIs.exists()) {
            Log.getInstance().log(Level.INFO, "The GI restrictions file " + restrictedGIs.getAbsolutePath() + " already exists, proceeding.");
            return restrictedGIs;
        }
        String countString = null;
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(eutilsCount.openConnection().getInputStream()));) {
            String line;
            Log.getInstance().log(Level.INFO, "Entrez query restrictions have not been created yet. Preparing a restricting GI file. This may take some 5-10 minutes, please wait.");
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("<Count>")) {
                    countString = line.substring("<Count>".length() + 1, line.indexOf("</Count>"));//Process the ouptut html for counts
                    Log.getInstance().log(Level.INFO, "Number of GIs in set: " + countString + ", downloading from NCBI.");
                }
            }
        }
        //In case no count have been found, that means that the output is empty and an exception should be thorwn at this point
        if (countString == null) {
            throw new IOException("Could not determine a count for the entrez_query '" + entrez_query + "'");
        }

        eutilsCount = new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=nuccore&retmax=" + countString + "&term=" + encodedEntrezQuery);
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(eutilsCount.openConnection().getInputStream()));
             BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(restrictedGIs));) {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.contains("<Id>")) {
                    bufferedWriter.write(line.substring("<Id>".length() + 1, line.indexOf("</Id>")));
                    bufferedWriter.newLine();
                }
            }
        }
        //Finally return a pointer to the file
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
            Log.getInstance().log(Level.INFO, "The GI restrictions file " + restrictedGIs.getAbsolutePath() + " already exists, proceeding.");
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
            Log.getInstance().log(Level.INFO, "Entrez query restrictions have not been created yet. Preparing a restricting GI file. This may take some 5-10 minutes, please wait.");
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
            Log.getInstance().log(Level.INFO, String.valueOf(taxids.size()) + " non-reliable nodes have been identified");
            if (!taxids.isEmpty()) {
                bufferedWriter = new BufferedWriter(new FileWriter(restrictedGIs, true));
                for (Integer i : taxids) {
                    for (Integer leaf : TUITFileOperatorHelper.leavesByTaxid(connection, i, new ArrayList<Integer>())) {
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
        Log.getInstance().log(Level.INFO, "Entrez query restrictions have been created successfully");
        //Return the restricting file
        return restrictedGIs;
    }

    /**
     * Returns a list of leaves for the given taxid.
     *
     * @param connection {@link Connection} to the database
     * @param taxid      which identifies the branch, which contains the leaves that need to be found
     * @param leaves     {@link List} that will append the taxids of the leaves
     * @return {@link List} with appended leave's taxids
     * @throws SQLException in case a database communication error occurs
     */
    @SuppressWarnings("WeakerAccess")
    public static List<Integer> leavesByTaxid(Connection connection, int taxid, List<Integer> leaves) throws SQLException {
        List<Integer> taxids;
        try (Statement statement = connection.createStatement();){
            statement.execute("USE " + LookupNames.dbs.NCBI.name);
            final ResultSet resultSet = statement.executeQuery(
                    "SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent
                            + " WHERE "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + "=" + String.valueOf(taxid)
            );
            taxids = new ArrayList<>();
            while (resultSet.next()) {
                taxids.add(resultSet.getInt(1));
            }
            if (taxids.isEmpty()) {
                leaves.add(taxid);
            } else {
                for (Integer i : taxids) {
                    leaves = TUITFileOperatorHelper.leavesByTaxid(connection, i, leaves);
                }
            }
        }
        return leaves;
    }
}
