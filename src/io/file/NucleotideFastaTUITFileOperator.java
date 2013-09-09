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
public class NucleotideFastaTUITFileOperator extends TUITFileOperator<NucleotideFasta> {

    @SuppressWarnings("WeakerAccess")
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
        return NucleotideFasta.newInstanceFromFormattedText(record);
    }

    @Override
    public boolean saveResults(NucleotideFasta query, NormalizedIteration<Iteration> normalizedIteration) throws IOException {
        if(normalizedIteration.getPivotalHit()!=null){
            this.bufferedWriter.write(query.getAC()+":\t"+normalizedIteration.getPivotalHit().getFocusNode().getFormattedLineage());
        }else {
            this.bufferedWriter.write(query.getAC()+": "+NucleotideFastaTUITFileOperator.NOT_IDENTIFIED);
        }
        this.bufferedWriter.newLine();
        this.bufferedWriter.flush();
        return false;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * A static factory to create a new instance of a {@link NucleotideFastaTUITFileOperator}
     * @return new {@link NucleotideFastaTUITFileOperator}
     */
    public static NucleotideFastaTUITFileOperator newInstance(){
        return new NucleotideFastaTUITFileOperator();
    }
}
