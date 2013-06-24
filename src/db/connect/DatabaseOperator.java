package db.connect;

import blast.normal.hit.NormalizedHit;
import taxonomy.TaxonomicNode;

/**
 * TODO: document
 * TODO: this should be extended by another interface that allows to communicate in order to get the taxonomy
 */
public interface DatabaseOperator {

    /**
     * @param normalizedHit
     * @return {@code null} in case the database has no match for a given hit GI
     * @throws Exception
     */
    public NormalizedHit assignTaxonomy(final NormalizedHit normalizedHit) throws Exception;

    public NormalizedHit liftRankForNormalyzedHit(final NormalizedHit normalizedHit) throws Exception;

    public TaxonomicNode attachChildrenForTaxonomicNode(TaxonomicNode parentNode) throws Exception;

    public boolean isParentOrSiblingTo(int parentTaxid, int taxid) throws Exception;
    public TaxonomicNode attachFullDirectLineage(TaxonomicNode taxonomicNode) throws Exception;
}
