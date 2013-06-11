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
     * @param hit
     * @return {@code null} in case the database has no match for a given hit GI
     * @throws Exception
     */
   public NormalizedHit normalyzeHit(Hit hit,int queryLength) throws Exception;

   public NormalizedHit liftRankForNormalyzedHit(NormalizedHit hit) throws SQLException;

}
