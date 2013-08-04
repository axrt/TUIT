package exception;

/**
 * An instance that holds info about a specific error with the fasta formatted record
 */
public class FastaInputFileException extends Exception {
    public FastaInputFileException() {
    }

    public FastaInputFileException(String message) {
        super(message);
    }
}
