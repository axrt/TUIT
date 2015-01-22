package blast.continous;

import format.fasta.nucleotide.NucleotideFasta;

import java.nio.file.Path;

/**
 * Created by alext on 1/22/15.
 */
public interface ContinousTUITDataProvider {

    public NucleotideFasta nextQuery() throws Exception;
    public long checkNumberOfRecords() throws Exception;

}
