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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.server.*;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class ToGoList extends DefaultHandler implements Constants {

    private static Logger log = Logger.getLogger(ToGoList.class.getName());

    private static String SHOW = "show";

    private static String TITLE = "title";

    private static String DESCRIPTION = "description";

    private static String EPISODE = "episode";

    private static String CHANNEL = "channel";

    private static String STATION = "station";

    private static String RATING = "rating";

    private static String QUALITY = "quality";

    private static String GENRE = "genre";

    private static String TYPE = "type";

    private static String CODE = "code";

    private static String DATE_RECORDED = "dateRecorded";

    private static String DURATION = "duration";

    private static String SIZE = "size";

    private static String SELECTION = "selection";

    private static String STATUS = "status";

    private static String PATH = "path";
    
    private static String ICON = "icon";

    public ToGoList() {
        super();
        mDateFormat = new SimpleDateFormat();
        mDateFormat.applyPattern("EEE MMM d yyyy HH:mm:ss z");
    }

    public boolean exists() {
        File file = new File(System.getProperty("cache") + File.separator + "togo" + File.separator + "recordings.xml");
        return file.exists();
    }

    public ArrayList load(int status) {
        ArrayList list = new ArrayList();
        ArrayList all = load();
        if (all != null) {
            Iterator iterator = all.listIterator();
            while (iterator.hasNext()) {
                Show show = (Show) iterator.next();
                if ((show.getStatus() & status) != 0)
                    list.add(show);
            }
        }
        return list;
    }

    public ArrayList load() {
        if (mItems!=null)
            mItems.clear();
        mItems = new ArrayList();
        try {
            XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(this);
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            File file = new File(System.getProperty("cache") + File.separator + "togo" + File.separator
                    + "recordings.xml");
            if (file.exists())
                xmlReader.parse(new InputSource(new FileInputStream(file)));
            if (mModified)
            {
                save(mItems);
            }
        } catch (IOException ex) {
            Tools.logException(ToGoList.class, ex);
        } catch (SAXException ex) {
            Tools.logException(ToGoList.class, ex);
        } catch (Exception ex) {
            Tools.logException(ToGoList.class, ex);
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
                    Show show = (Show) i.next();
                    buffer.append("<").append(SHOW);
                    buffer.append(" ").append(TITLE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getTitle()), ENCODING)).append("\"");
                    buffer.append(" ").append(DESCRIPTION).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getDescription()), ENCODING)).append("\"");
                    buffer.append(" ").append(EPISODE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getEpisode()), ENCODING)).append("\"");
                    buffer.append(" ").append(CHANNEL).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getChannel()), ENCODING)).append("\"");
                    buffer.append(" ").append(STATION).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getStation()), ENCODING)).append("\"");
                    buffer.append(" ").append(RATING).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getRating()), ENCODING)).append("\"");
                    buffer.append(" ").append(QUALITY).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getQuality()), ENCODING)).append("\"");
                    buffer.append(" ").append(GENRE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getGenre()), ENCODING)).append("\"");
                    buffer.append(" ").append(TYPE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getType()), ENCODING)).append("\"");
                    buffer.append(" ").append(CODE).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getCode()), ENCODING)).append("\"");
                    buffer.append(" ").append(DATE_RECORDED).append("=\"").append(
                            mDateFormat.format(show.getDateRecorded())).append("\"");
                    buffer.append(" ").append(DURATION).append("=\"").append(show.getDuration()).append("\"");
                    buffer.append(" ").append(SIZE).append("=\"").append(show.getSize()).append("\"");
                    buffer.append(" ").append(SELECTION).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getSelection()), ENCODING)).append("\"");
                    buffer.append(" ").append(STATUS).append("=\"").append(show.getStatus()).append("\"");
                    buffer.append(" ").append(PATH).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getPath()), ENCODING)).append("\"");
                    buffer.append(" ").append(ICON).append("=\"").append(
                            URLEncoder.encode(Tools.escapeXMLChars(show.getIcon()), ENCODING)).append("\"");                    
                    buffer.append("/>\n");
                }
                buffer.append("</togo>\n");
            }

            String path = System.getProperty("cache") + File.separator + "togo";
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(path + File.separator + "recordings.xml");
            PrintWriter printWriter = new PrintWriter(fileOutputStream);
            printWriter.print(buffer.toString());
            printWriter.close();
        } catch (Exception ex) {
            Tools.logException(ToGoList.class, ex);
        }
    }

    /**
     * Method used by SAX at the start of the XML document parsing.
     */
    public void startDocument() {
        mModified = false;
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
        if (localName.equalsIgnoreCase(SHOW)) {
            try {
                mShow = new Show();
                mShow.setTitle(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(TITLE), ENCODING)));
                mShow.setDescription(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(DESCRIPTION),
                        ENCODING)));
                mShow.setEpisode(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(EPISODE), ENCODING)));
                mShow.setChannel(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(CHANNEL), ENCODING)));
                mShow.setStation(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(STATION), ENCODING)));
                mShow.setRating(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(RATING), ENCODING)));
                mShow.setQuality(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(QUALITY), ENCODING)));
                mShow.setGenre(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(GENRE), ENCODING)));
                mShow.setType(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(TYPE), ENCODING)));
                mShow.setCode(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(CODE), ENCODING)));
                mShow.setDateRecorded(mDateFormat.parse(attributes.getValue(DATE_RECORDED)));
                mShow.setDuration(Integer.parseInt(attributes.getValue(DURATION)));
                mShow.setSize(Long.parseLong(attributes.getValue(SIZE)));
                mShow.setSelection(Tools.unEscapeXMLChars(URLDecoder
                        .decode(attributes.getValue(SELECTION), ENCODING)));
                mShow.setStatus(Integer.parseInt(attributes.getValue(STATUS)));
                mShow.setPath(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(PATH), ENCODING)));
                mShow.setIcon(Tools.unEscapeXMLChars(URLDecoder.decode(attributes.getValue(ICON), ENCODING)));
                
                if (mShow.getStatus()==Show.STATUS_DOWNLOADED)
                {
                    File file = new File(mShow.getPath());
                    if (!file.exists())
                    {
                        mShow = null;
                        mModified = true;
                    }
                }
            } catch (Exception ex) {
                Tools.logException(ToGoList.class, ex);
                mShow = null;
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
        if (localName.equalsIgnoreCase(SHOW)) {
            if (mShow != null)
                mItems.add(mShow);
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

    private ArrayList mItems;

    private Show mShow;

    private SimpleDateFormat mDateFormat;
    
    private boolean mModified = false;
}