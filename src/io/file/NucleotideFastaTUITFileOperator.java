package io.file;

import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import exception.FastaInputFileException;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import taxonomy.Ranks;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
 * A {@link TUITFileOperator} for the {@link NucleotideFasta}s.
 */
public abstract class NucleotideFastaTUITFileOperator extends TUITFileOperator<NucleotideFasta> {
    /**
     * Used in case the BLAST output iteration had no hits, or the output could not be classified to any possible depth.
     */
    @SuppressWarnings("WeakerAccess")
    public static final String NOT_IDENTIFIED = "<-not identified->";

    /**
     * Checks it the input file formatting is fine. Checks for uniqueness of the names in the ACs.
     *
     * @return {@code true} if is, {@code false} otherwise
     * @throws FastaInputFileException in case there is an error in any Fasta-formatted record within the query file
     * @throws IOException             in case a file read/write
     */
    @Override
    protected boolean inputFileFormattingIsFine() throws FastaInputFileException, IOException {

        try (BufferedReader bf = new BufferedReader(new FileReader(this.inputFile));) {
            final Set<String> fastaACs = new HashSet<String>();
            String nextLine;
            int fastaNumber = 0;
            while ((nextLine = bf.readLine()) != null) {
                if (line.startsWith(Fasta.fastaStart)) {
                    fastaNumber++;
                    if (!fastaACs.add(nextLine)) {
                        throw new FastaInputFileException("A non-unique name at " + fastaNumber + " record.");
                    }
                }
            }
        }
        return true;
    }

    /**
     * A helper method that creates a {@link format.fasta.nucleotide.NucleotideFasta} record from a given {@link java.lang.String} representation of that record.
     *
     * @param record {@link String} representation of a fasta-formatted record
     * @return a newly created {@link format.fasta.nucleotide.NucleotideFasta}
     * @throws Exception in case the {@link java.lang.String} record was not properly formatted or contained illegal characters for a nucleotide sequence.
     */
    @Override
    protected NucleotideFasta newFastaFromRecord(final String record) throws Exception {
        return NucleotideFasta.newInstanceFromFormattedText(record);
    }

    /**
     * A static factory to create a new instance of a {@link NucleotideFastaTUITFileOperator}
     *
     * @return new {@link NucleotideFastaTUITFileOperator}
     */
    public static NucleotideFastaTUITFileOperator newInstance() {
        return new NucleotideFastaTUITFileOperator() {
            /**
             *
             * @param query {@link format.fasta.nucleotide.NucleotideFasta} query sequence
             * @param normalizedIteration {@link blast.normal.iteration.NormalizedIteration} that contains an {@link blast.ncbi.output.Iteration}
             *                                                                              from the BLAST output for the given query
             * @return {@code true} if successfully saved, {@code false} otherwise
             * @throws IOException in case smth went wrong during file read/write
             */
            @Override
            public boolean saveResults(final NucleotideFasta query, final NormalizedIteration<Iteration> normalizedIteration) throws IOException {
                this.bufferedWriter.write(
                        TUITFileOperatorHelper.OutputFormat.defaultTUITFormatter.format(query.getAC().split("\t")[0], normalizedIteration));
                this.bufferedWriter.newLine();
                this.bufferedWriter.flush();
                return true;
            }
        };
    }

    /**
     * A static factory that creates an instance of {@link io.file.NucleotideFastaTUITFileOperator} with a given option for the output format.
     *
     * @param format    {@link io.file.TUITFileOperatorHelper.OutputFormat} that will be used to format the output (currently TUIT or RDP fixrank)
     * @param cutoffMap {@link java.util.Map} of {@link blast.specification.cutoff.TUITCutoffSet} that is needed to correctly format the output.
     *                  For example, for the RDP fixrank version there is not way to determine the confidence level, and it's
     *                  approximation id given as 1-alpha value.
     * @return a new instance of {@link io.file.NucleotideFastaTUITFileOperator}, that is ready to format the output in a desired way.
     */
    public static NucleotideFastaTUITFileOperator newInstance(final TUITFileOperatorHelper.OutputFormat format, final Map<Ranks, TUITCutoffSet> cutoffMap) {
        switch (format) {
            case TUIT: {
                return newInstance();
            }
            case MOTHUR:{
                return new NucleotideFastaTUITFileOperator() {
                    @Override
                    public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws Exception {
                        this.bufferedWriter.write(
                                TUITFileOperatorHelper.OutputFormat.defaultMothurFormatter(cutoffMap).format(query.getAC().split("\t")[0], normalizedIteration)
                        );
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                        return true;
                    }
                };
            }
            case RDP_FIXRANK: {
                return new NucleotideFastaTUITFileOperator() {
                    @Override
                    public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws Exception {
                        this.bufferedWriter.write(
                                TUITFileOperatorHelper.OutputFormat.defaultFixRankRDPFormatter(cutoffMap).format(query.getAC().split("\t")[0], normalizedIteration)
                        );
                        this.bufferedWriter.newLine();
                        this.bufferedWriter.flush();
                        return true;
                    }
                };
            }
        }
        throw new IllegalArgumentException("Wrong unknown format selected!");
    }
}
