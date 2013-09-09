package logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.*;
/**
 * Taxonomic Unit Identification Tool (TUIT) is a free open source platform independent
 * software for accurate taxonomic classification of nucleotide sequences.
 * Copyright (C) 2013  Alexander Tuzhikov, Alexander Panchin and Valery Shestopalov.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

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
