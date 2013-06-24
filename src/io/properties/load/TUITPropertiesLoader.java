package io.properties.load;

import exception.TUITPropertyBadFormatException;
import io.properties.jaxb.*;
import taxonomy.Ranks;
import io.file.TUTFileOperatorHelper;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * This class loads and checks the properties XML file, thereby passing the parameters values to the program
 */
public class TUITPropertiesLoader {
    /**
     * Is printed out as an example of DBConnection formatting
     */
    private static String DBCONNECTION_EXAMPLE = "please correct as shown below: \n" +
            "<TUITProperties>\n" +
            "    <DBConnection url=\"localhost (or an ip address)\" login=\"login\" password=\"passwd\"/>\n" +
            "    <BLASTNPath path=...";
    /**
     * Is printed out as an example of BLASTNPath formatting
     */
    private static String BLASTNPATH_EXAMPLE = "please correct as shown below: \n" +
            "<BLASTNPath path=\"/usr/bin/blastn\"/>";
    /**
     * Is printed out as an example of TMPDir formatting
     */
    private static String TMPDIRPATH_EXAMPLE = "please correct as shown below: \n" +
            "<TMPDir path=\"/home/user/tmp\"/>";
    /**
     * Is printed out as an example of CutoffSet formatting
     */
    private static String CUTOFFSET_EXAMPLE = "please correct as shown below: \n" +
            "<SpecificationParameters>\n" +
            "        <CutoffSet rank=\"species\">\n" +
            "            <pIdentCutoff value=\"97.5\"/>\n" +
            "            <QueryCoverageCutoff value=\"95\"/>\n" +
            "            <EvalueRatioCutoff value=\"100\"/>\n" +
            "        </CutoffSet>\n" +
            "    </SpecificationParameters>";
    /**
     * Is printed out as an example of BLASTNPath formatting
     */
    private static String CORRECT_TO_UNSIGNED_DOUBLE = ", please correct to a unsigned double value in [1.0,100.0]";
    /**
     * A preset of BLASTNParameters to substitue any of those missing from the properties file
     */
    private static BLASTNParameters DEFAULT_BLASTN_PARAMETERS = new BLASTNParameters();

    static {
        //Expect
        Expect defaultExpect = new Expect();
        defaultExpect.setValue("10");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setExpect(defaultExpect);
        //EntrezQuery
        EntrezQuery defaultEntrezQuery = new EntrezQuery();
        defaultEntrezQuery.setValue("");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setEntrezQuery(defaultEntrezQuery);
        //Remote?
        Remote defaultRemote = new Remote();
        defaultRemote.setDeligate(String.valueOf(false));
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setRemote(defaultRemote);
        //Maximum files in a batch
        MaxFilesInBatch defaultMaxFilesInBatch = new MaxFilesInBatch();
        defaultMaxFilesInBatch.setValue("100");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setMaxFilesInBatch(defaultMaxFilesInBatch);
        //Database
        Database defaultDatabase = new Database();
        defaultDatabase.setUse("nt");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase().add(defaultDatabase);
    }

    /**
     * Lazy inicialized TUITProperties
     */
    private volatile TUITProperties tuitProperties = null;

    /**
     * Properties file
     */
    private final File propertiesFile;

    /**
     * A lazy inicialization thread-safe implementation of a TUITProperties getter
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
                tuitProperties = tp = checkProperties(loadProperties());
            }
        }
        return tp;
    }

    /**
     * Constructor from the properties file
     * @param propertiesFile
     */
    private TUITPropertiesLoader(File propertiesFile) {
        this.propertiesFile = propertiesFile;
    }

