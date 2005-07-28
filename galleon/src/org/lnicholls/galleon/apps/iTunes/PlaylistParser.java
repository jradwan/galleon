package org.lnicholls.galleon.apps.iTunes;

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
import java.io.IOException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Iterator;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.Playlist;
import org.lnicholls.galleon.database.PlaylistManager;
import org.lnicholls.galleon.database.PlaylistTrack;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.util.Tools;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;

public class PlaylistParser extends DefaultHandler {
    private static Logger log = Logger.getLogger(PlaylistParser.class.getName());

    private static String DICT = "dict";

    private static String KEY = "key";

    public PlaylistParser(String path) {
        super();

        try {
        	mCurrentPlaylists = new ArrayList(); 
        	XMLReader xmlReader = XMLReaderFactory.createXMLReader("org.apache.xerces.parsers.SAXParser");
            xmlReader.setContentHandler(this);
            xmlReader.setErrorHandler(this);
            xmlReader.setFeature("http://xml.org/sax/features/validation", false);
            File file = new File(path);
            if (file.exists()) {
                FileInputStream fileInputStream = new FileInputStream(file);
                xmlReader.parse(new InputSource(fileInputStream));
                fileInputStream.close();
            }
            
            // Remove old playlists
            List list = PlaylistManager.listAll();
            if (list!=null && list.size()>0)
            {
	            Iterator playlistIterator = list.iterator();
	        	while (playlistIterator.hasNext())
	            {
	            	Playlist playlist = (Playlist)playlistIterator.next();
	            	boolean found = false;
	            	Iterator iterator = mCurrentPlaylists.iterator();
	                while (iterator.hasNext())
	                {
	                	Playlist currentPlaylist = (Playlist)iterator.next();
	                	if (currentPlaylist.getExternalId().equals(playlist.getExternalId()))
	                	{
	                		found = true;
	                		break;
	                	}
	                }
	                
	                if (!found)
	                {
	                	PlaylistManager.deletePlaylist(playlist);
	                	log.debug("Removed playlist: "+playlist.getTitle());
	                }
	            }
            }
            mCurrentPlaylists.clear();
        } catch (IOException ex) {
            Tools.logException(PlaylistParser.class, ex);
        } catch (SAXException ex) {
            Tools.logException(PlaylistParser.class, ex);
        } catch (Exception ex) {
            Tools.logException(PlaylistParser.class, ex);
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
        if (localName.equalsIgnoreCase(DICT)) {
            mDictLevel = mDictLevel + 1;
        } else if (localName.equalsIgnoreCase(KEY)) {
            mInKey = true;
            mKey = null;
        }

        if (mLastTag != null && mLastTag.endsWith(KEY) && !localName.equals(KEY) && !localName.equals(DICT)) {
            mInValue = true;
            mValue = null;
            mType = localName;
        } else if (mFoundTracks && mDictLevel == 3 && localName.equalsIgnoreCase(DICT)) {
            mTracks = new HashMap();
        }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
        String text = new String(ch, start, length).trim();
        if (mInKey) {
            if (mKey == null)
                mKey = text;
            else
                mKey = mKey + text;
        } else if (mInValue) {
            if (mValue == null)
                mValue = text;
            else
                mValue = mValue + text;
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

        if (mDictLevel == 1 && localName.equalsIgnoreCase(KEY) && mKey != null && mKey.equals("Tracks")) {
            mFoundTracks = true;
            mFoundPlaylists = false;
        } else if (mDictLevel == 1 && localName.equalsIgnoreCase(KEY) && mKey != null && mKey.equals("Playlists")) {
            mFoundTracks = false;
            mFoundPlaylists = true;
        } else if (mFoundTracks && mDictLevel == 3 && localName.equalsIgnoreCase(DICT)) {
            if (mTracks != null) {
                processTrack(mTracks);
                mTracks.clear();
                mTracks = null;

                try {
                    Thread.sleep(50); // give the CPU some breathing time
                } catch (Exception ex) {
                }
            }
        } else if (mFoundPlaylists && mDictLevel == 2 && localName.equalsIgnoreCase(DICT)) {
            if (mPlaylist != null && mPlaylistTracks != null) {
                try {
                    mPlaylist.getTracks().clear();
                    PlaylistManager.updatePlaylist(mPlaylist);
                    mPlaylist.setTracks(mPlaylistTracks);
                    PlaylistManager.updatePlaylist(mPlaylist);
                } catch (Exception ex) {
                    Tools.logException(PlaylistParser.class, ex);
                }

                mPlaylistTracks.clear();
                mPlaylistTracks = null;
            }
        }

        if (localName.equalsIgnoreCase(DICT)) {
            mDictLevel = mDictLevel - 1;
        } else if (localName.equalsIgnoreCase(KEY)) {
            mInKey = false;
        } else if (mType != null && localName.equalsIgnoreCase(mType)) {
            if (mFoundTracks) {
                if (mKey != null) {
                    mTracks.put(mKey, mValue);
                }
            } else if (mFoundPlaylists) {
                if (mDictLevel == 2 && mKey != null && mKey.equals("Name")) {
                    if (mValue != null) {
                        try {
                            List list = PlaylistManager.findByTitle(mValue);
                            if (list != null && list.size() > 0) {
                                mPlaylist = (Playlist) list.get(0);
                            } else {
                                mPlaylist = new Playlist(mValue, new Date(), new Date(), new Date(), 0, "iTunes",
                                        mValue, new ArrayList());
                            }

                            if (mPlaylist.getId() == null) {
                                PlaylistManager.createPlaylist(mPlaylist);
                            }
                            
                            mCurrentPlaylists.add(mPlaylist);

                            mPlaylistTracks = new ArrayList();
                        } catch (Exception ex) {
                            Tools.logException(PlaylistParser.class, ex);
                        }
                    }
                } else if (mDictLevel == 3 && mKey != null && mKey.equals("Track ID")) {
                    if (mValue != null) {
                        try {
                            List list = AudioManager.findByExternalId(mValue);
                            if (list != null && list.size() > 0) {
                                Audio audio = (Audio) list.get(0);
                                if (audio != null && mPlaylist != null) {
                                	if (!audio.getPath().startsWith("http"))
                                    {
                                    	File file = new File(audio.getPath());
                                    	if (!file.exists())
                                    	{
                                    		return;
                                    	}
                                    }
                                	
                                    //mPlaylist.getTracks().add(new PlaylistTrack(audio));
                                    mPlaylistTracks.add(new PlaylistTrack(audio));
                                    try {
                                        Thread.sleep(50); // give the CPU some breathing time
                                    } catch (Exception ex) {
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Tools.logException(PlaylistParser.class, ex);
                        }
                    }
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

    private void processTrack(HashMap track) {
        Audio audio = new Audio();
        Mp3File.defaultProperties(audio); // no size??

        String location = null;
        if (track.containsKey("Location")) {
            location = decode((String) track.get("Location"));
            if (location.endsWith("/"))
                location = location.substring(0, location.length());
            // TODO ignore others?
            if (location.startsWith("file://localhost/")) {
                if (SystemUtils.IS_OS_MAC_OSX)
                    location = location.substring("file://localhost".length(), location.length());
                else
                    location = location.substring("file://localhost/".length(), location.length() - 1);
                try {
                    File file = new File(location);
                    if (file.exists() && file.getName().toLowerCase().endsWith(".mp3")) {
                        location = file.getCanonicalPath();
                        audio.setPath(location);
                    } else
                        return;
                } catch (Exception ex) {
                    Tools.logException(PlaylistParser.class, ex);
                }
            } else
                audio.setPath(location); // http
        }

        if (location != null) {
            try {
                List list = AudioManager.findByPath(location);
                if (list != null && list.size() > 0) {
                    audio = (Audio) list.get(0);
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            
            try {
                if (track.containsKey("Date Modified")) {
                    Date modified = mDateFormat.parse((String) track.get("Date Modified"));
                    if (!audio.getDateModified().equals(modified))
                        audio.setDateModified(modified);
                    else {
                        return;
                    }
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }

            if (track.containsKey("Track ID")) {
                audio.setExternalId(decode((String) track.get("Track ID")));
            }

            if (track.containsKey("Name")) {
                audio.setTitle(decode((String) track.get("Name")));
            }

            if (track.containsKey("Artist")) {
                audio.setArtist(decode((String) track.get("Artist")));
            }

            if (track.containsKey("Album")) {
                audio.setAlbum(decode((String) track.get("Album")));
            }

            if (track.containsKey("Genre")) {
                audio.setGenre(decode((String) track.get("Genre")));
            }
            try {

                if (track.containsKey("Size")) {
                    audio.setSize(new Integer((String) track.get("Size")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Total Time")) {
                    audio.setDuration((new Integer((String) track.get("Total Time")).intValue()));
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Rating")) {
                    audio.setRating(new Integer((String) track.get("Rating")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Track Number")) {
                    audio.setTrack(new Integer((String) track.get("Track Number")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Year")) {
                    audio.setDate(new Integer((String) track.get("Year")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Date Added")) {
                    audio.setDateAdded(mDateFormat.parse((String) track.get("Date Added")));
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Bit Rate")) {
                    audio.setBitRate(new Integer((String) track.get("Bit Rate")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Sample Rate")) {
                    audio.setSampleRate(new Integer((String) track.get("Sample Rate")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }

            if (track.containsKey("Comments")) {
                audio.setComments((String) track.get("Comments"));
            }
            try {

                if (track.containsKey("Play Count")) {
                    audio.setPlayCount(new Integer((String) track.get("Play Count")).intValue());
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
            try {

                if (track.containsKey("Play Date UTC")) {
                    audio.setDatePlayed(mDateFormat.parse((String) track.get("Play Date UTC")));
                }
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }

            audio.setOrigen("iTunes");
            try {

                if (audio.getId() == null) {
                    AudioManager.createAudio(audio);
                } else
                    AudioManager.updateAudio(audio);
            } catch (Exception ex) {
                Tools.logException(PlaylistParser.class, ex);
            }
        }
    }

    private String decode(String value) {
        try {
            return Tools.unEscapeXMLChars(URLDecoder.decode(value, "UTF-8"));
        } catch (Exception ex) {
        }
        return value;
    }

    private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

    private boolean mFoundTracks;

    private boolean mFoundPlaylists;

    private int mDictLevel;

    private boolean mInKey;

    private String mKey;

    private boolean mInValue;

    private String mLastTag;

    private String mType;

    private String mValue;

    private HashMap mTracks;

    private Playlist mPlaylist;

    private ArrayList mPlaylistTracks;
    
    private ArrayList mCurrentPlaylists;
}