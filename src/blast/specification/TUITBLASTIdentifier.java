package blast.specification;

import blast.ncbi.output.Iteration;
import blast.ncbi.output.NCBI_BLAST_OutputHelper;
import blast.specification.cutoff.TUITCutoffSet;
import blast.normal.iteration.NormalizedIteration;
import format.BadFormatException;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * A {@link BLASTIdentifier} that is specifically adapted for the TUIT algorithm: it
 */
public class TUITBLASTIdentifier extends BLASTIdentifier<NucleotideFasta> {
    /**
     * A size of a batch
     */
    @SuppressWarnings("WeakerAccess")
    protected final int batchSize;
    /**
     * Indicates whether the module should cleanup temp files (such as BLAST output)
     */
    @SuppressWarnings("WeakerAccess")
    protected final boolean cleanup;
    /**
     * The number of the last record specified
     */
    @SuppressWarnings("WeakerAccess")
    protected int progressEdge;
    /**
     * A protected constructor.
     *
     * @param query                  {@link List} a list of query
     *                               fasta-formatted records
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String}[] A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the necessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     * @param batchSize              {@code int} number of fasta records in one batch
     */
    @SuppressWarnings("WeakerAccess")
    protected TUITBLASTIdentifier(List<NucleotideFasta> query, File tempDir, File executive, String[] parameterList,
                                  TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap,
                                  final int batchSize, final boolean cleanup) {
        super(query, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap);
        this.batchSize = batchSize;
        this.cleanup=cleanup;
        this.progressEdge=0;
    }

