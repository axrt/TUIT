package logger;

import sun.net.www.protocol.http.logging.HttpLogFormatter;

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
     * Console handler for the logger
     */
    private ConsoleHandler consoleHandler;
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

                    if (record.getThrown() != null) {
                        try {
                            StringWriter sw = new StringWriter();
                            PrintWriter pw = new PrintWriter(sw);
                            record.getThrown().printStackTrace(pw);
                            pw.close();
                            sb.append(sw.toString());
                        }finally {

                        }

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
        this.logger.addHandler(consoleHandler);
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
