package toolkit.livingtree;

import format.fasta.Fasta;
import org.junit.Test;
import toolkit.greengenes.GreenGenesDeployer;
import toolkit.silva.ConvertToGGFormat;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by alext on 1/29/15.
 */
public class LivingTreeToGGFormatConverterTest {

    @Test
    public void testConvert() {

        final Path toLTPFile = Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/LTPs119_SSU.csv");
        Path toOutput = toLTPFile.resolveSibling(toLTPFile.toFile().getName() + ".ggtax");
        final Path toFastaFile = Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/LTPs119_SSU.compressed.fasta");

        try {

            final Map<String, String> recodeMap = LivingTreeToGGFormatConverter.convert(toLTPFile, toOutput);
            toOutput = toFastaFile.resolveSibling(toFastaFile.toFile().getName() + ".recode");
            LivingTreeToGGFormatConverter.recodeFasta(toFastaFile, toOutput, recodeMap);
            GreenGenesDeployer.reformatSequenceDatabase(toOutput,toOutput.resolveSibling(toOutput.toFile().getName()+".format"),"ltpid","Life Tree Sequence");

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test
    public void redundantTest(){

        final Path toFastaFile = Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/LTPs119_SSU.compressed.fasta");
        final Set<String> set=new HashSet<>();
        try(BufferedReader bufferedReader=new BufferedReader(new FileReader(toFastaFile.toFile()))){
            String line;
            while((line=bufferedReader.readLine())!=null){
                if(line.startsWith(Fasta.fastaStart)){
                    if(!set.add(line.substring(1).split("\t")[0])){
                        throw new IllegalArgumentException(line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
