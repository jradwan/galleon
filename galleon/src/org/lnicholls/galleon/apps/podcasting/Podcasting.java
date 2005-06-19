package org.lnicholls.galleon.apps.podcasting;

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

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.Podcast;
import org.lnicholls.galleon.database.PodcastManager;
import org.lnicholls.galleon.database.PodcastTrack;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultPlayer;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.MusicPlayer;
import org.lnicholls.galleon.winamp.WinampPlayer;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

public class Podcasting extends DefaultApplication {

    private static Logger log = Logger.getLogger(Podcasting.class.getName());

    public final static String TITLE = "Podcasting";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mPlayerBackground;

    private Resource mLyricsBackground;

    private Resource mImagesBackground;

    private Resource mFolderIcon;

    private Resource mItemIcon;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mPlayerBackground = getSkinImage("player", "background");
        mLyricsBackground = getSkinImage("lyrics", "background");
        mImagesBackground = getSkinImage("images", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mItemIcon = getSkinImage("menu", "item");

        PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) context
                .getFactory()).getAppContext().getConfiguration();

        push(new PodcastingMenuScreen(this), TRANSITION_NONE);
    }

    public static Element getDocument(String location) {
        try {
            Document document = null;

            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Podcasting.class.getName()
                    + "." + location);
            String content = persistentValue == null ? null : persistentValue.getValue();
            if (PersistentValueManager.isAged(persistentValue) && !location.equals("local")) {
                try {
                    String page = Tools.getPage(new URL(location));
                    if (page != null)
                        content = page;
                } catch (Exception ex) {
                    Tools.logException(Podcasting.class, ex, "Could not cache document: " + location);
                }
            } else {
                if (location.equals("local"))
                    content = "";
            }
            if (content != null) {
                SAXReader saxReader = new SAXReader();
                try {
                    if (location.equals("local")) {
                        document = saxReader.read(new File("d:/galleon/ipodder.opml"));
                    } else {
                        StringReader stringReader = new StringReader(content);
                        document = saxReader.read(stringReader);

                        if (PersistentValueManager.isAged(persistentValue)) {
                            PersistentValueManager.savePersistentValue(Podcasting.class.getName() + "." + location,
                                    content, 60 * 60 * 6);
                        }
                    }
                } catch (Throwable ex) {
                    if (persistentValue != null) {
                        StringReader stringReader = new StringReader(persistentValue.getValue());
                        document = saxReader.read(stringReader);
                    }
                }

                if (document != null) {
                    Element root = document.getRootElement();
                    if (root.getName().equals("opml"))
                        return root.element("body");
                }
            }
        } catch (Exception ex) {
            Tools.logException(Podcasting.class, ex, "Could not download document: " + location);
        }
        return null;
    }

    private static List getElements(Element element) {
        ArrayList list = new ArrayList();

        try {
            if (element.getName().equals("outline") || element.getName().equals("body")) //OPML
            {
                for (Iterator i = element.elementIterator("outline"); i.hasNext();) {
                    Element outline = (Element) i.next();
                    list.add(outline);
                }
            }
        } catch (Exception ex) {
            Tools.logException(Podcasting.class, ex, "Could not determine data");
        }

        if (list.size() > 0) {
            Element[] elementArray = (Element[]) list.toArray(new Element[0]);
            /*
             * Arrays.sort(elementArray, new Comparator() { public int compare(Object o1, Object o2) { String element1 =
             * Tools.getAttribute((Element) o1, "text"); String element2 = Tools.getAttribute((Element) o2, "text"); if
             * (element1 != null && element2 != null) return element1.compareTo(element2); else return 0; } });
             */

            list.clear();
            for (int i = 0; i < elementArray.length; i++)
                list.add(elementArray[i]);
        }

        return list;
    }

    private boolean isFolder(Element element) {
        if (element.getName().equals("outline") || element.getName().equals("body")) //OPML
        {
            String type = Tools.getAttribute(element, "type");
            return (type == null || !type.equals("link"));
        }
        return false;
    }

    private String getUrl(Element element) {
        if (element.getName().equals("outline")) {
            return Tools.getAttribute(element, "url");
        }
        return null;
    }

    public static ChannelIF getChannel(String location) {
        ChannelIF channel = null;

        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Podcasting.class.getName() + "."
                + location);
        String content = persistentValue == null ? null : persistentValue.getValue();
        if (PersistentValueManager.isAged(persistentValue)) {
            try {
                String page = Tools.getPage(new URL(location));
                if (page != null)
                    content = page;
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex, "Could not cache listing: " + location);
            }
        }

        if (content != null) {
            try {
                ChannelBuilderIF builder = new ChannelBuilder();
                channel = FeedParser.parse(builder, new ByteArrayInputStream((content.getBytes("UTF-8"))));

                if (PersistentValueManager.isAged(persistentValue)) {
                    int ttl = channel.getTtl();
                    if (ttl == 0 || ttl == -1)
                        ttl = 60 * 60 * 6;

                    PersistentValueManager.savePersistentValue(Podcasting.class.getName() + "." + location, content,
                            ttl);
                }
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex, "Could not download listing: " + location);
            }
        }
        return channel;
    }

    public static List getListing(ChannelIF channel) {
        ArrayList listing = new ArrayList();

        if (channel != null) {
            try {
                if (channel.getItems().size() > 0) {
                    int count = 0;
                    Iterator chs = channel.getItems().iterator();
                    while (chs.hasNext()) {
                        ItemIF item = (ItemIF) chs.next();
                        if (item.getEnclosure() != null) {
                            if ((item.getEnclosure().getLocation() != null && item.getEnclosure().getLocation()
                                    .getFile().toLowerCase().endsWith(".mp3"))
                                    || (item.getEnclosure().getType() != null && item.getEnclosure().getType().equals(
                                            "audio/mpeg")))
                                listing.add(item);
                        }
                    }
                }
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
        }
        return listing;
    }

    public class PodcastingMenuScreen extends DefaultMenuScreen {
        public PodcastingMenuScreen(Podcasting app) {
            super(app, "Podcasting");

            getBelow().setResource(mMenuBackground);

            mMenuList.add(new NameValue("Now Playing", "Now Playing"));
            mMenuList.add(new NameValue("Subscriptions", "Subscriptions"));
            mMenuList.add(new NameValue("Directories", "Directories"));
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();

                    new Thread() {
                        public void run() {
                            try {
                                switch (mMenuList.getFocus()) {
                                case 0:
                                    NowPlayingMenuScreen nowPlayingMenuScreen = new NowPlayingMenuScreen(
                                            (Podcasting) getApp());
                                    push(nowPlayingMenuScreen, TRANSITION_LEFT);
                                    break;
                                case 1:
                                    SubscribedMenuScreen subscribedMenuScreen = new SubscribedMenuScreen(
                                            (Podcasting) getApp());
                                    push(subscribedMenuScreen, TRANSITION_LEFT);
                                    break;
                                case 2:
                                    DirectoriesMenuScreen directoriesMenuScreen = new DirectoriesMenuScreen(
                                            (Podcasting) getApp());
                                    push(directoriesMenuScreen, TRANSITION_LEFT);
                                    break;
                                default:
                                    break;
                                }
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                            getBApp().flush();
                        }
                    }.start();
                    return true;
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameValue nameValue = (NameValue) mMenuList.get(index);
            icon.setResource(mFolderIcon);

            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(nameValue.getName()), 40));
        }
    }

    public class NowPlayingMenuScreen extends DefaultMenuScreen {
        public NowPlayingMenuScreen(Podcasting app) {
            super(app, "Now Playing");

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext()
                    .getFactory()).getAppContext().getConfiguration();

            List list = null;
            try {
                list = PodcastManager.listAll();
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
            
            mList = new ArrayList();
            if (list != null && list.size() > 0) {
                for (Iterator i = list.iterator(); i.hasNext();) {
                    Podcast podcast = (Podcast) i.next();
                    List tracks = podcast.getTracks();
                    if (tracks != null && tracks.size() > 0) {
                        for (Iterator trackIterator = tracks.iterator(); trackIterator.hasNext();) {
                            PodcastTrack podcastTrack = (PodcastTrack) trackIterator.next();
                            if ((podcastTrack.getStatus() == PodcastTrack.STATUS_DOWNLOADED || podcastTrack.getStatus() == PodcastTrack.STATUS_PLAYED)
                                    && podcastTrack.getTrack() != null) {
                                mList.add(podcastTrack);
                            }
                        }
                    }
                }
            }
            
            PodcastTrack[] podcastTrackArray = (PodcastTrack[]) mList.toArray(new PodcastTrack[0]);
            mList.clear();
            Arrays.sort(podcastTrackArray, new Comparator() { 
                public int compare(Object o1, Object o2) { 
                    PodcastTrack podcastTrack1 = (PodcastTrack)o1; 
                    PodcastTrack podcastTrack2 = (PodcastTrack)o2; 
                    if (podcastTrack1 != null && podcastTrack2 != null) 
                        return -podcastTrack1.getPublicationDate().compareTo(podcastTrack2.getPublicationDate()); 
                    else 
                        return 0; 
                    } 
                }
            );            
            
            for (int i=0;i<podcastTrackArray.length;i++)
            {
                mMenuList.add(podcastTrackArray[i]);
                mList.add(podcastTrackArray[i]);
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (mTracker != null)
                mFocus = mTracker.getPos();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();

                    new Thread() {
                        public void run() {
                            try {
                                mTracker = new Tracker(mList, mMenuList.getFocus());

                                getBApp().push(new PodcastItemScreen((Podcasting) getBApp(), mTracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                        }
                    }.start();
                    return true;
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            PodcastTrack podcastTrack = (PodcastTrack) mMenuList.get(index);
            icon.setResource(mFolderIcon);

            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(podcastTrack.getTitle()), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        List mList;

        Tracker mTracker;
    }

    public class SubscribedMenuScreen extends DefaultMenuScreen {
        public SubscribedMenuScreen(Podcasting app) {
            super(app, "Subscriptions");

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext()
                    .getFactory()).getAppContext().getConfiguration();

            try {
                mList = PodcastManager.listAllSubscribed();
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }

            if (mList != null && mList.size() > 0) {
                for (Iterator i = mList.iterator(); i.hasNext(); /* Nothing */) {
                    Podcast podcast = (Podcast) i.next();
                    mMenuList.add(podcast);
                }
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();

                    new Thread() {
                        public void run() {
                            try {
                                Tracker tracker = new Tracker(mList, mMenuList.getFocus());

                                getBApp().push(new PodcastScreen((Podcasting) getBApp(), tracker, true), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                        }
                    }.start();
                    return true;
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Podcast podcast = (Podcast) mMenuList.get(index);
            icon.setResource(mFolderIcon);

            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(podcast.getTitle()), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        List mList;
    }

    public class DirectoriesMenuScreen extends DefaultMenuScreen {
        public DirectoriesMenuScreen(Podcasting app) {
            super(app, "Podcast Directories");

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext()
                    .getFactory()).getAppContext().getConfiguration();

            for (Iterator i = podcastingConfiguration.getDirectorys().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                mMenuList.add(nameValue);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();

                    new Thread() {
                        public void run() {
                            try {
                                NameValue nameValue = (NameValue) (mMenuList.get(mMenuList.getFocus()));
                                String location = nameValue.getValue();

                                Element element = getDocument(location);
                                if (element != null) {
                                    Tracker tracker = new Tracker(getElements(element), 0);

                                    DirectoryScreen directoryScreen = new DirectoryScreen((Podcasting) getBApp(),
                                            tracker);
                                    getBApp().push(directoryScreen, TRANSITION_LEFT);
                                    getBApp().flush();
                                }
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                        }
                    }.start();
                    return true;
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameValue nameValue = (NameValue) mMenuList.get(index);
            icon.setResource(mFolderIcon);

            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(nameValue.getName()), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public class DirectoryScreen extends DefaultMenuScreen {

        public DirectoryScreen(Podcasting app, Tracker tracker) {
            this(app, tracker, false);
        }

        public DirectoryScreen(Podcasting app, Tracker tracker, boolean first) {
            super(app, "Podcasting");

            getBelow().setResource(mMenuBackground);

            mTracker = tracker;
            mFirst = first;

            Iterator iterator = mTracker.getList().iterator();
            while (iterator.hasNext()) {
                Element element = (Element) iterator.next();
                mMenuList.add(element);
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            mFocus = mTracker.getPos();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();
                    final Element element = (Element) (mMenuList.get(mMenuList.getFocus()));
                    if (isFolder(element)) {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());
                                    Tracker tracker = new Tracker(getElements(element), 0);
                                    DirectoryScreen directoryScreen = new DirectoryScreen((Podcasting) getBApp(),
                                            tracker);
                                    getBApp().push(directoryScreen, TRANSITION_LEFT);
                                    getBApp().flush();
                                } catch (Exception ex) {
                                    Tools.logException(Podcasting.class, ex);
                                    unload();
                                }
                            }
                        }.start();
                    } else {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());

                                    String location = getUrl(element);
                                    if (location != null) {
                                        if (location.toLowerCase().endsWith(".mp3")) {
                                            /*
                                             * mTracker.setPos(mMenuList.getFocus()); PodcastItemScreen
                                             * podcastItemScreen = new PodcastItemScreen((Podcasting) getBApp(),
                                             * mTracker);
                                             * 
                                             * getBApp().push(podcastItemScreen, TRANSITION_LEFT); getBApp().flush();
                                             */
                                        } else {
                                            Element document = getDocument(location);
                                            if (document != null) {
                                                if (isFolder(document)) {
                                                    Tracker tracker = new Tracker(getElements(document), 0);

                                                    getBApp().push(
                                                            new DirectoryScreen((Podcasting) getBApp(), tracker),
                                                            TRANSITION_LEFT);
                                                    getBApp().flush();
                                                } else {
                                                    ArrayList list = new ArrayList();
                                                    Iterator iterator = mTracker.getList().iterator();
                                                    while (iterator.hasNext()) {
                                                        Element element = (Element) iterator.next();
                                                        location = getUrl(element);

                                                        Podcast podcast = null;
                                                        try {
                                                            List podcasts = PodcastManager.findByPath(location);
                                                            if (podcasts != null && podcasts.size() > 0) {
                                                                podcast = (Podcast) podcasts.get(0);
                                                            } else {
                                                                String title = element.attributeValue("text");
                                                                if (title == null) {
                                                                    title = "Unknown";
                                                                } else if (title.length() > 255)
                                                                    title = title.substring(0, 255);
                                                                podcast = new Podcast(title, 0, location, 0,
                                                                        new ArrayList());
                                                                
                                                                PodcastManager.createPodcast(podcast);
                                                            }
                                                        } catch (Exception ex) {
                                                            Tools.logException(Podcasting.class, ex);
                                                        }

                                                        if (podcast != null)
                                                            list.add(podcast);
                                                    }

                                                    Tracker tracker = new Tracker(list, mTracker.getPos());

                                                    getBApp().push(new PodcastScreen((Podcasting) getBApp(), tracker),
                                                            TRANSITION_LEFT);
                                                    getBApp().flush();
                                                }
                                            } else {
                                                ArrayList list = new ArrayList();
                                                Iterator iterator = mTracker.getList().iterator();
                                                while (iterator.hasNext()) {
                                                    Element element = (Element) iterator.next();
                                                    location = getUrl(element);

                                                    Podcast podcast = null;
                                                    try {
                                                        List podcasts = PodcastManager.findByPath(location);
                                                        if (podcasts != null && podcasts.size() > 0) {
                                                            podcast = (Podcast) podcasts.get(0);
                                                        } else {
                                                            String title = element.attributeValue("text");
                                                            if (title == null) {
                                                                title = "Unknown";
                                                            } else if (title.length() > 255)
                                                                title = title.substring(0, 255);
                                                            podcast = new Podcast(title, 0, location, 0,
                                                                    new ArrayList());
                                                            PodcastManager.createPodcast(podcast);
                                                        }
                                                    } catch (Exception ex) {
                                                        Tools.logException(Podcasting.class, ex);
                                                    }

                                                    if (podcast != null)
                                                        list.add(podcast);
                                                }

                                                Tracker tracker = new Tracker(list, mTracker.getPos());

                                                getBApp().push(new PodcastScreen((Podcasting) getBApp(), tracker),
                                                        TRANSITION_LEFT);
                                                getBApp().flush();
                                            }
                                        }
                                    }
                                } catch (Exception ex) {
                                    Tools.logException(Podcasting.class, ex);
                                    unload();
                                }
                            }
                        }.start();
                    }

                    return true;
                }
            } else if (action.equals("play")) {
                if (mMenuList.size() > 0) {
                    load();
                    final Element element = (Element) (mMenuList.get(mMenuList.getFocus()));
                    if (isFolder(element)) {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());
                                    // TODO Recurse
                                    Tracker tracker = new Tracker(getElements(element), 0);

                                    //MusicPlayerConfiguration musicPlayerConfiguration =
                                    // Server.getServer().getMusicPlayerConfiguration();
                                    //tracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
                                    getBApp().push(new PlayerScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                                    getBApp().flush();
                                } catch (Exception ex) {
                                    Tools.logException(Podcasting.class, ex);
                                }
                            }
                        }.start();
                    } else {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());
                                    // TODO Recurse
                                    Tracker tracker = new Tracker(getElements(element), 0);

                                    //MusicPlayerConfiguration musicPlayerConfiguration =
                                    // Server.getServer().getMusicPlayerConfiguration();
                                    //tracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
                                    getBApp().push(new PlayerScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                                    getBApp().flush();
                                } catch (Exception ex) {
                                    Tools.logException(Podcasting.class, ex);
                                }
                            }
                        }.start();
                    }
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Element element = (Element) mMenuList.get(index);
            String location = getUrl(element);
            if (location == null || !location.endsWith(".mp3"))
                icon.setResource(mFolderIcon);
            else
                icon.setResource(mItemIcon);

            String text = Tools.getAttribute(element, "text");
            if (text == null)
                text = "Unknown";
            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(text), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                if (!mFirst) {
                    postEvent(new BEvent.Action(this, "pop"));
                    return true;
                }
            }
            return super.handleKeyPress(code, rawcode);
        }

        private Tracker mTracker;

        private boolean mFirst;
    }

    public class PodcastScreen extends DefaultScreen {

        private BList list;
        
        public PodcastScreen(Podcasting app, Tracker tracker) {
            this(app, tracker, false);
        }

        public PodcastScreen(Podcasting app, Tracker tracker, boolean showView) {
            super(app, "Podcast", true);

            mTracker = tracker;
            mShowView = showView;

            getBelow().setResource(mInfoBackground);

            int start = TOP;

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d/yyyy hh:mm a");

            mTitleText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 80);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mTitleText.setFont("system-30.font");
            mTitleText.setShadow(true);
            mTitleText.setColor(Color.GREEN);

            start += 90;

            mDescriptionText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 90);
            mDescriptionText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mDescriptionText.setFont("default-18-bold.font");
            mDescriptionText.setShadow(true);

            start += 100;

            mDateText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
            mDateText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            mDateText.setFont("default-18.font");
            mDateText.setShadow(true);

            if (mShowView)
            {
                list = new DefaultOptionList(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 115, (int) Math
                    .round((getWidth() - (SAFE_TITLE_H * 2)) / 2.0), 125, 35);
                list.add("View");
            }
            else
            {
                list = new DefaultOptionList(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 80, (int) Math
                        .round((getWidth() - (SAFE_TITLE_H * 2)) / 2.0), 125, 35);
            }
            list.add("Subscribe");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            getBelow().setResource(mInfoBackground);
            updateView();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            mBusy.setVisible(true);
            mBusy.flush();
            try {
                setPainting(false);
                Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());
                if (podcast.getTracks() == null || podcast.getTracks().size() == 0) {
                    ChannelIF channel = getChannel(podcast.getPath());
                    if (channel != null) {
                        podcast.setDescription(channel.getDescription());
                        podcast.setDateUpdated(channel.getLastBuildDate());
                        
                        List items = getListing(channel);
                        if (items != null && items.size() > 0) {
                            ArrayList tracks = new ArrayList();
                            for (Iterator i = items.iterator(); i.hasNext(); /* Nothing */) {
                                ItemIF item = (ItemIF) i.next();
                                String description = item.getDescription();
                                if (description.length() > 4096)
                                    description = description.substring(0, 4096);
                                tracks.add(new PodcastTrack(item.getTitle(), description, item.getDate(), item
                                        .getEnclosure().getLocation().toExternalForm(), 0, 0, 0, 0, podcast.getId(),
                                        null));
                            }
                            podcast.setTracks(tracks);
                        }

                        try {
                            PodcastManager.updatePodcast(podcast);
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    }
                }

                mTitleText.setValue(cleanHTML(podcast.getTitle()));
                mDescriptionText.setValue(cleanHTML(podcast.getDescription()));
                if (podcast.getDateUpdated() != null)
                    mDateText.setValue(mDateFormat.format(podcast.getDateUpdated()));
                else
                    mDateText.setValue(mDateFormat.format(new Date()));

                if (mShowView)
                {
                    if (podcast.getStatus() == Podcast.STATUS_SUBSCRIBED)
                        list.set(1, "Cancel subscription");
                    else
                        list.set(1, "Subscribe");
                }
                else
                {
                    if (podcast.getStatus() == Podcast.STATUS_SUBSCRIBED)
                        list.set(0, "Cancel subscription");
                    else
                        list.set(0, "Subscribe");
                }
            } finally {
                setPainting(true);
            }
            mBusy.setVisible(false);
        }

        public boolean handleExit() {
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (mShowView)
                {
                    if (list.getFocus() == 0) {
                        postEvent(new BEvent.Action(this, "view"));
                        return true;
                    } else if (list.getFocus() == 1) {
                        try {
                            Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());
                            if (podcast.getStatus() == Podcast.STATUS_SUBSCRIBED) {
                                podcast.setStatus(Podcast.STATUS_DELETED);
                                list.set(1, "Subscribe");
                            } else {
                                podcast.setStatus(Podcast.STATUS_SUBSCRIBED);
                                list.set(1, "Cancel subscription");
                            }
    
                            try {
                                PodcastManager.updatePodcast(podcast);
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
    
                            list.flush();
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                        return true;
                    } else {
                        postEvent(new BEvent.Action(this, "pop"));
                        return true;
                    }
                }
                else
                {
                    if (list.getFocus() == 0) {
                        try {
                            Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());
                            if (podcast.getStatus() == Podcast.STATUS_SUBSCRIBED) {
                                podcast.setStatus(Podcast.STATUS_DELETED);
                                list.set(0, "Subscribe");
                            } else {
                                podcast.setStatus(Podcast.STATUS_SUBSCRIBED);
                                list.set(0, "Cancel subscription");
                            }
    
                            try {
                                PodcastManager.updatePodcast(podcast);
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
    
                            list.flush();
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                        return true;
                    } else {
                        postEvent(new BEvent.Action(this, "pop"));
                        return true;
                    }                    
                }
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_CHANNELUP:
                getBApp().play("pageup.snd");
                getBApp().flush();
                getPrevPos();
                updateView();
                return true;
            case KEY_CHANNELDOWN:
                getBApp().play("pagedown.snd");
                getBApp().flush();
                getNextPos();
                updateView();
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("view")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());

                        Tracker tracker = new Tracker(podcast.getTracks(), 0);

                        getBApp()
                                .push(new PodcastMenuScreen((Podcasting) getBApp(), tracker, podcast), TRANSITION_LEFT);
                        getBApp().flush();
                    }
                }.start();
                return true;
            }

            return super.handleAction(view, action);
        }

        private boolean mShowView;
        
        private Tracker mTracker;

        private SimpleDateFormat mDateFormat;

        private BText mTitleText;

        private BText mDescriptionText;

        private BText mDateText;
    }

    public class PodcastMenuScreen extends DefaultMenuScreen {
        public PodcastMenuScreen(Podcasting app, Tracker tracker, Podcast podcast) {
            super(app, "Podcast Tracks");

            mTracker = tracker;
            mPodcast = podcast;

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d/yyyy hh:mm a");

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext()
                    .getFactory()).getAppContext().getConfiguration();

            for (Iterator i = mTracker.getList().iterator(); i.hasNext(); /* Nothing */) {
                PodcastTrack podcastTrack = (PodcastTrack) i.next();
                mMenuList.add(podcastTrack);
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            mFocus = mTracker.getPos();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (mMenuList.size() > 0) {
                    load();

                    new Thread() {
                        public void run() {
                            mTracker.setPos(mMenuList.getFocus());
                            PodcastItemScreen podcastItemScreen = new PodcastItemScreen((Podcasting) getBApp(),
                                    mTracker);
                            getBApp().push(podcastItemScreen, TRANSITION_LEFT);
                            getBApp().flush();
                        }
                    }.start();
                    return true;
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            PodcastTrack podcastTrack = (PodcastTrack) mMenuList.get(index);
            icon.setResource(mItemIcon);

            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            String title = Tools.trim(cleanHTML(podcastTrack.getTitle()), 40);
            if (title.length() == 0)
                title = mDateFormat.format(podcastTrack.getPublicationDate());
            name.setValue(title);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        private Tracker mTracker;

        private Podcast mPodcast;

        private SimpleDateFormat mDateFormat;
    }

    public class PodcastItemScreen extends DefaultScreen {

        private BList list;

        public PodcastItemScreen(Podcasting app, Tracker tracker) {
            super(app, "Track", true);

            mTracker = tracker;

            getBelow().setResource(mInfoBackground);

            int start = TOP;

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d/yyyy hh:mm a");

            mTitleText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 80);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mTitleText.setFont("system-30.font");
            mTitleText.setShadow(true);
            mTitleText.setColor(Color.GREEN);

            start += 90;

            mDescriptionText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 90);
            mDescriptionText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mDescriptionText.setFont("default-18-bold.font");
            mDescriptionText.setShadow(true);

            start += 100;

            mDateText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
            mDateText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            mDateText.setFont("default-18.font");
            mDateText.setShadow(true);

            start += 30;

            mStatusText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
            mStatusText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            mStatusText.setFont("default-24-bolditalic.font");
            //mStatusText.setColor(new Color(150, 100, 100));
            mStatusText.setColor(Color.ORANGE);
            mStatusText.setShadow(true);

            start += 35;

            mStatusBarBg = new BView(getNormal(), getWidth() - SAFE_TITLE_H - BODY_WIDTH / 3, start, BODY_WIDTH / 3, 30);
            //mStatusBarBg.setResource(Color.WHITE);
            mStatusBarBg.setResource(Color.BLACK);
            mStatusBarBg.setTransparency(.5f);
            mStatusBarBg.setVisible(false);
            mStatusBar = new BView(getNormal(), getWidth() - SAFE_TITLE_H - BODY_WIDTH / 3 + 2, start + 2,
                    BODY_WIDTH / 3 - 4, 30 - 4);
            mStatusBar.setResource(Color.GREEN);
            mStatusBar.setVisible(false);

            list = new DefaultOptionList(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 80, (int) Math
                    .round((getWidth() - (SAFE_TITLE_H * 2)) / 2.3), 90, 35);
            list.add("Play");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            getBelow().setResource(mInfoBackground);

            try {
                setPainting(false);
                if (mUpdateThread != null && mUpdateThread.isAlive())
                    mUpdateThread.interrupt();
            } finally {
                setPainting(true);
            }

            mUpdateThread = new Thread() {
                public void run() {
                    int counter = 0;
                    while (getApp().getContext()!=null) {
                        try {
                            synchronized (this) {
                                updateView();
                            }

                            if (counter++ < 10)
                                sleep(1000 * 5);
                            else
                                sleep(1000 * 10);
                        } catch (InterruptedException ex) {
                            return;
                        } // handle silently for waking up
                        catch (Exception ex2) {
                            Tools.logException(Podcasting.class, ex2);
                            return;
                        }
                    }
                }

                public void interrupt() {
                    synchronized (this) {
                        super.interrupt();
                    }
                }
            };
            mUpdateThread.start();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
            try {
                Podcast currentPodcast = PodcastManager.retrievePodcast(podcastTrack.getPodcast());
                podcastTrack = currentPodcast.getTrack(podcastTrack.getUrl());

                mTitleText.setValue(cleanHTML(podcastTrack.getTitle()));
                mDescriptionText.setValue(cleanHTML(podcastTrack.getDescription()));
                if (podcastTrack.getPublicationDate() != null)
                    mDateText.setValue(mDateFormat.format(podcastTrack.getPublicationDate()));
                else
                    mDateText.setValue(mDateFormat.format(new Date()));
    
                if (podcastTrack.getStatus() == PodcastTrack.STATUS_DOWNLOADING
                        || podcastTrack.getStatus() == PodcastTrack.STATUS_DOWNLOADED) {
                    mStatusBarBg.setVisible(true);
                    mStatusBar.setVisible(true);
                    //speedText.setVisible(true);
    
                    if (podcastTrack.getDownloadTime() > 0) {
                        long rate = (podcastTrack.getDownloadSize() / 1024) / podcastTrack.getDownloadTime();
                        mStatusText.setValue(podcastTrack.getStatusString() + ": " + rate + " KB/Sec");
                        //speedText.setValue(rate+" KB/Sec");
                        if (podcastTrack.getStatus() == Video.STATUS_DOWNLOADED) {
                            //mStatusBar.setSize(mStatusBarBg.getWidth() - 4, mStatusBar.getHeight());
                            mStatusBar.setVisible(false);
                            mStatusBarBg.setVisible(false);
                        } else {
                            float barFraction = podcastTrack.getDownloadSize() / (float) podcastTrack.getSize();
                            if ((mStatusBarBg.getWidth() - 4) * barFraction < 1)
                                mStatusBar.setSize(1, mStatusBar.getHeight());
                            else
                                mStatusBar.setSize((int) (barFraction * (mStatusBarBg.getWidth() - 4)), mStatusBar
                                        .getHeight());
                        }
                    } else if (podcastTrack.getStatus() != PodcastTrack.STATUS_DOWNLOADED) {
                        String progress = "";
                        for (int i = 0; i < mCounter; i++)
                            progress = progress + ".";
                        mStatusText.setValue("Connecting" + progress);
                        //mSpeedText.setValue("0 KB/Sec");
                        mStatusBar.setVisible(false);
                        mCounter++;
                    } else {
                        mStatusText.setValue(podcastTrack.getStatusString());
                        mStatusBar.setVisible(false);
                        mStatusBarBg.setVisible(false);
                    }
                } else {
                    mStatusBarBg.setVisible(false);
                    mStatusBar.setVisible(false);
                    //speedText.setVisible(false);
                    mStatusText.setValue(" ");
                }
    
                if (podcastTrack.getStatus() == PodcastTrack.STATUS_PLAYED)
                    list.set(0, "Play"); // delete
                else if (podcastTrack.getStatus() == PodcastTrack.STATUS_QUEUED
                        || podcastTrack.getStatus() == PodcastTrack.STATUS_DOWNLOADING)
                    list.set(0, "Cancel download");
                else if (podcastTrack.getStatus() == PodcastTrack.STATUS_DOWNLOADED)
                    list.set(0, "Play");
                else
                    list.set(0, "Download");
                getBApp().flush();
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
        }

        public boolean handleExit() {
            try {
                setPainting(false);
                if (mUpdateThread != null && mUpdateThread.isAlive())
                    mUpdateThread.interrupt();
            } finally {
                setPainting(true);
            }
            mUpdateThread = null;
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (list.getFocus() == 0) {
                    String command = (String) list.get(list.getFocus());
                    if (command.equals("Play"))
                        postEvent(new BEvent.Action(this, "play"));
                    else if (command.equals("Delete")) {
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            Podcast podcast = PodcastManager.retrievePodcast(podcastTrack.getPodcast());

                            PodcastTrack track = podcast.getTrack(podcastTrack.getUrl());
                            track.setStatus(PodcastTrack.STATUS_DELETED);
                            track.setDownloadSize(0);
                            track.setDownloadTime(0);
                            Audio audio = track.getTrack();
                            track.setTrack(null);
                            PodcastManager.updatePodcast(podcast);
                            AudioManager.deleteAudio(audio);
                            File file = new File(audio.getPath());
                            if (file.exists()) {
                                file.delete();
                            }
                            list.set(0, "Download");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    } else if (command.equals("Download")) {
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            Podcast podcast = PodcastManager.retrievePodcast(podcastTrack.getPodcast());

                            PodcastTrack track = podcast.getTrack(podcastTrack.getUrl());
                            track.setStatus(PodcastTrack.STATUS_QUEUED);
                            PodcastManager.updatePodcast(podcast);
                            list.set(0, "Cancel download");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    } else if (command.equals("Cancel download")) {
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            Podcast podcast = PodcastManager.retrievePodcast(podcastTrack.getPodcast());

                            PodcastTrack track = podcast.getTrack(podcastTrack.getUrl());
                            track.setStatus(PodcastTrack.STATUS_DOWNLOAD_CANCELLED);
                            PodcastManager.updatePodcast(podcast);
                            list.set(0, "Download");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    }
                    mCounter = 0;
                    return true;
                } else {
                    postEvent(new BEvent.Action(this, "pop"));
                    return true;
                }
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_CHANNELUP:
                getBApp().play("pageup.snd");
                getBApp().flush();
                getPrevPos();
                updateView();
                return true;
            case KEY_CHANNELDOWN:
                getBApp().play("pagedown.snd");
                getBApp().flush();
                getNextPos();
                updateView();
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
                Object object = mTracker.getList().get(pos);
                while (object == null) {
                    pos = mTracker.getNextPos();
                    object = mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                Object object = mTracker.getList().get(pos);
                while (object == null) {
                    pos = mTracker.getPrevPos();
                    object = mTracker.getList().get(pos);
                }
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("play")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        try {
                            ArrayList tracks = new ArrayList();
                            for (Iterator i = mTracker.getList().iterator(); i.hasNext();) {
                                PodcastTrack podcastTrack = (PodcastTrack) i.next();
                                Podcast podcast = PodcastManager.retrievePodcast(podcastTrack.getPodcast());
                                PodcastTrack track = podcast.getTrack(podcastTrack.getUrl());
                                if (track.getTrack() != null) {
                                    tracks.add(new FileItem(track.getTitle(), new File(track.getTrack().getPath())));
                                } else
                                    tracks.add(null);
                            }

                            Tracker tracker = new Tracker(tracks, mTracker.getPos());
                            getBApp().push(new PlayerScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    }
                }.start();
                return true;
            }

            return super.handleAction(view, action);
        }

        private Audio currentAudio() {
            if (mTracker != null) {
                try {
                    Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
                    if (nameFile != null) {
                        if (nameFile.isFile())
                            return getAudio(((File) nameFile.getValue()).getCanonicalPath());
                        else
                            return getAudio((String) nameFile.getValue());
                    }
                } catch (Throwable ex) {
                    Tools.logException(Podcasting.class, ex);
                }
            }
            return null;
        }

        private SimpleDateFormat mDateFormat;

        private BText mTitleText;

        private BText mDescriptionText;

        private BText mDateText;

        private Tracker mTracker;

        private BText mStatusText;

        private BView mStatusBarBg;

        private BView mStatusBar;

        private Thread mUpdateThread;

        private int mCounter;
    }

    public class PlayerScreen extends DefaultScreen {

        public PlayerScreen(Podcasting app, Tracker tracker) {
            super(app, true);

            getBelow().setResource(mPlayerBackground);

            boolean sameTrack = false;
            DefaultApplication defaultApplication = (DefaultApplication) getApp();
            Audio currentAudio = defaultApplication.getCurrentAudio();
            Tracker currentTracker = defaultApplication.getTracker();
            if (currentTracker != null && currentAudio != null) {
                try {
                    Item newItem = (Item) tracker.getList().get(tracker.getPos());
                    if (currentAudio.getPath().equals(((File) newItem.getValue()).getCanonicalPath())) {
                        mTracker = currentTracker;
                        sameTrack = true;
                    } else {
                        mTracker = tracker;
                        app.setTracker(mTracker);
                    }
                } catch (Exception ex) {
                    mTracker = tracker;
                    app.setTracker(mTracker);
                }
            } else {
                mTracker = tracker;
                app.setTracker(mTracker);
            }

            setTitle(" ");

            if (!sameTrack || getPlayer().getState() == Player.STOP)
                getPlayer().startTrack();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            new Thread() {
                public void run() {
                    mBusy.setVisible(true);
                    mBusy.flush();

                    synchronized (this) {
                        try {
                            setPainting(false);
                            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                                    .getMusicPlayerConfiguration();
                            if (musicPlayerConfiguration.getPlayer().equals(MusicPlayerConfiguration.CLASSIC))
                                player = new MusicPlayer(PlayerScreen.this, BORDER_LEFT, SAFE_TITLE_V, BODY_WIDTH,
                                        BODY_HEIGHT - 20, false, (DefaultApplication) getApp(), mTracker, false);
                            else
                                player = new WinampPlayer(PlayerScreen.this, 0, 0, PlayerScreen.this.getWidth(),
                                        PlayerScreen.this.getHeight(), false, (DefaultApplication) getApp(), mTracker);
                            player.updatePlayer();
                            player.setVisible(true);

                            try {
                                Audio audio = ((DefaultApplication) getApp()).getCurrentAudio();
                                boolean found = false;
                                List list = PodcastManager.listAll();
                                for (Iterator i = list.iterator(); i.hasNext(); /* Nothing */) {
                                    Podcast podcast = (Podcast) i.next();
                                    List tracks = podcast.getTracks();
                                    for (Iterator j = tracks.iterator(); j.hasNext(); /* Nothing */) {
                                        PodcastTrack podcastTrack = (PodcastTrack) j.next();
                                        if (podcastTrack.getTrack() != null
                                                && podcastTrack.getTrack().getId().equals(audio.getId())) {
                                            podcastTrack.setStatus(PodcastTrack.STATUS_PLAYED);
                                            PodcastManager.updatePodcast(podcast);
                                            found = true;
                                            break;
                                        }
                                    }
                                    if (found)
                                        break;
                                }
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                        } finally {
                            setPainting(true);
                        }
                    }
                    setFocusDefault(player);
                    setFocus(player);
                    mBusy.setVisible(false);

                    mScreenSaver = new ScreenSaver(PlayerScreen.this);
                    mScreenSaver.start();
                    getBApp().flush();
                }

                public void interrupt() {
                    synchronized (this) {
                        super.interrupt();
                    }
                }
            }.start();

            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            try {
                setPainting(false);
                player.stopPlayer();

                if (mScreenSaver != null && mScreenSaver.isAlive()) {
                    mScreenSaver.interrupt();
                    mScreenSaver = null;
                }
                if (player != null) {
                    player.setVisible(false);
                    player.remove();
                    player = null;
                }
            } finally {
                setPainting(true);
            }
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            if (code != KEY_VOLUMEDOWN && code != KEY_VOLUMEUP) {
                if (getTransparency() != 0.0f)
                    setTransparency(0.0f);
            }
            return super.handleKeyPress(code, rawcode);
        }

        private DefaultPlayer player;

        private Tracker mTracker;

        private ScreenSaver mScreenSaver;
    }

    private static Audio getAudio(String path) {
        Audio audio = null;
        try {
            List list = AudioManager.findByPath(path);
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(Podcasting.class, ex);
        }

        if (audio == null) {
            try {
                File file = new File(path);
                if (file.exists()) {
                    audio = (Audio) MediaManager.getMedia(file.getCanonicalPath());
                    AudioManager.createAudio(audio);
                }
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
        }
        return audio;
    }

    private class ScreenSaver extends Thread {
        public ScreenSaver(PlayerScreen playerScreen) {
            mPlayerScreen = playerScreen;
        }

        public void run() {
            while (true) {
                try {
                    sleep(1000 * 5 * 60);
                    synchronized (this) {
                        mPlayerScreen.setTransparency(0.9f, getResource("*60000"));
                    }
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex2) {
                    Tools.logException(Podcasting.class, ex2);
                    break;
                }
            }
        }

        public void interrupt() {
            synchronized (this) {
                super.interrupt();
            }
        }

        private PlayerScreen mPlayerScreen;
    }

    private static String cleanHTML(String data) {
        String result = "";
        if (data != null) {
            data = data.replaceAll("\n", " ");
            int pos1 = data.indexOf("<");
            if (pos1 != -1) {
                while (pos1 != -1) {
                    int pos2 = data.indexOf(">");
                    if (pos2 == -1) {
                        result = result + data;
                        break;
                    }
                    result = result + data.substring(0, pos1);
                    data = data.substring(pos2 + 1);
                    pos1 = data.indexOf("<");
                }
            } else
                result = data;
        }
        return StringEscapeUtils.unescapeHtml(result);
    }

    public static class PodcastingFactory extends AppFactory {

        public PodcastingFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) getAppContext()
                    .getConfiguration();

            PodcastingThread podcastingThread = new PodcastingThread(podcastingConfiguration);
            podcastingThread.start();
        }
    }
}