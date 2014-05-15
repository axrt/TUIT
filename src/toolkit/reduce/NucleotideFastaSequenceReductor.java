package toolkit.reduce;

import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFormatException;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

//TODO Document

public class NucleotideFastaSequenceReductor {

    public static final String COUNT_MARKER="@";

    protected final Map<String, Representative> representativesMap;
    protected final Set<String> sequenceSet;

    protected NucleotideFastaSequenceReductor() {
        this.representativesMap = new HashMap<>();
        this.sequenceSet = new HashSet<>();
    }

    protected void add(final NucleotideFasta nucleotideFasta) {
        if (this.isNew(nucleotideFasta)) {
            this.representativesMap.put(nucleotideFasta.getSequence(), this.createNewRepresentative(nucleotideFasta));
        } else {
            this.representativesMap.get(nucleotideFasta.getSequence()).add(nucleotideFasta);
        }
        this.sequenceSet.add(nucleotideFasta.getSequence());
    }

    protected Representative createNewRepresentative(final NucleotideFasta nucleotideFasta) {
        final Representative representative = new Representative(nucleotideFasta);
        this.representativesMap.put(nucleotideFasta.getSequence(), representative);
        return representative;
    }

    protected boolean isNew(final NucleotideFasta nucleotideFasta) {
        return !this.sequenceSet.contains(nucleotideFasta.getSequence());
    }

    public List<Representative> representatives() {
        final List<Representative> representatives = new ArrayList<>();
        for (Map.Entry<String, Representative> e : this.representativesMap.entrySet()) {
            representatives.add(e.getValue());
        }
        return representatives;
    }

    @Override
    public String toString() {

        final StringBuilder stringBuilder=new StringBuilder();
        final List<Representative> representatives=this.representatives();
        for(Representative r:representatives){
            stringBuilder.append(r.toString());
            stringBuilder.append('\n');
        }
        return stringBuilder.toString();
    }

    public static NucleotideFastaSequenceReductor fromPath(final Path filePath)throws IOException,
            NucleotideFasta_AC_BadFormatException, NucleotideFasta_BadFormat_Exception, NucleotideFasta_Sequence_BadFormatException{
        try(InputStream inputStream=new FileInputStream(filePath.toFile())){
            return fromInputstream(inputStream);
        }
    }
    public static NucleotideFastaSequenceReductor fromInputstream(final InputStream inputStream) throws IOException,
            NucleotideFasta_AC_BadFormatException, NucleotideFasta_BadFormat_Exception, NucleotideFasta_Sequence_BadFormatException {
        final List<NucleotideFasta> nucleotideFastas = new ArrayList<>();
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            StringBuilder stringBuilder = null;
            while ((line = bufferedReader.readLine()) != null) {
                if (line.startsWith(Fasta.fastaStart)) {
                    if (stringBuilder != null) {
                        nucleotideFastas.add(NucleotideFasta.newInstanceFromFormattedText(stringBuilder.toString()));
                    }
                    stringBuilder = new StringBuilder(line);
                    stringBuilder.append('\n');
                } else{
                    stringBuilder.append(line);
                    stringBuilder.append('\n');
                }
            }
        }
        final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=new NucleotideFastaSequenceReductor();
        for(NucleotideFasta nucleotideFasta:nucleotideFastas){
            nucleotideFastaSequenceReductor.add(nucleotideFasta);
        }

        return nucleotideFastaSequenceReductor;
    }

    public static class Representative extends NucleotideFasta {
        protected static int AC = 0;
        protected final Set<NucleotideFasta> group;

        protected Representative(NucleotideFasta nucleotideFasta) {
            super(String.valueOf(AC++), nucleotideFasta.getSequence());
            this.group = new HashSet<>();
            this.add(nucleotideFasta);
        }

        protected boolean add(final NucleotideFasta nucleotideFasta) {
            return this.group.add(nucleotideFasta);
        }

        protected int groupSize() {
            return this.group.size();
        }
        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder();
            sb.append(Fasta.fastaStart);
            sb.append(this.AC);
            sb.append(COUNT_MARKER);
            sb.append(this.groupSize());
            sb.append('\n');
            int line = 0;
            for (int i = 0; i < this.sequence.length(); i++) {
                sb.append(this.sequence.charAt(i));
                line++;
                if (line % Fasta.fastaLineLenght == 0) {
                    sb.append('\n');
                }
            }
            return new String(sb);
        }
    }
}
