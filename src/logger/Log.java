package logger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * Console handler for the logger
     */
    private ConsoleHandler consoleHandler;

    /**
     * Private constructor
     */
    private Log() {
        this.logger= Logger.getLogger("tuit.log");
        this.consoleHandler=new ConsoleHandler();
        this.logger.addHandler(this.consoleHandler);
    }

    /**
     * A setter for the level of logging
     * @param level {@link Level} of output
     */
    public void setLevel(Level level) {
        this.consoleHandler.setLevel(level);
    }

    /**
     * A getter for the logger
     * @return {@link Logger} logger
     */
    public Logger getLogger(){
        return this.logger;
    }
}
