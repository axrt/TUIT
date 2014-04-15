package io.properties.load;

import exception.TUITPropertyBadFormatException;
import io.file.TUITFileOperatorHelper;
import io.properties.jaxb.*;
import logger.Log;
import taxonomy.Ranks;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;
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
 * This class loads and checks the properties XML file, thereby passing the parameters values to the program
 */
public class TUITPropertiesLoader {
    /**
     * A preset of BLASTNParameters to substitue any of those missing from the properties file
     */
    private static final BLASTNParameters DEFAULT_BLASTN_PARAMETERS = new BLASTNParameters();

    static {
        //Expect
        final Expect defaultExpect = new Expect();
        defaultExpect.setValue("10");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setExpect(defaultExpect);
        //EntrezQuery
        final EntrezQuery defaultEntrezQuery = new EntrezQuery();
        defaultEntrezQuery.setValue("");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setEntrezQuery(defaultEntrezQuery);
        //Remote?
        final Remote defaultRemote = new Remote();
        defaultRemote.setDelegate(String.valueOf(false));
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setRemote(defaultRemote);
        //Number of threads to use with local blast
        final NumThreads defaultNumThreads=new NumThreads();
        defaultNumThreads.setValue("1");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setNumThreads(defaultNumThreads);
        //Maximum files in a batch
        final MaxFilesInBatch defaultMaxFilesInBatch = new MaxFilesInBatch();
        defaultMaxFilesInBatch.setValue("5");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setMaxFilesInBatch(defaultMaxFilesInBatch);
        //Database
        final Database defaultDatabase = new Database();
        defaultDatabase.setUse("nt");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase().add(defaultDatabase);
        //Keep BLAST outputs?
        final KeepBLASTOuts defaultKeepBLASTOuts = new KeepBLASTOuts();
        defaultKeepBLASTOuts.setKeep(String.valueOf(false));
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setKeepBLASTOuts(defaultKeepBLASTOuts);
    }

    /**
     * Lazy initialized TUITProperties
     */
    private volatile TUITProperties tuitProperties = null;

    /**
     * Properties file
     */
    private final File propertiesFile;

    /**
     * A lazy initialization thread-safe implementation of a TUITProperties getter
     *
     * @return {@link TUITProperties}, loaded from the xml and checked for consistency.
     * @throws FileNotFoundException
     * @throws JAXBException
     * @throws SAXException
     * @throws TUITPropertyBadFormatException
     */
    public TUITProperties getTuitProperties() throws FileNotFoundException, JAXBException, SAXException, TUITPropertyBadFormatException {
        TUITProperties tp = tuitProperties;
        if (tp == null) {
            synchronized (this) {
                tp = tuitProperties;
            }
            if (tp == null) {
                tuitProperties = tp = checkBLASTNProperties(checkDatabaseProperties(checkSpecificationProperties(loadProperties())));
            }
        }
        return tp;
    }

