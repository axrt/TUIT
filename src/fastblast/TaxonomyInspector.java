package fastblast;

import base.buffer.IterationBlockingBuffer;


import blast.output.Hit;
import blast.output.Iteration;
import db.ram.RamDb;
import util.BlastOutputUtil;

import java.util.concurrent.Callable;


/**
 * Created by alext on 1/6/15.
 */
public class TaxonomyInspector implements Callable<TaxonomyInspector.TaxonomyInspectorStat> {

    protected final RamDb taxonomy;
    protected final long count;
    protected long counter;
    protected final DataHandler<Iteration> handler;
    protected final IterationBlockingBuffer iterations;
    protected final Integer rejectTaxid;

    protected int accepted = 0;
    protected int rejected = 0;

    public TaxonomyInspector(RamDb taxonomy, DataHandler<Iteration> handler, IterationBlockingBuffer iterations, int rejectTaxid,long count) {
        this.taxonomy = taxonomy;
        this.handler = handler;
        this.iterations = iterations;
        this.rejectTaxid = rejectTaxid;
        this.count=count;
        this.counter=0;
    }

    @Override
    public TaxonomyInspectorStat call() throws Exception {
        System.out.println("Sequences to go: "+this.count);
        System.out.printf("%s%.3f%s", "Done: ",(double) this.counter / count * 100, "%");
        outer:
        while (!iterations.isDone()) {
            counter++;
            System.out.print("\r");
            System.out.printf("%s%.3f%s", "Done: ",(double) this.counter / count * 100, "%");
            //System.out.println("size "+this.iterations.size());
            boolean found=false;
            final blast.output.Iteration iteration = iterations.take();
            //System.out.println("control point 1");
            if (iteration == IterationBlockingBuffer.DONE) {
                //System.out.println("control point 2");
                break outer;
            } else if (iteration.getIterationHits().getHit() != null && !iteration.getIterationHits().getHit().isEmpty()) {
                //System.out.println("control point 3");
                final Hit top= iteration.getIterationHits().getHit().get(0);
                final int gi=Integer.valueOf(BlastOutputUtil.extractGIFromHitID(top.getHitId()));
                Integer taxid=this.taxonomy.getTaxIdByGi(gi);
                Integer parentTaxid=taxid;
                int counter=0;
                inner:
                do{
                    //System.out.println("taxonomy roll");
                    taxid=parentTaxid;
                    if(taxid==this.rejectTaxid){
                        found=true;
                        break inner;
                    }
                    parentTaxid=this.taxonomy.getParetTaxIdByTaxId(taxid);
                    //System.out.println(parentTaxid);
                    counter++;
                    //System.out.println("end of taxonomy roll");
                }while(taxid!=parentTaxid&&counter<25);
                //System.out.println("inner broken");
                if(!found) {
                    //System.out.println("handling");
                    this.handler.handle(iteration);
                    //System.out.println("handled!");
                    this.accepted++;
                    //System.out.println(this.iterations.size());
                }else{
                    found = false;
                    this.rejected++;
                }
            }

        }

        //System.out.println("exited outer");

        return new TaxonomyInspectorStat(this.accepted,this.rejected);
    }

    protected static TaxonomyInspector get(RamDb taxonomy, DataHandler<Iteration> handler, IterationBlockingBuffer iterations, int rejectTaxid, long count) {
        return new TaxonomyInspector(taxonomy, handler, iterations,rejectTaxid, count);
    }

    public static class TaxonomyInspectorStat {

        protected final int accepted;
        protected final int rejected;

        protected TaxonomyInspectorStat(int accepted, int rejected) {
            this.accepted = accepted;
            this.rejected = rejected;
        }

        public int getAccepted() {
            return accepted;
        }

        public int getRejected() {
            return rejected;
        }
    }

}
