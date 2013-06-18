package io.file;

import BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator;
import blast.NormalizedIteration;
import format.EncodedFasta;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * //TODO: document
 * This class handles file opening for the  whole system
 */
public abstract class TUITFileOperator<T extends NucleotideFasta> extends NCBI_EX_BLAST_FileOperator {

    protected BufferedReader bufferedReader;
    protected File inputFile;
    protected String line;

    public void setInputFile(File inputFile) throws FileNotFoundException {
        if (this.inputFile == null && this.bufferedReader == null) {
            this.inputFile = inputFile;
            this.bufferedReader = new BufferedReader(new FileReader(inputFile));
        } else throw new IllegalStateException("Could not set a given file while the previous file is open.");
    }

    protected abstract T newFastaFromRecord(String record) throws Exception;

    public List<T> nextBatch(int size) throws Exception {
        List<T> batch = null;
        try {
            batch = new ArrayList<T>(size);
            StringBuilder stringBuilder = new StringBuilder();
            int fastaCounter = 1;
            if (this.line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
            }
            while ((this.line = this.bufferedReader.readLine()) != null) {
                if (line.contains(Fasta.fastaStart)) {
                    fastaCounter++;
                    if (fastaCounter >= size) {
                        break;
                    }
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
            }
            String[] split = stringBuilder.toString().split(Fasta.fastaStart);
            for (String s : split) {
                batch.add(this.newFastaFromRecord(Fasta.fastaStart + s));
            }
            if (this.line == null) {
                this.reset();
            }
        } catch (Exception e) {
            this.reset();
            throw e;
        }
        return batch;
    }

    protected void reset() throws IOException {
        this.line = null;
        this.inputFile = null;
        this.bufferedReader.close();
        this.bufferedReader = null;
    }


    public abstract boolean acceptResults(T query, NormalizedIteration normalizedIteration);

}
