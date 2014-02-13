package blast.specification;

import blast.normal.hit.NormalizedHit;
import blast.specification.cutoff.TUITCutoffSet;
import db.tables.LookupNames;
import format.fasta.nucleotide.NucleotideFasta;
import io.file.TUITFileOperator;
import taxonomy.Ranks;
import taxonomy.node.TaxonomicNode;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * Created by alext on 2/13/14.TODO: document
 */
public abstract class BLASTIdentifierDB extends BLASTIdentifier<NucleotideFasta> {

    /**
     * A connection to an SQL database, which contains a NCBI schema with taxonomic information
     */
    @SuppressWarnings("WeakerAccess")
    protected final Connection connection;

    public BLASTIdentifierDB(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap) {
        super(query, tempDir, executive, parameterList, identifierFileOperator, cutoffSetMap);
        this.connection = connection;
    }

    /**
     * Based on the SQL database NCBI schema and the {@link taxonomy.Ranks} of the given {@link taxonomy.node.TaxonomicNode}, assigned the taxonomy (taxid, scientific name)
     * **does not assign children down to the leaves, that is done by the {@code attachChildrenForTaxonomicNode(TaxonomicNode parentNode)}
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} which needs to know its taxonomy
     * @return {@link blast.normal.hit.NormalizedHit} which points to the same object as the given {@link blast.normal.hit.NormalizedHit} parameter, but with a newly attached
     * {@link taxonomy.node.TaxonomicNode}
     * @throws java.sql.SQLException in case a database communication error occurs
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
     * but with one step risen {@code Ranks}
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
     *
     * @param normalizedHit {@link NormalizedHit}
     * @return {@code true} if a given Hit has a no_rank parent, {@code false} otherwise
     * @throws SQLException in case a database communication error occurs
     */
    @Override
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
                if (Ranks.values()[resultSet.getInt(4) - 1].equals(Ranks.no_rank)) {
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
     * {@link TaxonomicNode} of full taxonomy including leaves from a given {@link Ranks}
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
     * the given taxid, {@code false} otherwise.
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

}
