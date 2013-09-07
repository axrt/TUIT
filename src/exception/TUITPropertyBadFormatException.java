package exception;

import format.BadFormatException;

/**
 * A type of exception that should be thrown any time that an error or a badly formatted property appears within the
 * properties configuration file
 */
public class TUITPropertyBadFormatException extends BadFormatException {
    /**
     * Call tot the super constructor
     */
    public TUITPropertyBadFormatException() {
        super();
    }
    /**
     * Call tot the super constructor with message
     */
    public TUITPropertyBadFormatException(String message) {
        super(message);
    }
}
