package org.lnicholls.galleon.togo;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.util.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class RulesList extends DefaultHandler implements Constants {

    private static Logger log = Logger.getLogger(RulesList.class.getName());

    private static String RULE = "rule";

    private static String CRITERIA = "criteria";

    private static String COMPARISON = "comparison";

    private static String VALUE = "value";

    private static String DOWNLOAD = "download";

    public RulesList() {
        super();
    }

    public boolean exists() {
        File file = new File(System.getProperty("conf") + File.separator + "togorules.xml");
        return file.exists();
    }

    public ArrayList load() {
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(this);
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            File file = new File(System.getProperty("conf") + File.separator + "togorules.xml");
            if (file.exists())
                xmlReader.parse(new InputSource(new FileInputStream(file)));
        } catch (IOException ex) {
            Tools.logException(RulesList.class, ex);
        } catch (SAXException ex) {
            Tools.logException(RulesList.class, ex);
        } catch (Exception ex) {
            Tools.logException(RulesList.class, ex);
        }
        return mItems;
    }

    public void save(ArrayList items) {
        try {
            StringBuffer buffer = new StringBuffer();
            synchronized (buffer) {
                buffer.append("<?xml version=\"1.0\" encoding=\"").append(ENCODING).append("\" ?>\n");
                // TODO Handle versioning
                buffer.append("<togo version=\"2.3\">\n");
                for (Iterator i = items.iterator(); i.hasNext(); /* Nothing */) {
                    Rule rule = (Rule) i.next();
                    buffer.append("<").append(RULE);
                    buffer.append(" ").append(CRITERIA).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(rule.getCriteria()), ENCODING)).append("\"");
                    buffer.append(" ").append(COMPARISON).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(rule.getComparison()), ENCODING)).append("\"");
                    buffer.append(" ").append(VALUE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(rule.getValue()), ENCODING)).append("\"");
                    buffer.append(" ").append(DOWNLOAD).append("=\"").append(rule.getDownload()).append("\"");
                    buffer.append("/>\n");
                }
                buffer.append("</togo>\n");
            }

            String path = System.getProperty("conf");
            FileOutputStream fileOutputStream = new FileOutputStream(path + File.separator + "togorules.xml");
            PrintWriter printWriter = new PrintWriter(fileOutputStream);
            printWriter.print(buffer.toString());
            printWriter.close();
        } catch (Exception ex) {
            Tools.logException(RulesList.class, ex);
        }
    }

    /**
     * Method used by SAX at the start of the XML document parsing.
     */
    public void startDocument() {
        mItems = new ArrayList();
    }

    /**
     * SAX XML parser method. Start of an element.
     * 
     * @param namespaceURI
     *            namespace URI this element is associated with, or an empty String
     * @param localName
     *            name of element (with no namespace prefix, if one is present)
     * @param qName
     *            XML 1.0 version of element name: [namespace prefix]:[localName]
     * @param attributes
     *            Attributes for this element
     */

    public void startElement(String namespaceURI, String localName, String qName, Attributes attributes)
            throws SAXException {
        if (localName.equalsIgnoreCase(RULE)) {
            try {
                mRule = new Rule();
                mRule
                        .setCriteria(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(CRITERIA),
                                ENCODING)));
                mRule.setComparison(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(COMPARISON),
                        ENCODING)));
                mRule.setValue(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(VALUE), ENCODING)));
                mRule.setDownload((new Boolean(attributes.getValue(DOWNLOAD)).booleanValue()));
            } catch (Exception ex) {
                Tools.logException(RulesList.class, ex);
                mRule = null;
            }
        }
    }

    /**
     * SAX XML parser method. End of an element.
     * 
     * @param namespaceURI
     *            namespace URI this element is associated with, or an empty String
     * @param localName
     *            name of element (with no namespace prefix, if one is present)
     * @param qName
     *            XML 1.0 version of element name: [namespace prefix]:[localName]
     * @param attributes
     *            Attributes for this element
     */

    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        if (localName.equalsIgnoreCase(RULE)) {
            if (mRule != null)
                mItems.add(mRule);
        }
    }

    /**
     * Method used by SAX at the end of the XML document parsing.
     */

    public void endDocument() {
    }

    /**
     * Receive notification of a parser warning.
     * 
     * <p>
     * The default implementation does nothing. Application writers may override this method in a subclass to take
     * specific actions for each warning, such as inserting the message in a log file or printing it to the console.
     * </p>
     * 
     * @param e
     *            The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException
     *                Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void warning(SAXParseException e) throws SAXException {
        throw e;
    }

    /**
     * Receive notification of a recoverable parser error.
     * 
     * <p>
     * The default implementation does nothing. Application writers may override this method in a subclass to take
     * specific actions for each error, such as inserting the message in a log file or printing it to the console.
     * </p>
     * 
     * @param e
     *            The warning information encoded as an exception.
     * @exception org.xml.sax.SAXException
     *                Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ErrorHandler#warning
     * @see org.xml.sax.SAXParseException
     */
    public void error(SAXParseException e) throws SAXException {
        throw e;
    }

    /**
     * Report a fatal XML parsing error.
     * 
     * <p>
     * The default implementation throws a SAXParseException. Application writers may override this method in a subclass
     * if they need to take specific actions for each fatal error (such as collecting all of the errors into a single
     * report): in any case, the application must stop all regular processing when this method is invoked, since the
     * document is no longer reliable, and the parser may no longer report parsing events.
     * </p>
     * 
     * @param e
     *            The error information encoded as an exception.
     * @exception org.xml.sax.SAXException
     *                Any SAX exception, possibly wrapping another exception.
     * @see org.xml.sax.ErrorHandler#fatalError
     * @see org.xml.sax.SAXParseException
     */
    public void fatalError(SAXParseException e) throws SAXException {
        throw e;
    }

    private ArrayList mItems = new ArrayList();

    private Rule mRule;

    private DateFormat mDateFormat = DateFormat.getDateInstance(DateFormat.LONG);
}