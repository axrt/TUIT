package fastblast;

import blast.blast.BlastHelper;
import gblaster.blast.GBlast;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Created by alext on 1/6/15.
 */
public class FastBlastN  extends GBlast{


    protected FastBlastN(BlastBuilder builder) {
        super(builder);
    }

    protected static class FastBlastNBuilder extends GBlastNBuilder{


        public FastBlastNBuilder(Path pathToBlast, Path queryFile, String database) {
            super(pathToBlast, queryFile, database);
        }

        @Override
        public FastBlastN build() {
            super.maxTargetSeqs(Optional.of(1));
            return new FastBlastN(this);
        }

        @Override
        public <BLASTN_TASK_VALS> FastBlastNBuilder task(Optional<BLASTN_TASK_VALS> value) {
            super.task(value);
            return this;
        }

        @Override
        public FastBlastNBuilder gapextend(Optional<Integer> value) {
            super.gapextend(value);
            return this;
        }

        @Override
        public FastBlastNBuilder penalty(Optional<Integer> value) {
            super.penalty(value);
            return this;
        }

        @Override
        public FastBlastNBuilder reward(Optional<Integer> value) {
            super.reward(value);
            return this;
        }

        @Override
        public FastBlastNBuilder use_index(Optional<Boolean> value) {
            super.use_index(value);
            return this;
        }

        @Override
        public FastBlastNBuilder index_name(Optional<String> value) {
            super.index_name(value);
            return this;
        }

        @Override
        public FastBlastNBuilder subject_loc(Optional<String> value) {
             super.subject_loc(value);
            return this;
        }

        @Override
        public FastBlastNBuilder outfmt(Optional<BlastHelper.OUTFMT_VALS> value, Optional<BlastHelper.OUTFMT_VALS.CUSTOM_FMT_VALS>... custom_fmt_vals) {
            super.outfmt(value, custom_fmt_vals);
            return this;
        }

        @Override
        public FastBlastNBuilder show_gis() {
             super.show_gis();
            return this;
        }

        @Override
        public FastBlastNBuilder query_loc(Optional<String> value) {
             super.query_loc(value);
            return this;
        }

        @Override
        public FastBlastNBuilder strand(Optional<BlastHelper.STRAND_VALS> value) {
             super.strand(value);
            return this;
        }

        @Override
        public FastBlastNBuilder out(Optional<Path> value) {
             super.out(value);
            return this;
        }

        @Override
        public FastBlastNBuilder evalue(Optional<Double> value) {
             super.evalue(value);
            return this;
        }

        @Override
        public FastBlastNBuilder word_size(Optional<Integer> value) {
             super.word_size(value);
            return this;
        }

        @Override
        public FastBlastNBuilder gapopen(Optional<Integer> value) {
             super.gapopen(value);
            return this;
        }

        @Override
        public FastBlastNBuilder maxTargetSeqs(Optional<Integer> value) {
            throw new IllegalArgumentException("Only 1 target makes sense for FastBlast analysis!");
        }

        @Override
        public FastBlastNBuilder subject(Optional<Path> value) {
             super.subject(value);
            return this;
        }

        @Override
        public FastBlastNBuilder num_threads(Optional<Integer> value) {
             super.num_threads(value);
            return this;
        }
    }
}
