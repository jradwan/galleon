package org.lnicholls.galleon.apps.musicOrganizer;

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
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.HibernateUtil;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.MediaRefreshThread;
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

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class MusicOrganizer extends DefaultApplication {

    private static Logger log = Logger.getLogger(MusicOrganizer.class.getName());

    public final static String TITLE = "Music Organizer";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mPlayerBackground;

    private Resource mLyricsBackground;

    private Resource mImagesBackground;

    private Resource mFolderIcon;

    private Resource mCDIcon;

    private Resource mPlaylistIcon;

    private MusicScreen mMusicScreen;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mPlayerBackground = getSkinImage("player", "background");
        mLyricsBackground = getSkinImage("lyrics", "background");
        mImagesBackground = getSkinImage("images", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mCDIcon = getSkinImage("menu", "item");
        mPlaylistIcon = getSkinImage("menu", "playlist");

        mMusicScreen = new MusicScreen(this);

        push(new MusicMenuScreen(this), TRANSITION_NONE);
    }

    public class MusicMenuScreen extends DefaultMenuScreen {
        public MusicMenuScreen(MusicOrganizer app) {
            super(app, "Music");

            below.setResource(mMenuBackground);

            MusicOrganizerConfiguration musicConfiguration = (MusicOrganizerConfiguration) ((MusicOrganizerFactory) context.factory)
                    .getAppContext().getConfiguration();

            for (Iterator i = musicConfiguration.getGroups().iterator(); i.hasNext(); /* Nothing */) {
                String group = (String) i.next();
                FormatString formatString = new FormatString(group);

                mMenuList.add(new FolderItem(formatString.getPart(1), formatString));
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            Item item = (Item) (mMenuList.get(mMenuList.getFocus()));
                            FormatString formatString = (FormatString) item.getValue();
                            PathScreen pathScreen = new PathScreen((MusicOrganizer) getBApp(), null, formatString, 2);
                            getBApp().push(pathScreen, TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(MusicOrganizer.class, ex);
                        }
                    }
                }.start();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Item nameFile = (Item) mMenuList.get(index);
            if (nameFile.isFolder()) {
                icon.setResource(mFolderIcon);
            } else {
                icon.setResource(mCDIcon);
            }

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(nameFile.getName(), 40));
        }

    }

    public class PathScreen extends DefaultMenuScreen {

        public PathScreen(MusicOrganizer app, PathScreen pathScreen, FormatString formatString, int level) {
            this(app, pathScreen, false, formatString, level);
        }

        public PathScreen(MusicOrganizer app, PathScreen pathScreen, boolean first, FormatString formatString, int level) {
            super(app, "Music");

            mParent = pathScreen;

            below.setResource(mMenuBackground);

            List list = getItems(this, formatString, level);

            mTracker = new Tracker(list, 0);
            mFirst = first;
            mFormatString = formatString;
            mLevel = level;

            Iterator iterator = mTracker.getList().iterator();
            while (iterator.hasNext()) {
                Item nameFile = (Item) iterator.next();
                mMenuList.add(nameFile);
            }
        }

        private void getConditions(List conditions) {
            if (mParent == null)
                return;
            else {
                int focus = mParent.mMenuList.getFocus() == -1 ? mParent.mFocus : mParent.mMenuList.getFocus();
                QueryPart queryPart = (QueryPart) (mParent.mMenuList.get(focus));
                conditions.add(queryPart);
                mParent.getConditions(conditions);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.isFolder()) {
                    new Thread() {
                        public void run() {
                            try {
                                PathScreen pathScreen = new PathScreen((MusicOrganizer) getBApp(), PathScreen.this,
                                        mFormatString, mLevel + 1);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(MusicOrganizer.class, ex);
                            }
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                mMusicScreen.setTracker(mTracker);

                                getBApp().push(mMusicScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(MusicOrganizer.class, ex);
                            }
                        }
                    }.start();
                }

                return true;
            } else if (action.equals("play")) {
                load();
                final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.isFolder()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                
                                String restrictions = "(1=1)";
                                List conditions = new ArrayList();
                                QueryPart queryPart = (QueryPart) (mMenuList.get(mMenuList.getFocus()));
                                conditions.add(queryPart);
                                getConditions(conditions);
                                Iterator conditionsIterator = conditions.iterator();
                                while (conditionsIterator.hasNext()) {
                                    queryPart = (QueryPart) conditionsIterator.next();
                                    for (int q = 0; q < queryPart.getFields().size(); q++) {
                                        NameValue nameValue = (NameValue) queryPart.getFields().get(q);
                                        boolean needQuote = isString(nameValue.getName());
                                        restrictions = restrictions + " AND " + nameValue.getValue();
                                    }
                                }
                                
                                List files = new ArrayList();
                                try {
                                    Transaction tx = null;
                                    Session session = HibernateUtil.openSession();
                                    try {
                                        String queryString = "from org.lnicholls.galleon.database.Audio audio where " + restrictions
                                                + "AND substr(audio.path,1,4)<>'http'";
                                        log.debug(queryString);
                                        Query query = session.createQuery(queryString).setCacheable(true);
                                        Audio audio = null;
                                        ScrollableResults items = query.scroll();
                                        if (items.first()) {
                                            items.beforeFirst();
                                            while (items.next()) {
                                                audio = (Audio) items.get(0);
                                                files.add(new FileItem(audio.getTitle(), new File(audio.getPath())));
                                            }
                                        }

                                        tx.commit();
                                    } catch (HibernateException he) {
                                        if (tx != null)
                                            tx.rollback();
                                        throw he;
                                    } finally {
                                        HibernateUtil.closeSession();
                                    }
                                } catch (Exception ex) {
                                    Tools.logException(MusicOrganizer.class, ex);
                                }                                    
                                
                                Tracker tracker = new Tracker(files, 0);
                                getBApp().push(new PlayerScreen((MusicOrganizer) getBApp(), tracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(MusicOrganizer.class, ex);
                            }
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                getBApp().push(new PlayerScreen((MusicOrganizer) getBApp(), mTracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(MusicOrganizer.class, ex);
                            }
                        }
                    }.start();
                }
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Item nameFile = (Item) mMenuList.get(index);
            if (nameFile.isFolder()) {
                icon.setResource(mFolderIcon);
            } else {
                if (nameFile.isPlaylist())
                    icon.setResource(mPlaylistIcon);
                else
                    icon.setResource(mCDIcon);
            }

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(nameFile.getName(), 40));
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

        private PathScreen mParent;

        private Tracker mTracker;

        private boolean mFirst;

        private FormatString mFormatString;

        private int mLevel;
    }

    public class MusicScreen extends DefaultScreen {

        private BList list;

        public MusicScreen(MusicOrganizer app) {
            super(app, "Song", true);

            below.setResource(mInfoBackground);

            mMusicInfo = new MusicInfo(this.normal, BORDER_LEFT, TOP - 30, BODY_WIDTH, BODY_HEIGHT
                    - (TOP - 30 - SAFE_TITLE_V), true);

            list = new DefaultOptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 80, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Play");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            below.setResource(mInfoBackground);
            updateView();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            Audio audio = currentAudio();
            if (audio != null) {
                mMusicInfo.setAudio(audio);
            }
        }

        public boolean handleExit() {
            mMusicInfo.clearResource();
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            if (mMusicInfo.handleKeyPress(code, rawcode))
                return true;
            Audio audio = currentAudio();
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (list.getFocus() == 0) {
                    postEvent(new BEvent.Action(this, "play"));
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
                Item nameFile = (Item) mTracker.getList().get(pos);
                while (nameFile.isFolder() || nameFile.isPlaylist()) {
                    pos = mTracker.getNextPos();
                    nameFile = (Item) mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                Item nameFile = (Item) mTracker.getList().get(pos);
                while (nameFile.isFolder() || nameFile.isPlaylist()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (Item) mTracker.getList().get(pos);
                }
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("play")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        getBApp().push(new PlayerScreen((MusicOrganizer) getBApp(), mTracker), TRANSITION_LEFT);
                        getBApp().flush();
                    }
                }.start();
                return true;
            }

            return super.handleAction(view, action);
        }

        public void setTracker(Tracker value) {
            mTracker = value;
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
                } catch (Exception ex) {
                    Tools.logException(MusicOrganizer.class, ex);
                }
            }
            return null;
        }

        private MusicInfo mMusicInfo;

        private Tracker mTracker;
    }

    public class PlayerScreen extends DefaultScreen {

        public PlayerScreen(MusicOrganizer app, Tracker tracker) {
            super(app, true);

            below.setResource(mPlayerBackground);

            mTracker = tracker;

            setTitle(" ");

            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
            if (musicPlayerConfiguration.isShowImages())
                setFooter("Press INFO for lyrics, 0 for images");
            else
                setFooter("Press INFO for lyrics");

            app.setTracker(mTracker);
            getPlayer().startTrack();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            new Thread() {
                public void run() {
                    mBusy.setVisible(true);
                    mBusy.flush();

                    synchronized (this) {
                        setPainting(false);
                        try {
                            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                                    .getMusicPlayerConfiguration();
                            if (musicPlayerConfiguration.getPlayer().equals(MusicPlayerConfiguration.CLASSIC))
                                player = new MusicPlayer(PlayerScreen.this, BORDER_LEFT, SAFE_TITLE_V, BODY_WIDTH,
                                        BODY_HEIGHT - 20, false, (DefaultApplication) getApp(), mTracker);
                            else
                                player = new WinampPlayer(PlayerScreen.this, 0, 0, PlayerScreen.this.width,
                                        PlayerScreen.this.height, false, (DefaultApplication) getApp(), mTracker);
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
            setPainting(false);
            try {
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
            if (transparency != 0.0f)
                setTransparency(0.0f);
            switch (code) {
            case KEY_INFO:
                getBApp().play("select.snd");
                getBApp().flush();
                LyricsScreen lyricsScreen = new LyricsScreen((MusicOrganizer) getBApp(), mTracker);
                getBApp().push(lyricsScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            case KEY_NUM0:
                MusicOrganizerConfiguration musicConfiguration = (MusicOrganizerConfiguration) ((MusicOrganizerFactory) context.factory)
                        .getAppContext().getConfiguration();
                MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
                if (musicPlayerConfiguration.isShowImages()) {
                    getBApp().play("select.snd");
                    getBApp().flush();
                    ImagesScreen imagesScreen = new ImagesScreen((MusicOrganizer) getBApp(), mTracker);
                    getBApp().push(imagesScreen, TRANSITION_LEFT);
                    getBApp().flush();
                    return true;
                } else
                    return false;
            }
            return super.handleKeyPress(code, rawcode);
        }

        //private WinampPlayer player;

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
            Tools.logException(MusicOrganizer.class, ex);
        }

        if (audio == null) {
            try {
                audio = (Audio) MediaManager.getMedia(path);
                AudioManager.createAudio(audio);
            } catch (Exception ex) {
                Tools.logException(MusicOrganizer.class, ex);
            }
        }
        return audio;
    }

    public class LyricsScreen extends DefaultScreen {
        private BList list;

        public LyricsScreen(MusicOrganizer app, Tracker tracker) {
            super(app, "Lyrics", false);

            below.setResource(mLyricsBackground);

            mTracker = tracker;

            scrollText = new ScrollText(normal, SAFE_TITLE_H, TOP, BODY_WIDTH - 10, height - SAFE_TITLE_V - TOP - 70,
                    "");
            scrollText.setVisible(false);

            setFocusDefault(scrollText);

            //setFooter("lyrc.com.ar");
            setFooter("lyrictracker.com");

            mBusy.setVisible(true);

            list = new DefaultOptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 60, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            //list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
            list.add("Back to player");
            setFocusDefault(list);
        }

        public void updateLyrics() {
            setPainting(false);
            try {
                if (mLyricsThread != null && mLyricsThread.isAlive())
                    mLyricsThread.interrupt();
            } finally {
                setPainting(true);
            }
            Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
            Audio audio = null;
            try {
                List list = null;
                if (nameFile.isFile())
                    list = AudioManager.findByPath(((File) nameFile.getValue()).getCanonicalPath());
                else
                    list = AudioManager.findByPath((String) nameFile.getValue());
                if (list != null && list.size() > 0) {
                    audio = (Audio) list.get(0);
                }
            } catch (Exception ex) {
                Tools.logException(MusicOrganizer.class, ex);
            }
            if (audio.getLyrics() != null && audio.getLyrics().length() > 0) {
                setPainting(false);
                try {
                    mBusy.setVisible(false);
                    getBApp().flush();
                    scrollText.setVisible(true);
                    scrollText.setText(audio.getLyrics());
                    getBApp().flush();
                } finally {
                    setPainting(true);
                }
            } else {
                final Audio lyricsAudio = audio;

                mLyricsThread = new Thread() {
                    public void run() {
                        try {
                            String lyrics = Lyrics.getLyrics(lyricsAudio.getTitle(), lyricsAudio.getArtist());
                            if (lyrics == null || lyrics.trim().length() == 0) {
                                lyrics = "Lyrics not found";
                            } else {
                                synchronized (this) {
                                    try {
                                        lyricsAudio.setLyrics(lyrics);
                                        AudioManager.updateAudio(lyricsAudio);
                                    } catch (Exception ex) {
                                        Tools.logException(MusicOrganizer.class, ex, "Could not update lyrics");
                                    }
                                }
                            }
                            synchronized (this) {
                                setPainting(false);
                                try {
                                    mBusy.setVisible(false);
                                    getBApp().flush();
                                    scrollText.setVisible(true);
                                    scrollText.setText(lyrics);
                                    getBApp().flush();
                                } finally {
                                    setPainting(true);
                                }
                            }
                        } catch (Exception ex) {
                            Tools.logException(MusicOrganizer.class, ex, "Could retrieve lyrics");
                        }
                    }

                    public void interrupt() {
                        synchronized (this) {
                            super.interrupt();
                        }
                    }
                };
                mLyricsThread.start();
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            updateLyrics();

            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            setPainting(false);
            try {
                if (mLyricsThread != null && mLyricsThread.isAlive()) {
                    mLyricsThread.interrupt();
                    mLyricsThread = null;
                }
            } finally {
                setPainting(true);
            }
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT: // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_UP:
            case KEY_DOWN:
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                scrollText.handleKeyPress(code, rawcode);
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        private ScrollText scrollText;

        private Thread mLyricsThread;

        private Tracker mTracker;
    }

    public class ImagesScreen extends DefaultScreen {
        private BList list;

        public ImagesScreen(MusicOrganizer app, Tracker tracker) {
            super(app, "Images", true);

            below.setResource(mImagesBackground);

            mTracker = tracker;

            mImageView = new BView(this.normal, BORDER_LEFT, TOP, BODY_WIDTH, height - SAFE_TITLE_V - TOP - 75);
            mImageView.setVisible(false);

            mPosText = new BText(normal, BORDER_LEFT, height - SAFE_TITLE_V - 60, BODY_WIDTH, 30);
            mPosText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mPosText.setFont("default-18-bold.font");
            mPosText.setColor(Color.CYAN);
            mPosText.setShadow(true);

            mUrlText = new BText(normal, SAFE_TITLE_H, height - SAFE_TITLE_V - 78, BODY_WIDTH, 15);
            mUrlText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
            mUrlText.setFont("default-12-bold.font");
            mUrlText.setColor(Color.WHITE);
            mUrlText.setShadow(true);

            setFooter("search.yahoo.com");

            mBusy.setVisible(true);

            list = new DefaultOptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 60, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            //list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
            list.add("Back to player");
            setFocusDefault(list);
        }

        public void updateImage() {
            Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
            Audio audio = null;
            try {
                List list = null;
                if (nameFile.isFile())
                    list = AudioManager.findByPath(((File) nameFile.getValue()).getCanonicalPath());
                else
                    list = AudioManager.findByPath((String) nameFile.getValue());
                if (list != null && list.size() > 0) {
                    audio = (Audio) list.get(0);
                }
            } catch (Exception ex) {
                Tools.logException(MusicOrganizer.class, ex);
            }
            final Audio lyricsAudio = audio;

            mImageThread = new Thread() {
                public void run() {
                    try {
                        synchronized (this) {
                            mBusy.setVisible(true);
                            getBApp().flush();
                        }

                        if (mResults == null || mResults.size() == 0) {
                            mResults = Yahoo.getImages("\"" + lyricsAudio.getArtist() + "\" music");
                            mPos = 0;
                        }
                        if (mResults.size() == 0) {
                            synchronized (this) {
                                setPainting(false);
                                try {
                                    mBusy.setVisible(false);
                                    getBApp().flush();
                                } finally {
                                    setPainting(true);
                                }
                            }
                            return;
                        }

                        NameValue nameValue = (NameValue) mResults.get(mPos);
                        BufferedImage image = Tools.getImage(new URL(nameValue.getValue()), -1, -1);

                        if (image != null) {
                            synchronized (this) {
                                setPainting(false);
                                try {
                                    if (mImageView.resource != null)
                                        mImageView.resource.remove();
                                    mUrlText.setValue(nameValue.getName());
                                    mImageView.setVisible(true);
                                    mImageView.setTransparency(1f);
                                    mImageView.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                    mImageView.setTransparency(0f, getResource("*500"));
                                    image.flush();
                                    image = null;
                                } finally {
                                    setPainting(true);
                                }
                            }
                        } else {
                            mResults.remove(mPos);
                        }

                    } catch (Exception ex) {
                        Tools.logException(MusicOrganizer.class, ex, "Could not retrieve image");
                        mResults.remove(mPos);
                    } finally {
                        synchronized (this) {
                            setPainting(false);
                            try {
                                if (mResults != null && mResults.size() > 0)
                                    mPosText.setValue(String.valueOf(mPos + 1) + " of "
                                            + String.valueOf(mResults.size()));
                                else
                                    mPosText.setValue("No images found");
                                mBusy.setVisible(false);
                            } finally {
                                setPainting(true);
                            }
                            getBApp().flush();
                        }
                    }
                }

                public void interrupt() {
                    synchronized (this) {
                        super.interrupt();
                    }
                }
            };
            mImageThread.start();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            updateImage();

            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            setPainting(false);
            try {
                if (mImageThread != null && mImageThread.isAlive()) {
                    mImageThread.interrupt();
                    mImageThread = null;
                    mResults.clear();
                    mResults = null;
                }
            } finally {
                setPainting(true);
            }
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT: // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_UP:
            case KEY_DOWN:
            case KEY_CHANNELUP:
                if (mResults != null && mResults.size() > 0) {
                    getBApp().play("pageup.snd");
                    getBApp().flush();
                    mPos = mPos - 1;
                    if (mPos == -1)
                        mPos = mResults.size() - 1;
                }
                updateImage();
                return true;
            case KEY_CHANNELDOWN:
                if (mResults != null && mResults.size() > 0) {
                    getBApp().play("pagedown.snd");
                    getBApp().flush();
                    mPos = mPos + 1;
                    if (mPos == mResults.size())
                        mPos = 0;
                }
                updateImage();
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        private BView mImageView;

        private Thread mImageThread;

        private Tracker mTracker;

        private List mResults;

        private int mPos;

        private BText mPosText;

        private BText mUrlText;
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
                    Tools.logException(MusicOrganizer.class, ex2);
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

    public static final String[] FIELDS = { "Song", "Artist", "Album", "Year", "Track", "Genre", "Bitrate", "Duration" };

    private static String getAudioValue(String value, Audio audio) {
        if (value.equals("title"))
            return audio.getTitle();
        else if (value.equals("artist"))
            return audio.getArtist();
        else if (value.equals("album"))
            return audio.getAlbum();
        else if (value.equals("date"))
            return new Integer(audio.getDate()).toString();
        else if (value.equals("track"))
            return Integer.toString(audio.getTrack());
        else if (value.equals("genre"))
            return audio.getGenre();
        else if (value.equals("bitRate"))
            return Integer.toString(audio.getBitRate());
        else if (value.equals("duration"))
            return new Long(audio.getDuration()).toString();
        else
            return audio.getTitle();
    }

    private static boolean isString(String value) {
        if (value.equals("title"))
            return true;
        else if (value.equals("artist"))
            return true;
        else if (value.equals("album"))
            return true;
        else if (value.equals("date"))
            return false;
        else if (value.equals("track"))
            return false;
        else if (value.equals("genre"))
            return true;
        else if (value.equals("bitRate"))
            return false;
        else if (value.equals("duration"))
            return false;
        else
            return true;
    }

    private static String getAudioField(String value) {
        if (value.equals(FIELDS[0]))
            return "title";
        else if (value.equals(FIELDS[1]))
            return "artist";
        else if (value.equals(FIELDS[2]))
            return "album";
        else if (value.equals(FIELDS[3]))
            return "date";
        else if (value.equals(FIELDS[4]))
            return "track";
        else if (value.equals(FIELDS[5]))
            return "genre";
        else if (value.equals(FIELDS[6]))
            return "bitRate";
        else if (value.equals(FIELDS[7]))
            return "duration";
        else
            return "title";
    }

    private static int count(String base, String searchFor) {
        int len = searchFor.length();
        int result = 0;

        if (len > 0) {
            int start = base.indexOf(searchFor);
            while (start != -1) {
                result++;
                start = base.indexOf(searchFor, start + len);
            }
        }
        return result;
    }

    private static String clean(String value) {
        value = value.replace('/', ' ');
        value = value.replace('?', ' ');
        value = value.replace('%', ' ');
        value = value.replace('=', ' ');
        if (value.trim().length() == 0)
            return "unknown";
        return value;
    }

    // Remove "a", "an", "the" from titles
    private static String removeLeading(String value) {
        String lower = value.toLowerCase();
        if (lower.startsWith("an "))
            return value.substring("an ".length());
        else if (lower.startsWith("a "))
            return value.substring("a ".length());
        else if (lower.startsWith("the "))
            return value.substring("the ".length());
        return value;
    }

    // Find key for sorting by decimal value
    private static String getDeciKey(String value, NameValue nameValue) {
        String number = "";
        if (value.length() == 0) {
            String criteria = "(audio." + nameValue.getName() + "=0 OR audio." + nameValue.getName() + "=-1)";
            nameValue.setValue(criteria);
            number = "-";
            return number;
        } else if (!Character.isDigit(value.charAt(0))) {
            String criteria = "(audio." + nameValue.getName() + "=0 OR audio." + nameValue.getName() + "=-1)";
            nameValue.setValue(criteria);
            number = "-";
            return number;
        } else
            for (int i = 0; i < value.length(); i++) {
                if (Character.isDigit(value.charAt(i))) {
                    number = number + value.charAt(i);
                } else
                    break;
            }
        String criteria = "(audio." + nameValue.getName() + "=" + number + ")";
        nameValue.setValue(criteria);
        return number;
    }

    //   Find key for sorting by decade
    private static String getDecadeKey(String value, NameValue nameValue) {
        String number = getDeciKey(value, nameValue);
        String decade = "-";
        try {
            double year = Double.parseDouble(number);
            if (year <= 1949) {
                String criteria = "(audio." + nameValue.getName() + ">=0 AND audio." + nameValue.getName() + "<=1949)";
                nameValue.setValue(criteria);
                decade = "0000-1949";
            } else if (year >= 1950 && year <= 1959) {
                String criteria = "(audio." + nameValue.getName() + ">=1950 AND audio." + nameValue.getName()
                        + "<=1959)";
                nameValue.setValue(criteria);
                decade = "1950-1959";
            } else if (year >= 1960 && year <= 1969) {
                String criteria = "(audio." + nameValue.getName() + ">=1960 AND audio." + nameValue.getName()
                        + "<=1969)";
                nameValue.setValue(criteria);
                decade = "1960-1969";
            } else if (year >= 1970 && year <= 1979) {
                String criteria = "(audio." + nameValue.getName() + ">=1970 AND audio." + nameValue.getName()
                        + "<=1979)";
                nameValue.setValue(criteria);
                decade = "1970-1979";
            } else if (year >= 1980 && year <= 1989) {
                String criteria = "(audio." + nameValue.getName() + ">=1980 AND audio." + nameValue.getName()
                        + "<=1989)";
                nameValue.setValue(criteria);
                decade = "1980-1989";
            } else if (year >= 1990 && year <= 1999) {
                String criteria = "(audio." + nameValue.getName() + ">=1990 AND audio." + nameValue.getName()
                        + "<=1999)";
                nameValue.setValue(criteria);
                decade = "1990-1999";
            } else if (year >= 2000 && year <= 2009) {
                String criteria = "(audio." + nameValue.getName() + ">=2000 AND audio." + nameValue.getName()
                        + "<=2009)";
                nameValue.setValue(criteria);
                decade = "2000-2009";
            } else if (year >= 2010 && year <= 2019) {
                String criteria = "(audio." + nameValue.getName() + ">=2010 AND audio." + nameValue.getName()
                        + "<=2019)";
                nameValue.setValue(criteria);
                decade = "2010-2019";
            }
            return decade;
        } catch (NumberFormatException e) {
        }

        String criteria = "(audio." + nameValue.getName() + "=0)";
        nameValue.setValue(criteria);
        return decade;
    }

    //   Find key for sorting by duration
    private static String getDurationKey(String value, NameValue nameValue) {
        String number = getDeciKey(value, nameValue);
        String result = "-";
        try {
            double duration = Double.parseDouble(number);
            double minutes = duration / (1000 * 60);
            if (minutes % 1 >= 0.5)
                result = Integer.toString((int) minutes / 1 + 1);
            else
                result = Integer.toString((int) minutes / 1);
            String criteria = "(audio." + nameValue.getName() + "=" + result + ")";
            nameValue.setValue(criteria);
            return result;
        } catch (NumberFormatException e) {
        }

        return "-";
    }

    //   Find key for sorting by alphabetical value
    private static String getAlphaKey(String value, NameValue nameValue) {
        value = value.toUpperCase();
        if ((value.charAt(0) >= 'A' && value.charAt(0) <= 'Z') || (value.charAt(0) >= 'a' && value.charAt(0) <= 'z')) {
            String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))='" + value.charAt(0) + "')";
            nameValue.setValue(criteria);
            return String.valueOf(value.charAt(0));
        } else {
            nameValue.setValue("1=1");
            return "-";
        }
    }

    //   Find key for sorting by alphabetical value
    private static String getGroupedAlphaKey(String value, NameValue nameValue) {
        String key = "-";
        if (value.length() > 0) {
            if (((value.charAt(0) >= 'A') && (value.charAt(0) <= 'C'))
                    || ((value.charAt(0) >= 'a') && (value.charAt(0) <= 'c'))) {
                key = "A-C";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='A' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='C')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'D') && (value.charAt(0) <= 'F'))
                    || ((value.charAt(0) >= 'd') && (value.charAt(0) <= 'f'))) {
                key = "D-F";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='D' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='F')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'G') && (value.charAt(0) <= 'I'))
                    || ((value.charAt(0) >= 'g') && (value.charAt(0) <= 'i'))) {
                key = "G-I";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='G' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='I')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'J') && (value.charAt(0) <= 'L'))
                    || ((value.charAt(0) >= 'j') && (value.charAt(0) <= 'l'))) {
                key = "J-L";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='J' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='L')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'M') && (value.charAt(0) <= 'O'))
                    || ((value.charAt(0) >= 'm') && (value.charAt(0) <= 'o'))) {
                key = "M-O";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='M' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='O')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'P') && (value.charAt(0) <= 'R'))
                    || ((value.charAt(0) >= 'p') && (value.charAt(0) <= 'r'))) {
                key = "P-R";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='P' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='R')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'S') && (value.charAt(0) <= 'U'))
                    || ((value.charAt(0) >= 's') && (value.charAt(0) <= 'u'))) {
                key = "S-U";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='S' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='U')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'V') && (value.charAt(0) <= 'X'))
                    || ((value.charAt(0) >= 'v') && (value.charAt(0) <= 'x'))) {
                key = "V-X";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='V' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='X')";
                nameValue.setValue(criteria);
            } else if (((value.charAt(0) >= 'Y') && (value.charAt(0) <= 'Z'))
                    || ((value.charAt(0) >= 'y') && (value.charAt(0) <= 'z'))) {
                key = "Y-Z";
                String criteria = "(upper(substr(audio." + nameValue.getName() + ",1,1))>='Y' AND upper(substr(audio."
                        + nameValue.getName() + ",1,1))<='Z')";
                nameValue.setValue(criteria);
            }
        }
        return key;
    }

    //   Find key for sorting by surname
    private static String getSurnameKey(String value, NameValue nameValue) {
        // Only consider values that are in the Firstname Surname format
        Pattern pattern = Pattern.compile("^([^\\s]*)\\s([^\\s]*)$");
        Matcher matcher = pattern.matcher(value);
        if (matcher.find()) {
            value = matcher.group(2) + " " + matcher.group(1);
        }
        return getAlphaKey(value, nameValue);
    }

    public static class FormatString {
        public FormatString(String value) {
            mCount = count(value, "\\");
            String mask = "";
            for (int i = 0; i <= mCount; i++) {
                if (mask.length() == 0)
                    mask = "([^\\\\]*)";
                else
                    mask = mask + "\\\\([^\\\\]*)";
            }

            Pattern pattern = Pattern.compile(mask);
            mMatcher = pattern.matcher(value);
            mMatcher.find();
        }

        public int getCount() {
            return mCount + 1;
        }

        public String getPart(int i) {
            return mMatcher.group(i);
        }

        private Matcher mMatcher;

        private int mCount;
    }

    public static class QueryPart extends FolderItem {
        public QueryPart(java.util.List fields, String format) {
            super(format, fields);
        }

        public java.util.List getFields() {
            return (java.util.List) getValue();
        }

        private String getFormat() {
            return getName();
        }

        public String toString() {
            return getName();
        }
    }

    private List getItems(PathScreen pathScreen, FormatString formatString, int level) {
        Pattern formatPattern1 = Pattern.compile("([^\\{\\}]*)\\{([^\\{\\}]*)\\}");
        List results = new ArrayList();
        try {
            Transaction tx = null;
            Session session = HibernateUtil.openSession();
            try {
                String part = formatString.getPart(level);

                // Which fields are needed?
                ArrayList columns = new ArrayList();
                int c = 0;
                while (c < part.length()) {
                    String sub = part.substring(c);
                    boolean found = false;
                    for (int k = 0; k < FIELDS.length; k++) {
                        if (sub.startsWith(FIELDS[k])) {
                            columns.add(getAudioField(FIELDS[k]));
                            c = c + FIELDS[k].length();
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        c++;
                    }
                }

                String view = "";
                String order = "";
                for (int f = 0; f < columns.size(); f++) {
                    String column = (String) columns.get(f);
                    if (view.length() == 0) {
                        view = "audio." + column;
                        order = view;
                    } else
                        view = view + ", audio." + column;
                }

                String restrictions = "(1=1)";
                List conditions = new ArrayList();
                pathScreen.getConditions(conditions);
                Iterator conditionsIterator = conditions.iterator();
                while (conditionsIterator.hasNext()) {
                    QueryPart queryPart = (QueryPart) conditionsIterator.next();
                    for (int q = 0; q < queryPart.getFields().size(); q++) {
                        NameValue nameValue = (NameValue) queryPart.getFields().get(q);
                        boolean needQuote = isString(nameValue.getName());
                        restrictions = restrictions + " AND " + nameValue.getValue();
                    }
                }

                String sorter = null;

                // Artist{A-Z}
                Matcher formatMatcher1 = formatPattern1.matcher(part);
                if (formatMatcher1.find()) {
                    part = formatMatcher1.group(1);
                    sorter = formatMatcher1.group(2);
                }

                tx = session.beginTransaction();

                Query query = null;
                if (level == formatString.getCount()) {
                    // Retrieve fields
                    String queryString = "from org.lnicholls.galleon.database.Audio audio where " + restrictions
                            + "AND substr(audio.path,1,4)<>'http' order by " + order + " asc";
                    log.debug(queryString);
                    query = session.createQuery(queryString).setCacheable(true);
                } else {
                    // Retrieve fields
                    String queryString = "select distinct " + view
                            + " from org.lnicholls.galleon.database.Audio audio where " + restrictions
                            + " AND substr(audio.path,1,4)<>'http' order by " + order + " asc";
                    log.debug(queryString);
                    query = session.createQuery(queryString).setCacheable(true);
                }

                Audio audio = null;
                ScrollableResults items = query.scroll();
                if (items.first()) {
                    items.beforeFirst();
                    while (items.next()) {
                        Object[] values = new Object[columns.size()];
                        if (level == formatString.getCount()) {
                            audio = (Audio) items.get(0);
                            for (int f = 0; f < columns.size(); f++) {
                                values[f] = getAudioValue((String) columns.get(f), audio);
                            }
                        } else {
                            if (columns.size() == 1) {
                                values[0] = items.get(0).toString();
                            } else {
                                for (int f = 0; f < columns.size(); f++) {
                                    values[f] = items.get(f).toString();
                                }
                            }
                        }

                        // Song [Album]
                        String formatted = "";
                        ArrayList fields = new ArrayList();
                        int column = 0;
                        int j = 0;
                        while (j < part.length()) {
                            String sub = part.substring(j);
                            boolean found = false;
                            for (int k = 0; k < FIELDS.length; k++) {
                                if (sub.startsWith(FIELDS[k])) {
                                    String value = (String) values[column++];
                                    formatted = formatted + value;
                                    String field = getAudioField(FIELDS[k]);
                                    boolean needQuote = isString(field);
                                    fields.add(new NameValue(field, "(audio." + field + "=" + (needQuote ? "'" : "")
                                            + value + (needQuote ? "'" : "") + ")"));
                                    j = j + FIELDS[k].length();
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                formatted = formatted + part.charAt(j);
                                j++;
                            }
                        }
                        // TODO Handle this case
                        //formatted = removeLeading(formatted);

                        if (sorter != null) {
                            NameValue nameValue = (NameValue) fields.get(fields.size() - 1);
                            if (sorter.equals("A-Z"))
                                formatted = getAlphaKey(formatted, nameValue);
                            else if (sorter.equals("ABC-XYZ"))
                                formatted = getGroupedAlphaKey(formatted, nameValue);
                            else if (sorter.equals("Surname"))
                                formatted = getSurnameKey(formatted, nameValue);
                            else if (sorter.equals("0-9"))
                                formatted = getDeciKey(formatted, nameValue);
                            else if (sorter.equals("Decade"))
                                formatted = getDecadeKey(formatted, nameValue);
                            else if (sorter.equals("Duration"))
                                formatted = getDurationKey(formatted, nameValue);
                        }

                        if (level == formatString.getCount()) {
                            if (audio.getPath().startsWith("http")) {
                                // TODO Handle steams; make configurable
                                results.add(new Item(formatted, audio.getPath()));
                            } else
                                results.add(new FileItem(formatted, new File(audio.getPath())));
                        } else {
                            boolean duplicate = false;
                            for (Iterator iterator = results.iterator(); iterator.hasNext();) {
                                QueryPart queryPart = (QueryPart) iterator.next();
                                if (queryPart.getName().equals(formatted)) {
                                    duplicate = true;
                                    break;
                                }
                            }
                            if (!duplicate) {
                                QueryPart queryPart = new QueryPart(fields, formatted);
                                results.add(queryPart);
                            }
                        }
                    }
                }

                tx.commit();
            } catch (HibernateException he) {
                if (tx != null)
                    tx.rollback();
                throw he;
            } finally {
                HibernateUtil.closeSession();
            }
        } catch (Exception ex) {
            Tools.logException(MusicOrganizer.class, ex);
        }
        if (level != formatString.getCount()) {
            Item[] items = (Item[]) results.toArray(new Item[0]);
            Arrays.sort(items, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Item item1 = (Item) o1;
                    Item item2 = (Item) o2;
    
                    return item1.toString().compareTo(item2.toString());
                }
            });
            results.clear();
            for (int i=0;i<items.length;i++)
                results.add(items[i]);
        }
        return results;
    }

    public static class MusicOrganizerFactory extends AppFactory {

        public MusicOrganizerFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            MusicOrganizerConfiguration musicConfiguration = (MusicOrganizerConfiguration) getAppContext()
                    .getConfiguration();

            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
            String skin = null;
            if (musicPlayerConfiguration == null || musicPlayerConfiguration.getSkin() == null) {
                List skins = Server.getServer().getWinampSkins();
                skin = ((File) skins.get(0)).getAbsolutePath();
            } else
                skin = musicPlayerConfiguration.getSkin();
            mClassicSkin = new ClassicSkin(skin);

            new Thread() {
                public void run() {
                    try {
                        if (mMediaRefreshThread == null || !mMediaRefreshThread.isAlive()) {
                            updatePaths();
                        }
                        sleep(1000 * 60 * 60 * 6);
                    } catch (Exception ex) {
                        log.error("Could not download stations", ex);
                    }
                }
            }.start();
        }

        public void setAppContext(AppContext appContext) {
            super.setAppContext(appContext);

            updatePaths();
        }

        private void updatePaths() {
            try {
                if (mMediaRefreshThread != null && mMediaRefreshThread.isAlive()) {
                    mMediaRefreshThread.interrupt();
                }
                mMediaRefreshThread = new MediaRefreshThread();

                MusicOrganizerConfiguration musicConfiguration = (MusicOrganizerConfiguration) getAppContext()
                        .getConfiguration();

                for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
                    String path = (String) i.next();
                    mMediaRefreshThread.addPath(new MediaRefreshThread.PathInfo(path,
                            FileFilters.audioFileDirectoryFilter));
                }
                mMediaRefreshThread.start();
            } catch (Exception ex) {
                Tools.logException(MusicOrganizer.class, ex, "Refresh failed");
            }
        }

    }

    private static ClassicSkin mClassicSkin;

    private static MediaRefreshThread mMediaRefreshThread;
}