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
        TUITFileOperator<NucleotideFasta> tuitFileOperator=(TUITFileOperator<NucleotideFasta>)this.fileOperator;
        try {

            do{
                this.BLAST();
                this.normalizedIterations = new ArrayList<NormalizedIteration<Iteration>>(this.blastOutput.getBlastOutputIterations().getIteration().size());
                this.normalizeIterations();
                for (int i=0;i<this.normalizedIterations.size();i++) {
                    NormalizedIteration<Iteration> normalizedIteration=(NormalizedIteration<Iteration>)this.normalizedIterations.get(i);
                    normalizedIteration.specify();
                }

            }while ((this.query=tuitFileOperator.nextBatch(this.batchSize))!=null);
            tuitFileOperator.reset();
            this.BLASTed = true;

        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (JAXBException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (BadFromatException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (Exception e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
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
