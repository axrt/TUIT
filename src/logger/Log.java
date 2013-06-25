package logger;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * This class is a singleton, that incapsulates a Logger and allows to log messages from every part of the TUIT
 */
public enum Log {
    //A singleton instance
    Instance;

    /**
     * Singleton instance accessor
     * @return {@link Log} singleton instance
     */
    public static Log getInstance() {
        return Instance;
    }

    /**
     * Logger
     */
    private Logger logger;
    /**
     * File handler for the logger
     */
    private FileHandler fileHandler;

    /**
     * Private constructor
     */
    private Log() {
        this.logger= Logger.getLogger("tuit.log");
        try {
            this.fileHandler =new FileHandler("tuit.log");
            this.fileHandler.setFormatter(new SimpleFormatter());
        } catch (IOException e) {
           this.logger.severe(e.getMessage());
        }
        this.logger.addHandler(this.fileHandler);
    }

    /**
     * A setter for the level of logging
     * @param level {@link Level} of output
     */
    public void setLevel(Level level) {
        this.fileHandler.setLevel(level);
    }

    /**
     * A getter for the logger
     * @return {@link Logger} logger
     */
    public Logger getLogger(){
        return this.logger;
    }
}
