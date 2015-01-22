package fastblast;

import base.buffer.IterationBlockingBuffer;
import blast.ncbi.output.Iteration;
import db.ram.RamDb;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Created by alext on 1/6/15.
 */
public class TaxonomyInspectorTest {


    @Test
    public void test(){

        final Path toFasta= Paths.get("/home/alext/Documents/Research/brain_rnaseq/SRP005169/paired/tophat/nfowleri/1_tophat_out/hits.fasta");
        final Path toRamDBobj=Paths.get("/home/alext/Developer/TUIT/out/artifacts/tuit/ramdb.obj");

        final IterationBlockingBuffer iterations=IterationBlockingBuffer.get(1000);
        final ExecutorService executorService= Executors.newFixedThreadPool(2);
        try {


            final RamDb ramDb=RamDb.loadSelfFromFile(toRamDBobj.toFile());

            final FastBlastN fastBlastN=new FastBlastN.FastBlastNBuilder(Paths.get("/usr/local/bin/blastn"),toFasta,"nt").num_threads(Optional.of(6)).build();

            fastBlastN.addListener(iterations);

            final FileDataHandler fileDataHandler=FileDataHandler.get();


            final TaxonomyInspector taxonomyInspector=new TaxonomyInspector(
                    ramDb, fileDataHandler, iterations,7742,1);

            final Future<TaxonomyInspector.TaxonomyInspectorStat> future = executorService.submit(taxonomyInspector);
            final Future<?> blastFuture=executorService.submit(fastBlastN);
            blastFuture.get();
            iterations.release();
            final TaxonomyInspector.TaxonomyInspectorStat stat=future.get();

            System.out.println("Accepted: " + stat.getAccepted());
            System.out.println("Rejected: "+stat.getRejected());
            executorService.shutdown();


            fileDataHandler.restrict(toFasta,toFasta.resolveSibling("test.fasta"));

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }


    }


}
