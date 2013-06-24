package blast;

import BLAST.NCBI.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import format.BadFromatException;
import format.fasta.nucleotide.NucleotideFasta;
import helper.Ranks;
import io.file.TUITFileOperator;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link BLAST_Identifier} that is specifically adapted for the TUIT algorithm: it
 */
public class TUITBLAST_Identifier extends BLAST_Identifier {

    protected int batchSize;

    /**
     * A protected constructor.
     *
     * @param query                  {@link List<NucleotideFasta  >} a list of query
     *                               fasta-formatted records
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String[]} A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map<Ranks, TUITCutoffSet>}, provided by the user and that may differ from the
     *                               default set
     * @param batchSize              {@code int} number of fasta records in one batch
     */
    protected TUITBLAST_Identifier(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) {
        super(query, null, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap);
        this.batchSize = batchSize;
    }

    /**
     * An overridden run() for the TUIT version of the {@link BLAST_Identifier}. Does the BLASTN and specification in batches, so that the input fasta file does not cause
     * a memory error or a BLAST query error due to too many sequences loaded at a time.
     */
    @Override
    public void run() {
        TUITFileOperator<NucleotideFasta> tuitFileOperator = (TUITFileOperator<NucleotideFasta>) this.fileOperator;
        try {
            //See if the remote option is on. If so - do the BLASTN with the NCBI server, otherwise - BLASTN locally
            boolean remote = false;
            for (String s : this.parameterList) {
                if (s.equals("-remote")) {
                    remote = true;
                    break;
                }
            }
            if (remote) {
                System.out.println("Starting job, using NCBI server BLAST");
            } else {
                System.out.println("Starting job, using local machine BLAST");
            }
            do {
                if (remote) {
                    System.out.println("Sending BLASTN request..");
                } else {
                    System.out.println("BLASTN started..");
                }

                this.BLAST();

                if (remote) {
                    System.out.println("BLASTN results received..");
                } else {
                    System.out.println("BLASTN finished");
                }
                if (this.blastOutput.getBlastOutputIterations().getIteration().size() > 0) {
                    this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
                    this.normalizeIterations();
                    for (int i = 0; i < this.normalizedIterations.size(); i++) {
                        NormalizedIteration<Iteration> normalizedIteration = (NormalizedIteration<Iteration>) this.normalizedIterations.get(i);
                        normalizedIteration.specify();
                    }
                } else {
                    System.err.println("No Iterations were returned, an error might have occurred during BLAST, proceeding with the next query.");
                }

            } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            tuitFileOperator.reset();
            this.BLASTed = true;

        } catch (IOException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (InterruptedException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (JAXBException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (SAXException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (BadFromatException e) {
            System.err.println(e.getMessage());
            //e.printStackTrace();
        } catch (Exception e) {
            System.err.println(e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * A static factory that returns a newly created {@link TUITBLAST_Identifier} which had been loaded with the first batch
     * from the input file.
     *
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String[]} A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map<Ranks, TUITCutoffSet>}, provided by the user and that may differ from the
     *                               default set
     * @return                       {@link TUITBLAST_Identifier} ready  to perform the first iteraton of BLASTN and specification
     * @throws Exception if the input file read error occurs
     */
    public static TUITBLAST_Identifier newInstanceFromFileOperator(
            File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
            Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            TUITBLAST_Identifier tuitblastIdentifier = new TUITBLAST_Identifier(batch, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, batchSize);
            return tuitblastIdentifier;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