    /**
     * Constructor from the properties file
     *
     * @param propertiesFile {@link File} that points to the properties file
     */
    private TUITPropertiesLoader(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    /**
     * Checks the database-related part of the properties in order to ensure its consistency.
     * @param tuitProperties {@link TUITProperties}
     * @return the same reference {@link TUITProperties} as the input, and throws {@link TUITPropertyBadFormatException} if the properties fail to pass checks
     * @throws TUITPropertyBadFormatException if the properties fail to pass checks
     */
    private TUITProperties checkDatabaseProperties(TUITProperties tuitProperties) throws TUITPropertyBadFormatException {

        //Check the database connection
        DBConnection dbConnection = tuitProperties.getDBConnection();
        /*
      Is printed out as an example of DBConnection formatting
     */
        String DBCONNECTION_EXAMPLE = "please correct as shown below: \n" +
                "<TUITProperties>\n" +
                "    <DBConnection url=\"localhost (or an ip address)\" login=\"login\" password=\"passwd\"/>\n" +
                "    <BLASTNPath path=...";
        if (dbConnection == null) {
            throw new TUITPropertyBadFormatException("Nothing provides database connection io.properties, " + DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getUrl() == null || dbConnection.getUrl().equals("")) {
            throw new TUITPropertyBadFormatException("No URL provided for the database connection property, " + DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getLogin() == null || dbConnection.getLogin().equals("")) {
            throw new TUITPropertyBadFormatException("No login provided for the database connection property, " + DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getPassword() == null || dbConnection.getPassword().equals("")) {
            throw new TUITPropertyBadFormatException("No password provided for the database connection property, " + DBCONNECTION_EXAMPLE);
        }

        return tuitProperties;

    }
    /**
     * Checks the taxonomic specification-related part of the properties in order to ensure its consistency.
     * @param tuitProperties {@link TUITProperties}
     * @return the same reference {@link TUITProperties} as the input, and throws {@link TUITPropertyBadFormatException} if the properties fail to pass checks
     * @throws TUITPropertyBadFormatException if the properties fail to pass checks
     */
    private TUITProperties checkSpecificationProperties(TUITProperties tuitProperties) throws TUITPropertyBadFormatException {
        //Check specification parameters manual input
        if (tuitProperties.getSpecificationParameters() != null) {
            int i = 0;
            for (SpecificationParameters specificationParameters : tuitProperties.getSpecificationParameters()) {
                i++;
                //Check the rank
                /*
      Is printed out as an example of CutoffSet formatting
     */
                String CUTOFFSET_EXAMPLE = "please correct as shown below: \n" +
                        "<SpecificationParameters>\n" +
                        "        <CutoffSet rank=\"species\">\n" +
                        "            <pIdentCutoff value=\"97.5\"/>\n" +
                        "            <QueryCoverageCutoff value=\"95\"/>\n" +
                        "            <EvalueRatioCutoff value=\"100\"/>\n" +
                        "        </CutoffSet>\n" +
                        "    </SpecificationParameters>";
                if (specificationParameters.getCutoffSet() == null) {
                    throw new TUITPropertyBadFormatException("No rank specified at cutoff set number " + i + ", " + CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        if (specificationParameters.getCutoffSet().getRank() != null) {
                            Ranks.valueOf(specificationParameters.getCutoffSet().getRank());
                        } else {
                            throw new TUITPropertyBadFormatException("No rank property specified at cutoff set number  " + i);
                        }
                    } catch (IllegalArgumentException iae) {
                        throw new TUITPropertyBadFormatException("A bad rank specified at cutoff set number " + i + ", please use one of the following:\n" +
                                Ranks.LIST_RANKS);
                    }
                }
                /*
      Is printed out as an example of BLASTNPath formatting
     */
                String CORRECT_TO_UNSIGNED_DOUBLE= ", please correct to a unsigned double value in [1.0,100.0]";
                String CORRECT_TO_UNSIGNED_DOUBLE_AL = ", please correct to a unsigned double value in [0,0.5]";
                if (specificationParameters.getCutoffSet().getPIdentCutoff() == null || specificationParameters.getCutoffSet().getPIdentCutoff().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No pIdent cutoff specified at cutoff set number " + i + ", " + CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getPIdentCutoff().getValue());
                        if (d < 1 || d > 100) {
                            throw new TUITPropertyBadFormatException("Bad pIdent cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad pIdent cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE);
                    }
                }
                if (specificationParameters.getCutoffSet().getQueryCoverageCutoff() == null || specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No Query coverage cutoff specified at cutoff set number " + i + ", " + CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue());
                        if (d < 1 || d > 100) {
                            throw new TUITPropertyBadFormatException("Bad Query coverage cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad Query coverage cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE);
                    }
                }
                if (specificationParameters.getCutoffSet().getAlpha() == null || specificationParameters.getCutoffSet().getAlpha().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No alpha cutoff specified at cutoff set number " + i + ", " + CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getAlpha().getValue());
                        if (d < 0 || d > 0.5) {
                            throw new TUITPropertyBadFormatException("Bad alpha cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE_AL);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad alpha cutoff at cutoff set number " + i + CORRECT_TO_UNSIGNED_DOUBLE_AL);
                    }
                }
            }
        } else {
            Log.getInstance().log(Level.WARNING,"No specification parameters given, using defaults.");
        }
        return tuitProperties;
    }


    /**
     * Performs a full check of the properties, preloaded form the properties file.
     *
     * @param tuitProperties {@link TUITProperties} loaded from the XML properties file
     * @return {@link TUITProperties} that points to the same object, but has been checked for consistency.
     * @throws TUITPropertyBadFormatException thrown in case any of the properties were badly formatted or make no sense.
     */
    private TUITProperties checkBLASTNProperties(TUITProperties tuitProperties) throws TUITPropertyBadFormatException {

        //Check BLASTN path
        BLASTNPath blastnPath = tuitProperties.getBLASTNPath();
        /*
      Is printed out as an example of BLASTNPath formatting
     */
        String BLASTNPATH_EXAMPLE = "please correct as shown below: \n" +
                "<BLASTNPath path=\"/usr/bin/blastn\"/>";
        if (blastnPath == null) {
            throw new TUITPropertyBadFormatException("Nothing provides a path to blastn, " + BLASTNPATH_EXAMPLE);
        }
        File f;
        if (blastnPath.getPath() != null) {
            f = new File(blastnPath.getPath());
        } else {
            throw new TUITPropertyBadFormatException("No path is given for blastn, " + BLASTNPATH_EXAMPLE);
        }
        if (!f.exists() || f.isDirectory() || !f.canExecute()) {
            throw new TUITPropertyBadFormatException("No executable for blastn found, " + BLASTNPATH_EXAMPLE + "\nand check access rights.");
        }

        //Check tmpdir
        TMPDir tmpDir = tuitProperties.getTMPDir();
        /*
      Is printed out as an example of TMPDir formatting
     */
        String TMPDIRPATH_EXAMPLE = "please correct as shown below: \n" +
                "<TMPDir path=\"/home/user/tmp\"/>";
        if (tmpDir == null) {
            throw new TUITPropertyBadFormatException("Nothing provides a path to a temporary directory for file download and blastn temporary files, "
                    + TMPDIRPATH_EXAMPLE);
        }
        if (tmpDir.getPath() != null) {
            f = new File(tmpDir.getPath());
        } else {
            throw new TUITPropertyBadFormatException("No path is given for the temporary directory, " + TMPDIRPATH_EXAMPLE);
        }
        if (!f.exists() || !f.isDirectory() || !f.canRead() || !f.canWrite()) {
            throw new TUITPropertyBadFormatException("A given temporary directory does not exist, " + TMPDIRPATH_EXAMPLE + "\nand check access rights.");
        }

        //Check BLASTN parameters
        BLASTNParameters blastnParameters = tuitProperties.getBLASTNParameters();
        if (blastnParameters == null) {
            tuitProperties.setBLASTNParameters(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS);
            Log.getInstance().log(Level.WARNING,"No BLASTN parameters loaded, using default.");
        }
        if ((blastnParameters != null ? blastnParameters.getDatabase().size() : 0) == 0) {
            //noinspection ConstantConditions
            blastnParameters.getDatabase().addAll(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase());
            Log.getInstance().log(Level.WARNING,"No BLASTN Database property, using default: nt.");
        } else {
            for (Database database : blastnParameters.getDatabase()) {
                if (database.getUse() == null) {
                    throw new TUITPropertyBadFormatException("Bad parameter for a BLAST database.");
                }
            }
        }
        if (blastnParameters.getExpect() != null && blastnParameters.getExpect().getValue() != null) {
            try {
                Double d = Double.parseDouble(blastnParameters.getExpect().getValue());
                if (d < 0 || d > 10000) {
                    throw new TUITPropertyBadFormatException("Erroneous Expect value, "
                            + "please use reasonable unsigned values.");
                }
            } catch (NumberFormatException ne) {
                throw new TUITPropertyBadFormatException("Erroneous Expect value, please provide an unsigned numeric value.");
            }

        } else {
            //noinspection ConstantConditions,ConstantConditions
            tuitProperties.getBLASTNParameters().setExpect(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect());
            Log.getInstance().log(Level.WARNING,"No BLASTN Expect property, using default: " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect().getValue() + ".");
        }

        if (blastnParameters.getEntrezQuery() == null || blastnParameters.getEntrezQuery().getValue() == null) {
            Log.getInstance().log(Level.WARNING,"No entrez_query provided, setting to default value");
            //noinspection ConstantConditions
            tuitProperties.getBLASTNParameters().setEntrezQuery(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getEntrezQuery());
        }

        if (blastnParameters.getRemote() == null || blastnParameters.getRemote().getDelegate() == null) {
            //noinspection ConstantConditions
            Log.getInstance().log(Level.WARNING,"No BLASTN Remote property, using default: " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote().getDelegate() + ".");
            tuitProperties.getBLASTNParameters().setRemote(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote());
        } else {
            if (!blastnParameters.getRemote().getDelegate().equals("yes") && !blastnParameters.getRemote().getDelegate().equals("no")) {
                throw new TUITPropertyBadFormatException("Erroneous Remote value, please provide \"yes\" or \"no\"");
            }
        }
        if (blastnParameters.getMaxFilesInBatch() != null && blastnParameters.getMaxFilesInBatch().getValue() != null) {
            try {
                Integer i = Integer.parseInt(blastnParameters.getMaxFilesInBatch().getValue());
                if (i <= 0) {
                    throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, "
                            + "please use reasonable unsigned integer values.");
                }
            } catch (NumberFormatException ne) {
                throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, please provide an unsigned integer value.");
            }
        } else {
            //noinspection ConstantConditions
            tuitProperties.getBLASTNParameters().setMaxFilesInBatch(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch());
            Log.getInstance().log(Level.WARNING,"No \"maximum files in a batch property, using default\": " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch().getValue() + ".");
        }
        if (blastnParameters.getNumThreads() != null && blastnParameters.getNumThreads().getValue() != null) {
            try {
                Integer i = Integer.parseInt(blastnParameters.getNumThreads().getValue());
                if (i <= 0) {
                    throw new TUITPropertyBadFormatException("Erroneous \"number of threads\" property, "
                            + "please use reasonable unsigned integer values.");
                }
            } catch (NumberFormatException ne) {
                throw new TUITPropertyBadFormatException("Erroneous \"number of threads\" property, please provide an unsigned integer value.");
            }
        } else {
            //noinspection ConstantConditions
            tuitProperties.getBLASTNParameters().setNumThreads(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getNumThreads());
            Log.getInstance().log(Level.WARNING,"No \"maximum files in a batch property, using default\": " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getNumThreads().getValue() + ".");
        }
        if (blastnParameters.getKeepBLASTOuts() == null || blastnParameters.getKeepBLASTOuts()== null) {
            //noinspection ConstantConditions
            tuitProperties.getBLASTNParameters().setKeepBLASTOuts(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getKeepBLASTOuts());
            Log.getInstance().log(Level.WARNING,"No keep BLAST output property, using default: " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getKeepBLASTOuts() + ".");
            tuitProperties.getBLASTNParameters().setKeepBLASTOuts(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getKeepBLASTOuts());
        } else {
            if (!blastnParameters.getKeepBLASTOuts().getKeep().equals("yes") && !blastnParameters.getKeepBLASTOuts().getKeep().equals("no")) {
                throw new TUITPropertyBadFormatException("Erroneous keep BLAST outputs value, please provide \"yes\" or \"no\"");
            }
        }

        return tuitProperties;
    }

    /**
     * Loads properties from a formatted XML file into a {@link TUITProperties} object
     *
     * @return {@link TUITProperties} loaded from the properties file
     * @throws FileNotFoundException
     * @throws JAXBException
     * @throws SAXException
     */
    private TUITProperties loadProperties() throws FileNotFoundException, JAXBException, SAXException {
        return TUITFileOperatorHelper.catchProperties(new FileInputStream(this.propertiesFile));
    }

    /**
     * A static factory to create a new instance of {@link TUITPropertiesLoader}
     *
     * @param propertiesFile {@link File} that contains the XML formatted TUIT properties.
     * @return a new instance of {@link TUITPropertiesLoader}
     */
    public static TUITPropertiesLoader newInstanceFromFile(File propertiesFile) {
        return new TUITPropertiesLoader(propertiesFile);
    }
}
