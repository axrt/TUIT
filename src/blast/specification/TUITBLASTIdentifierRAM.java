package blast.specification;

import blast.ncbi.output.Iteration;
import blast.ncbi.output.NCBI_BLAST_OutputHelper;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import db.ram.RamDb;
import format.BadFormatException;
import format.fasta.nucleotide.NucleotideFasta;
import io.file.TUITFileOperator;
import logger.Log;
import org.xml.sax.SAXException;
import taxonomy.Ranks;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/**
 * Created by alext on 2/13/14.
 */
//TODO: Document
public class TUITBLASTIdentifierRAM extends BLASTIdentifierRAM {


    protected TUITBLASTIdentifierRAM(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList,
                                     TUITFileOperator identifierFileOperator, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize, boolean cleanup, RamDb ramDb) {
        super(query, tempDir, executive, parameterList, identifierFileOperator, cutoffSetMap, batchSize, cleanup, ramDb);
    }

    /**
     * An overridden implementation takes into account that the batch contains a limited number of records and shifts frame by a batch size within the list of
     * iterations.
     */
    @Override
    protected void normalizeIterations() {
        final int allowedProgress = this.progressEdge + this.batchSize - 1;
        for (int i = 0; this.progressEdge <= allowedProgress && this.progressEdge < this.blastOutput.getBlastOutputIterations().getIteration().size(); this.progressEdge++, i++) {
            final Iteration iteration = this.blastOutput.getBlastOutputIterations().getIteration().get(this.progressEdge);
            this.normalizedIterations.add(NormalizedIteration.<Iteration>newDefaultInstanceFromIteration((NucleotideFasta) this.query.get(i), iteration, this));
        }
    }

    /**
     * An overridden run() for the TUIT version of the {@link BLASTIdentifier}. Does the BLASTN and specification in batches, so that the input fasta file does not cause
     * a memory error or a BLAST query error due to too many sequences loaded at a time.
     */
    @Override
    public void run() {
        @SuppressWarnings("unchecked")
        TUITFileOperator<NucleotideFasta> tuitFileOperator = (TUITFileOperator<NucleotideFasta>) this.fileOperator;
        Log.getInstance().log(Level.INFO, "Using "+this.batchSize+" sequences per batch.");
        try {
            if (this.blastOutput == null) {
                //See if the remote option is on. If so - do the BLASTN with the NCBI server, otherwise - BLASTN locally
                boolean remote = false;
                for (String s : this.parameterList) {
                    if (s.equals("-remote")) {
                        remote = true;        //TODO: correct
                        break;
                    }
                }
                if (remote) {
                    Log.getInstance().log(Level.INFO, "Starting job, using NCBI server BLAST");
                } else {
                    Log.getInstance().log(Level.INFO, "Starting job, using local machine BLAST on "+this.parameterList[this.parameterList.length-1]+" threads.");
                }
                do {
                    if (remote) {
                        Log.getInstance().log(Level.INFO, "Sending BLASTN request..");
                    } else {
                        Log.getInstance().log(Level.INFO, "BLASTN started..");
                    }
                    this.inputFile = new File(this.tempDir.getPath(),
                            "in_" + String.valueOf(this.hashCode() + UUID.randomUUID().toString()));
                    this.outputFile = new File(this.tempDir.getPath(),
                            "out_" + String.valueOf(this.hashCode() + UUID.randomUUID().toString()));
                    this.BLAST();

                    if (remote) {
                        Log.getInstance().log(Level.INFO, "BLASTN results received..");
                    } else {
                        Log.getInstance().log(Level.INFO, "BLASTN finished");
                    }
                    this.progressEdge = 0;
                    this.specify();
                    this.cleanup();
                } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            } else {
                do {
                    this.specify();
                    this.cleanup();
                } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            }

            tuitFileOperator.reset();
            this.BLASTed = true;

        } catch (IOException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (JAXBException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (SAXException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (BadFormatException e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.getInstance().log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        }
    }

    protected void cleanup(){
        if (this.cleanup) {
            Log.getInstance().log(Level.INFO, "Cleaning up temporary files...");
            if (this.inputFile.exists()) {
                this.cleanup(this.inputFile);
            }
            if (this.outputFile.exists()) {
                this.cleanup(this.outputFile);
            }
        }
    }

    public static TUITBLASTIdentifierRAM newInstanceFromFileOperator(File tempDir, File executive, String[] parameterList,
                                                                     TUITFileOperator identifierFileOperator, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize, boolean cleanup, RamDb ramDb) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            return new TUITBLASTIdentifierRAM(batch, tempDir, executive, parameterList, identifierFileOperator, cutoffSetMap, batchSize, cleanup, ramDb);
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }

    public static TUITBLASTIdentifierRAM newInstanceFromBLASTOutput(TUITFileOperator<NucleotideFasta> identifierFileOperator, Map<Ranks, TUITCutoffSet> cutoffSetMap,
                                                                     File blastOutput, final int batchSize, final boolean cleanup, final RamDb ramDb) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            final TUITBLASTIdentifierRAM tuitblastIdentifierRAM = new TUITBLASTIdentifierRAM(batch, new File(""), null, null, identifierFileOperator, cutoffSetMap, batchSize, cleanup, ramDb);
            tuitblastIdentifierRAM.setBlastOutput(NCBI_BLAST_OutputHelper
                    .catchBLASTOutput(identifierFileOperator
                            .readOutputXML(blastOutput)));
            return tuitblastIdentifierRAM;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
