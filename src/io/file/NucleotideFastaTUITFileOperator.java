package io.file;

import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import exception.FastaInputFileException;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link TUITFileOperator} for the {@link NucleotideFasta}s.
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
    protected boolean inputFileFormattingIsFine() throws FastaInputFileException, IOException {

        BufferedReader bf=new BufferedReader(new FileReader(this.inputFile));
        Set<String> fastaACs=new HashSet<String>();
        String nextLine;
        int fastaNumber=0;
        while ((nextLine=bf.readLine())!=null){
             if(line.startsWith(Fasta.fastaStart)){
                 fastaNumber++;
                 if(!fastaACs.add(nextLine)){
                      throw new FastaInputFileException("A non-unique name at "+fastaNumber+" record.");
                 }
             }
        }
        return true;
    }

    @Override
    protected NucleotideFasta newFastaFromRecord(String record) throws Exception {
        return NucleotideFasta.newInstanceFromFromattedText(record);
    }

    @Override
    public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws IOException {
        this.bufferedWriter.write(query.getAC()+": "+normalizedIteration.getPivotalHit().getFocusNode().getFormattedLineage());
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
