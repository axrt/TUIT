package blast.continous;

import base.buffer.IterationBlockingBuffer;
import blast.ncbi.output.BlastOutput;
import gblaster.blast.GBlast;
import logger.Log;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Created by alext on 1/22/15.
 */
public class ContinousBlastN extends GBlast {


    protected ContinousBlastN(BlastBuilder builder) {
        super(builder);
    }

    @Override
    public Optional<BlastOutput> call() throws Exception {
        Log.getInstance().log(Level.FINE,"Continous BLASTN started.");
        final Optional<BlastOutput> output=super.call();
        this.notifyListeners(new BlastEvent<>(IterationBlockingBuffer.DONE));
        return output;
    }

    @Override
    public void process(InputStream inputStream) throws Exception {
        super.process(inputStream);
        this.notifyListeners(new BlastEvent<>(IterationBlockingBuffer.DONE));
    }

    public static class ContinousBlastnBuilder extends GBlastNBuilder{

        public ContinousBlastnBuilder(Path pathToBlast, Path queryFile, String database) {
            super(pathToBlast, queryFile, database);
        }

        @Override
        public ContinousBlastN build() {
            return new ContinousBlastN(this);
        }

        @Override
        public ContinousBlastnBuilder num_threads(Optional<Integer> value) {
            super.num_threads(value);
            return this;
        }

        @Override
        public ContinousBlastnBuilder evalue(Optional<Double> value) {
            super.evalue(value);
            return this;
        }

        @Override
        public ContinousBlastnBuilder remote(Optional<Boolean> value) {
            super.remote(value);
            return this;
        }

        @Override
        public ContinousBlastnBuilder maxTargetSeqs(Optional<Integer> value) {
            super.maxTargetSeqs(value);
            return this;
        }

        @Override
        public ContinousBlastnBuilder negative_gilist(Optional<Path> value) {
            super.negative_gilist(value);
            return this;
        }

        @Override
        public ContinousBlastnBuilder gilist(Optional<Path> value) {
            super.gilist(value);
            return this;
        }
    }
}
