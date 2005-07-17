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

import java.io.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.database.*;

import org.lnicholls.galleon.util.*;

import org.dom4j.*;
import org.dom4j.io.*;

public class ToGo {
	
	/*
	 * 	https://<tivo ip>/nowplaying/index.html
	 *
	 *	using user: tivo
	 *	pass: <MAK>
	 *
	 * http://<your tivo ip>/TiVoConnect?AnchorOffset=0&Command=QueryContainer&Details=All&ItemCount=1 
	 */

    private static String QUERY_CONTAINER = "/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying";

    private static String RECURSE = "&Recurse=Yes";

    private static String ANCHOR_OFFSET = "&AnchorOffset=";

    private static String ITEM_COUNT = "&ItemCount=";

    // Needed to reduce timeouts on TiVo for too many requests
    private static int MAX = 20;

    private static Logger log = Logger.getLogger(ToGo.class.getName());

    public ToGo() {
        mFileDateFormat = new SimpleDateFormat();
        mFileDateFormat.applyPattern("EEE MMM d yyyy hh mma");
        mTimeDateFormat = new SimpleDateFormat();
        mTimeDateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); //2005-02-23T11:59:58Z
        mCalendar = new GregorianCalendar();
    }

    public ArrayList getRecordings(List tivos, ProgressListener progressIndicator ) {
        ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
        ArrayList videos = new ArrayList();
        log.debug("getRecordings: "+tivos.size());
        log.debug("mServerConfiguration.getMediaAccessKey()=" + serverConfiguration.getMediaAccessKey().length());
        if (serverConfiguration.getMediaAccessKey().length() > 0) {
            GetMethod get = null;
            Iterator tivosIterator = tivos.iterator();
            while (tivosIterator.hasNext()) {
                TiVo tivo = (TiVo) tivosIterator.next();
                try {
                    Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
                    HttpClient client = new HttpClient();
                    // TODO How to get TiVo address??
                    client.getHostConfiguration().setHost(tivo.getAddress(), 443, protocol);
                    Credentials credentials = new UsernamePasswordCredentials("tivo", Tools
                            .decrypt(serverConfiguration.getMediaAccessKey()));
                    client.getState().setCredentials("TiVo DVR", tivo.getAddress(), credentials);

                    int total = -1;
                    int counter = 0;
                    do {
                        get = new GetMethod(QUERY_CONTAINER + RECURSE + ITEM_COUNT + MAX + ANCHOR_OFFSET + counter);
                        client.executeMethod(get);

                        //log.debug(get.getResponseBodyAsString());

                        SAXReader saxReader = new SAXReader();
                        Document document = saxReader.read(get.getResponseBodyAsStream());
                        
                        // Get the root element
                        Element root = document.getRootElement(); //<TiVoContainer>

                        Date lastChangedDate = new Date();
                        Element detailsElement = root.element("Details");
                        if (detailsElement!=null)
                        {
                            Element totalItemsElement = detailsElement.element("TotalItems");
                            if (totalItemsElement!=null)
                            {
                                try {
                                    total = Integer.parseInt(totalItemsElement.getText());
                                } catch (NumberFormatException ex) {
                                }   
                            }
                            Element lastChangeDateElement = detailsElement.element("LastChangeDate");
                            if (lastChangeDateElement!=null)
                            {
                                try {
                                    lastChangedDate = Tools.hexDate(lastChangeDateElement.getText());
                                } catch (NumberFormatException ex) {
                                }    
                            }
                        }
                        
                        log.debug("lastChangedDate=" + lastChangedDate);
                        log.debug("tivo.getLastChangedDate()=" + tivo.getLastChangedDate());
                        log.debug("total=" + total);
                        log.debug("tivo.getNumShows()=" + tivo.getNumShows());
                        if (lastChangedDate.after(tivo.getLastChangedDate()) || total != tivo.getNumShows()) {
                            tivo.setLastChangedDate(lastChangedDate);
                            tivo.setNumShows(0);
                        } else {
                            // Nothing changed
                        	
                        	synchronized(this)
                        	{
	                        	List recordings = VideoManager.listAll();
	                        	Iterator iterator = recordings.listIterator();
	                            while (iterator.hasNext()) {
	                                Video video = (Video) iterator.next();
	                                if (video.getUrl().indexOf(tivo.getAddress())!=-1)
	                                	videos.add(video);
	                            }
                        	}
                            break;
                        }

                        for (Iterator iterator = root.elementIterator(); iterator.hasNext();) {
                            Element child = (Element) iterator.next();
                            if (child.getName().equals("Item")) {
                                Video video = new Video();
                                video.setMimeType("mpeg");
                                video.setDateModified(new Date());
                                
                                counter = counter + 1;
                                if (progressIndicator != null) {
                                    if (total > 0) {
                                        progressIndicator.progress(counter + " of " + total);
                                    } else
                                        progressIndicator.progress(counter + "");
                                }
                                Element details = child.element("Details");
                                if (details != null) {
                                    String value = Tools.getAttribute(details, "Title");
                                    if (value != null)
                                        video.setTitle(value);
                                    value = Tools.getAttribute(details, "SourceSize");
                                    if (value != null)
                                        video.setSize(Long.parseLong(value));
                                    value = Tools.getAttribute(details, "Duration");
                                    if (value != null)
                                        try
                                        {
                                            video.setDuration(Integer.parseInt(value));
                                        } catch (NumberFormatException ex) {
                                            log.error("Could not set duration", ex);
                                        }
                                    value = Tools.getAttribute(details, "CaptureDate");
                                    if (value != null)
                                        video.setDateRecorded(Tools.hexDate(value));
                                    value = Tools.getAttribute(details, "EpisodeTitle");
                                    if (value != null)
                                        video.setEpisodeTitle(value);
                                    value = Tools.getAttribute(details, "Description");
                                    if (value != null)
                                        video.setDescription(value);
                                    value = Tools.getAttribute(details, "SourceChannel");
                                    if (value != null)
                                        video.setChannel(value);
                                    value = Tools.getAttribute(details, "SourceStation");
                                    if (value != null)
                                        video.setStation(value);
                                    value = Tools.getAttribute(details, "InProgress");
                                    if (value != null)
                                        video.setStatus(Video.STATUS_RECORDING);

                                    video.setSource(tivo.getAddress());
                                }

                                Element links = child.element("Links");
                                if (links != null) {
                                    Element element = links.element("Content");
                                    if (element != null) {
                                        String value = Tools.getAttribute(element, "Url");
                                        if (value != null)
                                            video.setUrl(value);
                                    }

                                    element = links.element("CustomIcon");
                                    if (element != null) {
                                        String value = Tools.getAttribute(element, "Url");
                                        if (value != null) {
                                            video.setIcon(value.substring(value.lastIndexOf(":") + 1));
                                        }
                                    }

                                    element = links.element("TiVoVideoDetails");
                                    if (element != null) {
                                        String value = Tools.getAttribute(element, "Url");
                                        if (element != null)
                                            getvideoDetails(client, video, value);
                                    }
                                }
                                videos.add(video);
                                tivo.setNumShows(tivo.getNumShows() + 1);
                            }
                        }
                    } while (counter < total);
                } catch (MalformedURLException ex) {
                    Tools.logException(ToGo.class, ex);
                } catch (Exception ex) {
                    Tools.logException(ToGo.class, ex);
                } finally {
                    if (get != null)
                        get.releaseConnection();
                }
            }
        }
        return videos;
    }

    public void getvideoDetails(HttpClient client, Video video, String url) {
        try {
            //System.out.println(httpget.getResponseBodyAsString());
            GetMethod get = new GetMethod(url);
            client.executeMethod(get);

            SAXReader saxReader = new SAXReader();
            Document document = saxReader.read(get.getResponseBodyAsStream());

            // Get the root element
            Element root = document.getRootElement(); //<TiVoContainer>

            Element showing = root.element("vActualShowing");
            if (showing != null) {
                Element element = showing.element("element");
                if (element != null) {
                    Element program = element.element("program");
                    if (program != null) {
                        Element node = program.element("vActor");
                        if (node != null)
                            video.setActors(getElements(node));
                        node = program.element("vAdvisory");
                        if (node != null)
                            video.setAdvisories(getElements(node));
                        node = program.element("vChoreographer");
                        if (node != null)
                            video.setChoreographers(getElements(node));
                        node = program.element("vChoreographer");
                        if (node != null)
                            video.setChoreographers(getElements(node));
                        node = program.element("colorCode");
                        if (node != null)
                            try
                            {
                                video.setColorCode(Integer.parseInt(node.attribute("value").getText()));
                            } catch (NumberFormatException ex) {
                                log.error("Could not set color code", ex);
                            }
                        node = program.element("description");
                        if (node != null)
                            video.setDescription(node.getTextTrim());
                        node = program.element("vDirector");
                        if (node != null)
                            video.setDirectors(getElements(node));
                        node = program.element("episodeNumber");
                        if (node != null)
                            try
                            {
                                video.setEpisodeNumber(Integer.parseInt(node.getTextTrim()));
                            } catch (NumberFormatException ex) {
                                log.error("Could not episode number", ex);
                            }
                        node = program.element("episodeTitle");
                        if (node != null)
                            video.setEpisodeTitle(node.getTextTrim());
                        node = program.element("vExecProducer");
                        if (node != null)
                            video.setExecProducers(getElements(node));
                        node = program.element("vProgramGenre");
                        if (node != null)
                            video.setProgramGenre(getElements(node));
                        node = program.element("vGuestStar");
                        if (node != null)
                            video.setGuestStars(getElements(node));
                        node = program.element("vHost");
                        if (node != null)
                            video.setHosts(getElements(node));
                        node = program.element("isEpisode");
                        if (node != null)
                            video.setEpisodic(Boolean.valueOf(node.getTextTrim()));
                        node = program.element("originalAirDate");
                        if (node != null) {
                            ParsePosition pos = new ParsePosition(0);
                            Date date = mTimeDateFormat.parse(node.getTextTrim(), pos);
                            if (date == null)
                                date = new Date(0);
                            video.setOriginalAirDate(date); //2000-03-25T00:00:00Z
                        }
                        node = program.element("vProducer");
                        if (node != null)
                            video.setProducers(getElements(node));

                        Element series = program.element("series");
                        if (series != null) {
                            node = series.element("vSeriesGenre");
                            if (node != null)
                                video.setSeriesGenre(getElements(node));
                            node = series.element("seriesTitle");
                            if (node != null)
                                video.setSeriesTitle(node.getTextTrim());
                        }

                        node = program.element("showType");
                        if (node != null)
                            video.setShowType(node.getTextTrim());
                        node = program.element("title");
                        if (node != null)
                            video.setTitle(node.getTextTrim());
                        node = program.element("vWriter");
                        if (node != null)
                            video.setWriters(getElements(node));
                    }
                    Element channel = element.element("channel");
                    if (channel != null) {
                        Element node = channel.element("displayMajorNumber");
                        if (node != null)
                            try
                            {
                                video.setChannelMajorNumber(Integer.parseInt(node.getTextTrim()));
                            } catch (NumberFormatException ex) {
                                log.error("Could not set channel major number", ex);
                            }
                        node = channel.element("displayMinorNumber");
                        if (node != null)
                            try
                            {
                                video.setChannelMinorNumber(Integer.parseInt(node.getTextTrim()));
                            } catch (NumberFormatException ex) {
                                log.error("Could not set channel minor number", ex);
                            }
                        node = channel.element("callsign");
                        if (node != null)
                            video.setCallsign(node.getTextTrim());
                    }
                    Element rating = element.element("tvRating");
                    if (rating!= null)
                        video.setRating(rating.getTextTrim());
                }
            }
            Element node = root.element("vBookmark");
            if (node != null) {
                /*
                 * <vBookmark> <element> <time>PT4M42.137000000S </time> </element> </vBookmark>
                 *  
                 */
                StringBuffer buffer = new StringBuffer();
                int counter = 0;
                for (Iterator iterator = node.elementIterator("element"); iterator.hasNext();) {
                    Element bookmarkElement = (Element) iterator.next();
                    if (counter++ > 0)
                        buffer.append(";");
                    buffer.append(Tools.getAttribute(bookmarkElement, "time"));
                }
                video.setBookmarks(buffer.toString());
            }            
            Element quality = root.element("recordingQuality");
            if (quality != null)
                video.setRecordingQuality(quality.getTextTrim());

            Element time = root.element("startTime");
            if (time != null) {
                ParsePosition pos = new ParsePosition(0);
                Date date = mTimeDateFormat.parse(time.getTextTrim(), pos);
                if (date == null)
                    date = new Date(0);
                video.setStartTime(date); //2005-02-23T11:59:58Z
            }
            time = root.element("stopTime");
            if (time != null) {
                ParsePosition pos = new ParsePosition(0);
                Date date = mTimeDateFormat.parse(time.getTextTrim(), pos);
                if (date == null)
                    date = new Date(0);
                video.setStopTime(date); //2005-02-23T11:59:58Z
            }
            time = root.element("expirationTime");
            if (time != null) {
                ParsePosition pos = new ParsePosition(0);
                Date date = mTimeDateFormat.parse(time.getTextTrim(), pos);
                if (date == null)
                    date = new Date(0);
                video.setExpirationTime(date); //2005-02-23T11:59:58Z
            }
        } catch (MalformedURLException ex) {
            Tools.logException(ToGo.class, ex);
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex);
        }
    }

    private String getElements(Element node) {
        StringBuffer buffer = new StringBuffer();
        int counter = 0;
        for (Iterator iterator = node.elementIterator("element"); iterator.hasNext();) {
            Element element = (Element) iterator.next();
            if (counter++ > 0)
                buffer.append(";");
            buffer.append(element.getTextTrim());
        }
        return buffer.toString();
    }

    public boolean Download(Video video, CancelDownload cancelDownload) {
        ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
        ArrayList videos = new ArrayList();
        GetMethod get = null;
        try {
            URL url = new URL(video.getUrl());
            Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
            HttpClient client = new HttpClient();
            // TODO How to get TiVo address??
            client.getHostConfiguration().setHost(url.getHost(), 443, protocol);
            Credentials credentials = new UsernamePasswordCredentials("tivo", Tools.decrypt(serverConfiguration
                    .getMediaAccessKey()));
            client.getState().setCredentials("TiVo DVR", url.getHost(), credentials);
            get = new GetMethod(video.getUrl());
            client.executeMethod(get);

            if (get.getStatusCode() == 503)
                return false;

            InputStream input = get.getResponseBodyAsStream();

            String path = serverConfiguration.getRecordingsPath();
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = getFilename(video);
            log.info("Downloading: " + name);
            File file = new File(path + File.separator + name);
            FileOutputStream output = new FileOutputStream(file, false);

            long total = 0;
            double diff = 0.0;
            byte[] buf = new byte[1024 * 4];
            int amount = 0;
            long target = video.getSize();
            long start = System.currentTimeMillis();
            long last = start;
            while (amount == 0 && total < target) {
                while ((amount = input.read(buf)) > 0 && !cancelDownload.cancel()) {
                    total = total + amount;
                    try {
                        output.write(buf, 0, amount);
                        output.flush();
                    } catch (IOException e) {
                    }

                    if ((System.currentTimeMillis()-last>10000) && (total>0))
                    {
                        try {
                            video = VideoManager.retrieveVideo(video.getId());
                            if (video.getStatus()==Video.STATUS_DOWNLOADING)
                            {
                                diff = (System.currentTimeMillis() - start) / 1000.0;
                                if (diff>0)
                                {
                                    video.setDownloadSize(total);
                                    video.setDownloadTime((int)diff);
                                    VideoManager.updateVideo(video);
                                }
                            }
                        } catch (HibernateException ex) {
                            log.error("Video update failed", ex);
                        }
                        last = System.currentTimeMillis();
                    }
                    
                }
                if (cancelDownload.cancel()) {
                    output.close();
                    return false;
                }
            }
            diff = (System.currentTimeMillis() - start) / 1000.0;
            output.close();
            if (diff != 0)
                log.info("Download rate=" + (total / 1024) / diff + " KBps");
            try {
                video.setPath(file.getAbsolutePath());
                VideoManager.updateVideo(video);
            } catch (HibernateException ex) {
                log.error("Video update failed", ex);
            }
        } catch (MalformedURLException ex) {
            Tools.logException(ToGo.class, ex, video.getUrl());
            return false;
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex, video.getUrl());
            return false;
        }
        finally
        {
            get.releaseConnection();
        }
        return true;
    }

    private String getFilename(Video video) {
        String name = video.getTitle();
        if (video.getEpisodeTitle()!=null && video.getEpisodeTitle().length() > 0)
            name = name + " - " + video.getEpisodeTitle();

        //  Round off to the closest minutes; TiVo seems to start recordings 2 seconds before the scheduled time
        mCalendar.setTime(video.getDateRecorded());
        mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
        mCalendar.set(GregorianCalendar.SECOND, 0);
        name = name + " (Recorded " + mFileDateFormat.format(mCalendar.getTime());
        name = name + ", " + video.getStation() + ")";

        return clean(name) + ".TiVo";
    }

    private String clean(String value) {
        value = value.replaceAll(":", " ");
        value = value.replaceAll("\\\\", " ");
        value = value.replaceAll("/", " ");
        value = value.replaceAll("\"", " ");
        return value;
    }
    
    public Video pickNextVideoForDownloading() {
        Video next = null;

        try {
            List recordings = VideoManager.listAll();

            // Sort by descending date
            Collections.sort(recordings, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Video contact1 = (Video) o1;
                    Video contact2 = (Video) o2;
                    return contact1.getDateRecorded().compareTo(contact2.getDateRecorded());
                }
            });

            Iterator downloadedIterator = recordings.iterator();
            while (downloadedIterator.hasNext()) {
                Video downloadedvideo = (Video) downloadedIterator.next();
                
                if (downloadedvideo.getStatus() == Video.STATUS_DOWNLOADING) { // Interrupted download
                    next = downloadedvideo;
                    break;
                } else if (downloadedvideo.getStatus() == Video.STATUS_USER_SELECTED) { // User selected download
                    next = downloadedvideo;
                    break;
                } else if (downloadedvideo.getStatus() == Video.STATUS_RULE_MATCHED) { // Rules selected download
                    next = downloadedvideo;
                    break;
                }
            }
            recordings.clear();
        } catch (HibernateException ex) {
            log.error("Getting recordings failed", ex);
        }

        return next;
    }

    public void applyRules() {
        try {
            List recordings = VideoManager.listAll();

            // Use rules to pick recording
            List rules = Server.getServer().getRules();

            Iterator iterator = recordings.iterator();
            while (iterator.hasNext()) {
                Video video = (Video) iterator.next();
                if (video.getStatus() != Video.STATUS_RECORDING && video.getStatus() != Video.STATUS_DOWNLOADED
                        && video.getStatus() != Video.STATUS_USER_CANCELLED && video.getStatus() != Video.STATUS_DELETED) {
                    boolean prohibited = false;

                    // Find a rule that restricts downloads
                    Iterator rulesIterator = rules.iterator();
                    while (rulesIterator.hasNext()) {
                        Rule rule = (Rule) rulesIterator.next();
                        if (rule.match(video) && !rule.getDownload()) {
                            prohibited = true;
                            break;
                        }
                    }

                    if (!prohibited) {
                        // Find a rule that allows downloads
                        boolean matched = false;
                        rulesIterator = rules.iterator();
                        while (rulesIterator.hasNext()) {
                            Rule rule = (Rule) rulesIterator.next();
                            if (rule.match(video) && rule.getDownload()) {
                                matched = true;
                                break;
                            }
                        }

                        if (matched) {
                            video.setStatus(Video.STATUS_RULE_MATCHED);
                            try {
                                VideoManager.updateVideo(video);
                            } catch (HibernateException ex) {
                                log.error("Video update failed", ex);
                            }
                        }
                    }
                }
            }
            recordings.clear();
        } catch (HibernateException ex) {
            log.error("Getting recordings failed", ex);
        }
    }

    protected SimpleDateFormat mFileDateFormat;

    protected SimpleDateFormat mTimeDateFormat;

    protected GregorianCalendar mCalendar;
}