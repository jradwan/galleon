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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Logger;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.lnicholls.galleon.server.*;

import org.lnicholls.galleon.util.*;

public class ToGo {

    private static String QUERY_CONTAINER = "/TiVoConnect?Command=QueryContainer&Container=%2FNowPlaying";

    private static String RECURSE = "&Recurse=Yes";

    private static String ANCHOR_OFFSET = "&AnchorOffset=";

    private static String ITEM_COUNT = "&ItemCount=";

    // Needed to reduce timeouts on TiVo for too many requests
    private static int MAX = 20;

    private static Logger log = Logger.getLogger(ToGo.class.getName());

    public ToGo(ServerConfiguration serverConfiguration) {
        mDateFormat = new SimpleDateFormat();
        mDateFormat.applyPattern("EEE MMM d yyyy");
        mCalendar = new GregorianCalendar();
        mServerConfiguration = serverConfiguration;
    }

    public void getRecordings(ArrayList tivos, ProgressListener progressIndicator, ArrayList shows) {
        log.debug("getRecordings");
        log.debug("mServerConfiguration.getMediaAccessKey()="+mServerConfiguration.getMediaAccessKey());
        if (mServerConfiguration.getMediaAccessKey().length() > 0) {
            GetMethod get = null;
            Iterator tivosIterator = tivos.iterator();
            while (tivosIterator.hasNext()) {
                TiVo tivo = (TiVo) tivosIterator.next();
                try {
                    Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
                    HttpClient client = new HttpClient();
                    // TODO How to get TiVo address??
                    //client.getHostConfiguration().setHost("192.168.0.5", 443, protocol);
                    client.getHostConfiguration().setHost(tivo.getAddress(), 443, protocol);
                    Credentials credentials = new UsernamePasswordCredentials("tivo", Tools
                            .decrypt(mServerConfiguration.getMediaAccessKey()));
                    //client.getState().setCredentials("TiVo DVR", "192.168.0.5", credentials);
                    client.getState().setCredentials("TiVo DVR", tivo.getAddress(), credentials);

                    int total = -1;
                    int counter = 0;
                    do {
                        get = new GetMethod(QUERY_CONTAINER + RECURSE + ITEM_COUNT + MAX + ANCHOR_OFFSET + counter);
                        client.executeMethod(get);

                        //System.out.println(get.getResponseBodyAsString());

                        SAXBuilder builder = new SAXBuilder();
                        //String body = get.getResponseBodyAsString();
                        //log.debug("body=" + body);
                        //Document doc = builder.build(new ByteArrayInputStream(body.getBytes()));
                        Document doc = builder.build(get.getResponseBodyAsStream());

                        // Get the root element
                        Element root = doc.getRootElement(); //<TiVoContainer>

                        // Print servlet information
                        List children = root.getChildren();
                        Iterator iterator = children.iterator();
                        while (iterator.hasNext()) {
                            Element child = (Element) iterator.next();
                            if (child.getName().equals("Details")) {
                                Element element = child.getChild("LastChangeDate", root.getNamespace());
                                Date lastChangedDate = new Date();
                                if (element != null) {
                                    try {
                                        lastChangedDate = Tools.hexDate(element.getTextTrim());
                                    } catch (NumberFormatException ex) {
                                    }
                                }
                                element = child.getChild("TotalItems", root.getNamespace());
                                if (element != null) {
                                    try {
                                        total = Integer.parseInt(element.getTextTrim());
                                    } catch (NumberFormatException ex) {
                                    }
                                }
                                log.debug("lastChangedDate="+lastChangedDate);
                                log.debug("total="+total);
                                // Only update list if something changed since the last retrieve
                                if (lastChangedDate.after(tivo.getLastChangedDate()) || total!=tivo.getNumShows())
                                {
                                    ArrayList copy = (ArrayList)shows.clone();
                                    shows.clear();
                                    Iterator showIterator = copy.listIterator();
                                    while (showIterator.hasNext())
                                    {
                                        Show show = (Show)showIterator.next();
                                        if (!show.getSource().equals(tivo.getAddress()))
                                        {
                                            shows.add(show);
                                        }
                                    }
                                    tivo.setLastChangedDate(lastChangedDate);
                                    tivo.setNumShows(0);
                                }
                                else
                                {
                                    counter = total;
                                    break;
                                }
                            } else if (child.getName().equals("Item")) {
                                Show show = new Show();
                                counter = counter + 1;
                                if (progressIndicator != null) {
                                    if (total > 0) {
                                        progressIndicator.progress(counter + " of " + total);
                                    } else
                                        progressIndicator.progress(counter + "");
                                }
                                Element details = child.getChild("Details", root.getNamespace());
                                if (details != null) {
                                    Element element = details.getChild("Title", root.getNamespace());
                                    if (element != null)
                                        show.setTitle(element.getTextTrim());
                                    element = details.getChild("SourceSize", root.getNamespace());
                                    if (element != null)
                                        show.setSize(Long.parseLong(element.getTextTrim()));
                                    element = details.getChild("Duration", root.getNamespace());
                                    if (element != null)
                                        show.setDuration(Integer.parseInt(element.getTextTrim()));
                                    element = details.getChild("CaptureDate", root.getNamespace());
                                    if (element != null)
                                        show.setDateRecorded(Tools.hexDate(element.getTextTrim()));
                                    element = details.getChild("EpisodeTitle", root.getNamespace());
                                    if (element != null)
                                        show.setEpisode(element.getTextTrim());
                                    element = details.getChild("Description", root.getNamespace());
                                    if (element != null)
                                        show.setDescription(element.getTextTrim());
                                    element = details.getChild("SourceChannel", root.getNamespace());
                                    if (element != null)
                                        show.setChannel(element.getTextTrim());
                                    element = details.getChild("SourceStation", root.getNamespace());
                                    if (element != null)
                                        show.setStation(element.getTextTrim());
                                    element = details.getChild("InProgress", root.getNamespace());
                                    if (element != null)
                                        show.setStatus(Show.STATUS_RECORDING);
                                    
                                    show.setSource(tivo.getAddress());
                                }

                                Element links = child.getChild("Links", root.getNamespace());
                                if (links != null) {
                                    Element element = links.getChild("Content", root.getNamespace());
                                    if (element != null) {
                                        element = element.getChild("Url", root.getNamespace());
                                        if (element != null)
                                            show.setPath(element.getTextTrim());
                                    }

                                    element = links.getChild("CustomIcon", root.getNamespace());
                                    if (element != null) {
                                        element = element.getChild("Url", root.getNamespace());
                                        if (element != null) {
                                            String icon = element.getTextTrim();
                                            show.setIcon(icon.substring(icon.lastIndexOf(":") + 1));
                                        }
                                    }

                                    element = links.getChild("TiVoVideoDetails", root.getNamespace());
                                    if (element != null) {
                                        element = element.getChild("Url", root.getNamespace());
                                        if (element != null)
                                            getShowDetails(client, show, element.getTextTrim());
                                    }
                                }
                                shows.add(show);
                                tivo.setNumShows(tivo.getNumShows()+1);
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
    }

    public void getShowDetails(HttpClient client, Show show, String url) {
        try {
            //System.out.println(httpget.getResponseBodyAsString());
            GetMethod get = new GetMethod(url);
            client.executeMethod(get);

            SAXBuilder builder = new SAXBuilder();
            //String body = get.getResponseBodyAsString();
            //log.debug("body=" + body);
            //Document doc = builder.build(new ByteArrayInputStream(body.getBytes()));
            Document doc = builder.build(get.getResponseBodyAsStream());

            // Get the root element
            Element root = doc.getRootElement(); //<TiVoContainer>

            Element showing = root.getChild("showing");
            if (showing != null) {
                Element program = showing.getChild("program");
                if (program != null) {
                    Element node = program.getChild("description");
                    if (node != null)
                        show.setDescription(node.getTextTrim());
                    node = program.getChild("episodeTitle");
                    if (node != null)
                        show.setEpisode(node.getTextTrim());
                    node = program.getChild("showType");
                    if (node != null)
                        show.setType(node.getTextTrim());
                    node = program.getChild("colorCode");
                    if ((node != null) && (node.getAttribute("value") != null))
                        show.setCode(node.getAttribute("value").getValue());

                    Element programGenre = program.getChild("vProgramGenre");
                    if (programGenre != null) {
                        List children = programGenre.getChildren();
                        Iterator iterator = children.iterator();
                        String genre = "";
                        while (iterator.hasNext()) {
                            Element child = (Element) iterator.next();

                            if (child.getName().equals("element")) {
                                if (genre.length() == 0)
                                    genre = child.getTextTrim();
                                else
                                    genre = genre + "," + child.getTextTrim();
                            }
                        }
                        if (genre.length() != 0)
                            show.setGenre(genre);
                    }
                }
                Element node = showing.getChild("tvRating");
                if (node != null)
                    show.setRating(node.getTextTrim());
            }
            Element quality = root.getChild("recordingQuality");
            if (quality != null)
                show.setQuality(quality.getTextTrim());
        } catch (MalformedURLException ex) {
            Tools.logException(ToGo.class, ex);
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex);
        }
    }

    public boolean Download(Show show, CancelDownload cancelDownload) {
        ArrayList shows = new ArrayList();
        GetMethod get = null;
        try {
            URL url = new URL(show.getPath());
            Protocol protocol = new Protocol("https", new TiVoSSLProtocolSocketFactory(), 443);
            HttpClient client = new HttpClient();
            // TODO How to get TiVo address??
            client.getHostConfiguration().setHost(url.getHost(), 443, protocol);
            Credentials credentials = new UsernamePasswordCredentials("tivo", Tools.decrypt(mServerConfiguration
                    .getMediaAccessKey()));
            client.getState().setCredentials("TiVo DVR", url.getHost(), credentials);
            get = new GetMethod(show.getPath());
            client.executeMethod(get);

            if (get.getStatusCode() == 503)
                return false;

            InputStream input = get.getResponseBodyAsStream();

            String path = mServerConfiguration.getRecordingsPath();
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String name = getFilename(show);
            log.info("Downloading: " + name);
            File file = new File(path + File.separator + name);
            FileOutputStream output = new FileOutputStream(file, false);

            long total = 0;
            double diff = 0.0;
            byte[] buf = new byte[1024 * 4];
            int amount = 0;
            long target = show.getSize();
            long start = System.currentTimeMillis();
            while (amount == 0 && total < target) {
                while ((amount = input.read(buf)) > 0 && !cancelDownload.cancel()) {
                    total = total + amount;
                    try {
                        output.write(buf, 0, amount);
                        output.flush();
                    } catch (IOException e) {
                    }
                }
                if (cancelDownload.cancel())
                {
                    output.close();
                    return false;
                }
            }
            diff = (System.currentTimeMillis() - start) / 1000.0;
            output.close();
            if (diff != 0)
                log.info("Download rate=" + ((total * 8) / (1024 * 1024)) / diff + " Mbps");
            show.setPath(file.getAbsolutePath());
            
            get.releaseConnection();
        } catch (MalformedURLException ex) {
            Tools.logException(ToGo.class, ex, show.getPath());
        } catch (Exception ex) {
            Tools.logException(ToGo.class, ex, show.getPath());
        }
        return true;
    }

    private String getFilename(Show show) {
        String name = show.getTitle();
        if (show.getEpisode().length() > 0)
            name = name + " - " + show.getEpisode();

        //  Round off to the closest minutes; TiVo seems to start recordings 2 seconds before the scheduled time
        mCalendar.setTime(show.getDateRecorded());
        mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
        mCalendar.set(GregorianCalendar.SECOND, 0);
        name = name + " (Recorded " + mDateFormat.format(mCalendar.getTime());
        name = name + ", " + show.getStation() + ")";

        return clean(name) + ".TiVo";
    }

    private String clean(String value) {
        value = value.replaceAll(":", " ");
        value = value.replaceAll("\\\\", " ");
        value = value.replaceAll("/", " ");
        value = value.replaceAll("\"", " ");
        return value;
    }

    public Show pickNextShowForDownloading() {
        Show next = null;

        // Interrupted download
        ToGoList togoList = new ToGoList();
        ArrayList downloaded = togoList.load();

        // Sort by descending date
        Collections.sort(downloaded, new ShowComparator());

        Iterator downloadedIterator = downloaded.iterator();
        while (downloadedIterator.hasNext()) {
            Show downloadedShow = (Show) downloadedIterator.next();
            if (downloadedShow.getStatus() == Show.STATUS_DOWNLOADING) {
                next = downloadedShow;
                break;
            }
        }

        // User selected download
        downloadedIterator = downloaded.iterator();
        while (downloadedIterator.hasNext()) {
            Show downloadedShow = (Show) downloadedIterator.next();
            if (downloadedShow.getStatus() == Show.STATUS_USER_SELECTED) {
                next = downloadedShow;
                break;
            }
        }

        // pending rules selected download
        downloadedIterator = downloaded.iterator();
        while (downloadedIterator.hasNext()) {
            Show downloadedShow = (Show) downloadedIterator.next();
            if (downloadedShow.getStatus() == Show.STATUS_PENDING) {
                next = downloadedShow;
                break;
            }
        }

        return next;
    }

    public void applyRules() {
        ToGoList togoList = new ToGoList();
        ArrayList downloaded = togoList.load();

        // Use rules to pick recording
        RulesList rulesList = new RulesList();
        ArrayList rules = rulesList.load();

        Iterator iterator = downloaded.iterator();
        while (iterator.hasNext()) {
            Show show = (Show) iterator.next();
            if (show.getStatus() != Show.STATUS_RECORDING && show.getStatus() != Show.STATUS_DOWNLOADED
                    && show.getStatus() != Show.STATUS_USER_CANCELLED) {
                boolean prohibited = false;

                // Find a rule that restricts downloads
                Iterator rulesIterator = rules.iterator();
                while (rulesIterator.hasNext()) {
                    Rule rule = (Rule) rulesIterator.next();
                    if (rule.match(show) && !rule.getDownload()) {
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
                        if (rule.match(show) && rule.getDownload()) {
                            matched = true;
                            break;
                        }
                    }

                    if (matched) {
                        show.setStatus(Show.STATUS_PENDING);
                    }
                }
            }
            togoList.save(downloaded);
        }
    }

    public void setServerConfiguration(ServerConfiguration value) {
        mServerConfiguration = value;
    }

    public ServerConfiguration getServerConfiguration() {
        return mServerConfiguration;
    }

    class ShowComparator implements Comparator {
        public ShowComparator() {
        }

        public int compare(Object o1, Object o2) {
            Show contact1 = (Show) o1;
            Show contact2 = (Show) o2;
            return contact1.getDateRecorded().compareTo(contact2.getDateRecorded());
        }
    }

    protected SimpleDateFormat mDateFormat;

    protected GregorianCalendar mCalendar;

    private ServerConfiguration mServerConfiguration;
}