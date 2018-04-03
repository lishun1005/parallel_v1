package com.rsclouds.gtparallel.core.gtdata.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

public class XmlDom4J {
	private static final Log logger = LogFactory.getLog(XmlDom4J.class);
	
	/**
	 * @param xmlFilePath
	 * @return Document
	 */
	public static Document parse2Document(String xmlFilePath) {
		SAXReader reader = new SAXReader();
		Document document = null;
		File f = null;
		InputStream in = null;
		try {
			f = new File(xmlFilePath);
			in = new FileInputStream(f);
			document = reader.read(in);
		} catch (Exception e) {
			logger.info(e.getMessage());
			return null;
		} finally {
			if (in != null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return document;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static Element selectNodeFirst(Document doc, String nodename, String nodeText) {
		List list = doc.selectNodes(nodename);
		Iterator<Element> it = list.iterator();
		if ( nodeText == null) {
			if (it.hasNext()) {
				return it.next();
			} 
		} else {
			while (it.hasNext()) {
				Element ele = it.next();
				if (ele.getText().equals(nodeText)) {
					return ele;
				}
			}		
		}
		return null;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static void changNodeText(Document doc, String node, String value) {
		List list = doc.selectNodes(node);
		Iterator<Element> it = list.iterator();
		while (it.hasNext()) {
			Element el = it.next();
			el.setText(value);
		}
	}
	
	/**
	 * add one node to the specified node, and set node's text
	 * 
	 * @param ele
	 *            the specified node
	 * @param node
	 *            the name of add node
	 * @param value
	 *            the text of add node
	 * @param attri
	 *            attribute's name
	 * @param attriValue
	 *            attribute's value
	 * @return Element the new node which added
	 */
	public static Element addNode(Element ele, String node, String value,
			String attri, String attriValue) {
		Element element = null;
		element = ele.addElement(node);
		if (value != null) {
			element.setText(value);
		}
		if (attri != null) {
			element.addAttribute(attri, attriValue);
		}
		return element;
	}
}
