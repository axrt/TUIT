package helper.greengenes;

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

    //@Test
    public void test(){
       Log.getInstance().setLogName("tuit.log");
       final Path taxonomy= Paths.get("/home/alext/Documents/tuit/greengenes/gg_13_5_taxonomy.txt");
       final Path outputDir=Paths.get("/home/alext/Documents/tuit/greengenes/");
        try {
            final NCBITablesDeployer.TaxonomyFiles taxonomyFiles=GreenGenesDeployer.convertToTaxonomyFiles(taxonomy, outputDir);
            NCBITablesDeployer.fastDeployRamDatabaseFromFiles(taxonomyFiles,outputDir.resolve("ramdb.obj").toFile());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void reformatSequenceDatabaseTest(){
        final Path input=Paths.get("/home/alext/Documents/tuit/greengenes/gg_13_5.fasta");
        final Path output=input.getParent().resolve("gg.fasta");
        try {
            GreenGenesDeployer.reformatSequenceDatabase(input,output);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
