package blast;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Hit;
import db.connect.DatabaseOperator;
import format.fasta.nucleotide.NculeotideFasta;
import helper.Ranks;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

/**
 */
public class BLAST_Identifier extends NCBI_EX_BLASTN implements DatabaseOperator {

    protected DatabaseOperator databaseOperator;


    protected BLAST_Identifier(List<? extends NculeotideFasta> query, List<String> query_IDs, File tempDir, File executive, String[] parameterList) {
        super(query, query_IDs, tempDir, executive, parameterList);
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    protected boolean normalyzedHitChecksAgainstParametersForRank(NormalyzedHit normalizedHit, Ranks ranks){
        return false;
    }

    @Override
    public NormalyzedHit normalyzeHit(Hit hit, int queryLength) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public NormalyzedHit liftRankForNormalyzedHit(NormalyzedHit hit) throws SQLException {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