    /**
     * An overridden run() for the TUIT version of the {@link BLASTIdentifier}. Does the BLASTN and specification in batches, so that the input fasta file does not cause
     * a memory error or a BLAST query error due to too many sequences loaded at a time.
     */
    @Override
    public void run() {
        @SuppressWarnings("unchecked")
        TUITFileOperator<NucleotideFasta> tuitFileOperator = (TUITFileOperator<NucleotideFasta>) this.fileOperator;
        try {
            if (this.blastOutput == null) {
                //See if the remote option is on. If so - do the BLASTN with the NCBI server, otherwise - BLASTN locally
                boolean remote = false;
                for (String s : this.parameterList) {
                    if (s.equals("-remote")) {
                        remote = true;
                        break;
                    }
                }
                if (remote) {
                    Log.getInstance().log(Level.INFO, "Starting job, using NCBI server BLAST");
                } else {
                    Log.getInstance().log(Level.INFO,"Starting job, using local machine BLAST");
                }
                do {
                    if (remote) {
                        Log.getInstance().log(Level.INFO,"Sending BLASTN request..");
                    } else {
                        Log.getInstance().log(Level.INFO,"BLASTN started..");
                    }
                    this.inputFile = new File(this.tempDir.getPath(),
                            "in_" + String.valueOf(this.hashCode() + UUID.randomUUID().toString()));
                    this.outputFile = new File(this.tempDir.getPath(),
                            "out_" + String.valueOf(this.hashCode() + UUID.randomUUID().toString()));
                    this.BLAST();

                    if (remote) {
                        Log.getInstance().log(Level.INFO,"BLASTN results received..");
                    } else {
                        Log.getInstance().log(Level.INFO,"BLASTN finished");
                    }
                    this.progressEdge=0;
                    this.specify();

                } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            } else {
                do {
                    this.specify();
                } while ((this.query = tuitFileOperator.nextBatch(this.batchSize)) != null);
            }
            if(this.cleanup){
                if(this.inputFile.exists()){
                    this.cleanup(this.inputFile);
                }
                if(this.outputFile.exists()){
                    this.cleanup(this.outputFile);
                }
            }
            tuitFileOperator.reset();
            this.BLASTed = true;

        } catch (IOException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (JAXBException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (SAXException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (SQLException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (BadFormatException e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            Log.getInstance().log(Level.SEVERE,e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Utility method that handles the process of taxonomic specification
     *
     * @throws SQLException       in case an error occurs during database communication
     * @throws BadFormatException in case an error in case formatting the {@link blast.ncbi.output.Hit} GI fails
     */
    @SuppressWarnings("WeakerAccess")
    protected void specify() throws SQLException, BadFormatException {
        Log.getInstance().log(Level.FINE,"Specifying the BLAST output.");
        if (this.blastOutput.getBlastOutputIterations().getIteration().size() > 0) {
            this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
            this.normalizeIterations();
            for (NormalizedIteration<Iteration> normalizedIteration : this.normalizedIterations) {
                normalizedIteration.specify();
            }
        } else {
            Log.getInstance().log(Level.SEVERE,"No Iterations were returned, an error might have occurred during BLAST, proceeding with the next query.");
        }
    }

    /**
     * An overridden implementation takes into account that the batch contains a limited number of records and shifts frame by a batch size within the list of
     * iterations.
     */
    @Override
    protected void normalizeIterations() {
        final int allowedProgress=this.progressEdge+this.batchSize-1;
        for(int i=0;this.progressEdge<=allowedProgress&&this.progressEdge<this.blastOutput.getBlastOutputIterations().getIteration().size();this.progressEdge++,i++){
            final Iteration iteration = this.blastOutput.getBlastOutputIterations().getIteration().get(this.progressEdge);
            this.normalizedIterations.add(NormalizedIteration.<Iteration>newDefaultInstanceFromIteration((NucleotideFasta) this.query.get(i), iteration, this));
        }
    }

    /**
     * A static factory that returns a newly created {@link TUITBLASTIdentifier} which had been loaded with the first batch
     * from the input file.
     *
     * @param tempDir                {@link File} - A temporary directory that will be used to dump
     *                               the input and output files, that are used by the ncbi+
     *                               executable
     * @param executive              {@link File} A {@link blast.ncbi.local.exec.NCBI_EX_BLAST_FileOperator} that will
     *                               allow to create an input file as well as catch the blast
     *                               output
     * @param parameterList          {@link String}[] A list of parameters. Should maintain a
     *                               certain order. {"<-command>", "[value]"}, just the way if in
     *                               the blast+ executable input
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the necessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     * @param batchSize              a size of a batch that's being sent to BLASTN at a time
     * @return {@link TUITBLASTIdentifier} ready  to perform the first iteration of BLASTN and specification
     * @throws Exception if the input file read error occurs
     */
    public static TUITBLASTIdentifier newInstanceFromFileOperator(
            File tempDir, File executive, String[] parameterList, TUITFileOperator<NucleotideFasta> identifierFileOperator,
            Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, final int batchSize, final boolean cleanup) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            return new TUITBLASTIdentifier(batch, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, batchSize,cleanup);
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }

    /**
     * A static factory that returns a newly created {@link TUITBLASTIdentifier}, which has already been provided with an XML-formatted blast output
     *
     * @param identifierFileOperator {@link TUITFileOperator} that performs batch-read from the fasta file and saves results
     * @param connection             a connection to the SQL Database that contains a NCBI schema with all the necessary
     *                               taxonomic information
     * @param cutoffSetMap           a {@link Map}, provided by the user and that may differ from the
     *                               default set
     * @param blastOutput            {@link File} that contains an XML BLASTN output
     * @param batchSize              a size of a batch that's being specified at a time
     * @return TUITBLASTIdentifier ready  to perform the first iteration of BLASTN and specification
     * @throws Exception if the input file read error occurs
     */
    public static TUITBLASTIdentifier newInstanceFromBLASTOutput(TUITFileOperator<NucleotideFasta> identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap,
                                                                 File blastOutput, final int batchSize, final boolean cleanup) throws Exception {

        List<NucleotideFasta> batch =identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            TUITBLASTIdentifier tuitblastIdentifier = new TUITBLASTIdentifier(batch, new File(""), null, null, identifierFileOperator, connection, cutoffSetMap, batchSize,cleanup);
            tuitblastIdentifier.setBlastOutput(NCBI_BLAST_OutputHelper
                    .catchBLASTOutput(identifierFileOperator
                            .readOutputXML(blastOutput)));
            return tuitblastIdentifier;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
