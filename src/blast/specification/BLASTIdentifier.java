package blast.specification;


import blast.ncbi.local.exec.NCBI_EX_BLASTN;
import blast.ncbi.output.BlastOutput;
import blast.ncbi.output.Iteration;
import blast.specification.cutoff.TUITCutoffSet;
import blast.normal.hit.NormalizedHit;
import blast.normal.iteration.NormalizedIteration;
import db.connect.TaxonomicDatabaseOperator;
import db.tables.LookupNames;
import format.BadFormatException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
 * Combines functionality of a local (remote with "-remote" option) BLASTN and an ability to assign a taxonomy to the
 * given queries automatically.
 */
public abstract class BLASTIdentifier<T extends NucleotideFasta> extends NCBI_EX_BLASTN<T> implements TaxonomicDatabaseOperator {

    /**
     * A Map for default cutoff sets, which are used whenever a custom set was not given
     */
    @SuppressWarnings("WeakerAccess")
    protected static final Map<Ranks, TUITCutoffSet> DEFAULT_CUTOFFS = new HashMap<Ranks, TUITCutoffSet>();

    /**
     * Filling in the cutoff set map
     */
    static {

        //Species-level-related
        DEFAULT_CUTOFFS.put(Ranks.subspecies, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species_subgroup, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species_group, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.varietas, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.forma, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));


        //Genus-level-related
        DEFAULT_CUTOFFS.put(Ranks.subgenus, TUITCutoffSet.newDefaultInstance(95, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.genus, TUITCutoffSet.newDefaultInstance(95, 90, 0.05));

        //Family-level-related
        DEFAULT_CUTOFFS.put(Ranks.subfamily, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.family, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superfamily, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.tribe, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subtribe, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));

