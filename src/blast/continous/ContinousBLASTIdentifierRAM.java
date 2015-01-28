package blast.continous;

import base.buffer.IterationBlockingBuffer;
import blast.blast.BlastHelper;
import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.BLASTIdentifierRAM;
import blast.specification.cutoff.TUITCutoffSet;
import db.ram.RamDb;
import format.fasta.nucleotide.NucleotideFasta;
import logger.Log;
import taxonomy.Ranks;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

/**
 * Created by alext on 1/22/15.
 */
public class ContinousBLASTIdentifierRAM extends BLASTIdentifierRAM {

    protected final IterationBlockingBuffer buffer;
    protected final ContinousBlastN blastn;
    protected final ContinousTUITFileOperator fileOperator;
    protected final ExecutorService executorService;


    protected ContinousBLASTIdentifierRAM(ContinousTUITFileOperator fileOperator, File tempDir, String[] parameterList, Map<Ranks, TUITCutoffSet> cutoffSetMap, boolean cleanup, int batchSize, RamDb ramDb) {
        super(null, tempDir, fileOperator.getExecutable().toFile(), parameterList, null, cutoffSetMap, batchSize, cleanup, ramDb);
        this.buffer = IterationBlockingBuffer.get(batchSize);
        this.fileOperator = fileOperator;
        this.blastn = get(parameterList, this.fileOperator, fileOperator.getExecutable());
        this.blastn.addListener(this.buffer);
        this.executorService = Executors.newSingleThreadExecutor();
    }

    protected static ContinousBlastN get(String[] command, ContinousTUITFileOperator fileOperator, Path executable) {
        String db = "";
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-db")) {
                db = command[i + 1];
                break;
            }
        }
        int numThreads = 1;
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-num_threads")) {
                numThreads = Integer.valueOf(command[i + 1]);
                break;
            }
        }
        double eval = 0.0;
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-evalue")) {
                eval = Double.parseDouble(command[i + 1]);
                break;
            }
        }
        boolean remote = false;
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-remote")) {
                remote = true;
                break;
            }
        }
        Path negativeGiList = null;
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-negative_gilist")) {
                negativeGiList = Paths.get(command[i + 1]);
                break;
            }
        }
        Path giList = null;
        for (int i = 0; i < command.length; i++) {
            if (command[i].equals("-gilist")) {
                giList = Paths.get(command[i + 1]);
                break;
            }
        }

        final Optional<Path> giListOpt;
        if (giList == null) {
            giListOpt = Optional.empty();
        } else {
            giListOpt = Optional.of(giList);
        }
        final Optional<Path> negGiListOpt;
        if (negativeGiList == null) {
            negGiListOpt = Optional.empty();
        } else {
            negGiListOpt = Optional.of(negativeGiList);
        }
        return new ContinousBlastN.ContinousBlastnBuilder(executable, fileOperator.getQuery(), db)
                .num_threads(Optional.of(numThreads))
                .evalue(Optional.of(eval))
                .remote(Optional.of(remote))
                .maxTargetSeqs(Optional.of(1000))
                .negative_gilist(negGiListOpt)
                .gilist(giListOpt).build();
    }

    @Override
    public void run() {
        try {
            if (this.fileOperator instanceof ContinousTUITDataProviderBlastOutput) {
                try (InputStream inputStream = new BufferedInputStream(new FileInputStream(((ContinousTUITDataProviderBlastOutput) this.fileOperator).toBlastOutput().toFile()))) {
                    this.executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                blastn.process(inputStream);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    this.process();
                }
            } else {
                this.executorService.submit(this.blastn);
                this.process();
            }

            System.out.println();
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.executorService.shutdown();
    }

    public void process() throws Exception{
        long blastsToDo = this.fileOperator.checkNumberOfRecords();
        Log.getInstance().log(Level.INFO, "BLASTs to go: " + blastsToDo);
        int blastsDone = 0;
        System.out.print("Done: 0");
        while (true) {
            final Iteration iteration;

            iteration = this.buffer.take();
            if (iteration == IterationBlockingBuffer.DONE) {
                break;
            } else if (iteration.getIterationHits().getHit() != null) {
                blastsDone++;
                final NucleotideFasta nextQuery = this.fileOperator.nextQuery();
                final NormalizedIteration<Iteration> normalizedIteration =
                        NormalizedIteration.<Iteration>newDefaultInstanceFromIteration(nextQuery, iteration, this);
                this.fileOperator.saveTaxonomyLine(this.cutoffSetMap, nextQuery, normalizedIteration.specify());
                System.out.print('\r');
                System.out.print("Done: " + Log.DF4.format((double) blastsDone / blastsToDo * 100) + '%');
            }
        }
    }

    public static ContinousBLASTIdentifierRAM newInstance(ContinousTUITFileOperator fileOperator, File tempDir,
                                                          String[] parameterList, Map<Ranks, TUITCutoffSet> cutoffSetMap, boolean cleanup, int batchSize, RamDb ramDb) {
        return new ContinousBLASTIdentifierRAM(fileOperator, tempDir, parameterList, cutoffSetMap, cleanup, batchSize, ramDb);
    }
}
