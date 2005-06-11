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
import java.awt.Image;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.*;
import java.io.StringReader;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.iTunes.PlaylistParser;
import org.lnicholls.galleon.apps.music.Music;
import org.lnicholls.galleon.apps.music.Music.MusicScreen;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.*;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Playlist;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.Lyrics;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.Yahoo;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultPlayer;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.MusicInfo;
import org.lnicholls.galleon.widget.MusicPlayer;
import org.lnicholls.galleon.widget.ScrollText;
import org.lnicholls.galleon.winamp.ClassicSkin;
import org.lnicholls.galleon.winamp.WinampPlayer;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.PersistentValue;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;

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

        PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) context.getFactory()).getAppContext()
                .getConfiguration();

        push(new PodcastingMenuScreen(this), TRANSITION_NONE);
    }
    
    private Element getDocument(String location)
    {
        System.out.println("getDocument: "+location);
        try {
            Document document = null;
            
            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(this.getClass().getName() + "." + location);
            System.out.println("persistentValue="+persistentValue);
            String content = persistentValue==null?null:persistentValue.getValue();
            if (PersistentValueManager.isAged(persistentValue) && !location.equals("local"))
            {
                if (persistentValue!=null)
                {
                    System.out.println("persistentValue.getDateModified="+persistentValue.getDateModified());
                    System.out.println("persistentValue.getTimeToLive="+persistentValue.getTimeToLive());
                }
                
                try
                {
                    String page = Tools.getPage(new URL(location));
                    if (page!=null)
                        content = page;
                }
                catch (Exception ex)
                {
                    log.error("Could not cache document: " + location);
                }
            }
            else
            {
                System.out.println("hit cache");
                if (location.equals("local"))
                    content = "";
            }
            System.out.println("content="+content);
            if (content!=null)
            {
                System.out.println("content="+content.length());
                SAXReader saxReader = new SAXReader();
                try
                {
                    if (location.equals("local"))
                    {
                        System.out.println("a");
                        document = saxReader.read(new File("d:/galleon/ipodder.opml"));    
                    }
                    else
                    {
                        System.out.println("b");
                        StringReader stringReader = new StringReader(content);
                        document = saxReader.read(stringReader);            
                        
                        if (PersistentValueManager.isAged(persistentValue))
                        {
                            System.out.println("c");
                            PersistentValueManager.savePersistentValue(this.getClass().getName() + "." + location, content, 60*60);
                        }        
                    }
                }
                catch (Throwable ex)
                {
                    if (persistentValue!=null)
                    {
                        System.out.println("d");
                        StringReader stringReader = new StringReader(persistentValue.getValue());
                        document = saxReader.read(stringReader);
                    }
                }
                
                if (document!=null)
                {
                    System.out.println("e");
                    Element root = document.getRootElement();
                    if (root.getName().equals("opml"))
                        return root.element("body");
                }
            }
        } catch (Exception ex) {
            log.error("Could not download document: "+location, ex);
        }        
        return null;
    }
    
    private List getElements(Element element)
    {
        System.out.println("getElements: ");
        ArrayList list = new ArrayList();
        
        try {
            if (element.getName().equals("outline") || element.getName().equals("body"))  //OPML
            {
                for (Iterator i = element.elementIterator("outline"); i.hasNext();) {
                    Element outline = (Element) i.next();
                    list.add(outline);
                    System.out.println(Tools.getAttribute(outline, "text"));
                }
            }
        } catch (Exception ex) {
            log.error("Could not determine data", ex);
        }
        
        if (list.size()>0)
        {
            Element[] elementArray = (Element[]) list.toArray(new Element[0]);
            Arrays.sort(elementArray, new Comparator() {
                public int compare(Object o1, Object o2) {
                    String element1 = Tools.getAttribute((Element) o1, "text");
                    String element2 = Tools.getAttribute((Element) o2, "text");
    
                    return element1.compareTo(element2);
                }
            });
            
            list.clear();
            for (int i=0;i<elementArray.length;i++)
                list.add(elementArray[i]);
        }
        
        return list;
    }
    
    private boolean isFolder(Element element)
    {
        if (element.getName().equals("outline") || element.getName().equals("body"))  //OPML
        {
            String type = Tools.getAttribute(element,"type");
            return (type==null || !type.equals("link"));
        }
        return false;
    }
    
    private String getUrl(Element element)
    {
        System.out.println("getUrl: "); //+element);
        if (element.getName().equals("outline"))
        {
            return Tools.getAttribute(element,"url");
        }
        return null;
    }
    
    private ChannelIF getChannel(String location)
    {
        System.out.println("getChannel: "+location);
        ChannelIF channel = null;

        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(this.getClass().getName() + "." + location);
        String content = persistentValue==null?null:persistentValue.getValue();
        if (PersistentValueManager.isAged(persistentValue))
        {
            try
            {
                String page = Tools.getPage(new URL(location));
                if (page!=null)
                    content = page;
            }
            catch (Exception ex)
            {
                log.error("Could not cache listing: " + location);
            }
        }
        
        if (content!=null)
        {
            try
            {
                ChannelBuilderIF builder = new ChannelBuilder();
                channel = FeedParser.parse(builder, new ByteArrayInputStream((content.getBytes("UTF-8"))));
                
                System.out.println("items="+channel.getItems().size());
                
                if (PersistentValueManager.isAged(persistentValue))
                {
                    int ttl = channel.getTtl();
                    if (ttl==0 || ttl == -1)
                        ttl = 60*60;
                
                    PersistentValueManager.savePersistentValue(this.getClass().getName() + "." + location, content, ttl);
                }
            }
            catch (Exception ex)
            {
                Tools.logException(Podcasting.class, ex, "Could not download listing: " + location );
            }   
        }
        return channel;
    }    
    
    private List getListing(ChannelIF channel)
    {
        System.out.println("getListing: "+channel);
        ArrayList listing = new ArrayList();

        if (channel!=null)
        {
            try
            {
                System.out.println("items="+channel.getItems().size());
                if (channel.getItems().size()>0)
                {
                    int count = 0;
                    Iterator chs = channel.getItems().iterator();
                    while (chs.hasNext()) {
                        ItemIF item = (ItemIF) chs.next();
                        if (item.getEnclosure()!=null)
                        {
                            if (item.getEnclosure().getLocation().getFile().toLowerCase().endsWith(".mp3"))
                                listing.add(item);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Tools.logException(Podcasting.class, ex);
            }   
        }
        return listing;
    }

    public class PodcastingMenuScreen extends DefaultMenuScreen {
        public PodcastingMenuScreen(Podcasting app) {
            super(app, "Podcasting");

            getBelow().setResource(mMenuBackground);

            mMenuList.add(new NameValue("Now Playing","Now Playing"));
            mMenuList.add(new NameValue("Subscriptions","Subscriptions"));
            mMenuList.add(new NameValue("Directories","Directories"));
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            switch (mMenuList.getFocus()) {
                            case 0:
                                NowPlayingMenuScreen nowPlayingMenuScreen = new NowPlayingMenuScreen((Podcasting)getApp());
                                push(nowPlayingMenuScreen, TRANSITION_LEFT);
                                break;
                            case 1:
                                SubscribedMenuScreen subscribedMenuScreen = new SubscribedMenuScreen((Podcasting)getApp());
                                push(subscribedMenuScreen, TRANSITION_LEFT);
                                break;
                            case 2:
                                DirectoriesMenuScreen directoriesMenuScreen = new DirectoriesMenuScreen((Podcasting)getApp());
                                push(directoriesMenuScreen, TRANSITION_LEFT);
                                break;
                            default:
                                break;
                            }
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }
                    }
                }.start();
                return true;
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

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();
            
            List list = null;
            try {
                list = PodcastManager.listAll();
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
            
            if (list!=null && list.size()>0)
            {
                for (Iterator i = list.iterator(); i.hasNext(); ) {
                    Podcast podcast = (Podcast) i.next();
                    List tracks = podcast.getTracks();
                    if (tracks!=null && tracks.size()>0)
                    {
                        for (Iterator trackIterator = tracks.iterator(); trackIterator.hasNext(); ) {
                            PodcastTrack podcastTrack = (PodcastTrack) trackIterator.next();
                            if (podcastTrack.getStatus()==PodcastTrack.STATUS_DOWNLOADED && podcastTrack.getTrack()!=null)
                            {
                                mMenuList.add(podcastTrack);
                                mList.add(podcastTrack);
                            }
                        }
                    }
                }
            }
        }
    
        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            Tracker tracker = new Tracker(mList, 0);

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
    }
    
    public class SubscribedMenuScreen extends DefaultMenuScreen {
        public SubscribedMenuScreen(Podcasting app) {
            super(app, "Subscriptions");

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();

            try {
                mList = PodcastManager.listAll();
            } catch (Exception ex) {
                Tools.logException(Podcasting.class, ex);
            }
            
            if (mList!=null && mList.size()>0)
            {
                for (Iterator i = mList.iterator(); i.hasNext(); /* Nothing */) {
                    Podcast podcast = (Podcast) i.next();
                    mMenuList.add(podcast);
                }
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            Tracker tracker = new Tracker(mList, mMenuList.getFocus());
                            
                            getBApp().push(new PodcastScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
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

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();

            for (Iterator i = podcastingConfiguration.getDirectorys().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                mMenuList.add(nameValue);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            NameValue nameValue = (NameValue) (mMenuList.get(mMenuList.getFocus()));
                            String location = nameValue.getValue();
                            System.out.println(location);
                            
                            Element element = getDocument(location);
                            if (element!=null)
                            {
                                Tracker tracker = new Tracker(getElements(element), 0);
                            
                                DirectoryScreen directoryScreen = new DirectoryScreen((Podcasting) getBApp(), tracker);
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
            
            System.out.println(tracker.getList().size());

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
                load();
                final Element element = (Element) (mMenuList.get(mMenuList.getFocus()));
                if (isFolder(element)) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                Tracker tracker = new Tracker(getElements(element), 0);
                                DirectoryScreen directoryScreen = new DirectoryScreen((Podcasting) getBApp(), tracker);
                                getBApp().push(directoryScreen, TRANSITION_LEFT);
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
                                
                                String location = getUrl(element);
                                System.out.println("location="+location);
                                if (location!=null)
                                {
                                    if (location.toLowerCase().endsWith(".mp3"))
                                    {
                                        mTracker.setPos(mMenuList.getFocus());
                                        PodcastItemScreen podcastItemScreen = new PodcastItemScreen((Podcasting) getBApp(), mTracker);

                                        getBApp().push(podcastItemScreen, TRANSITION_LEFT);
                                        getBApp().flush();
                                    }
                                    else
                                    {
                                        Element document = getDocument(location);
                                        System.out.println("document="+document);
                                        if (document!=null)
                                        {
                                            System.out.println("folder="+isFolder(document));
                                            if (isFolder(document))
                                            {
                                                Tracker tracker = new Tracker(getElements(document), 0);
                                                
                                                getBApp().push(new DirectoryScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                                                getBApp().flush();
                                            }
                                            else
                                            {
                                                ArrayList list = new ArrayList();
                                                Iterator iterator = mTracker.getList().iterator();
                                                while (iterator.hasNext()) {
                                                    Element element = (Element) iterator.next();
                                                    location = getUrl(element);
                                                    System.out.println("location="+location);
                                                    
                                                    Podcast podcast = null;
                                                    try {
                                                        List podcasts = PodcastManager.findByPath(location);
                                                        if (podcasts!=null && podcasts.size()>0)
                                                        {
                                                            podcast = (Podcast)podcasts.get(0);
                                                        }
                                                        else
                                                        {
                                                            podcast = new Podcast(element.attributeValue("text"), 0, location, 0, new ArrayList());
                                                        }
                                                    } catch (Exception ex) {
                                                        Tools.logException(Podcasting.class, ex);
                                                    }
                                                    
                                                    if (podcast!=null)
                                                        list.add(podcast);
                                                }
                                                
                                                Tracker tracker = new Tracker(list, 0);
                                                
                                                getBApp().push(new PodcastScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                                                getBApp().flush();
                                            }
                                        }
                                    }
                                }
                            } catch (Exception ex) {
                                Tools.logException(Podcasting.class, ex);
                            }
                        }
                    }.start();
                }

                return true;
            } else if (action.equals("play")) {
                load();
                final Element element = (Element) (mMenuList.get(mMenuList.getFocus()));
                if (isFolder(element)) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                // TODO Recurse
                                Tracker tracker = new Tracker(getElements(element), 0);
                                
                                //MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
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
                                
                                //MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
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
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Element element = (Element) mMenuList.get(index);
            String location = getUrl(element);
            if (location==null || !location.endsWith(".mp3"))
                icon.setResource(mFolderIcon);
            else
                icon.setResource(mItemIcon);

            String text = Tools.getAttribute(element,"text");
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
            super(app, "Podcast", true);
            
            mTracker = tracker;
            
            getBelow().setResource(mInfoBackground);
            
            int start = TOP;
            
            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d/yyyy hh:mm a");

            mTitleText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 80);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mTitleText.setFont("system-30.font");
            mTitleText.setShadow(true);

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
            
            list = new DefaultOptionList(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 115, (int) Math
                    .round((getWidth() - (SAFE_TITLE_H * 2)) / 2.0), 125, 35);
            list.add("View");
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
            Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());
            if (podcast.getTracks()==null || podcast.getTracks().size()==0)
            {
                ChannelIF channel = getChannel(podcast.getPath());
                if (channel != null) {
                    podcast.setDescription(channel.getDescription());
                    podcast.setDateUpdated(channel.getLastBuildDate());
                    
                    List items = getListing(channel);
                    
                    if (items!=null && items.size()>0)
                    {
                        ArrayList tracks = new ArrayList();
                        for (Iterator i = items.iterator(); i.hasNext(); /* Nothing */) {
                            ItemIF item = (ItemIF)i.next();
                            tracks.add(new PodcastTrack(item.getTitle(), item.getDescription(), item.getDate(), item.getEnclosure().getLocation().toExternalForm(), 0, 0, 0, null));
                        }
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
            if (podcast.getDateUpdated()!=null)
                mDateText.setValue(mDateFormat.format(podcast.getDateUpdated()));
            else
                mDateText.setValue(mDateFormat.format(new Date()));
            
            if (podcast.getStatus()==Podcast.STATUS_SUBSCRIBED)
                list.set(1, "Cancel subscription");
            else
                list.set(1, "Subscribe");
        }

        public boolean handleExit() {
            return super.handleExit();
        }
        
        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (list.getFocus() == 0) {
                    postEvent(new BEvent.Action(this, "view"));
                    return true;
                } else 
                if (list.getFocus() == 1) {
                    try {
                        Podcast podcast = (Podcast) mTracker.getList().get(mTracker.getPos());
                        if (podcast.getStatus()==Podcast.STATUS_SUBSCRIBED)
                        {
                            podcast.setStatus(Podcast.STATUS_DELETED);
                            list.set(1, "Subscribe");
                        }
                        else
                        {
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
                        
                        getBApp().push(new PodcastMenuScreen((Podcasting) getBApp(), tracker), TRANSITION_LEFT);
                        getBApp().flush();
                    }
                }.start();
                return true;
            }

            return super.handleAction(view, action);
        }

        private Tracker mTracker;
        private SimpleDateFormat mDateFormat;
        private BText mTitleText;
        private BText mDescriptionText;
        private BText mDateText;
    }    
    
    public class PodcastMenuScreen extends DefaultMenuScreen {
        public PodcastMenuScreen(Podcasting app, Tracker tracker) {
            super(app, "Podcast Tracks");
            
            mTracker = tracker;

            getBelow().setResource(mMenuBackground);

            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) ((PodcastingFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();

            for (Iterator i = mTracker.getList().iterator(); i.hasNext(); /* Nothing */) {
                PodcastTrack podcastTrack = (PodcastTrack) i.next();
                mMenuList.add(podcastTrack);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        PodcastItemScreen podcastItemScreen = new PodcastItemScreen((Podcasting) getBApp(), mTracker);
                        getBApp().push(podcastItemScreen, TRANSITION_LEFT);
                        getBApp().flush();
                    }
                }.start();
                return true;
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
        
        private Tracker mTracker;
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

            list = new DefaultOptionList(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 80, (int) Math
                    .round((getWidth() - (SAFE_TITLE_H * 2)) / 2.0), 90, 35);
            list.add("Play");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            getBelow().setResource(mInfoBackground);
            updateView();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
            mTitleText.setValue(cleanHTML(podcastTrack.getTitle()));
            mDescriptionText.setValue(cleanHTML(podcastTrack.getDescription()));
            if (podcastTrack.getPublicationDate()!=null)
                mDateText.setValue(mDateFormat.format(podcastTrack.getPublicationDate()));
            else
                mDateText.setValue(mDateFormat.format(new Date()));
            
            if (podcastTrack.getStatus()==PodcastTrack.STATUS_PLAYED)
                list.set(0, "Delete");
            else
            if (podcastTrack.getStatus()==PodcastTrack.STATUS_QUEUED)
                list.set(0, "Cancel download");
            else                        
                list.set(0, "Play");
            list.flush();
        }

        public boolean handleExit() {
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (list.getFocus() == 0) {
                    String command = (String)list.get(list.getFocus());
                    System.out.println("command="+command);
                    if (command.equals("Play"))
                        postEvent(new BEvent.Action(this, "play"));
                    else
                    if (command.equals("Delete"))
                    {
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            podcastTrack.setStatus(PodcastTrack.STATUS_DELETED);
                            //PodcastManager.updatePodcast(podcastTrack); ???
                            list.set(0, "Download");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        } 
                    }
                    else
                    if (command.equals("Download"))
                    {
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            podcastTrack.setStatus(PodcastTrack.STATUS_QUEUED);
                            //PodcastManager.updatePodcast(podcastTrack); ???
                            list.set(0, "Delete");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }                       
                    }
                    else
                    if (command.equals("Cancel download"))
                    {
                        ItemIF item = (ItemIF) mTracker.getList().get(mTracker.getPos());
                        PodcastTrack podcastTrack = (PodcastTrack) mTracker.getList().get(mTracker.getPos());
                        try {
                            podcastTrack.setStatus(PodcastTrack.STATUS_DOWNLOAD_CANCELLED);
                            //PodcastManager.updatePodcast(podcastTrack); ???
                            list.set(0, "Download");
                        } catch (Exception ex) {
                            Tools.logException(Podcasting.class, ex);
                        }                                               
                    }
                    list.flush();
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
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("play")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        getBApp().push(new PlayerScreen((Podcasting) getBApp(), mTracker), TRANSITION_LEFT);
                        getBApp().flush();
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
                Item newItem = (Item) tracker.getList().get(tracker.getPos());
                if (currentAudio.getPath().equals(newItem.getValue().toString())) {
                    mTracker = currentTracker;
                    sameTrack = true;
                } else {
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
                                        BODY_HEIGHT - 20, false, (DefaultApplication) getApp(), mTracker);
                            else
                                player = new WinampPlayer(PlayerScreen.this, 0, 0, PlayerScreen.this.getWidth(),
                                        PlayerScreen.this.getHeight(), false, (DefaultApplication) getApp(), mTracker);
                            player.updatePlayer();
                            player.setVisible(true);
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
            if (code!=KEY_VOLUMEDOWN && code!=KEY_VOLUMEUP)
            {
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
                audio = (Audio) MediaManager.getMedia(path);
                AudioManager.createAudio(audio);
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
        return StringEscapeUtils.unescapeHtml(result);
    }
    
    public static class PodcastingFactory extends AppFactory {

        public PodcastingFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            PodcastingConfiguration podcastingConfiguration = (PodcastingConfiguration) getAppContext().getConfiguration();

            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
        }
    }
}