    /**
     * Performs a full check of the properties, praloaded form the properties file.
     * @param tuitProperties {@link TUITProperties} loaded from the XML properties file
     * @return {@link TUITProperties} that points to the same object, but has been checked for consistency.
     * @throws TUITPropertyBadFormatException thrown in case any of the properties were badly formatted or make no sense.
     */
    private TUITProperties checkProperties(TUITProperties tuitProperties) throws TUITPropertyBadFormatException {

        //Check the database connection
        DBConnection dbConnection = tuitProperties.getDBConnection();
        if (dbConnection == null) {
            throw new TUITPropertyBadFormatException("Nothing provides database connection io.properties, " + TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getUrl() == null || dbConnection.getUrl().equals("")) {
            throw new TUITPropertyBadFormatException("No URL provided for the database connection property, " + TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getLogin() == null || dbConnection.getLogin().equals("")) {
            throw new TUITPropertyBadFormatException("No login provided for the database connection property, " + TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }
        if (dbConnection.getPassword() == null || dbConnection.getPassword().equals("")) {
            throw new TUITPropertyBadFormatException("No password provided for the database connection property, " + TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }

        //Check BLASTN path
        BLASTNPath blastnPath = tuitProperties.getBLASTNPath();
        if (blastnPath == null) {
            throw new TUITPropertyBadFormatException("Nothing provides a path to blastn, " + TUITPropertiesLoader.BLASTNPATH_EXAMPLE);
        }
        File f = null;
        if (blastnPath.getPath() != null) {
            f = new File(blastnPath.getPath());
        } else {
            throw new TUITPropertyBadFormatException("No path is given for blastn, " + TUITPropertiesLoader.BLASTNPATH_EXAMPLE);
        }
        if (!f.exists() || f.isDirectory() || !f.canExecute()) {
            throw new TUITPropertyBadFormatException("No executable for blastn found, " + TUITPropertiesLoader.BLASTNPATH_EXAMPLE + "\nand check access rights.");
        }

        //Check tmpdir
        TMPDir tmpDir = tuitProperties.getTMPDir();
        if (tmpDir == null) {
            throw new TUITPropertyBadFormatException("Nothing provides a path to a temporary directory for file download and blastn temporary files, "
                    + TUITPropertiesLoader.TMPDIRPATH_EXAMPLE);
        }
        if (tmpDir.getPath() != null) {
            f = new File(tmpDir.getPath());
        } else {
            throw new TUITPropertyBadFormatException("No path is given for the temporary directory, " + TUITPropertiesLoader.TMPDIRPATH_EXAMPLE);
        }
        if (!f.exists() || !f.isDirectory() || !f.canRead() || !f.canWrite()) {
            throw new TUITPropertyBadFormatException("A given temporary directory does not exist, " + TUITPropertiesLoader.TMPDIRPATH_EXAMPLE + "\nand check access rights.");
        }

        //Check BLASTN parameters
        BLASTNParameters blastnParameters = tuitProperties.getBLASTNParameters();
        if (blastnParameters == null) {
            tuitProperties.setBLASTNParameters(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS);
            System.out.println("No BLASTN parameters loaded, using default.");
        }
        if (blastnParameters.getDatabase().size() == 0) {
            blastnParameters.getDatabase().addAll(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase());
            System.out.println("No BLASTN Database property, using default: nt.");
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
            tuitProperties.getBLASTNParameters().setExpect(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect());
            System.out.println("No BLASTN Expect property, using default: " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect().getValue() + ".");
        }

        if (blastnParameters.getEntrezQuery() == null || blastnParameters.getEntrezQuery().getValue() == null) {
            System.out.println("No entrez_query provided, setting to default value");
            tuitProperties.getBLASTNParameters().setEntrezQuery(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getEntrezQuery());
        }

        if (blastnParameters.getRemote() == null || blastnParameters.getRemote().getDeligate() == null) {
            tuitProperties.getBLASTNParameters().setRemote(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote());
            System.out.println("No BLASTN Remote property, using default: " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote().getDeligate() + ".");
            tuitProperties.getBLASTNParameters().setRemote(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote());
        } else {
            if (!blastnParameters.getRemote().getDeligate().equals("yes") && !blastnParameters.getRemote().getDeligate().equals("no")) {
                throw new TUITPropertyBadFormatException("Erroneous Remote value, please provide \"yes\" or \"no\"");
            }
        }
        if (blastnParameters.getMaxFilesInBatch() != null && blastnParameters.getMaxFilesInBatch().getValue() != null) {
            try {
                Integer i = Integer.parseInt(blastnParameters.getMaxFilesInBatch().getValue());
                if (i < 0) {
                    throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, "
                            + "please use reasonable unsigned integer values.");
                }
            } catch (NumberFormatException ne) {
                throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, please provide an unsigned integer value.");
            }
        } else {
            tuitProperties.getBLASTNParameters().setMaxFilesInBatch(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch());
            System.out.println("No \"maximum files in a batch property, using default\": " + TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch().getValue() + ".");
        }

        //Check specification parameters manual input
        if (tuitProperties.getSpecificationParameters() != null) {
            int i = 0;
            for (SpecificationParameters specificationParameters : tuitProperties.getSpecificationParameters()) {
                i++;
                //Check the rank
                if (specificationParameters.getCutoffSet() == null) {
                    throw new TUITPropertyBadFormatException("No rank specified at cutoff set number " + i + ", " + TUITPropertiesLoader.CUTOFFSET_EXAMPLE);
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
                if (specificationParameters.getCutoffSet().getPIdentCutoff() == null || specificationParameters.getCutoffSet().getPIdentCutoff().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No pIdent cutoff specified at cutoff set number " + i + ", " + TUITPropertiesLoader.CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getPIdentCutoff().getValue());
                        if (d < 1 || d > 100) {
                            throw new TUITPropertyBadFormatException("Bad pIdent cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad pIdent cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                    }
                }
                if (specificationParameters.getCutoffSet().getQueryCoverageCutoff() == null || specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No Query coverage cutoff specified at cutoff set number " + i + ", " + TUITPropertiesLoader.CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue());
                        if (d < 1 || d > 100) {
                            throw new TUITPropertyBadFormatException("Bad Query coverage cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad Query coverage cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                    }
                }
                if (specificationParameters.getCutoffSet().getEvalueRatioCutoff() == null || specificationParameters.getCutoffSet().getEvalueRatioCutoff().getValue() == null) {
                    throw new TUITPropertyBadFormatException("No E-value ratio cutoff specified at cutoff set number " + i + ", " + TUITPropertiesLoader.CUTOFFSET_EXAMPLE);
                } else {
                    try {
                        Double d = Double.parseDouble(specificationParameters.getCutoffSet().getQueryCoverageCutoff().getValue());
                        if (d < 1 || d > 100) {
                            throw new TUITPropertyBadFormatException("Bad E-value ratio cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                        }
                    } catch (NumberFormatException ne) {
                        throw new TUITPropertyBadFormatException("Bad E-value ratio cutoff at cutoff set number " + i + TUITPropertiesLoader.CORRECT_TO_UNSIGNED_DOUBLE);
                    }
                }
            }
        } else {
            System.out.println("No specification parameters given, using defaults.");
        }
        return tuitProperties;
    }

    /**
     * Loads properties from a formatted XML file into a {@link TUITProperties} object
     * @return {@link TUITProperties} loaded from the properties file
     * @throws FileNotFoundException
     * @throws JAXBException
     * @throws SAXException
     */
    private TUITProperties loadProperties() throws FileNotFoundException, JAXBException, SAXException {
        return TUTFileOperatorHelper.catchProperties(new FileInputStream(this.propertiesFile));
    }

    /**
     * A static factory to create a new instance of {@link TUITPropertiesLoader}
     * @param propertiesFile {@link File} that contains the XML formatted TUIT properties.
     * @return a new instance of {@link TUITPropertiesLoader}
     */
    public static TUITPropertiesLoader newInstanceFromFile(File propertiesFile) {
        return new TUITPropertiesLoader(propertiesFile);
    }
}