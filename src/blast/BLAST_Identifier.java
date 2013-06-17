package blast;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Iteration;
import db.connect.DatabaseOperator;
import db.tables.LookupNames;
import format.BadFromatException;
import format.fasta.nucleotide.NucleotideFasta;
import helper.Ranks;
import io.file.TUITFileOperator;
import io.file.TUITFileOperator;
import org.xml.sax.SAXException;
import taxonomy.TaxonomicNode;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Combines functionality of a local (remote with "-remote" option) BLASTN and an ability to assign a taxonomy to the
 * given queries automatically.
 */
public class BLAST_Identifier extends NCBI_EX_BLASTN implements DatabaseOperator {

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
    protected List<NormalizedIteration> normalizedIterations;

    /**
     * @param query         {@link List<? extends   format.fasta.nucleotide.NucleotideFasta  >} a list of query
     *                      fasta-formatted records
     * @param query_IDs     {@link List<String>} a list of AC numbers of sequences in a
     *                      database
     * @param tempDir       {@link File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String[]} A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                      taxonomic information
     * @param cutoffSetMap  a {@link Map<Ranks, TUITCutoffSet>}, provided by the user and that may differ from the
     *                      default set
     */
    protected BLAST_Identifier(List<? extends NucleotideFasta> query, List<String> query_IDs,
                               File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                               Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        super(query, query_IDs, tempDir, executive, parameterList,
                identifierFileOperator);
        this.connection = connection;
        this.cutoffSetMap = cutoffSetMap;
    }

    /**
     * Overridden run() that calls BLAST(), normalizes the iterations and calls specify() on each iteration.
     */
    @Override
    public void run() {

        try {
            this.BLAST();
            //TODO: input checks for whether the output iterations have at least one iteration
            this.normalizedIterations = new ArrayList<NormalizedIteration>(this.blastOutput.getBlastOutputIterations().getIteration().size());
            this.BLASTed = true;
            this.normalizeIterations();
            for (NormalizedIteration normalizedIteration : this.normalizedIterations) {
                normalizedIteration.specify();
            }
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BadFromatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    /**
     * Checks whether a given {@link NormalizedHit} checks against the cutoffs at a given {@link Ranks} of specification
     *
     * @param normalizedHit {@link NormalizedHit} a hit to check
     * @param rank          {@link Ranks} a rank at which to check
     * @return {@link true} if the {@link NormalizedHit} checks, otherwise {@link false} is returned. Upon null instead of either normalizedHit or rank
     *         returns {@link false}.
     */
    protected boolean normalyzedHitChecksAgainstParametersForRank(final NormalizedHit normalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        //Cecks if a cutoff set exists at a given ranks
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null) {
            //If not - substitutes it with a default cutoff set
            tuitCutoffSet = BLAST_Identifier.DEFAULT_CUTOFFS.get(rank);
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
     * @return {@link true} if the {@link NormalizedHit}'s ratio (difference in folds) is greater than the cutoff, otherwise {@link false}
     *         is returned. Upon null instead of either normalizedHit or rank
     *         returns {@link false}.
     */
    protected boolean hitsAreFarEnoughByEvalueAtRank(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit, Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null || oneNormalizedHit == null || anotherNormalizedHit == null) {
            tuitCutoffSet = BLAST_Identifier.DEFAULT_CUTOFFS.get(rank);
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
                return null;
            }
        } finally {
            //Close and cleanup
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        //TODO: remove all resultsets.close() from the finally blocks
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
            //Asuming the database is consistent - one taxid should have only one immediate parent
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
     * Based on the SQL database NCBI schema and the {@link Ranks} of the given {@link NormalizedHit} and its {@link Ranks}
     * reassembles and assignes the full taxonomy for the  {@link NormalizedHit} for its current {@link Ranks} down to the leaves
     *
     * @param parentNode {@link NormalizedHit} that needs to know its children
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter, but with a newly attached
     *         {@link TaxonomicNode} of full taxonomy including leaves from a given {@link Ranks}
     * @throws SQLException in case a database communication error occurs
     */
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
     * Normalizes the {@link Iteration}s returned by the BLASTN within the output
     */
    protected void normalizeIterations() {
        //Normalize each iteration
        for (Iteration iteration : this.blastOutput.getBlastOutputIterations().getIteration()) {
            this.normalizedIterations.add(NormalizedIteration.newDefaultInstanceFromIteration(iteration, this));
        }
    }

    /**
     * A static factory to get a new instance of a {@link BLAST_Identifier}
     /**
     * @param query         {@link List<? extends   format.fasta.nucleotide.NucleotideFasta  >} a list of query
     *                      fasta-formatted records
     * @param tempDir       {@link File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String[]} A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                      taxonomic information
     * @param cutoffSetMap  a {@link Map<Ranks, TUITCutoffSet>}, provided by the user and that may differ from the
     *                      default set
     * @return a new instance of {@link BLAST_Identifier} from the given parameters
     */
    public static BLAST_Identifier newDefaultInstance(List<? extends NucleotideFasta> query,
                                                      File tempDir, File executive, String[] parameterList,TUITFileOperator identifierFileOperator,
                                                      Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        return new BLAST_Identifier(query, null, tempDir, executive, parameterList,identifierFileOperator, connection, cutoffSetMap);
    }

}
