package db.connect;

import blast.normal.hit.NormalizedHit;
import taxonomy.node.TaxonomicNode;

/**
 *
 */
public interface TaxonomicDatabaseOperator extends DatabaseOperator {

    /**
     * Should be able to assign a taxonomic node features to the given {@link NormalizedHit}, at least: a taxid, a rank and scientific name
     *
     * @param normalizedHit {@link NormalizedHit} that needs to get its own taxonomy
     * @return {@link NormalizedHit} the same pointer to the same object, but with assigned taxonomic node parameters
     * @throws Exception
     */
    public NormalizedHit assignTaxonomy(final NormalizedHit normalizedHit) throws Exception;

    /**
     * Should be able to lift a hit's taxonomic rank one level higher and return a pointer to the same {@link NormalizedHit}.
     *
     * @param normalizedHit {@link NormalizedHit} that needs to have its focus node lifted to a higher rank
     * @return a {@link NormalizedHit} with a focus node lifted one level higher
     * @throws Exception
     */
    public NormalizedHit liftRankForNormalizedHit(final NormalizedHit normalizedHit) throws Exception;

    /**
     * For a given {@link TaxonomicNode} should be able to reconstruct from the database and assign a full taxonomic subtree of
     * descendants.
     *
     * @param parentNode a {@link TaxonomicNode} that needs to have its taxonomic tree of descendants
     * @return {@link TaxonomicNode} pointer to the same object as the input, but with attached taxonomic tree of descendants
     * @throws Exception
     */
    public TaxonomicNode attachChildrenForTaxonomicNode(final TaxonomicNode parentNode) throws Exception;

    /**
     * Should be able to check whether a given parent taxid is indeed a parent taxid for the given one, as well as it should check
     * whether the parent taxid may be a sibling taxid for the given.
     *
     * @param parentTaxid a taxid of a {@link TaxonomicNode} that should be a parent to the given taxid in order to
     *                    support the choice of the pivotal normalized hit
     * @param taxid       of the {@link TaxonomicNode} of the pivotal taxid
     * @return {@code true} if the parent taxid is indeed parent (direct parent or grand parent within the lineage) of
     *         the given taxid, {@code false} otherwise.
     * @throws Exception
     */
    public boolean isParentOf(int parentTaxid, int taxid) throws Exception;

    /**
     * For a given {@link TaxonomicNode} should be able to attach its parent and higher lineage structure.
     *
     * @param taxonomicNode {@link TaxonomicNode} that needs to get its full lineage structure
     * @return a pointer to the same {@link TaxonomicNode} object, but with attached pointers to its taxonomic lineage
     * @throws Exception
     */
    public TaxonomicNode attachFullDirectLineage(final TaxonomicNode taxonomicNode) throws Exception;
}
