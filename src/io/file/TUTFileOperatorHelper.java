package io.file;

import format.EncodedFasta;
import format.fasta.Fasta;
import format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException;
import format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception;
import format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException;
import io.file.properties.jaxb.TUITProperties;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A helper class that allows to load TUIT properties from an XML formatted input
 */
public class TUTFileOperatorHelper {

    private TUTFileOperatorHelper() {
        throw new AssertionError();
    }

    /**
     * Return a {@link TUITProperties} from an {@code InputStream}. Used by
     * {@link TUITProperties} to get the output. Being produced in such a form, it
     * allows to store the schemas in the same package as the
     * {@link TUITProperties}, thereby allowing to make it obscure from the user
     * within the package
     *
     * @param in :{@link java.io.InputStream } from a URL or other type of connection
     * @return {@link TUITProperties}
     * @throws javax.xml.bind.JAXBException
     * @throws SAXException                 upon SAX parsing error
     * @throws JAXBException                upon unmarshalling
     */
    public static TUITProperties catchProperties(InputStream in)
            throws SAXException, JAXBException {
        JAXBContext jc = JAXBContext.newInstance(TUITProperties.class);
        Unmarshaller u = jc.createUnmarshaller();
        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        xmlreader.setFeature("http://xml.org/sax/features/namespaces", true);
        xmlreader.setFeature("http://xml.org/sax/features/namespace-prefixes",
                true);
        xmlreader.setEntityResolver(new EntityResolver() {

            @Override
            public InputSource resolveEntity(String publicId, String systemId)
                    throws SAXException, IOException {
                String file = null;
                if (systemId.contains("properties.dtd")) {
                    file = "properties.dtd";
                } else{
                    throw new SAXException("Wrong name for the schema dtd, please correct to \n" +
                            "<!DOCTYPE TUITProperties PUBLIC \"-//TUIT//TUITProperties/EN\" \"properties.dtd\">");
                }
                return new InputSource(TUITProperties.class
                        .getResourceAsStream(file));
            }
        });
        InputSource input = new InputSource(in);
        Source source = new SAXSource(xmlreader, input);
        return (TUITProperties) u.unmarshal(source);
    }

    /**
     * @param file {@link java.io.File} a file that contains the a list of fasta records (may be reperesented by a single record
     * @return {@link java.util.List < format.EncodedFasta >} of fasta records
     * @throws IOException in case opening and reading the file fails
     * @throws format.fasta.nucleotide.NucleotideFasta_BadFromat_Exception
     *                     in case of a single line format or none at all
     * @throws format.fasta.nucleotide.NucleotideFasta_AC_BadFormatException
     *                     in case the AC is formatted badly
     * @throws format.fasta.nucleotide.NucleotideFasta_Sequence_BadFromatException
     *                     in case it encounters an error within the nucleotide compound
     */
    public static List<EncodedFasta> loadOTURecords(File file) throws IOException,
            NucleotideFasta_BadFromat_Exception, NucleotideFasta_AC_BadFormatException,
            NucleotideFasta_Sequence_BadFromatException {
        //Open file and check whether it is even Fasta at all
        List<EncodedFasta> encodedFastas = null;
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();
            String recordAC = file.getName().split(".")[0];//Get the file name that is supposed to be the AC without ".fasta" extention or whatever the extention is bein used
            String line;
            //Read the first line to see if it is fromatted properly
            line = bufferedReader.readLine().trim();//trim() is needed in case there had been white traces
            if (line.startsWith(Fasta.fastaStart)) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            } else {
                bufferedReader.close();
                throw new NucleotideFasta_BadFromat_Exception("Nucleotide Fasta record: bad format; record does not start with " + Fasta.fastaStart + " identifier ");
            }
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
                stringBuilder.append('\n');
            }
            //Try splitting the file by > if it is possible
            String[] splitter = stringBuilder.toString().split(Fasta.fastaStart);
            stringBuilder = null;
            //Prepare a list of a split size to store the records
            encodedFastas = new ArrayList<EncodedFasta>(splitter.length);
            //Parse every record and then store it in the list
            for (String s : splitter) {
                encodedFastas.add(EncodedFasta.newInstanceFromFromattedText(recordAC, s));
            }
        } finally {
            //Finally return the prepared list of records
            if(bufferedReader!=null){
                bufferedReader.close();
            }
        }
        return encodedFastas;
    }
}
