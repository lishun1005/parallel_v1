package com.rsclouds.common.utils;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Common utilities for easily parsing XML without duplicating logic.
 *
 * @author Scott Battaglia
 * @version $Revision: 11729 $ $Date: 2007-09-26 14:22:30 -0400 (Tue, 26 Sep 2007) $
 * @since 3.0
 */
public final class XmlUtils {

    /**
     * Static instance of Commons Logging.
     */
    private final static Log LOG = LogFactory.getLog(XmlUtils.class);

    /**
     * Get an instance of an XML reader from the XMLReaderFactory.
     *
     * @return the XMLReader.
     */
    public static XMLReader getXmlReader() {
        try {
            return XMLReaderFactory.createXMLReader();
        } catch (final SAXException e) {
            throw new RuntimeException("Unable to create XMLReader", e);
        }
    }

    /**
     * Retrieve the text for a group of elements. Each text element is an entry
     * in a list.
     * <p>This method is currently optimized for the use case of two elements in a list.
     *
     * @param xmlAsString the xml response
     * @param element     the element to look for
     * @return the list of text from the elements.
     */
    public static List<String> getTextForElements(final String xmlAsString,
                                          final String element) {
        final List<String> elements = new ArrayList<String>(2);
        final XMLReader reader = getXmlReader();

        final DefaultHandler handler = new DefaultHandler() {

            private boolean foundElement = false;

            private StringBuilder buffer = new StringBuilder();

            public void startElement(final String uri, final String localName,
                                     final String qName, final Attributes attributes)
                    throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = true;
                }
            }

            public void endElement(final String uri, final String localName,
                                   final String qName) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = false;
                    elements.add(this.buffer.toString());
                    this.buffer = new StringBuilder();
                }
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                if (this.foundElement) {
                    this.buffer.append(ch, start, length);
                }
            }
        };

        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        try {
            reader.parse(new InputSource(new StringReader(xmlAsString)));
        } catch (final Exception e) {
            LOG.error(e, e);
            return null;
        }

        return elements;
    }

    /**
     * Retrieve the text for a specific element (when we know there is only
     * one).
     *
     * @param xmlAsString the xml response
     * @param element     the element to look for
     * @return the text value of the element.
     */
    public static String getTextForElement(final String xmlAsString,
                                           final String element) {
        final XMLReader reader = getXmlReader();
        final StringBuilder builder = new StringBuilder();

        final DefaultHandler handler = new DefaultHandler() {

            private boolean foundElement = false;

            public void startElement(final String uri, final String localName,
                                     final String qName, final Attributes attributes)
                    throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = true;
                }
            }

            public void endElement(final String uri, final String localName,
                                   final String qName) throws SAXException {
                if (localName.equals(element)) {
                    this.foundElement = false;
                }
            }

            public void characters(char[] ch, int start, int length)
                    throws SAXException {
                if (this.foundElement) {
                    builder.append(ch, start, length);
                }
            }
        };

        reader.setContentHandler(handler);
        reader.setErrorHandler(handler);

        try {
            reader.parse(new InputSource(new StringReader(xmlAsString)));
        } catch (final Exception e) {
            LOG.error(e, e);
            return null;
        }

        return builder.toString();
    }
    /**
     * 
    * Description: 把xml转为map
    *  @param xml
    *  @param attr
    *  @return 
    * @author lishun 
    * @date 2017年7月2日 
    * @return Map<String,Object>
     */
    public static Map<String,Object> extractCustomAttributes(final String xml,String attr) {
    	final int pos1 = xml.indexOf("<"+attr+">");
    	final int pos2 = xml.indexOf("</"+attr+">");
    	if (pos1 == -1) {
    		return Collections.emptyMap();
    	}
    	final String attributesText = xml.substring(pos1+16, pos2);
    	final Map<String,Object> attributes = new HashMap<String,Object>();
    	final BufferedReader br = new BufferedReader(new StringReader(attributesText));
    	
    	String line;
    	final List<String> attributeNames = new ArrayList<String>();
    	try {
	    	while ((line = br.readLine()) != null) {
	    		final String trimmedLine = line.trim();
	    		if (trimmedLine.length() > 0) {
		    		final int leftPos = trimmedLine.indexOf(":");
		    		final int rightPos = trimmedLine.indexOf(">");
		    		attributeNames.add(trimmedLine.substring(leftPos+1, rightPos));
	    		}
	    	}
	    	br.close();
    	} catch (final IOException e) {
    		//ignore
    	}
        for (final String name : attributeNames) {
            final List<String> values = getTextForElements(xml, name);

            if (values.size() == 1) {
                attributes.put(name, values.get(0));
            } else {
                attributes.put(name, values);
            }
    	}
    	return attributes;
    }
}
