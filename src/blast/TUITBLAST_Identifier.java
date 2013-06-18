package blast;

import format.fasta.nucleotide.NucleotideFasta;
import helper.Ranks;
import io.file.TUITFileOperator;

import java.io.File;
import java.sql.Connection;
import java.util.List;
import java.util.Map;

/**
 * //TODO:Document as soon as works
 * To change this template use File | Settings | File Templates.
 */
public class TUITBLAST_Identifier extends BLAST_Identifier {

    protected int batchSize;

    protected TUITBLAST_Identifier(List<NucleotideFasta> query, List<String> query_IDs, File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) {
        super(query, query_IDs, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap);
        this.batchSize = batchSize;
    }

    @Override
    public void run() {

    }

    public static TUITBLAST_Identifier newInstanceFromFileOperator(
            File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
            Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            TUITBLAST_Identifier tuitblastIdentifier = new TUITBLAST_Identifier(batch, null, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, batchSize);
            return tuitblastIdentifier;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
