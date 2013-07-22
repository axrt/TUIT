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

    public static final String NOT_IDENTIFIED="<-not identified->";

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
        if(normalizedIteration.getPivotalHit()!=null){
            this.bufferedWriter.write(query.getAC()+": "+normalizedIteration.getPivotalHit().getFocusNode().getFormattedLineage());
        }else {
            this.bufferedWriter.write(NucleotideFastaTUITFileOperator.NOT_IDENTIFIED);
        }
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    //TODO document
    public static NucleotideFastaTUITFileOperator newInstance(){
        return new NucleotideFastaTUITFileOperator();
    }
}
