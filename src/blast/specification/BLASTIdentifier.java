package blast.specification;


import blast.ncbi.local.exec.NCBI_EX_BLASTN;
import blast.ncbi.output.BlastOutput;
import blast.ncbi.output.Iteration;
import blast.normal.hit.NormalizedHit;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import db.connect.TaxonomicDatabaseOperator;
import format.BadFormatException;
import format.fasta.nucleotide.NucleotideFasta;
import io.file.TUITFileOperator;
import logger.Log;
import org.xml.sax.SAXException;
import taxonomy.Ranks;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Combines functionality of a local (remote with "-remote" option) BLASTN and an ability to assign a taxonomy to the
 * given queries automatically.
 */
public abstract class BLASTIdentifier<T extends NucleotideFasta> extends NCBI_EX_BLASTN<T> implements TaxonomicDatabaseOperator {

    /**
     * A Map for default cutoff sets, which are used whenever a custom set was not given
     */
    @SuppressWarnings("WeakerAccess")
    protected static final Map<Ranks, TUITCutoffSet> DEFAULT_CUTOFFS = new HashMap<Ranks, TUITCutoffSet>();

    /**
     * Filling in the cutoff set map
     */
    static {

        //Species-level-related
        DEFAULT_CUTOFFS.put(Ranks.subspecies, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species_subgroup, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.species_group, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.varietas, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.forma, TUITCutoffSet.newDefaultInstance(97.5, 95, 0.05));


        //Genus-level-related
        DEFAULT_CUTOFFS.put(Ranks.subgenus, TUITCutoffSet.newDefaultInstance(95, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.genus, TUITCutoffSet.newDefaultInstance(95, 90, 0.05));

        //Family-level-related
        DEFAULT_CUTOFFS.put(Ranks.subfamily, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.family, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superfamily, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.tribe, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subtribe, TUITCutoffSet.newDefaultInstance(80, 90, 0.05));

        //Order-level-related
        DEFAULT_CUTOFFS.put(Ranks.order, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.parvorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.infraorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.suborder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superorder, TUITCutoffSet.newDefaultInstance(70, 90, 0.05));

        //Any other level-related
        DEFAULT_CUTOFFS.put(Ranks.subclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.infraclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.c_lass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superclass, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subphylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.phylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superphylum, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.subkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.kingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.superkingdom, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.no_rank, TUITCutoffSet.newDefaultInstance(60, 80, 0.05));
        DEFAULT_CUTOFFS.put(Ranks.root_of_life, TUITCutoffSet.newDefaultInstance(0, 0, 0.05));

    }

    /**
     * A size of a batch
     */
    @SuppressWarnings("WeakerAccess")
    protected final int batchSize;
    /**
     * Indicates whether the module should cleanup temp files (such as BLAST output)
     */
    @SuppressWarnings("WeakerAccess")
    protected final boolean cleanup;
    /**
     * The number of the last record specified
     */
    @SuppressWarnings("WeakerAccess")
    protected int progressEdge;

    /**
     * Returns a cutoff set for a given taxonomic rank
     *
     * @param rank {@link Ranks} taxonomic rank
     * @return {@link TUITCutoffSet} cutoff set
     */
    public TUITCutoffSet getCufoffsetByRank(Ranks rank) {
        if(this.cutoffSetMap.get(rank)==null){
            return DEFAULT_CUTOFFS.get(rank);
        }
        return this.cutoffSetMap.get(rank);
    }

    /**
     * A setter for a BLAST output to identify
     *
     * @param blastOutput {@link BlastOutput} that will be used for taxonomic identification
     */
    @SuppressWarnings("WeakerAccess")
    public void setBlastOutput(BlastOutput blastOutput) {
        this.blastOutput = blastOutput;
    }

    /**
     * A custom cutoff set map, provided by the user
     */
    @SuppressWarnings("WeakerAccess")
    protected final Map<Ranks, TUITCutoffSet> cutoffSetMap;
    /**
     * A list of normalized hits that the algorithm will operate upon
     */
    @SuppressWarnings("WeakerAccess")
    protected List<NormalizedIteration<Iteration>> normalizedIterations;

    /**
     * @param query         {@link java.util.List} a list of query
     *                      fasta-formatted records
     * @param tempDir       {@link java.io.File} - A temporary directory that will be used to dump
     *                      the input and output files, that are used by the ncbi+
     *                      executable
     * @param executive     {@link java.io.File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                      allow to create an input file as well as catch the blast
     *                      output
     * @param parameterList {@link String}[] A list of parameters. Should maintain a
     *                      certain order. {"<-command>", "[value]"}, just the way if in
     *                      the blast+ executable input
     * @param cutoffSetMap  a {@link java.util.Map}, provided by the user and that may differ from the
     */
    @SuppressWarnings("WeakerAccess")
    protected BLASTIdentifier(List<T> query,
                              File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
                              Map<Ranks, TUITCutoffSet> cutoffSetMap,final int batchSize, final boolean cleanup) {
        super(query, null, tempDir, executive, parameterList,
                identifierFileOperator);
        this.cutoffSetMap = cutoffSetMap;
        this.batchSize=batchSize;
        this.cleanup=cleanup;
        this.progressEdge=0;
    }

    /**
     * Checks whether a given {@link blast.normal.hit.NormalizedHit} checks against the cutoffs at a given {@link Ranks} of specification
     *
     * @param normalizedHit {@link blast.normal.hit.NormalizedHit} a hit to check
     * @param rank          {@link Ranks} a rank at which to check
     * @return {@code true} if the {@link blast.normal.hit.NormalizedHit} checks, otherwise {@code false} is returned. Upon null instead of either normalizedHit or rank
     * returns {@code false}.
     */
    public boolean normalisedHitChecksAgainstParametersForRank(final NormalizedHit normalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        //Checks if a cutoff set exists at a given ranks
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null) {
            //If not - substitutes it with a default cutoff set
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        return !(normalizedHit == null || rank == null) && tuitCutoffSet.normalizedHitPassesCheck(normalizedHit);
    }

    /**
     * Checks whether the two given {@link NormalizedHit}s are far enough by the E-value (the ratio difference (in folds) is greater than the cutoff
     * value at the given rank)
     *
     * @param oneNormalizedHit     {@link NormalizedHit} first hit (a hit with a worse E-value)
     * @param anotherNormalizedHit a {@link NormalizedHit}  with a better E-value)
     * @param rank                 {@link Ranks} at which the E-value difference is being monitored
     * @return {@code true} if the {@link NormalizedHit}'s ratio (difference in folds) is greater than the cutoff, otherwise {@code false}
     * is returned. Upon null instead of either normalizedHit or rank
     * returns {@code false}.
     */
    public boolean hitsAreStatisticallyDifferentAtRank(final NormalizedHit oneNormalizedHit, final NormalizedHit anotherNormalizedHit, final Ranks rank) {
        TUITCutoffSet tuitCutoffSet;
        if ((tuitCutoffSet = this.cutoffSetMap.get(rank)) == null || oneNormalizedHit == null || anotherNormalizedHit == null) {
            tuitCutoffSet = BLASTIdentifier.DEFAULT_CUTOFFS.get(rank);
        }
        return tuitCutoffSet.hitsAreStatisticallyDifferent(oneNormalizedHit, anotherNormalizedHit);
    }

    /**
     * Normalizes the {@link Iteration}s returned by the BLASTN within the output
     */
    @SuppressWarnings("WeakerAccess")
    protected void normalizeIterations() {
        //Normalize each iteration
        int i = 0;
        for (Iteration iteration : this.blastOutput.getBlastOutputIterations().getIteration()) {
            this.normalizedIterations.add(NormalizedIteration.<Iteration>newDefaultInstanceFromIteration((NucleotideFasta) this.query.get(i), iteration, this));
            i++;
        }
    }

    /**
     * Accepts a result pair of a query {@link NucleotideFasta} and its {@link NormalizedIteration} (thereby specified)
     * {@link TUITFileOperator} in order to save the result in the way defined by the current file operator
     *
     * @param query               {@link NucleotideFasta}
     * @param normalizedIteration {@link NormalizedIteration}
     * @return {@code true} if the file operator returns success, {@code false} otherwise
     */
    @SuppressWarnings({"unchecked", "UnusedReturnValue"})
    public boolean acceptResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws Exception {
        return ((TUITFileOperator) this.fileOperator).saveResults(query, normalizedIteration);
    }

    /**
     * Utility method that handles the process of taxonomic specification
     *
     * @throws SQLException       in case an error occurs during database communication
     * @throws BadFormatException in case an error in case formatting the {@link blast.ncbi.output.Hit} GI fails
     */
    @SuppressWarnings("WeakerAccess")
    protected void specify() throws Exception {
        Log.getInstance().log(Level.FINE,"Specifying the BLAST output.");
        if (this.blastOutput.getBlastOutputIterations().getIteration().size() > 0) {
            this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
            this.normalizeIterations();
            for (NormalizedIteration<Iteration> normalizedIteration : this.normalizedIterations) {
                normalizedIteration.specify();
            }
            this.normalizedIterations=null;
        } else {
            Log.getInstance().log(Level.SEVERE,"No Iterations were returned, an error might have occurred during BLAST, proceeding with the next query.");
        }
    }
}
