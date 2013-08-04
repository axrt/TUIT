package logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.*;

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
    @SuppressWarnings("SameReturnValue")
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
    private Handler fileHandler;
    /**
     * Console handler for the logger
     */
    private Handler consoleHandler;
    /**
     * Private constructor
     */
    private Log() {
    }

    /**
     * Allows to set a name for the logger
     * @param logName {@link String} name for the logger, presumably - input file name-derived
     */
    public void setLogName(String logName){
        this.logger= Logger.getLogger(logName);
        this.logger.setUseParentHandlers(false);
        try {
            Formatter formatter=new Formatter() {
                @Override
                public String format(LogRecord record) {
                    StringBuilder sb = new StringBuilder();

                    sb.append(new Date(record.getMillis()))
                            .append(" ")
                            .append(record.getLevel().getLocalizedName())
                            .append(": ")
                            .append(formatMessage(record))
                            .append(System.getProperty("line.separator"));

                    //noinspection ThrowableResultOfMethodCallIgnored
                    if (record.getThrown() != null) {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                        //noinspection ThrowableResultOfMethodCallIgnored
                        record.getThrown().printStackTrace(pw);
                            pw.close();
                            sb.append(sw.toString());
                    }
                    return sb.toString();
                }
            };
            this.consoleHandler=new ConsoleHandler();
            this.consoleHandler.setFormatter(formatter);
            this.fileHandler =new FileHandler(logName);
            this.fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            this.logger.severe(e.getMessage());
        }
        this.logger.addHandler(this.consoleHandler);
        this.logger.addHandler(this.fileHandler);

    }
    /**
     * A setter for the level of logging
     * @param level {@link Level} of output
     */
    public void setLevel(Level level) {
        this.logger.setLevel(level);
        this.consoleHandler.setLevel(level);
        this.fileHandler.setLevel(level);
    }

    /**
     * Logs a specific message to a file and console
     * @param level  {@link Level} of logging
     * @param message {@link String} message to log
     */
    public void log(Level level, String message){
        this.logger.log(level,message);
    }
}
