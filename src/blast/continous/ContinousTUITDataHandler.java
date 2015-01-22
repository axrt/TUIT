package blast.continous;

import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import format.fasta.nucleotide.NucleotideFasta;
import taxonomy.Ranks;

import java.util.Map;


/**
 * Created by alext on 1/22/15.
 */
public interface ContinousTUITDataHandler {

    public boolean saveIteration(Iteration iteration) throws Exception;
    public boolean saveTaxonomyLine(
                                    Map<Ranks, TUITCutoffSet> cutoffSetMap,
                                    NucleotideFasta query,
                                    NormalizedIteration<blast.ncbi.output.Iteration> normalizedIteration) throws Exception;

}
