package blast.specification;

import BLAST.NCBI.output.Iteration;
import blast.specification.cutoff.TUITCutoffSet;
import blast.normal.iteration.NormalizedIteration;
import format.BadFromatException;
import format.fasta.nucleotide.NucleotideFasta;
import logger.Log;
import taxonomy.Ranks;
import io.file.TUITFileOperator;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A {@link BLASTIdentifier} that is specifically adapted for the TUIT algorithm: it
 */
public class TUITBLASTIdentifier extends BLASTIdentifier {

    protected int batchSize;

    /**
     * A protected constructor.
     *
     * @param query                  {@link List} a list of query
     *                               fasta-formatted records
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String}[] A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     * @param restrictedNames  a {@link String}[] list that contains names that should be restricted (such as "unclassified" etc.)
     * @param batchSize              {@code int} number of fasta records in one batch
     */
    protected TUITBLASTIdentifier(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap,String[]restrictedNames, int batchSize) {
        super(query, null, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, restrictedNames);
        this.batchSize = batchSize;
    }

    /**
     * An overridden run() for the TUIT version of the {@link BLASTIdentifier}. Does the BLASTN and specification in batches, so that the input fasta file does not cause
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
                Log.getInstance().getLogger().info("Starting job, using NCBI server BLAST");
            } else {
                Log.getInstance().getLogger().info("Starting job, using local machine BLAST");
            }
            do {
                if (remote) {
                    Log.getInstance().getLogger().info("Sending BLASTN request..");
                } else {
                    Log.getInstance().getLogger().info("BLASTN started..");
                }
                StringBuilder stringBuilder=new StringBuilder();
                for (String s:this.parameterList){
                    stringBuilder.append(s);
                    stringBuilder.append(" ");
                }
                Log.getInstance().getLogger().info("BLASTN command: "+stringBuilder.toString());
                this.BLAST();

                if (remote) {
                    Log.getInstance().getLogger().info("BLASTN results received..");
                } else {
                    Log.getInstance().getLogger().info("BLASTN finished");
                }
                if (this.blastOutput.getBlastOutputIterations().getIteration().size() > 0) {
                    this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
                    this.normalizeIterations();
                    for (int i = 0; i < this.normalizedIterations.size(); i++) {
                        NormalizedIteration<Iteration> normalizedIteration = (NormalizedIteration<Iteration>) this.normalizedIterations.get(i);
                        normalizedIteration.specify();
                    }
                } else {
                    Log.getInstance().getLogger().severe("No Iterations were returned, an error might have occurred during BLAST, proceeding with the next query.");
                }

            } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            tuitFileOperator.reset();
            this.BLASTed = true;

        } catch (IOException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (InterruptedException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (JAXBException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (SAXException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (SQLException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (BadFromatException e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        } catch (Exception e) {
            Log.getInstance().getLogger().severe(e.getMessage());
        }
    }

    /**
     * A static factory that returns a newly created {@link TUITBLASTIdentifier} which had been loaded with the first batch
     * from the input file.
     *
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link BLAST.NCBI.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String}[] A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the nessessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     * @param entrez_query that contains "not smth" formatted restriction names (like "unclassified, etc")
     * @return                       {@link TUITBLASTIdentifier} ready  to perform the first iteraton of BLASTN and specification
     * @throws Exception if the input file read error occurs
     */
    public static TUITBLASTIdentifier newInstanceFromFileOperator(
            File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
            Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap,String entrez_query, int batchSize) throws Exception {
        String[]split=null;
        split=entrez_query.split("not ");

        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            TUITBLASTIdentifier tuitblastIdentifier = new TUITBLASTIdentifier(batch, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, Arrays.copyOfRange(split, 1, split.length), batchSize);
            return tuitblastIdentifier;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
