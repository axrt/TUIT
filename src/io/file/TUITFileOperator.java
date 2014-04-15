package io.file;

import blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator;
import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
 * This class handles file opening for the {@link blast.specification.BLASTIdentifier}
 */
public abstract class TUITFileOperator<T extends NucleotideFasta> extends NCBI_EX_BLAST_FileOperator<T> implements AutoCloseable {
    /**
     * A BufferedReader to read the input file
     */
    @SuppressWarnings("WeakerAccess")
    protected BufferedReader bufferedReader;
    /**
     * A BufferedWriter to write the output
     */
    @SuppressWarnings("WeakerAccess")
    protected BufferedWriter bufferedWriter;
    /**
     * Output file
     */
    @SuppressWarnings("WeakerAccess")
    protected File outputFile;
    /**
     * Input file
     */
    @SuppressWarnings("WeakerAccess")
    protected File inputFile;
    /**
     * A line that holds current line of reading
     */
    @SuppressWarnings("WeakerAccess")
    protected String line;

    /**
     * Sets the input file. If both the input file or the designated BufferedReader are {@code null} - sets the local variable
     * to the input file and creates a new BufferedReader for the input file
     *
     * @param inputFile {@link File} to the input file (fasta formatted)
     * @throws FileNotFoundException in case this method is called in an inappropriate time when the module is in the middle
     *                               of reading a previously set input file
     */
    public void setInputFile(File inputFile) throws FileNotFoundException {
        if (this.inputFile == null && this.bufferedReader == null) {
            this.inputFile = inputFile;
            this.bufferedReader = new BufferedReader(new FileReader(this.inputFile));
        } else throw new IllegalStateException("Could not set a given input file while the previous file is open.");
    }

    /**
     * Sets the output file. If both the input file or the designated BufferedReader are {@code null} - sets the local variable
     * to the input file and creates a new BufferedReader for the input file
     *
     * @param outputFile {@link File} sets the output file
     * @throws IOException in case writing to the output file fails
     */
    public void setOutputFile(File outputFile) throws IOException {
        if (this.outputFile == null && this.bufferedWriter == null) {
            this.outputFile = outputFile;
            this.bufferedWriter = new BufferedWriter(new FileWriter(this.outputFile));
        } else throw new IllegalStateException("Could not set a given output file while the previous file is open.");
    }

    /**
     * Should be able to create a new T-representation for a {@link String} representation
     *
     * @param record {@link String} representation of a fasta-formatted record
     * @return newly created from the record.
     * @throws Exception
     */
    protected abstract T newFastaFromRecord(String record) throws Exception;

    /**
     * Should be able to check whether the input file formatting is fine. It may not be in a set of conditions especially when
     * a file contains multiple fastas with the same AC, which is a bad thing for further results interpretation
     *
     * @return {@code true} if the fasta file passes checks, {@code false} otherwise
     * @throws Exception
     */
    @SuppressWarnings("SameReturnValue")
    protected abstract boolean inputFileFormattingIsFine() throws Exception;

    /**
     * Returns a {@code List} of T that will be used as query for specification.
     *
     * @param size {@code int} effective size of the batch
     * @return {@link List} of query Ts if the file contained any more records, {@code null} if
     *         the file contained none.
     * @throws Exception
     */
    public List<T> nextBatch(int size) throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        int fastaCounter = 0;
        if (this.line != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
            fastaCounter++;
        }
        while ((this.line = this.bufferedReader.readLine()) != null) {
            if (line.startsWith(Fasta.fastaStart)) {
                if (fastaCounter >= size) {
                    break;
                }
                fastaCounter++;
            }
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        final List<T> batch=new ArrayList<T>(size);
        final String[] split = stringBuilder.toString().split(Fasta.fastaStart);
        for (int i = 1; i < split.length; i++) {
            batch.add(this.newFastaFromRecord(Fasta.fastaStart + split[i]));
        }
        if (batch.isEmpty()) {
            return null;
        } else {
            return batch;
        }
    }

    /**
     * Resets the {@link TUITFileOperator} so that all the readers/writers get assigned nulls
     *
     * @throws IOException
     */
    public void reset() throws IOException {
        this.line = null;
        this.inputFile = null;
        this.outputFile = null;
        this.bufferedWriter.close();
        this.bufferedWriter = null;
        this.bufferedReader.close();
        this.bufferedReader = null;
    }

    /**
     * Overridden close that makes the code work with try-with-resources
     * @throws Exception in case smth goes wrong during close();
     */
    @Override
    public void close() throws Exception {
        this.bufferedWriter.close();
        this.bufferedReader.close();
    }

    /**
     * Overridden version has public access instead of protected
     *
     * @param outputFile {@link File} a file that contains an XML BLASTN output
     * @return {@link InputStream} that allows to read an {@link blast.ncbi.output.BlastOutput} representation from the XML representation
     * @throws IOException in case an error reading file or the input stream occurs
     */
    @Override
    public InputStream readOutputXML(File outputFile) throws IOException {
        return super.readOutputXML(outputFile);
    }

    @SuppressWarnings("SameReturnValue")
    public abstract boolean saveResults(T query, NormalizedIteration<Iteration> normalizedIteration) throws Exception;

}
