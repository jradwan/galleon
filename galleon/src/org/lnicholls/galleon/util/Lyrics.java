package org.lnicholls.galleon.util;

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
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Lyrics {

    private static final Logger log = Logger.getLogger(Lyrics.class.getName());

    /*
     * http://lyrc.com.ar/xsearch.php?songname=Faith&artist=Celine%20Dion&act=1
     */

    private static String RESULT = "result";

    private static String NAME = "name";

    private static String GROUP = "group";

    private static String LYRIC = "lyric";

    public static String getLyrics(String song, String artist) {
        try {
            if (System.currentTimeMillis() - mTime < 1000) {
                // Dont overload the server
                try {
                    Thread.currentThread().sleep(1000);
                } catch (Exception ex) {
                }
            }
            mTime = System.currentTimeMillis();

            URL url = new URL("http://lyrc.com.ar/xsearch.php?songname=" + URLEncoder.encode(song) + "&artist="
                    + URLEncoder.encode(artist) + "&act=1");
            String page = Tools.getPage(url);

            if (page != null) {
                SAXReader saxReader = new SAXReader();
                Document document = saxReader.read(new File("d:/galleon/lyrics.xml"));
                //StringReader stringReader = new StringReader(value);
                //Document document = saxReader.read(stringReader);

                // <lyrc>
                Element root = document.getRootElement();

                for (Iterator i = root.elementIterator(); i.hasNext();) {
                    Element element = (Element) i.next();
                    boolean found = false;
                    if (element.getName().equals(RESULT)) {
                        for (Iterator detailsIterator = element.elementIterator(); detailsIterator.hasNext();) {
                            Element detailsNode = (Element) detailsIterator.next();
                            if (detailsNode.getName().equals(LYRIC))
                                return detailsNode.getText();
                        }
                    }
                }
            }
        } catch (Exception ex) {
            Tools.logException(Lyrics.class, ex, "Could not get lyrics for: " + song);
        }
        return null;
    }

    private static long mTime = System.currentTimeMillis();
}