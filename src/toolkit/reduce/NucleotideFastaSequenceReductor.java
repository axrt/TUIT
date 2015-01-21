package toolkit.reduce;

import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFormatException;

import java.io.*;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

//TODO Document

public class NucleotideFastaSequenceReductor implements SequenceReductor<NucleotideFasta> {
    public static final DecimalFormat DF=new DecimalFormat("0.0000");
    public static final String COUNT_MARKER = "@";

    protected final Map<String, Representative> representativesMap;
    protected double[][] weightMatrix;

    protected NucleotideFastaSequenceReductor() {
        this.representativesMap = new HashMap<>();
    }

    protected void add(final NucleotideFasta nucleotideFasta) {
        if (this.isNew(nucleotideFasta)) {
            this.representativesMap.put(nucleotideFasta.getSequence(), this.createNewRepresentative(nucleotideFasta));
        } else {
            this.representativesMap.get(nucleotideFasta.getSequence()).add(nucleotideFasta);
        }
    }

    protected Representative createNewRepresentative(final NucleotideFasta nucleotideFasta) {
        final Representative representative = new Representative(nucleotideFasta);
        this.representativesMap.put(nucleotideFasta.getSequence(), representative);
        return representative;
    }

    protected List<String> sortRepresentatives() {
        //Sort sequences acceding length
        final List<String> sortedRepresentatives = this.representativesMap.keySet().stream().sorted(
                new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        return o1.length() - o2.length();
                    }
                }
        ).collect(Collectors.toList());
        return sortedRepresentatives;
    }

    protected static double[][] newMatrix(int size) {
        //create a new matrix
        final double[][] matrix = new double[size][];
        for (int k = size - 1; k > -1; k--) {
            matrix[k] = new double[k + 1];
        }
        return matrix;
    }

    protected static double[][] diagonalMatrix(double[][] matrix) {
        Arrays.asList(matrix).parallelStream().forEach(row -> {
            Arrays.setAll(row, p -> 0);
        });
        for (int k = 0; k < matrix.length; k++) {
            matrix[k][k] = 1;
        }
        return matrix;
    }

    protected double[][] fillInMatrix(double[][] matrix, List<String> sortedRepresentatives) {//TODO could be parallelized
        for (int i = 0; i < sortedRepresentatives.size(); i++) {
            for (int j = i + 1; j < sortedRepresentatives.size(); j++) {
                if (sortedRepresentatives.get(i).length() == sortedRepresentatives.get(j).length()) {
                    continue;
                }
                if (sortedRepresentatives.get(j).contains(sortedRepresentatives.get(i))) {
                    matrix[j][i] = 1;
                }
            }
        }
        return matrix;
    }

    protected double[][] normalizeMatrix(double[][] matrix, List<String> sortedRepresentatives) {
        matrix = this.adjustMatrixToReadCount(matrix, sortedRepresentatives);
        final double[] scores = new double[matrix[matrix.length - 1].length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                scores[j] += matrix[i][j];
            }
        }
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] /= scores[j];
            }
        }

        return matrix;
    }

    protected double[][] adjustMatrixToReadCount(double[][] matrix, List<String> sortedRepresentatives) {

        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] *= this.representativesMap.get(sortedRepresentatives.get(j)).groupSize();
            }
        }

        return matrix;
    }

    protected void assignWeights(double[][] matrix, List<String> sortedRepresentatives) {
        final double[] weights = new double[matrix.length];
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                weights[i] += matrix[i][j];
            }
        }
        for (int i = 0; i < matrix.length; i++) {

            this.representativesMap.get(sortedRepresentatives.get(i)).count = weights[i];
        }
    }
    public double weight(int position,List<String> sortedRepresentatives){
        final List<Representative> containers=new ArrayList<>();
        final Map<Representative,Integer> map=new HashMap<>();
        final Pattern pattern=Pattern.compile(sortedRepresentatives.get(position));
        final Representative currentPosition=this.representativesMap.get(sortedRepresentatives.get(position));
            sortedRepresentatives.subList(position+1,sortedRepresentatives.size()).
                    parallelStream().
                    filter(line->{
                        return line.length()!=sortedRepresentatives.get(position).length();
                    }).
                    forEach(line -> {
                        final Matcher matcher = pattern.matcher(line);
                        int count = 0;
                        Representative rep=null;
                        while (matcher.find()) {
                            count++;
                            rep= this.representativesMap.get(line);
                            synchronized (containers) {
                                containers.add(rep);
                            }
                        }
                        if(count>0){
                            synchronized (map) {
                                map.put(rep, count);
                            }
                        }
                    });
        if(containers.size()==0){
            currentPosition.setPrime();
            return 1;
        }

        final double weight=(double)currentPosition.groupSize()/containers.size();
        for(Representative r:containers){
            final Integer denominator=map.get(r);
            r.count+=weight/denominator/r.groupSize();
        }

        return weight;
    }

    protected void collapse() {
        //Sort sequences acceding length
        final List<String> sortedRepresentatives = this.sortRepresentatives();
        //go in a loop from shorter to longer
        final List<Double>weigths=new ArrayList<>();
        for (int i = 0; i < sortedRepresentatives.size(); i++) {
            weigths.add(this.weight(i, sortedRepresentatives));
            if(i%100==0) {
                System.out.print("Complete: " + DF.format((double) i / sortedRepresentatives.size() * 100) + "%");
                System.out.print('\r');
            }
        }
        for (int i = 0; i < sortedRepresentatives.size(); i++) {
            final Representative r = this.representativesMap.get(sortedRepresentatives.get(i));
            r.count += r.groupSize();
        }

    }

    protected boolean isNew(final NucleotideFasta nucleotideFasta) {
        return !this.representativesMap.keySet().contains(nucleotideFasta.getSequence());
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

        final StringBuilder stringBuilder = new StringBuilder();
        final List<Representative> representatives = this.representatives();
        for (Representative r : representatives) {
            if(r.isPrime) {
                stringBuilder.append(r.toString());
                stringBuilder.append('\n');
            }
        }
        return stringBuilder.toString();
    }

    public static NucleotideFastaSequenceReductor fromPath(final Path filePath) throws IOException,
            NucleotideFasta_AC_BadFormatException, NucleotideFasta_BadFormat_Exception, NucleotideFasta_Sequence_BadFormatException {
        try (InputStream inputStream = new FileInputStream(filePath.toFile())) {
            return fromInputstream(inputStream);
        }
    }

    public static NucleotideFastaSequenceReductor fromInputstream(final InputStream inputStream) throws IOException,
            NucleotideFasta_AC_BadFormatException, NucleotideFasta_BadFormat_Exception, NucleotideFasta_Sequence_BadFormatException {
        final List<NucleotideFasta> nucleotideFastas = NucleotideFasta.loadFromText(inputStream);
        final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor = new NucleotideFastaSequenceReductor();
        for (NucleotideFasta nucleotideFasta : nucleotideFastas) {
            nucleotideFastaSequenceReductor.add(nucleotideFasta);
        }
        nucleotideFastaSequenceReductor.collapse();
        return nucleotideFastaSequenceReductor;
    }

    public static class Representative extends NucleotideFasta {

        protected static int AC = 0;
        protected boolean isPrime=false;
        protected final Set<NucleotideFasta> group;
        protected double count = 0;

        protected Representative(NucleotideFasta nucleotideFasta) {
            super(String.valueOf(AC++), nucleotideFasta.getSequence());
            this.group = new HashSet<>();
            this.add(nucleotideFasta);
        }

        protected void setPrime(){
            this.isPrime=true;
        }
        public boolean isPrime(){
            return this.isPrime;
        }

        protected boolean add(final NucleotideFasta nucleotideFasta) {
            return this.group.add(nucleotideFasta);
        }

        protected boolean addAll(final Set<NucleotideFasta> seqs) {
            return this.group.addAll(seqs);
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
            sb.append(DF.format(this.count));
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
