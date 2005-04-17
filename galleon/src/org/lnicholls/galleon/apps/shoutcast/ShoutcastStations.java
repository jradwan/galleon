package org.lnicholls.galleon.apps.shoutcast;

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

import java.io.BufferedReader;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.*;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.util.Tools;

public class ShoutcastStations {
    private static Logger log = Logger.getLogger(ShoutcastStations.class.getName());

    public static String SHOUTCAST = "Shoutcast.com";
    
    private static int MAX_REQUESTS_PER_DAY = 20;

    public ShoutcastStations(ShoutcastConfiguration configuration) {
        mConfiguration = configuration;
        
        new Thread() {
            public void run() {
                try {
                    getPlaylists();
                    sleep(1000*60*60);
                } catch (Exception ex) {
                    log.error("Could not download stations", ex);
                }
            }
        }.start();
    }

    public void getPlaylists() {
        List list = mConfiguration.getGenres();
        for (Iterator i=list.iterator();i.hasNext();)
        {
            String genre = (String)i.next();
            int genreCounter = MAX_REQUESTS_PER_DAY/4; 
            try {
                HttpClient httpclient = new HttpClient();
                httpclient.getParams().setParameter("http.socket.timeout", new Integer(30000));
                httpclient.getParams().setParameter("http.useragent", System.getProperty("http.agent"));
    
                GetMethod get = new GetMethod("http://www.shoutcast.com/directory/?sgenre="
                        + URLEncoder.encode(URLDecoder.decode(genre, "UTF-8"), "UTF-8"));
                get.setFollowRedirects(true);
    
                try {
                    int iGetResultCode = httpclient.executeMethod(get);
                    final String strGetResponseBody = get.getResponseBodyAsString();
                    //log.debug(strGetResponseBody);
    
                    if (strGetResponseBody != null) {
                        //"/sbin/shoutcast-playlist.pls?rn=5224&file=filename.pls"
                        String REGEX = "=\"/sbin/shoutcast-playlist.pls(.*)\">";
                        Pattern p = Pattern.compile(REGEX);
                        Matcher m = p.matcher(strGetResponseBody);
                        boolean found = false;
                        while (m.find() && !found && !isLimit()) {
                            if (log.isDebugEnabled())
                                log.debug("Parameters: " + m.group(1));
                            String link = "http://www.shoutcast.com/sbin/shoutcast-playlist.pls" + m.group(1);
    
                            URL url = new URL(link);
                            String page = Tools.getPage(url);
                            if (page != null) {
                                String inputLine = "";
                                BufferedReader reader = new BufferedReader(new StringReader(page));
                                while ((inputLine = reader.readLine()) != null) {
                                    if (inputLine.startsWith("File")) {
                                        String u = inputLine.substring(inputLine.indexOf("=") + 1);
                                        inputLine = reader.readLine();
                                        String t = inputLine.substring(inputLine.indexOf("=") + 1);
                                        if (t.startsWith("(")) {
                                            t = t.substring(t.indexOf(")") + 1);
                                        }
                                        if (log.isDebugEnabled())
                                            log.debug("PlaylistItem: " + t + "=" + u);
    
                                        Audio audio = null;
                                        try {
                                            audio = (Audio) AudioManager.findByPath(u);
                                        } catch (Exception ex) {
                                        }
    
                                        if (audio != null)
                                            break;
                                        else if (audio == null) {
                                            try {
                                                audio = (Audio) MediaManager.getMedia(u);
                                                audio.setTitle(t);
                                                audio.setGenre(genre);
                                                audio.setOrigen(SHOUTCAST);
                                                AudioManager.createAudio(audio);
                                                found = true;
                                                break;
                                            } catch (Exception ex) {
                                            }
                                        }
                                    }
                                }
                                reader.close();
                            }
                            found = genreCounter--==0;
                        }
                    }
                } catch (Exception ex) {
                    log.error("Could not download stations", ex);
                } finally {
                    get.releaseConnection();
                }
            } catch (Exception ex) {
                Tools.logException(ShoutcastStations.class, ex);
            }
        }
    }
    
    private boolean isLimit()
    {
        if ((System.currentTimeMillis() - mTime < 1000 * 60 * 60 * 24)) {
            if (mCounter <= 0) {
                // Not allowed to exceed daily limit
                log.info("Exceeded daily search limit for Shoutcast.com");
                return true;
            }
        } else {
            mTime = System.currentTimeMillis();
            mCounter = MAX_REQUESTS_PER_DAY;
        }

        mCounter--;
        return false;
    }

    private ShoutcastConfiguration mConfiguration;

    private static long mTime = System.currentTimeMillis();

    private static int mCounter = MAX_REQUESTS_PER_DAY;
}