package io.file;

import BLAST.NCBI.output.Iteration;
import blast.NormalizedIteration;
import format.fasta.nucleotide.NucleotideFasta;

import java.io.IOException;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/18/13
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class NucleotideFastaTUITFileOperator extends TUITFileOperator<NucleotideFasta> {
    /**
     * A static getter for the singleton instance
     *
     * @return a singleton instance of the {@link TUITFileOperator}
     */
    public static NucleotideFastaTUITFileOperator getInstance() {
        return NucleotideFastaTUITFileOperator.SingletonHolder.instance;
    }
    /**
     * Not initialized until referenced
     */
    private static final class SingletonHolder {
        static final NucleotideFastaTUITFileOperator instance = new NucleotideFastaTUITFileOperator();
    }

    @Override
    protected NucleotideFasta newFastaFromRecord(String record) throws Exception {
        return NucleotideFasta.newInstanceFromFromattedText(record);
    }

    @Override
    public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws IOException {
        //TODO: input preparsing for fasta naming collisions
        this.bufferedWriter.write(query.getAC()+": "+normalizedIteration.getPivotalHit().getFocusNode().getFormattedLineage());
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
