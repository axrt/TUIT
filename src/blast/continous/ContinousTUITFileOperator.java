package blast.continous;

import blast.blast.BlastHelper;
import blast.ncbi.output.Iteration;
import blast.normal.iteration.NormalizedIteration;
import blast.specification.cutoff.TUITCutoffSet;
import fastblast.Main;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import io.file.TUITFileOperatorHelper;
import taxonomy.Ranks;

import java.io.*;
import java.nio.file.Path;
import java.util.Map;

/**
 * Created by alext on 1/22/15.
 */
public class ContinousTUITFileOperator implements ContinousTUITDataHandler,ContinousTUITDataProvider,AutoCloseable{

    protected final Path executable;
    protected final Path query;
    protected final Path output;
    protected final Path toBlastOut;
    protected String line;
    protected final BufferedReader queryReader;
    protected final TUITFileOperatorHelper.OutputFormat format;

    protected ContinousTUITFileOperator(Path executable, Path query, Path output, TUITFileOperatorHelper.OutputFormat format) throws IOException{
        this.executable=executable;
        this.query = query;
        this.output = output;
        this.toBlastOut=this.output.resolveSibling(this.output.toFile().getName()+".blastn");
        this.queryReader=new BufferedReader(new FileReader(this.query.toFile()));
        this.format=format;
    }

    public Path getExecutable() {
        return executable;
    }

    public Path getQuery() {
        return query;
    }

    public Path getOutput() {
        return output;
    }

    @Override
    public void close() throws Exception {
        this.queryReader.close();
    }

    @Override
    public long checkNumberOfRecords() throws Exception {
        return Main.checkNumberOfRecords(this.query);
    }

    @Override
    public NucleotideFasta nextQuery() throws Exception {
        final StringBuilder stringBuilder = new StringBuilder();
        int fastaCounter = 0;
        if (this.line != null) {
            stringBuilder.append(line);
            stringBuilder.append('\n');
            fastaCounter++;
        }
        while ((this.line = this.queryReader.readLine()) != null) {
            if (line.startsWith(Fasta.fastaStart)) {
                if (fastaCounter >= 1) {
                    break;
                }
                fastaCounter++;
            }
            stringBuilder.append(line);
            stringBuilder.append('\n');
        }
        return NucleotideFasta.newInstanceFromFormattedText(stringBuilder.toString());
    }

    @Override
    public boolean saveTaxonomyLine(Map<Ranks, TUITCutoffSet> cutoffSetMap,
                                    NucleotideFasta query, NormalizedIteration<blast.ncbi.output.Iteration> normalizedIteration) throws Exception {
        try(BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(this.output.toFile(),true))){
           if(this.format== TUITFileOperatorHelper.OutputFormat.TUIT){
               bufferedWriter.write(TUITFileOperatorHelper.OutputFormat.defaultTUITFormatter.format(query.getAC().split("\t")[0], normalizedIteration));
               bufferedWriter.newLine();
           }
            if(this.format== TUITFileOperatorHelper.OutputFormat.RDP_FIXRANK){
                bufferedWriter.write(TUITFileOperatorHelper.OutputFormat.defaultFixRankRDPFormatter(cutoffSetMap).format(query.getAC().split("\t")[0], normalizedIteration));
                bufferedWriter.newLine();
            }
        }
        return true;
    }

    @Override
    public boolean saveIteration(Iteration iteration) throws Exception {
        try(BufferedWriter bufferedWriter=new BufferedWriter(new FileWriter(this.toBlastOut.toFile(),true))){
            bufferedWriter.write(BlastHelper.marshallIterationToString(iteration));
            bufferedWriter.newLine();
        }
        return true;
    }

    public static ContinousTUITFileOperator get(Path executable, Path query, Path output,TUITFileOperatorHelper.OutputFormat format) throws IOException{
        return new ContinousTUITFileOperator(executable, query, output,format);
    }
}
