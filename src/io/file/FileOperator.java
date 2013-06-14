package io.file;

import format.EncodedFasta;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * This class handles file opening for the  whole system
 */
public enum FileOperator {
    /**
     * Singleton instance
     */
    INSTANCE;

    /**
     * @param file {@link File} a file that contains the a list of fasta records (may be reperesented by a single record
     * @return {@link List<EncodedFasta>} of fasta records
     * @throws IOException in case opening and reading the file fails
     * @throws NucleotideFasta_BadFromat_Exception
     *                     in case of a single line format or none at all
     * @throws NucleotideFasta_AC_BadFormatException
     *                     in case the AC is formatted badly
     * @throws NucleotideFasta_Sequence_BadFromatException
     *                     in case it encounters an error within the nucleotide compound
     */
    public static List<EncodedFasta> recordsFromFile(File file) throws IOException,
            NucleotideFasta_BadFromat_Exception, NucleotideFasta_AC_BadFormatException,
            NucleotideFasta_Sequence_BadFromatException {
        //Open file and check whether it is even Fasta at all
        List<EncodedFasta> encodedFastas = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String recordAC = file.getName().split(".")[0];//Get the file name that is supposed to be the AC without ".fasta" extention or whatever the extention is bein used
            String line;
            //Read the first line to see if it is fromatted properly
            line = bufferedReader.readLine().trim();//trim() is needed in case there had been white traces
            if (line.startsWith(Fasta.fastaStart)) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            } else {
                bufferedReader.close();
                throw new NucleotideFasta_BadFromat_Exception("Nucleotide Fasta record: bad format; record does not start with '>' identifier ");
            }
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            //Try splitting the file by > if it is possible
            String[] splitter = stringBuilder.toString().split(Fasta.fastaStart);
            stringBuilder = null;
            //Prepare a list of a split size to store the records
            encodedFastas = new ArrayList<EncodedFasta>(splitter.length);
            //Parse every record and then store it in the list
            for (String s : splitter) {
                encodedFastas.add(EncodedFasta.newInstanceFromFromattedText(recordAC, s));
            }
        } finally {
            //Finally return the prepared list of records
            bufferedReader.close();
            return encodedFastas;
        }
    }
    //TODO: implement the double locking singleton here

}
