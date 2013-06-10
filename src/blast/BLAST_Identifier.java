package blast;

import BLAST.NCBI.local.exec.NCBI_EX_BLASTN;
import BLAST.NCBI.output.Iteration;
import db.connect.DatabaseOperator;
import format.fasta.nucleotide.NculeotideFasta;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 */
public class BLAST_Identifier extends NCBI_EX_BLASTN {

    protected DatabaseOperator databaseOperator;


    protected BLAST_Identifier(List<? extends NculeotideFasta> query, List<String> query_IDs, File tempDir, File executive, String[] parameterList) {
        super(query, query_IDs, tempDir, executive, parameterList);
    }

    @Override
    public void run() {
        //To change body of implemented methods use File | Settings | File Templates.
    }

}
