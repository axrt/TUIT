package exception;

import format.BadFromatException;
import io.file.properties.jaxb.DBConnection;

/**
* //Todo: document
 */
public class TUITPropertyBadFormatException extends BadFromatException{

    public TUITPropertyBadFormatException() {

    }

    public TUITPropertyBadFormatException(String message) {
        super(message);
    }
}
