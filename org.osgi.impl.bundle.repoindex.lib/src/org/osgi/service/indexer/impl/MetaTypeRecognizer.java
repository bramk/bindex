package org.osgi.service.indexer.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.osgi.service.indexer.Resource;
import org.osgi.service.indexer.ResourceRecognizer;
import org.osgi.service.log.LogService;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MetaTypeRecognizer implements ResourceRecognizer {

    private static final String NAMESPACE_1_0 = "http://www.osgi.org/xmlns/metatype/v1.0.0";
    private static final String NAMESPACE_1_1 = "http://www.osgi.org/xmlns/metatype/v1.1.0";
    private static final String NAMESPACE_1_2 = "http://www.osgi.org/xmlns/metatype/v1.2.0";

    private final SAXParserFactory saxParserFactory;

	@SuppressWarnings("unused")
	private final LogService log;

	public MetaTypeRecognizer(LogService log) {
		this.log = log;
        
		saxParserFactory = SAXParserFactory.newInstance();
        saxParserFactory.setNamespaceAware(false);
        saxParserFactory.setValidating(false);
	}

	public Resource recognizeResource(File file, Resource resource) {

		if (resource == null) {
			resource = new FileResource(file);
		}

		if (!resource.getMimeType().equals(MimeType.Xml.toString())) {
			return resource;
		}

		MetaDataNamespaceCollector handler = new MetaDataNamespaceCollector();
		InputStream input = null;
		try {
			input = resource.getStream();
			SAXParser parser = saxParserFactory.newSAXParser();
			parser.parse(input, handler);
		} catch (Exception e) {
			String namespace = handler.getMetaDataNamespace();
			if (namespace != null && (namespace.equals(NAMESPACE_1_0) || namespace.equals(NAMESPACE_1_1) || namespace.equals(NAMESPACE_1_2))) {
				resource.getProperties().put(Resource.MIMETYPE, MimeType.MetaType.toString());
			}
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
		}
		return resource;
	}	
	
    static class MetaDataNamespaceCollector extends DefaultHandler {

        private String m_metaDataNameSpace = "";

        public String getMetaDataNamespace() {
            return m_metaDataNameSpace;
        }

        @Override
        public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
            if (qName.equals("MetaData") || qName.endsWith(":MetaData")) {
                String nsAttributeQName = "xmlns";
                if (qName.endsWith(":MetaData")) {
                    nsAttributeQName = "xmlns" + ":" + qName.split(":")[0];
                }
                for (int i = 0; i < attributes.getLength(); i++) {
                    if (attributes.getQName(i).equals(nsAttributeQName)) {
                        m_metaDataNameSpace = attributes.getValue(i);
                    }
                }
            }
            // first element is expected to have been the MetaData
            // root so we can now terminate processing.
            throw new SAXException("Done");
        }
    }

}
