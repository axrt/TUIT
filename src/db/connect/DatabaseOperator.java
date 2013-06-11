package db.connect;

import BLAST.NCBI.output.Hit;
import blast.NormalizedHit;
import taxonomy.TaxonomicNode;

import java.sql.SQLException;

/**
 TODO: document

 */
public interface DatabaseOperator {

    /**
     *
     * @param normalizedHit
     * @return {@code null} in case the database has no match for a given hit GI
     * @throws Exception
     */
   public NormalizedHit assignTaxonomy(final NormalizedHit normalizedHit) throws SQLException;

   public NormalizedHit liftRankForNormalyzedHit(final NormalizedHit normalizedHit) throws SQLException;

}
