package blast;

import BLAST.NCBI.output.Iteration;
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
 * //TODO:Document as soon as works
 * To change this template use File | Settings | File Templates.
 */
public class TUITBLAST_Identifier extends BLAST_Identifier {

    protected int batchSize;

    protected TUITBLAST_Identifier(List<NucleotideFasta> query, List<String> query_IDs, File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator, Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) {
        super(query, query_IDs, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap);
        this.batchSize = batchSize;
    }

    @Override
    public void run() {
        TUITFileOperator<NucleotideFasta> tuitFileOperator = (TUITFileOperator<NucleotideFasta>) this.fileOperator;
        try {
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
                if(remote){
                    System.out.println("Sending BLASTN request..");
                } else{
                    System.out.println("BLASTN started..");
                }

                this.BLAST();

                if(remote){
                    System.out.println("BLASTN results received..");
                } else{
                    System.out.println("BLASTN finished");
                }
                this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
                this.normalizeIterations();
                for (int i = 0; i < this.normalizedIterations.size(); i++) {
                    NormalizedIteration<Iteration> normalizedIteration = (NormalizedIteration<Iteration>) this.normalizedIterations.get(i);
                    normalizedIteration.specify();
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
           //e.printStackTrace();
        }
    }

    public static TUITBLAST_Identifier newInstanceFromFileOperator(
            File tempDir, File executive, String[] parameterList, TUITFileOperator identifierFileOperator,
            Connection connection, Map<Ranks, TUITCutoffSet> cutoffSetMap, int batchSize) throws Exception {
        List<NucleotideFasta> batch = identifierFileOperator.nextBatch(batchSize);
        if (batch != null) {
            TUITBLAST_Identifier tuitblastIdentifier = new TUITBLAST_Identifier(batch, null, tempDir, executive, parameterList, identifierFileOperator, connection, cutoffSetMap, batchSize);
            return tuitblastIdentifier;
        } else {
            throw new Exception("The batch is empty, please check the input file");
        }
    }
}
