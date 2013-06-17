package io.file;

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
import java.io.IOException;
import java.io.InputStream;

/**
 * A helper class that allows to load TUIT properties from an XML formatted input
 */
public class TUTFileOperatorHelper {

    private TUTFileOperatorHelper (){
        throw new AssertionError();
    }

    /**
     * Return a {@link TUITProperties} from an {@code InputStream}. Used by
     * {@link TUITProperties} to get the output. Being produced in such a form, it
     * allows to store the schemas in the same package as the
     * {@link TUITProperties}, thereby allowing to make it obscure from the user
     * within the package
     *
     * @param in
     *            :{@link java.io.InputStream } from a URL or other type of connection
     * @return {@link TUITProperties}
     * @throws javax.xml.bind.JAXBException
     * @throws SAXException upon SAX parsing error
     * @throws JAXBException upon unmarshalling
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
                }
                return new InputSource(TUITProperties.class
                        .getResourceAsStream(file));
            }
        });
        InputSource input = new InputSource(in);
        Source source = new SAXSource(xmlreader, input);
        return (TUITProperties) u.unmarshal(source);
    }

}
