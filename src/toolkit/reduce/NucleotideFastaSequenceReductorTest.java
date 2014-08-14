package toolkit.reduce;

import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFormatException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 5/15/14.
 */
public class NucleotideFastaSequenceReductorTest {

    @Test
    public void test(){
        final Path fastaFile= Paths.get("/home/alext/Documents/tuit/final testing/distro 1.0.5/test.fasta");
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            FileUtils.writeStringToFile(fastaFile.getParent().resolve("reductor.fasta").toFile(),nucleotideFastaSequenceReductor.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }

}
