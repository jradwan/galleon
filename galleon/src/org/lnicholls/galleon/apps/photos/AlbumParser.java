/* -*- tab-width: 4 -*- */
package org.lnicholls.galleon.apps.photos;

/*
 * Copyright (C) 2005 Leon Nicholls.
 * Copyright (C) 2007 John Kohl.
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
/* Modeled on iTunes PlaylistParser */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.database.ImageAlbums;
import org.lnicholls.galleon.database.ImageAlbumsManager;
import org.lnicholls.galleon.database.ImageAlbumsPictures;
import org.lnicholls.galleon.database.ImageAlbumsPicturesManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.util.Tools;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class AlbumParser {
    private static Logger log = Logger.getLogger(AlbumParser.class.getName());

    private static String DICT = "dict";

    private static String KEY = "key";
    
    private static String ARRAY = "array";

    private static String STRING = "string";
    
    public static void main(String args[]) {
    	System.out.println("testing AlbumData parser");
    	System.out.println("You asked for parsing of: \"" + args[0] + "\".");
    	AlbumParser x = new AlbumParser(args[0], true);
    }
    public AlbumParser(String path) {
        this(path, false);
    }
    
    public AlbumParser(String path, boolean debugging) {
        try {
        	//path = "D:/galleon/iPhoto Music Library.xml";
        	ArrayList<String> currentImageAlbums = new ArrayList<String>(); 
        	File file;
//        if (!debugging) {	// Read all images
        	XMLReader imageReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        	ImageParser imageParser = new ImageParser(debugging);
        	imageReader.setContentHandler(imageParser);
        	imageReader.setErrorHandler(imageParser);
        	imageReader.setFeature("http://xml.org/sax/features/validation", false);
            file = new File(path);
            if (file.exists()) {
                InputStream inputStream = Tools.getInputStream(file);
                imageReader.parse(new InputSource(inputStream));
                inputStream.close();
                String msg = "Found " + imageParser.getImageCount() + " images";
                if (debugging)
                    System.out.println(msg);
                else
                    log.debug(msg);
            }
//        }
            // Read all imageAlbums
            XMLReader albumReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
        	AlbumListParser albumParser = new AlbumListParser(currentImageAlbums, debugging);
        	albumReader.setContentHandler(albumParser);
        	albumReader.setErrorHandler(albumParser);
        	albumReader.setFeature("http://xml.org/sax/features/validation", false);
            file = new File(path);
            if (file.exists()) {
                InputStream inputStream = Tools.getInputStream(file);
                albumReader.parse(new InputSource(inputStream));
                inputStream.close();
                String msg = "Found " + albumParser.getAlbumCount() + " albums";
                if (debugging)
                    System.out.println(msg);
                else
                    log.debug(msg);
            }

            if (debugging)
                return;

            // Remove old imageAlbums
            List list = ImageAlbumsManager.listAll();
            if (list!=null && list.size()>0)
            {
	            Iterator imageAlbumIterator = list.iterator();
	        	while (imageAlbumIterator.hasNext())
	            {
	            	ImageAlbums imageAlbum = (ImageAlbums)imageAlbumIterator.next();
	            	boolean found = false;
	            	Iterator iterator = currentImageAlbums.iterator();
	                while (iterator.hasNext())
	                {
	                	String externalId = (String)iterator.next();
	                	if (externalId.equals(imageAlbum.getExternalId()))
	                	{
	                		found = true;
	                		break;
	                	}
	                }
	                
	                if (!found)
	                {
	                	ImageAlbumsManager.deleteImageAlbumsPictures(imageAlbum);
	                	ImageAlbumsManager.deleteImageAlbums(imageAlbum);
	                	log.debug("Removed imageAlbum: "+imageAlbum.getTitle());
	                }
	            }
	        	list.clear();
            }
            currentImageAlbums.clear();
        } catch (IOException ex) {
            Tools.logException(AlbumParser.class, ex);
        } catch (SAXException ex) {
            Tools.logException(AlbumParser.class, ex);
        } catch (Exception ex) {
            Tools.logException(AlbumParser.class, ex);
        }
    }
    
    class ImageParser extends DefaultHandler
    {
        // What is the epoch for the timer intervals? It's January 1, 2001.
	    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        private Date mEpochDate;
        
 		public ImageParser(boolean debug)
    	{
    		super();
    		mImages = new HashMap();
    		mKey = new StringBuffer(100);
    		mValue = new StringBuffer(100);
            mDebugging = debug;
            // Should be an easier way with Calendar, eh?
            try {
				mEpochDate = mDateFormat.parse("2001-01-01T00:00:00Z");
			} catch (ParseException ex) {
	            Tools.logException(AlbumParser.class, ex);
			}
}
    	
	    /**
	     * Method used by SAX at the start of the XML document parsing.
	     */
	    public void startDocument() {
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
	        if (localName.equals(DICT)) {
	            mDictLevel = mDictLevel + 1;
	        } else if (localName.equals(KEY)) {
	            mInKey = true;
	            mKey.setLength(0);
	        }
	
            if (mLastTag != null && mLastTag.equals(KEY) && !localName.equals(KEY) && !localName.equals(DICT)) {
	            mInValue = true;
	            mValue.setLength(0);
	            mType = localName;
	        } else if (mFoundImages && mDictLevel == 3 && localName.equals(DICT)) {
	        	mImages.clear();
	        }
	    }
	
	    public void characters(char[] ch, int start, int length) throws SAXException {
	    	if (mInKey) {
                mKey.append(new String(ch, start, length).trim());
	        } else if (mInValue) {
                mValue.append(new String(ch, start, length).trim());
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
	    	mLastTag = localName;
	
	        if (mDictLevel == 1 && localName.equals(KEY)) {
	        		mFoundImages = same(mKey, "Master Image List");
	        } else if (mFoundImages && mDictLevel == 2 && localName.equals(KEY)) {
	        		mImageId = mKey.toString();
	        } else if (mFoundImages && mDictLevel == 3 && localName.equals(DICT)) {
                processImage(mImages, mImageId);
                mImages.clear();

                if (++mCounter%100==0)
         		   System.gc();
                if (!mDebugging) {      // if debugging, go full tilt
                    try {
                        Thread.sleep(50); // give the CPU some breathing time
                    } catch (Exception ex) {
                    }
                }
	        }
	
	        if (localName.equals(DICT)) {
	            mDictLevel = mDictLevel - 1;
	        } else if (localName.equals(KEY)) {
	            mInKey = false;
	        } else if (mType != null && localName.equals(mType)) {
	            if (mFoundImages) {
	            	mImages.put(mKey.toString(), mValue.toString());
	            }
	            mInValue = false;
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
	    private Date intervalToDate(String interval) {
        	Float f = new Float(interval);
        	Date foo = new Date(mEpochDate.getTime() + f.longValue());
        	return foo;
	    }
	    private void processImage(HashMap imagemap, String externalId) {
        	if (imagemap.containsKey("MediaType")) {
        		String mediatype = decode((String)imagemap.get("MediaType"));
        		if (!mediatype.equals("Image"))
        			return;
        	}
            if (!imagemap.containsKey("ImagePath"))
                return;
            Image image;
            String location;
            try {
                location = decode((String)imagemap.get("ImagePath"));
                File file = new File(location);
                if (file.exists()) {
                    location = file.getCanonicalPath();
                } else
                    return;
                image = (Image) MediaManager.getMedia(location);
            } catch (Exception ex) {
                Tools.logException(AlbumParser.class, ex);
                return;
            }


            String msg = "processing Image ID " + externalId + " at " + location;
            if (mDebugging)
                System.out.println(msg);
            else
                log.debug(msg);

            if (!mDebugging) {
                try {
                    List list = ImageManager.findByPath(location);
                    if (list != null && list.size() > 0) {
                        image = (Image) list.get(0);
                        list.clear();
                    }
                } catch (Exception ex) {
                    Tools.logException(AlbumParser.class, ex);
                }
            }

            image.setExternalId(externalId);
           
            // do something with ModDateAsTimerInterval, MetaModDateAsTimerInterval?
            if (imagemap.containsKey("DateAsTimerInterval")) {
            	image.setDateAdded(intervalToDate((String) imagemap.get("DateAsTimerInterval")));
            }
            
            if (imagemap.containsKey("Rating")) {
            	// do some scaling of the rating?
            	image.setRating(new Integer((String) imagemap.get("Rating")).intValue());
            }
            
            image.setOrigen("iPhoto");
            if (!mDebugging)
                try {
	                if (image.getId()==0) {
	                    ImageManager.createImage(image);
	                } else {
                        ImageManager.updateImage(image);
                    }
	            } catch (Exception ex) {
	                Tools.logException(AlbumParser.class, ex);
	            }
	    }

	    private String decode(String value) {
	        try {
	            return Tools.unEscapeXMLChars(URLDecoder.decode(value, "UTF-8"));
	        } catch (Exception ex) {
	        }
	        return value;
	    }	    
	    
        public int getImageCount() {
            return mCounter;
        }

	    private boolean mFoundImages;

	    private int mDictLevel;

	    private boolean mInKey;

	    private StringBuffer mKey;

	    private boolean mInValue;

	    private String mLastTag;

	    private String mType;

	    private StringBuffer mValue;

	    private boolean mInArray;

	    private HashMap mImages;
	    
	    private int mCounter;

        private boolean mDebugging;

        private String mImageId;
    }
    
    class AlbumListParser extends DefaultHandler
    {
    	private int i;

		public AlbumListParser(List<String> imageAlbums, boolean debugging)
    	{
    		mCurrentImageAlbums = imageAlbums;
    		mKey = new StringBuffer(100);
    		mValue = new StringBuffer(100);
            mDebugging = debugging;
    	}
    	
	    /**
	     * Method used by SAX at the start of the XML document parsing.
	     */
	    public void startDocument() {
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
	        if (localName.equals(DICT)) {
	            mDictLevel = mDictLevel + 1;
	            mInArray = false;
	        } else if (localName.equals(KEY)) {
	            mInKey = true;
	            mKey.setLength(0);
	        }
            if (mLastTag != null && mLastTag.equals(KEY) && localName.equals(ARRAY)) {
	            mInArray = true;
                mArrayKey = mKey.toString();
	            mArray = new ArrayList<String>();
	            mType = localName;
            } else if (mLastTag != null && (mLastTag.equals(KEY) || mLastTag.equals(STRING)) && !localName.equals(KEY) && !localName.equals(DICT) && !localName.equals(ARRAY)) {
	            mInValue = true;
	            mValue.setLength(0);
	            mType = localName;
	        }
	    }
	
	    public void characters(char[] ch, int start, int length) throws SAXException {
	        if (mInKey) {
                mKey.append(new String(ch, start, length).trim());
	        } else if (mInValue) {
                mValue.append(new String(ch, start, length).trim());
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
	    	mLastTag = localName;
	
	        if (mDictLevel == 1 && localName.equals(KEY)) {
	            mFoundAlbums = same(mKey, "List of Albums");
	            mFoundRolls = same(mKey, "List of Rolls");
	            // anything else to treat as albums?
	        }
	        if (localName.equals(DICT)) {
	            mDictLevel = mDictLevel - 1;
	        } else if (localName.equals(KEY)) {
	            mInKey = false;
	        } else if (localName.equals(ARRAY)) {
	            mInArray = false;
                if (mDictLevel == 2 && mArrayKey.equals("KeyList")) {
                    // mArray has list of Image key strings
                    String msg = "found " + (mFoundRolls ? "roll " : "album ") + mAlbum + " ID " + mAlbumId;
                    if (mDebugging)
                        System.out.println(msg);
                    else
                        log.debug(msg);
                    if (!mDebugging)
                        try {
                            boolean found = false;
                    		
                            List list = ImageAlbumsManager.findByExternalId(mAlbumId);
                            if (list != null && list.size()>0)
                                {
                                    ImageAlbums imageAlbums = (ImageAlbums)list.get(0);
                                    ImageAlbumsManager.deleteImageAlbumsPictures(imageAlbums);
                                    imageAlbums.setIsRoll(mFoundRolls);
                                    found = true;
                                    list.clear();
                                }
                            
                            if (!found)
                                {
                                    try {
                                        ImageAlbums imageAlbum = new ImageAlbums(mAlbum, new Date(), new Date(), new Date(), 0, 
                                                                                 mFoundRolls, "iPhoto", mAlbumId);
                                        ImageAlbumsManager.createImageAlbums(imageAlbum);
                                    } catch (Exception ex) {
                                        Tools.logException(AlbumParser.class, ex);
                                    }
                                }
                    		
                            log.info("Processing ImageAlbum: "+mAlbum);
                        } catch (Exception ex) {
                                Tools.logException(AlbumParser.class, ex);
                        }
                    Iterator arrayIter = mArray.iterator();
                    while (arrayIter.hasNext()) {
                        String imgnumber = (String) arrayIter.next();
                        try {
                            msg = "\tfound image ID " + imgnumber;
                            if (mDebugging)
                                System.out.println(msg);
                            else
                                log.debug(msg);
                            List list = ImageManager.findByExternalId(imgnumber);
                            if (list != null && list.size() > 0) {
                                Image img = (Image) list.get(0);
                                if (img != null && mAlbum != null) {
                                    File file = new File(img.getPath());
                                    if (!file.exists()) {
                                        continue;
                                    }
                                	
                                    List plist = ImageAlbumsManager.findByExternalId(mAlbumId);
                                    if (plist != null && plist.size()>0)
                                        {
                                            ImageAlbums imageAlbums = (ImageAlbums)plist.get(0);
                                            ImageAlbumsPicturesManager.createImageAlbumsPictures(new ImageAlbumsPictures(imageAlbums.getId(), img.getId()));
                                            plist.clear();
                                        }
                                	
                                    if (!mDebugging)
                                        try { // only pause if not debugging
                                        	Thread.sleep(50); // give the CPU some breathing time
                                        } catch (Exception ex) {
                                        }
                                }
                                list.clear();
                            }
                        } catch (Exception ex) {
                            Tools.logException(AlbumParser.class, ex);
                        }
                    }
                }
                mArray.clear();
	        } else if (localName.equals(STRING) && mInArray) {
                String value = mValue.toString();
                mArray.add(value);
                mInValue = false;
	        } else if (mType != null && localName.equals(mType)) {
	            if (mFoundAlbums || mFoundRolls) {
	                if (mDictLevel == 2 && same(mKey, "AlbumName")) {
	                	if (mValue != null) {
	                        mAlbum = mValue.toString();
	                    }
                        mParsedAlbumCount++;
	                } 
	                else
                	if (mDictLevel == 2 && mKey != null && same(mKey, "AlbumId")) {
                		String value = mValue.toString();
                		
                		mAlbumId = value;
                		mCurrentImageAlbums.add(value);

	                } 
	            }
	            mInValue = false;
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
	    
        public int getAlbumCount() {
            return mParsedAlbumCount;
        }

	    private boolean mFoundAlbums;
	    
	    private boolean mFoundRolls;

	    private int mDictLevel;

	    private boolean mInKey;

	    private StringBuffer mKey;

	    private boolean mInValue;

	    private String mLastTag;

	    private String mType;

	    private StringBuffer mValue;

   	    private String mAlbum;
   	    
   	    private String mAlbumId;
   	    
	    private List<String> mCurrentImageAlbums;
	    
	    private int mCounter;

        private int mParsedAlbumCount;
        
        private boolean mDebugging;

        private List<String> mArray;
        
        private String mArrayKey;
        
        private boolean mInArray;
    }
    
    private static boolean same(StringBuffer buffer, String value)
    {
    	if (buffer.length()==value.length())
    	{
	    	for (int i=0;i < buffer.length();i++)
	    	{
	    		if (value.charAt(i)!=buffer.charAt(i))
	    			return false;
	    	}
	    	return true;
    	}
    	return false;
    }
}
