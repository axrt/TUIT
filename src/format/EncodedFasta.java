package format;

import format.fasta.nucleotide.NculeotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;

/**
 * A representation of a sepcial case of a nucleotide fasta record that can also have a patient barcode for further
 * processing analysis
 */
public class EncodedFasta extends NculeotideFasta {

    /**
     * A barcode that is used to identify a patient in a uniform way
     */
    protected final String barcode;

    /**
     *
     * @param AC {@link String} record AC (most likely to be the name of the file
     * @param barcode {@link String} barcode that is used to identify a patient in a uniform way
     * @param sequence  {@link String} of the record
     * @throws NucleotideFasta_AC_BadFormatException in case the AC is formatted badly
     * @throws NucleotideFasta_Sequence_BadFromatException in case it encounters an error within the nucleotide compound
     */
    protected EncodedFasta(String AC, String barcode, String sequence) throws NucleotideFasta_AC_BadFormatException, NucleotideFasta_Sequence_BadFromatException {
        super(AC, sequence);
        this.barcode = barcode;
    }


    /**
     * @param fastaRecord  {@link String} that contains a fasta formatted record
     * @param recordAC {@link String} record AC (most likely to be the name of the file
     * @return a new {@link EncodedFasta} from the parameters given
     * @throws NucleotideFasta_BadFromat_Exception  in case of a single line format or none at all
     * @throws NucleotideFasta_AC_BadFormatException in case the AC is formatted badly
     * @throws NucleotideFasta_Sequence_BadFromatException in case it encounters an error within the nucleotide compound
     */
    public static EncodedFasta newInstanceFromFromattedText(String fastaRecord, String recordAC) throws NucleotideFasta_BadFromat_Exception,
            NucleotideFasta_AC_BadFormatException,
            NucleotideFasta_Sequence_BadFromatException {

        // Get the first row and check whether it is good for an AC
        String[] splitter = fastaRecord.split("\n");
        if (splitter.length < 2) {
            throw new NucleotideFasta_BadFromat_Exception(
                    "Nucleotide Fasta record: bad format; represented by a single line.");
        } else {
            // Prepare a StringBuilder of a proper (at least close to proper)
            // size
            StringBuilder sb = new StringBuilder(splitter.length
                    * splitter[1].length());
            for (int i = 1; i < splitter.length; i++) {
                // Get rid of all the spaces on the fly
                // Concatenate the sequence
                sb.append(splitter[i].replaceAll(" ", ""));
            }

            //Now, further ensure that the AC has no more then just barcode at the beginning
            splitter=splitter[0].split("\t");

            return new EncodedFasta(recordAC,splitter[0],sb.toString());
        }
    }
}