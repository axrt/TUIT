package db.connect;

import BLAST.NCBI.output.Hit;
import blast.NormalyzedHit;

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
   public NormalyzedHit normalyzeHit(Hit hit,int queryLength) throws SQLException;

   public NormalyzedHit liftRankForNormalyzedHit(NormalyzedHit hit) throws SQLException;

}
