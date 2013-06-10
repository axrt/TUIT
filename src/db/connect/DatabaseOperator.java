package db.connect;

import BLAST.NCBI.output.Hit;
import blast.NormalizedHit;

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
   public NormalizedHit  normalyzeHit(Hit hit,int queryLength) throws SQLException;

}
