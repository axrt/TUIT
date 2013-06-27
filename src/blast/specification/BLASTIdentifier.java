package blast.specification;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Iteration;
import blast.specification.cutoff.TUITCutoffSet;
import blast.normal.hit.NormalizedHit;
import blast.normal.iteration.NormalizedIteration;
import db.connect.TaxonomicDatabaseOperator;
import db.tables.LookupNames;
import format.BadFromatException;
import format.fasta.nucleotide.NucleotideFasta;
import logger.Log;
import taxonomy.Ranks;
import io.file.TUITFileOperator;
import org.xml.sax.SAXException;
import taxonomy.node.TaxonomicNode;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;

/**
 * Combines functionality of a local (remote with "-remote" option) BLASTN and an ability to assign a taxonomy to the
 * given queries automatically.
 */
public abstract class BLASTIdentifier<T extends NucleotideFasta> extends NCBI_EX_BLASTN implements TaxonomicDatabaseOperator {

    /**
     * A Map for default cutoff sets, which are used whenever a custom set was not given
     */
    protected static final Map<Ranks, TUITCutoffSet> DEFAULT_CUTOFFS = new HashMap<Ranks, TUITCutoffSet>();

    /**
     * Filling in the cutoff set map
     */
    static {

        //Species-level-related
        DEFAULT_CUTOFFS.put(Ranks.subspecies, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));
        DEFAULT_CUTOFFS.put(Ranks.species, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));
        DEFAULT_CUTOFFS.put(Ranks.species_subgroup, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));
        DEFAULT_CUTOFFS.put(Ranks.species_group, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));
        DEFAULT_CUTOFFS.put(Ranks.varietas, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));
        DEFAULT_CUTOFFS.put(Ranks.forma, TUITCutoffSet.newDefaultInstance(97.5, 95, 100));


        //Genus-level-related
        DEFAULT_CUTOFFS.put(Ranks.subgenus, TUITCutoffSet.newDefaultInstance(95, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.genus, TUITCutoffSet.newDefaultInstance(95, 90, 100));

        //Family-level-related
        DEFAULT_CUTOFFS.put(Ranks.subfamily, TUITCutoffSet.newDefaultInstance(80, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.family, TUITCutoffSet.newDefaultInstance(80, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.superfamily, TUITCutoffSet.newDefaultInstance(80, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.tribe, TUITCutoffSet.newDefaultInstance(80, 90, 100));

        //Oreder-level-related
        DEFAULT_CUTOFFS.put(Ranks.order, TUITCutoffSet.newDefaultInstance(70, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.parvorder, TUITCutoffSet.newDefaultInstance(70, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.infraorder, TUITCutoffSet.newDefaultInstance(70, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.suborder, TUITCutoffSet.newDefaultInstance(70, 90, 100));
        DEFAULT_CUTOFFS.put(Ranks.superorder, TUITCutoffSet.newDefaultInstance(70, 90, 100));

        //Any other level-related
        DEFAULT_CUTOFFS.put(Ranks.subclass, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.c_lass, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.superclass, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.subphylum, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.phylum, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.superphylum, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.subkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.kingdom, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.superkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.no_rank, TUITCutoffSet.newDefaultInstance(60, 80, 100));
        DEFAULT_CUTOFFS.put(Ranks.root_of_life, TUITCutoffSet.newDefaultInstance(0, 0, 1));

    }

    /**
     * A connection to an SQL database, which contains a NCBI schema with taxonomic information
     */
    protected final Connection connection;
    /**
     * A custom cutoff set map, provided by the user
     */
    protected final Map<Ranks, TUITCutoffSet> cutoffSetMap;
    /**
     * A list of normalized hits that the algorithm will operate upon
     */
    protected List<NormalizedIteration<Iteration>> normalizedIterations;

    /**
     * Strings within the scientific names, restricted by the entrez query

     */
    protected String[] restrictedNames;

    /**
     * @param query         {@link List} a list of query
     *                      fasta-formatted records
     * @param query_IDs     {@link List} a list of AC numbers of sequences in a
     *                      database
     * @param tempDir       {@link File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String}[] A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                      taxonomic information
     * @param restrictedNames  a {@link String}[] list that contains names that should be restricted (such as "unclassified" etc.)
     * @param cutoffSetMap  a {@link Map}, provided by the user and that may differ from the
     *                      default set
     */
    protected BLASTIdentifier(List<T> query, List<String> query_IDs,
                              File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                              Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, String[]restrictedNames) {
        super(query, query_IDs, tempDir, executive, parameterList,
                identifierFileOperator);

        this.restrictedNames=restrictedNames;
        this.connection = connection;
        this.cutoffSetMap = cutoffSetMap;
    }

    /**
     * Getter for the restricted names
     * @return {@link String}[] names restricted by the entrez query
     */
    public String[] getRestrictedNames() {
        return restrictedNames;
    }

    /**
     * Checks whether a given {@link blast.normal.hit.NormalizedHit} checks against the cutoffs at a given {@link Ranks} of specification
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} a hit to check
     * @param rank          {@link Ranks} a rank at which to check
     * @return {@code true} if the {@link blast.normal.hit.NormalizedHit} checks, otherwise {@code false} is returned. Upon null instead of either normalizedHit or rank
     *         returns {@code false}.
     */
    public boolean normalyzedHitChecksAgainstParametersForRank(final NormalizedHit normalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        //Cecks if a cutoff set exists at a given ranks
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null) {
            //If not - substitutes it with a default cutoff set
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        if (normalizedHit == null || rank == null) {
            return false;
        }
        return tuitCutoffSet.normalizedHitPassesCheck(normalizedHit);
    }

    /**
     * Checks whether the two given {@link NormalizedHit}s are far enough by the E-value (the ratio difference (in folds) is greater than the cutoff
     * value at the given rank)
     *
     * @param oneNormalizedHit     {@link NormalizedHit} first hit (a hit with a worse E-value)
     * @param anotherNormalizedHit a {@link NormalizedHit}  with a better E-value)
     * @param rank                 {@link Ranks} at which the E-value difference is being monitored
     * @return {@code true} if the {@link NormalizedHit}'s ratio (difference in folds) is greater than the cutoff, otherwise {@code false}
     *         is returned. Upon null instead of either normalizedHit or rank
     *         returns {@code false}.
     */
    public boolean hitsAreFarEnoughByEvalueAtRank(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit, Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null || oneNormalizedHit == null || anotherNormalizedHit == null) {
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        return tuitCutoffSet.hitsAreFarEnoughByEvalue(oneNormalizedHit, anotherNormalizedHit);
    }

    /**
     * Based on the SQL database NCBI schema and the {@link Ranks} of the given {@link TaxonomicNode}, assignes the taxnomoy (taxid, scientific name)
     * **does not assign children downt to the leaves, that is done by the {@code attachChildrenForTaxonomicNode(TaxonomicNode parentNode)}
     *
     * @param normalizedHit {@link NormalizedHit} which needs to know its taxonomy
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter, but with a newly attached
     *         {@link TaxonomicNode}
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public NormalizedHit assignTaxonomy(final NormalizedHit normalizedHit) throws SQLException {

        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //Try selecting the child nodes for the given hit
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.taxon_by_gi.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name()
                            + "=?");
            preparedStatement.setInt(1, normalizedHit.getGI());
            resultSet = preparedStatement.executeQuery();

            int taxid;
            Ranks rank;
            String scientificName;
            //If any children exist for the given taxid
            if (resultSet.next()) {
                taxid = resultSet.getInt(2);
                scientificName = resultSet.getString(3);
                rank = Ranks.values()[resultSet.getInt(5) - 1];
                TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(taxid, rank, scientificName);
                normalizedHit.setTaxonomy(taxonomicNode);
                normalizedHit.setFocusNode(taxonomicNode);
            } else {
                preparedStatement.close();
                return null;
            }
        } finally {
            //Close and cleanup
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return normalizedHit;
    }

    /**
     * Based on the SQL database NCBI schema and the {@link Ranks} of the given {@link NormalizedHit}
     * rises its rank one step higher (say, for subspecies rizes fot species)
     *
     * @param normalizedHit {@link NormalizedHit}
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter,
     *         but with one step risen {@code Ranks}
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public NormalizedHit liftRankForNormalyzedHit(final NormalizedHit normalizedHit) throws SQLException {
        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //Try selecting the parent node for the given hit
            //Assuming the database is consistent - one taxid should have only one immediate parent
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name()
                            + "=(SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.nodes.name
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name() + "=?)");
            preparedStatement.setInt(1, normalizedHit.getAssignedTaxid());
            resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                TaxonomicNode taxonomicNode = TaxonomicNode.newDefaultInstance(resultSet.getInt(2),
                        Ranks.values()[resultSet.getInt(5) - 1],
                        resultSet.getString(3));
                taxonomicNode.addChild(normalizedHit.getFocusNode());
                normalizedHit.setTaxonomy(taxonomicNode);
                normalizedHit.setFocusNode(taxonomicNode);
            } else {
                preparedStatement.close();
                return null;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return normalizedHit;
    }

    /**
     * For a given {@link TaxonomicNode} attaches its parent and higher lineage structure. Originally used to
     * save results in a form of a taxonomic branch.
     *
     * @param taxonomicNode {@link TaxonomicNode} that needs to get its full lineage structure
     * @return a pointer to the same {@link TaxonomicNode} objcet, but with attached pointers to its taxonomic lineage
     * @throws SQLException in case an error in database communication occurs
     */
    @Override
    public TaxonomicNode attachFullDirectLineage(TaxonomicNode taxonomicNode) throws SQLException {

        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        TaxonomicNode parentTaxonomicNode = null;
        try {
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name()
                            + "=(SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.nodes.name
                            + " WHERE "
                            + LookupNames.dbs.NCBI.names.columns.taxid.name() + "=?)");
            preparedStatement.setInt(1, taxonomicNode.getTaxid());
            resultSet = preparedStatement.executeQuery();
            int parent_taxid = 0;
            int taxid = 0;
            String scientificName;
            Ranks rank;
            if (resultSet.next()) {
                parent_taxid = resultSet.getInt(1);
                taxid = resultSet.getInt(2);
                scientificName = resultSet.getString(3);
                rank = Ranks.values()[resultSet.getInt(5) - 1];
                parentTaxonomicNode = TaxonomicNode.newDefaultInstance(taxid, rank, scientificName);
                parentTaxonomicNode.addChild(taxonomicNode);
                taxonomicNode.setParent(parentTaxonomicNode);
                if (parent_taxid != taxid) {
                    parentTaxonomicNode = this.attachFullDirectLineage(parentTaxonomicNode);
                }
            } else {
                preparedStatement.close();
                return null;
            }

        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return taxonomicNode;
    }

    /**
     * Based on the SQL database NCBI schema and the {@link Ranks} of the given {@link NormalizedHit} and its {@link Ranks}
     * reassembles and assignes the full taxonomy for the  {@link NormalizedHit} for its current {@link Ranks} down to the leaves
     *
     * @param parentNode {@link NormalizedHit} that needs to know its children
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter, but with a newly attached
     *         {@link TaxonomicNode} of full taxonomy including leaves from a given {@link Ranks}
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode parentNode) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //try selecting all children for a given taxid
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + "=?");
            preparedStatement.setInt(1, parentNode.getTaxid());
            resultSet = preparedStatement.executeQuery();

            TaxonomicNode taxonomicNode;
            //Clear any children that the taxonomicNode may already have (as a result of leveling up from some other rank)
            parentNode.getChildren().clear();
            while (resultSet.next()) {
                taxonomicNode = TaxonomicNode.newDefaultInstance(
                        resultSet.getInt(2),
                        Ranks.values()[resultSet.getInt(5) - 1],
                        resultSet.getString(3));
                taxonomicNode.setParent(parentNode);
                //Recursively return to this procedure in order to get everything down to the leaves
                parentNode.addChild(this.attachChildrenForTaxonomicNode(taxonomicNode));
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return parentNode;
    }

    /**
     * Checks whether a given parent taxid is indeed a parent taxid for the given one, as well as it checks
     * whether the parant taxid may be a sibling taxid for the given. Used to check for if the normalized hits with
     * better E-value restrict the chosen pivotal hit.
     *
     * @param parentTaxid a taxid of a {@link TaxonomicNode} that should be a parent to the given taxid in order to
     *                    support the choice of the pivotal normalized hit
     * @param taxid       of the {@link TaxonomicNode} of the pivotal taxid
     * @return {@code true} if the parent taxid is indeed parent (direct parent or grand parent within the lineage) of
     *         the given taxid, {@code false} otherwise.
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public boolean isParentOf(int parentTaxid, int taxid) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        try {
            //try selecting all children for a given taxid
            preparedStatement = this.connection.prepareStatement(
                    "SELECT "
                            + LookupNames.dbs.NCBI.nodes.columns.parent_taxid.name()
                            + " FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.f_level_children_by_parent.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.nodes.columns.taxid.name()
                            + "=?");
            preparedStatement.setInt(1, taxid);
            resultSet = preparedStatement.executeQuery();
            TaxonomicNode taxonomicNode;
            if (resultSet.next()) {
                if (parentTaxid == resultSet.getInt(1)) {
                    return true;
                } else if (resultSet.getInt(1) != 1) {
                    return this.isParentOf(parentTaxid, resultSet.getInt(1));
                } else {
                    return false;
                }
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return false;
    }

    /**
     * Normalizes the {@link Iteration}s returned by the BLASTN within the output
     */
    protected void normalizeIterations() {
        //Normalize each iteration
        int i = 0;
        for (Iteration iteration : this.blastOutput.getBlastOutputIterations().getIteration()) {
            this.normalizedIterations.add(NormalizedIteration.newDefaultInstanceFromIteration((NucleotideFasta) this.query.get(i), iteration, this));
            i++;
        }
    }

    /**
     * Accepts a result pair of a query {@link NucleotideFasta} and its {@link NormalizedIteration} (thereby specified)
     * {@link TUITFileOperator} in order to save the result in the way defined by the current file operator
     *
     * @param query               {@link NucleotideFasta}
     * @param normalizedIteration {@link NormalizedIteration}
     * @return {@code true} if the file operator returns success, {@code false} otherwise
     */
    public boolean acceptResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws Exception {
        if (((TUITFileOperator) this.fileOperator).saveResults(query, normalizedIteration)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * A static factory to get a new instance of a {@link BLASTIdentifier}
     * /**
     *
     * @param query         {@link List} a list of query
     *                      fasta-formatted records
     * @param tempDir       {@link File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String}[] A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                      taxonomic information
     * @param cutoffSetMap  a {@link Map}, provided by the user and that may differ from the
     *                      default set
     * @param entrez_query that contains "not smth" formatted restriction names (like "unclassified, etc")
     * @return a new instance of {@link BLASTIdentifier} from the given parameters
     */
    public static BLASTIdentifier newDefaultInstance(List<NucleotideFasta> query,
                                                     File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                                                     Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, String entrez_query) {
        String[]split=null;
        split=entrez_query.split("not ");

        return new BLASTIdentifier(query, null, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, Arrays.copyOfRange(split,1,split.length)) {
            /**
             * Overridden run() that calls BLAST(), normalizes the iterations and calls specify() on each iteration.
             */
            @Override
            public void run() {
                try {
                    this.BLAST();
                    if (this.blastOutput.getBlastOutputIterations().getIteration().size() > 0) {

                        this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
                        this.BLASTed = true;
                        this.normalizeIterations();
                        for (int i = 0; i < this.normalizedIterations.size(); i++) {
                            NormalizedIteration<Iteration> normalizedIteration = (NormalizedIteration<Iteration>) this.normalizedIterations.get(i);
                            normalizedIteration.specify();
                        }
                    } else {
                        Log.getInstance().getLogger().severe("No Iterations were returned, an error might have occured during BLAST.");
                        return;
                    }
                } catch (IOException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                } catch (InterruptedException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                } catch (JAXBException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                } catch (SAXException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                } catch (SQLException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                } catch (BadFromatException e) {
                    Log.getInstance().getLogger().severe(e.getMessage());
                }
            }
        };
    }
}
