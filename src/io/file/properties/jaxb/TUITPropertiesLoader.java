package io.file.properties.jaxb;

import exception.TUITPropertyBadFormatException;
import io.file.TUTFileOperatorHelper;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;

/**
 * //TODO:document
 */
public class TUITPropertiesLoader {

    protected static String DBCONNECTION_EXAMPLE="please correct as shown below: \n"+
            "<TUITProperties>\n" +
            "    <DBConnection url=\"localhost (or an ip address)\" login=\"login\" password=\"passwd\"/>\n" +
            "    <BLASTNPath path=...";

    protected static String BLASTNPATH_EXAMPLE="please correct as shown below: \n"+
            "<BLASTNPath path=\"/usr/bin/blastn\"/>";

    protected static String TMPDIRPATH_EXAMPLE="please correct as shown below: \n"+
            "<TMPDir path=\"/home/user/tmp\"/>";


    protected static BLASTNParameters DEFAULT_BLASTN_PARAMETERS =new BLASTNParameters();
    static {
        //Expect
        Expect defaultExpect=new Expect();
        defaultExpect.setValue("10");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setExpect(defaultExpect);
        //EntrezQuery
        EntrezQuery defaultEntrezQuery=new EntrezQuery();
        defaultEntrezQuery.setValue("");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setEntrezQuery(defaultEntrezQuery);
        //Remote?
        Remote defaultRemote=new Remote();
        defaultRemote.setDeligate(String.valueOf(false));
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setRemote(defaultRemote);
        //Maximum files in a batch
        MaxFilesInBatch defaultMaxFilesInBatch=new MaxFilesInBatch();
        defaultMaxFilesInBatch.setValue("100");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.setMaxFilesInBatch(defaultMaxFilesInBatch);
        //Database
        Database defaultDatabase=new Database();
        defaultDatabase.setUse("nt");
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.database=new ArrayList<Database>();
        TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase().add(defaultDatabase);
    }




    private volatile TUITProperties tuitProperties=null;
    private final File propertiesFile;

    public TUITProperties getTuitProperties() throws FileNotFoundException, JAXBException, SAXException, TUITPropertyBadFormatException {
        TUITProperties tp=tuitProperties;
        if(tp==null){
           synchronized (this){
               tp=tuitProperties;
           } if(tp==null){
                tuitProperties=tp=checkProperties(loadProperties());
            }
        }
        return tp;
    }

    protected TUITPropertiesLoader(File propertiesFile) {
         this.propertiesFile=propertiesFile;
    }

    private TUITProperties checkProperties(TUITProperties tuitProperties) throws TUITPropertyBadFormatException {

        //Check the database connection
        DBConnection dbConnection=tuitProperties.getDBConnection();
        if(dbConnection==null){
            throw new TUITPropertyBadFormatException("Nothing provides database connection properties, " +TUITPropertiesLoader.DBCONNECTION_EXAMPLE) ;
        }
        if(dbConnection.getUrl()==null||dbConnection.getUrl().equals("")){
            throw new TUITPropertyBadFormatException("No URL provided for the database connection property, "+TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }
        if(dbConnection.getLogin()==null||dbConnection.getLogin().equals("")){
            throw new TUITPropertyBadFormatException("No login provided for the database connection property, "+TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }
        if(dbConnection.getPassword()==null||dbConnection.getPassword().equals("")){
            throw new TUITPropertyBadFormatException("No password provided for the database connection property, "+TUITPropertiesLoader.DBCONNECTION_EXAMPLE);
        }

        //Check BLASTN path
        BLASTNPath blastnPath=tuitProperties.getBLASTNPath();
        if(blastnPath==null){
            throw new TUITPropertyBadFormatException("Nothing provides a path to blastn, " +TUITPropertiesLoader.BLASTNPATH_EXAMPLE) ;
        }
        if(!new File(blastnPath.getPath()).exists()){
            throw new TUITPropertyBadFormatException("No executable for blastn found, " +TUITPropertiesLoader.BLASTNPATH_EXAMPLE+"\nand check access rights.") ;
        }

        //Check tmpdir
        TMPDir tmpDir=tuitProperties.getTMPDir();
        if(tmpDir==null){
            throw new TUITPropertyBadFormatException("Nothing provides a path to a temporary directory for file download and blastn temporary files, "
                    +TUITPropertiesLoader.TMPDIRPATH_EXAMPLE) ;
        }
        if(new File(tmpDir.getPath()).exists()){
            throw new TUITPropertyBadFormatException("A given temporary directory does not exist, " +TUITPropertiesLoader.TMPDIRPATH_EXAMPLE+"\nand check access rights.") ;
        }

        //Check BLASTN parameters
        BLASTNParameters blastnParameters=tuitProperties.getBLASTNParameters();
        if(blastnParameters==null){
            tuitProperties.setBLASTNParameters(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS);
            System.out.println("No BLASTN parameters loaded, using default.");
        }
        if(blastnParameters.getDatabase()==null||blastnParameters.getDatabase().size()==0){
           blastnParameters.database=new ArrayList<Database>(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getDatabase());
           System.out.println("No BLASTN Database property, using default: nt.");
        }
        if(blastnParameters.getExpect()!=null){
            try{
                Double d=Double.parseDouble(blastnParameters.getExpect().getValue());
                if(d<0||d>10000){
                    throw new TUITPropertyBadFormatException("Erroneous Expect value, "
                            +"please use reasonable unsigned values.") ;
                }
            }catch (NumberFormatException ne){
                throw new TUITPropertyBadFormatException("Erroneous Expect value, please provide an unsigned numeric value.");
            }

        }else{
            tuitProperties.getBLASTNParameters().setExpect(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect());
            System.out.println("No BLASTN Expect property, using default: "+TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getExpect().getValue()+".");
        }
        if(blastnParameters.getRemote()==null){
            tuitProperties.getBLASTNParameters().setRemote(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote());
            System.out.println("No BLASTN Remote property, using default: "+TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getRemote().getDeligate()+".");
        }else{
            if(!blastnParameters.getRemote().getDeligate().equals("yes")&&!blastnParameters.getRemote().getDeligate().equals("no")){
                throw new TUITPropertyBadFormatException("Erroneous Remote value, please provide \"yes\" or \"no\"");
            }
        }
        if(blastnParameters.getMaxFilesInBatch()!=null){
            try{
                Integer i=Integer.parseInt(blastnParameters.getMaxFilesInBatch().getValue());
                if(i<1){
                    throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, using defaultvalue, "
                            +"please use reasonable unsigned values.") ;
                }
            }catch (NumberFormatException ne){
                throw new TUITPropertyBadFormatException("Erroneous \"maximum files in a batch\" property, using default value, please provide an unsigned numeric value.");
            }
        } else{
            tuitProperties.getBLASTNParameters().setMaxFilesInBatch(TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch());
            System.out.println("No \"maximum files in a batch property, using default\": "+TUITPropertiesLoader.DEFAULT_BLASTN_PARAMETERS.getMaxFilesInBatch().getValue()+".");
        }






        return tuitProperties;
    }

    private TUITProperties loadProperties() throws FileNotFoundException, JAXBException, SAXException {
        return TUTFileOperatorHelper.catchProperties(new FileInputStream(this.propertiesFile));
    }
}
