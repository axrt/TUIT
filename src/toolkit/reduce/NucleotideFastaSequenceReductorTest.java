package toolkit.reduce;

import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFormat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFormatException;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by alext on 5/15/14.
 */
public class NucleotideFastaSequenceReductorTest {
    private static final Path fastaFile= Paths.get("/home/alext/Developer/TUIT/testing/res/reduction.fasta");
    @Test
    public void test(){

        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            System.out.println("Final scores:");
            System.out.println(nucleotideFastaSequenceReductor.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testSort(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            System.out.println("Unsorted");
            System.out.println(nucleotideFastaSequenceReductor.toString());
            final List<String> sorted=nucleotideFastaSequenceReductor.sortRepresentatives();
            System.out.println("Sorted");
            sorted.stream().forEach(System.out::println);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void testNewMatrix(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            final double[][]matrix=NucleotideFastaSequenceReductor.newMatrix(3);
            System.out.println("Matrix structure:");
            for(int i=0;i<matrix.length;i++){
                for(int j=0;j<matrix[i].length;j++){
                    System.out.print("[ ]");
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void testZeroMatrix(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            double[][]matrix=NucleotideFastaSequenceReductor.newMatrix(3);
            matrix=NucleotideFastaSequenceReductor.diagonalMatrix(matrix);
            System.out.println("Diagonal matrix:");
            for(int i=0;i<matrix.length;i++){
                for(int j=0;j<matrix[i].length;j++){
                    System.out.print(""+'['+matrix[i][j]+']');
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void testFillinMatrix(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            List<String>sortReps=nucleotideFastaSequenceReductor.sortRepresentatives();
            sortReps.stream().forEach(System.out::println);
            double[][]matrix=NucleotideFastaSequenceReductor.newMatrix(sortReps.size());
            matrix=NucleotideFastaSequenceReductor.diagonalMatrix(matrix);
            matrix=nucleotideFastaSequenceReductor.fillInMatrix(matrix,sortReps);
            System.out.println("Matrix filled in:");
            for(int i=0;i<matrix.length;i++){
                for(int j=0;j<matrix[i].length;j++){
                    System.out.print(""+'['+matrix[i][j]+']');
                }
                System.out.println();
            }
            System.out.println("Collapsed:");
            sortReps=nucleotideFastaSequenceReductor.sortRepresentatives();
            sortReps.stream().forEach(System.out::println);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }
    //@Test
    public void simple(){
        System.out.println("ATGCATGCATGCATGCATGC".contains("ATGCATGCATGC"));
    }

    //@Test
    public void normalizeMatrix(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            List<String>sortReps=nucleotideFastaSequenceReductor.sortRepresentatives();
            double[][]matrix=NucleotideFastaSequenceReductor.newMatrix(sortReps.size());
            matrix=NucleotideFastaSequenceReductor.diagonalMatrix(matrix);
            matrix=nucleotideFastaSequenceReductor.fillInMatrix(matrix,sortReps);
            matrix=nucleotideFastaSequenceReductor.normalizeMatrix(matrix,sortReps);
            System.out.println("\n\nMatrix normalized:");
            for(int i=0;i<matrix.length;i++){
                for(int j=0;j<matrix[i].length;j++){
                    System.out.print(""+'['+matrix[i][j]+']');
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }

    //@Test
    public void adjustMatrixToReadCount(){
        try {
            final NucleotideFastaSequenceReductor nucleotideFastaSequenceReductor=NucleotideFastaSequenceReductor.fromPath(fastaFile);
            List<String>sortReps=nucleotideFastaSequenceReductor.sortRepresentatives();
            double[][]matrix=NucleotideFastaSequenceReductor.newMatrix(sortReps.size());
            matrix=NucleotideFastaSequenceReductor.diagonalMatrix(matrix);
            matrix=nucleotideFastaSequenceReductor.fillInMatrix(matrix, sortReps);
            matrix=nucleotideFastaSequenceReductor.normalizeMatrix(matrix,sortReps);

            System.out.println("\n\nMatrix adjusted:");
            for(int i=0;i<matrix.length;i++){
                for(int j=0;j<matrix[i].length;j++){
                    System.out.print(""+'['+matrix[i][j]+']');
                }
                System.out.println();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_AC_BadFormatException e) {
            e.printStackTrace();
        } catch (NucleotideFasta_BadFormat_Exception e) {
            e.printStackTrace();
        } catch (NucleotideFasta_Sequence_BadFormatException e) {
            e.printStackTrace();
        }
    }
}