        //Order-level-related
        DEFAULT_CUTOFFS.put(Ranks.order, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.parvorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.infraorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.suborder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));

        //Any other level-related
        DEFAULT_CUTOFFS.put(Ranks.subclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.infraclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.c_lass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subphylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.phylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superphylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.kingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.no_rank, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.root_of_life, TUITCutoffSet.newDefaultInstance(0, 0, 0.05));

    }

    /**
     * Returns a cutoff set for a given taxonomic rank
     * @param rank {@link Ranks} taxonomic rank
     * @return {@link TUITCutoffSet} cutoff set
     */
    public TUITCutoffSet getCufoffsetByRank(Ranks rank){
        return this.cutoffSetMap.get(rank);
    }
    /**
     * A setter for a BLAST output to identify
     * @param blastOutput {@link BlastOutput} that will be used for taxonomic identification
     */
    @SuppressWarnings("WeakerAccess")
    public void setBlastOutput(BlastOutput blastOutput){
        this.blastOutput=blastOutput;
    }
    /**
     * A connection to an SQL database, which contains a NCBI schema with taxonomic information
     */
    @SuppressWarnings("WeakerAccess")
    protected final Connection connection;
    /**
     * A custom cutoff set map, provided by the user
     */
    @SuppressWarnings("WeakerAccess")
    protected final Map<Ranks, TUITCutoffSet> cutoffSetMap;
    /**
     * A list of normalized hits that the algorithm will operate upon
     */
    @SuppressWarnings("WeakerAccess")
    protected List<NormalizedIteration<Iteration>> normalizedIterations;

    /**
     * @param query         {@link java.util.List} a list of query
     *                      fasta-formatted records
     * @param tempDir       {@link java.io.File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link java.io.File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
 *                      allow to create an input file as well as catch the blast
 *                      output
     * @param parameterList {@link String}[] A list of parameters. Should maintain a
*                      certain order. {"<-command>", "[value]"}, just the way if in
*                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the necessary
*                      taxonomic information
     * @param cutoffSetMap  a {@link java.util.Map}, provided by the user and that may differ from the
     */
    @SuppressWarnings("WeakerAccess")
    protected BLASTIdentifier(List<T> query,
                              File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                              Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        super(query, null, tempDir, executive, parameterList,
                identifierFileOperator);
        this.connection = connection;
        this.cutoffSetMap = cutoffSetMap;
    }

    /**
     * Checks whether a given {@link blast.normal.hit.NormalizedHit} checks against the cutoffs at a given {@link Ranks} of specification
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} a hit to check
     * @param rank          {@link Ranks} a rank at which to check
     * @return {@code true} if the {@link blast.normal.hit.NormalizedHit} checks, otherwise {@code false} is returned. Upon null instead of either normalizedHit or rank
     *         returns {@code false}.
     */
    public boolean normalisedHitChecksAgainstParametersForRank(final NormalizedHit normalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        //Checks if a cutoff set exists at a given ranks
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null) {
            //If not - substitutes it with a default cutoff set
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        return !(normalizedHit == null || rank == null) && tuitCutoffSet.normalizedHitPassesCheck(normalizedHit);
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
    public boolean hitsAreStatisticallyDifferentAtRank(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null || oneNormalizedHit == null || anotherNormalizedHit == null) {
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        return tuitCutoffSet.hitsAreStatisticallyDifferent(oneNormalizedHit, anotherNormalizedHit);
    }

    /**
     * Based on the SQL database NCBI schema and the {@link Ranks} of the given {@link TaxonomicNode}, assigned the taxonomy (taxid, scientific name)
     * **does not assign children down to the leaves, that is done by the {@code attachChildrenForTaxonomicNode(TaxonomicNode parentNode)}
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
        ResultSet resultSet;
        try {
            //Try selecting the child nodes for the given hit
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.taxon_by_gi.getName()
                            + " where "
                            + LookupNames.dbs.NCBI.gi_taxid.columns.gi.name()
                            + "=? ");
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
     * rises its rank one step higher (say, for subspecies raised for species)
     *
     * @param normalizedHit {@link NormalizedHit}
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter,
     *         but with one step risen {@code Ranks}
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public NormalizedHit liftRankForNormalizedHit(final NormalizedHit normalizedHit) throws SQLException {
        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
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
     * Allows to reduce those hits, which have a no_rank parent (such as unclassified Bacteria)
     * @param normalizedHit {@link NormalizedHit}
     * @return {@code true} if a given Hit has a no_rank parent, {@code false} otherwise
     * @throws SQLException in case a database communication error occurs
     */
    public boolean hitHasANoRankParent(final NormalizedHit normalizedHit) throws SQLException {
        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        try {
            //Try selecting the parent node for the given hit
            //Assuming the database is consistent - one taxid should have only one immediate parent
            preparedStatement = this.connection.prepareStatement(
                    "SELECT * FROM "
                            + LookupNames.dbs.NCBI.name + "."
                            + LookupNames.dbs.NCBI.views.rank_by_taxid.getName()
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
                if(Ranks.values()[resultSet.getInt(4) - 1].equals(Ranks.no_rank)){
                    preparedStatement.close();
                    return true;
                }
            } else {
                preparedStatement.close();
                return true;
            }
        } finally {
            if (preparedStatement != null) {
                preparedStatement.close();
            }
        }
        return false;
    }


    /**
     * For a given {@link TaxonomicNode} attaches its parent and higher lineage structure. Originally used to
     * save results in a form of a taxonomic branch.
     *
     * @param taxonomicNode {@link TaxonomicNode} that needs to get its full lineage structure
     * @return a pointer to the same {@link TaxonomicNode} object, but with attached pointers to its taxonomic lineage
     * @throws SQLException in case an error in database communication occurs
     */
    @Override
    public TaxonomicNode attachFullDirectLineage(TaxonomicNode taxonomicNode) throws SQLException {

        //Get its taxid and reconstruct its child taxonomic nodes
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
        TaxonomicNode parentTaxonomicNode;
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
            int parent_taxid;
            int taxid;
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
                    preparedStatement.close();
                    //noinspection UnusedAssignment
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
     * reassembles and assigned the full taxonomy for the  {@link NormalizedHit} for its current {@link Ranks} down to the leaves
     *
     * @param parentNode {@link NormalizedHit} that needs to know its children
     * @return {@link NormalizedHit} which points to the same object as the given {@link NormalizedHit} parameter, but with a newly attached
     *         {@link TaxonomicNode} of full taxonomy including leaves from a given {@link Ranks}
     * @throws SQLException in case a database communication error occurs
     */
    @Override
    public TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode parentNode) throws SQLException {
        PreparedStatement preparedStatement = null;
        ResultSet resultSet;
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
     * whether the parent taxid may be a sibling taxid for the given. Used to check for if the normalized hits with
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
        ResultSet resultSet;
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
            if (resultSet.next()) {
                return parentTaxid == resultSet.getInt(1) || resultSet.getInt(1) != 1 && this.isParentOf(parentTaxid, resultSet.getInt(1));
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
    @SuppressWarnings("WeakerAccess")
    protected void normalizeIterations() {
        //Normalize each iteration
        int i=0;
        for (Iteration iteration : this.blastOutput.getBlastOutputIterations().getIteration()) {
            this.normalizedIterations.add(NormalizedIteration.<Iteration>newDefaultInstanceFromIteration((NucleotideFasta) this.query.get(i), iteration, this));
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
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public boolean acceptResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws Exception {
        return ((TUITFileOperator) this.fileOperator).saveResults(query, normalizedIteration);
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
     * @param executive     {@link File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String}[] A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param connection    a connection to the SQL Database that contains a NCBI schema with all the necessary
     *                      taxonomic information
     * @param cutoffSetMap  a {@link Map}, provided by the user and that may differ from the
     *                      default set
     * @return a new instance of {@link BLASTIdentifier} from the given parameters
     */
    public static BLASTIdentifier newDefaultInstance(List<NucleotideFasta> query,
                                                     File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                                                     Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        return new BLASTIdentifier<NucleotideFasta>(query, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap) {
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
                        for (NormalizedIteration<Iteration> normalizedIteration : this.normalizedIterations) {
                            normalizedIteration.specify();
                        }
                    } else {
                        Log.getInstance().log(Level.SEVERE, "No Iterations were returned, an error might have occurred during BLAST.");
                    }
                } catch (IOException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                } catch (InterruptedException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                } catch (JAXBException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                } catch (SAXException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                } catch (SQLException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                } catch (BadFormatException e) {
                    Log.getInstance().log(Level.SEVERE,e.getMessage());
                }
            }
        };
    }
}
