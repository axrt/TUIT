package toolkit.greengenes;

import helper.NCBITablesDeployer;
import logger.Log;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by alext on 5/14/14.
 */
public class GreenGenesDeployerTest {

    @Test
    public void test(){
        //This one converts taxonomy to a ram database object
       Log.getInstance().setLogName("tuit.log");
        //Get both input output paths
        //final Path taxonomy= Paths.get("/home/alext/Documents/Research/Ocular/HCE/gg/Gg_13_5_99.taxonomy/gg_13_5_99.gg.tax");
       final Path taxonomy= Paths.get("/home/alext/Documents/Research/Ocular/HCE/ltp/LTPs119_SSU.csv.ggtax"); //the database itself in the form that can be downloaded form the ftp
       final Path outputDir=taxonomy.getParent();
        try {
            //First converts the taxonomy to a compatible fromat,
            final NCBITablesDeployer.TaxonomyFiles taxonomyFiles=GreenGenesDeployer.convertToTaxonomyFiles(taxonomy, outputDir);
            //Then converts to a ramdb object
            NCBITablesDeployer.fastDeployRamDatabaseFromFiles(taxonomyFiles,outputDir.resolve("ramdb.obj").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void reformatSequenceDatabaseTest(){
        final Path input=Paths.get("/home/alext/Documents/Research/Ocular/HCE/gg/gg_13_8_99.fasta");
        final Path output=input.getParent().resolve("gg.fasta");
        try {
            GreenGenesDeployer.reformatSequenceDatabase(input,output, "ggid", "Green Genes Sequence");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
