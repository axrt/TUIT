package fastblast;

import base.buffer.IterationBlockingBuffer;
import db.ram.RamDb;
import format.fasta.Fasta;
import org.apache.commons.cli.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Created by alext on 1/6/15.
 */
public class Main {

    public static final String RAM_DATABASE = "tax";
    public static final String INPUT = "i";
    public static final String OUTPUT = "o";
    public static final String BLASTN = "b";
    public static final String BLAST_DATABASE = "db";
    public static final String THREADS = "t";
    public static final String RESTRICTED_TAXID = "x";

    public static void main(String[] args) {

        final CommandLineParser parser = new GnuParser();
        final Options options = new Options();

        Option option = new Option(RAM_DATABASE, "taxonomy", true, "A full path to an instance of RamDB object.");
        options.addOption(option);
        option.setRequired(true);
        option = new Option(INPUT, "input", true, "A full path to an input file in FASTA format.");
        options.addOption(option);
        option.setRequired(true);
        option = new Option(OUTPUT, "output", true, "A full path to an output (FASTA formated).");
        options.addOption(option);
        option = new Option(BLASTN, "blast", true, "Explicit path to blastn.");
        options.addOption(option);
        option = new Option(BLAST_DATABASE, "blastdb", true, "Blast database. Example: \"nt\"");
        options.addOption(option);
        option.setRequired(true);
        option = new Option(THREADS, "threads", true, "Number of threads to use. Defaults to 1");
        options.addOption(option);
        option.setRequired(true);
        option = new Option(RESTRICTED_TAXID, "taxid", true, "Taxid of the node that needs to be excluded (as well as all successors).");
        options.addOption(option);
        option.setRequired(true);

        //Checking parameters
        final String blastn;
        final int numberOfThreads;
        final Path output;
        final Path toFasta;
        final Path toRamDBobj;
        final String blastDatabase;
        final int restrictedTaxid;
        try {
            final CommandLine commandLine = parser.parse(options, args, true);
            System.out.print("Command parameters: ");
            System.out.println(Arrays.asList(args).stream().collect(Collectors.joining(" ")));
            //Mandatory
            toFasta = Paths.get(commandLine.getOptionValue(INPUT));
            toRamDBobj = Paths.get(commandLine.getOptionValue(RAM_DATABASE));
            blastDatabase = commandLine.getOptionValue(BLAST_DATABASE);
            restrictedTaxid=Integer.parseInt(commandLine.getOptionValue(RESTRICTED_TAXID));
            //Additional
            if (commandLine.hasOption(THREADS)) {
                numberOfThreads = Integer.parseInt(commandLine.getOptionValue(THREADS));
            } else {
                numberOfThreads = 1;
            }
            if (commandLine.hasOption(BLASTN)) {
                blastn = commandLine.getOptionValue(BLASTN);
            } else {
                blastn = "blastn";
            }
            if (commandLine.hasOption(OUTPUT)) {
                output = Paths.get(commandLine.getOptionValue(OUTPUT));
            } else {
                output = toFasta.resolveSibling(toFasta.getFileName() + ".rest");
            }



            final IterationBlockingBuffer iterations = IterationBlockingBuffer.get(1000);
            final ExecutorService executorService = Executors.newFixedThreadPool(2);
            try {

                final long count = checkNumberOfRecords(toFasta);

                final RamDb ramDb = RamDb.loadSelfFromFile(toRamDBobj.toFile());

                final FastBlastN fastBlastN = new FastBlastN.FastBlastNBuilder(Paths.get(blastn), toFasta, blastDatabase).num_threads(Optional.of(numberOfThreads)).build();

                fastBlastN.addListener(iterations);

                final FileDataHandler fileDataHandler = FileDataHandler.get();


                final TaxonomyInspector taxonomyInspector = new TaxonomyInspector(
                        ramDb, fileDataHandler, iterations, restrictedTaxid,count);

                final Future<TaxonomyInspector.TaxonomyInspectorStat> future = executorService.submit(taxonomyInspector);
                final Future<?> blastFuture = executorService.submit(fastBlastN);
                blastFuture.get();
                iterations.release();
                final TaxonomyInspector.TaxonomyInspectorStat stat = future.get();

                System.out.println("Accepted: " + stat.getAccepted());
                System.out.println("Rejected: " + stat.getRejected());

                executorService.shutdown();

                fileDataHandler.restrict(toFasta, output);

            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public static long checkNumberOfRecords(Path toFastaFile) throws IOException{
        final long count;
        try(BufferedReader bufferedReader=new BufferedReader(new FileReader(toFastaFile.toFile()))){
            count=bufferedReader.lines().filter(line->line.startsWith(Fasta.fastaStart)).count();
        }
        return count;
    }
}
