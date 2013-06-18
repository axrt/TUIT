package io.file;

import blast.NormalizedIteration;
import format.fasta.nucleotide.NucleotideFasta;

/**
 * Created with IntelliJ IDEA.
 * User: alext
 * Date: 6/18/13
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class NucleotideFastaTUITFileOperator extends TUITFileOperator {
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
    public boolean acceptResults(NucleotideFasta query, NormalizedIteration normalizedIteration) {
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
