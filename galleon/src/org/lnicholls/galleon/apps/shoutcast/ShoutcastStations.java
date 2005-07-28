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
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.PersistentValue;

public class ShoutcastStations {
    private static Logger log = Logger.getLogger(ShoutcastStations.class.getName());

    public static String SHOUTCAST = "Shoutcast.com";

    private static int MAX_REQUESTS_PER_DAY = 20;

    public ShoutcastStations(ShoutcastConfiguration configuration) {
        mConfiguration = configuration;

        Server.getServer().scheduleShortTerm(new ReloadTask(new ReloadCallback() {
            public void reload() {
                try {
                	log.debug("Shoutcast");
                    getPlaylists();
                } catch (Exception ex) {
                    log.error("Could not download stations", ex);
                }
            }
        }), 60);
    }

    public void getPlaylists() {
        List list = mConfiguration.getGenres();
        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(ShoutcastStations.this.getClass().getName() + "." + "genre");
        int start = 0;
        if (persistentValue != null) {
            String lastGenre = persistentValue.getValue(); 
            try {
                start = Integer.parseInt(lastGenre);
            } catch (Exception ex) {
            }
        }
        int next = (start + 1) % list.size();
        while (mCounter > 0) {
            String genre = (String) list.get(next);
            int genreCounter = MAX_REQUESTS_PER_DAY / 4;
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
                        String REGEX = "=\"/sbin/shoutcast-playlist.pls([^<]*)\">";
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
                                while ((inputLine = reader.readLine()) != null && !found) {
                                    if (inputLine.startsWith("File")) {
                                        String u = inputLine.substring(inputLine.indexOf("=") + 1);
                                        inputLine = reader.readLine();
                                        String t = inputLine.substring(inputLine.indexOf("=") + 1).trim();
                                        if (t.startsWith("(")) {
                                            t = t.substring(t.indexOf(")") + 1);
                                        }
                                        if (log.isDebugEnabled())
                                            log.debug("PlaylistItem: " + t + "=" + u);

                                        try {
                                            // Remove duplicates
                                            List all = AudioManager.findByOrigenGenre(SHOUTCAST, genre);
                                            for (int i = 0; i < all.size(); i++) {
                                                Audio audio = (Audio) all.get(i);
                                                for (int j = i; j < all.size(); j++) {
                                                    Audio other = (Audio) all.get(j);
                                                    if (!audio.getId().equals(other.getId())) {
                                                        if (audio.getTitle().equals(other.getTitle())) {
                                                            AudioManager.deleteAudio(other);
                                                        }
                                                    }
                                                }
                                            }

                                            Audio current = null;
                                            List same = AudioManager.findByTitle(t);
                                            if (same.size() > 0)
                                                current = (Audio) same.get(0);

                                            if (current != null) {
                                                current.setPath(u);
                                                AudioManager.updateAudio(current);
                                            } else {
                                                Audio audio = (Audio) MediaManager.getMedia(u);
                                                audio.setTitle(t);
                                                audio.setGenre(genre);
                                                audio.setOrigen(SHOUTCAST);
                                                AudioManager.createAudio(audio);
                                            }
                                            found = true;
                                        } catch (Exception ex) {
                                        }
                                    }
                                }
                                reader.close();
                            }
                            found = genreCounter-- == 0;
                        }
                        PersistentValueManager.savePersistentValue(ShoutcastStations.this.getClass().getName() + "." + "genre", String.valueOf(next));
                    }
                } catch (Exception ex) {
                    log.error("Could not download stations", ex);
                    return;
                } finally {
                    get.releaseConnection();
                }
            } catch (Exception ex) {
                Tools.logException(ShoutcastStations.class, ex);
            }
            next = (next + 1) % list.size();
        }
    }

    private boolean isLimit() {
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