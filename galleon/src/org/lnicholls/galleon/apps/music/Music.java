package org.lnicholls.galleon.apps.music;

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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.Media;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.util.FileSystemContainer.NameFile;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.ScrollText;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;
import com.tivo.hme.util.ArgumentList;

public class Music extends DefaultApplication {

    private static Logger log = Logger.getLogger(Music.class.getName());

    public final static String TITLE = "Music";

    private Resource mFolderIcon;

    private Resource mCDIcon;
    
    private Resource mPlaylistIcon;

    private MusicScreen mMusicScreen;

    protected void init(Context context) {
        super.init(context);

        mFolderIcon = getResource("folder.png");

        mCDIcon = getResource("cd.png");
        
        mPlaylistIcon = getResource("playlist.png");

        mMusicScreen = new MusicScreen(this);

        MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory).getAppContext()
                .getConfiguration();

        if (musicConfiguration.getPaths().size() == 1) {
            try {
                NameValue nameValue = (NameValue) musicConfiguration.getPaths().get(0);
                NameFile nameFile = new NameFile(nameValue.getName(), new File(nameValue.getValue()));
                FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile().getCanonicalPath());
                setCurrentDirectory(nameFile.getFile().getCanonicalPath());
                Tracker tracker = new Tracker(fileSystemContainer.getItems(FileFilters.audioDirectoryFilter), 0);
                PathScreen pathScreen = new PathScreen(this, tracker, true);
                push(pathScreen, TRANSITION_LEFT);
            } catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
        } else
            push(new MusicMenuScreen(this), TRANSITION_NONE);
    }

    public class MusicMenuScreen extends DefaultMenuScreen {
        public MusicMenuScreen(Music app) {
            super(app, "Music");

            MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory)
                    .getAppContext().getConfiguration();

            for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                mMenuList.add(new NameFile(nameValue.getName(), new File(nameValue.getValue())));
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            NameFile nameFile = (NameFile) (mMenuList.get(mMenuList.getFocus()));
                            FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                    .getCanonicalPath());
                            ((DefaultApplication) getBApp()).setCurrentDirectory(nameFile.getFile().getCanonicalPath());
                            Tracker tracker = new Tracker(fileSystemContainer
                                    .getItems(FileFilters.audioDirectoryFilter), 0);
                            PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                            getBApp().push(pathScreen, TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Music.class, ex);
                        }
                    }
                }.start();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameFile nameFile = (NameFile) mMenuList.get(index);
            if (nameFile.getFile().isDirectory()) {
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

        public PathScreen(Music app, Tracker tracker) {
            this(app, tracker, false);
        }
        
        public PathScreen(Music app, Tracker tracker, boolean first) {
            super(app, "Music");

            mTracker = tracker;
            mFirst = first;

            Iterator iterator = mTracker.getList().iterator();
            while (iterator.hasNext()) {
                NameFile nameFile = (NameFile) iterator.next();
                mMenuList.add(nameFile);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                final NameFile nameFile = (NameFile) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.getFile().isDirectory()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                        .getCanonicalPath());
                                ((DefaultApplication) getBApp()).setCurrentDirectory(nameFile.getFile()
                                        .getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer
                                        .getItems(FileFilters.audioDirectoryFilter), 0);
                                PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex);
                            }
                        }
                    }.start();
                } else {
                    if (FileFilters.playlistFilter.accept(nameFile.getFile()))
                    {
                        try
                        {
                            Playlist playlist = (Playlist)MediaManager.getMedia(nameFile.getFile().getCanonicalPath());
                            Tracker tracker = new Tracker(playlist.getList(), 0);
                            PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                            getBApp().push(pathScreen, TRANSITION_LEFT);
                            getBApp().flush();
                        }
                        catch (Exception ex) 
                        {
                            ex.printStackTrace();
                        }
                    }
                    else
                    {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());
                                    mMusicScreen.setTracker(mTracker);
    
                                    getBApp().push(mMusicScreen, TRANSITION_LEFT);
                                    getBApp().flush();
                                } catch (Exception ex) {
                                    Tools.logException(Music.class, ex);
                                }
                            }
                        }.start();
                    }
                }

                return true;
            }
            else
            if (action.equals("play")) {
                load();
                final NameFile nameFile = (NameFile) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.getFile().isDirectory()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                        .getCanonicalPath(), true);
                                ((DefaultApplication) getBApp()).setCurrentDirectory(nameFile.getFile()
                                        .getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer
                                        .getItems(FileFilters.audioDirectoryFilter), 0);
                                
                                getBApp().push(new PlayerScreen((Music) getBApp(), tracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex);
                            }
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                getBApp().push(new PlayerScreen((Music) getBApp(), mTracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex);
                            }
                        }
                    }.start();
                }                
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameFile nameFile = (NameFile) mMenuList.get(index);
            if (nameFile.getFile().isDirectory()) {
                icon.setResource(mFolderIcon);
            } else {
                if (FileFilters.playlistFilter.accept(nameFile.getFile()))
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

        private Tracker mTracker;
        
        private boolean mFirst;
    }

    public class MusicScreen extends DefaultScreen {

        private BList list;

        public MusicScreen(Music app) {
            super(app, "Song", true);

            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("mm:ss");

            int start = TOP;

            mCover = new BView(below, width - SAFE_TITLE_H - 210, height - SAFE_TITLE_V - 200, 200, 200, false);

            mTitleText = new BText(normal, BORDER_LEFT, start - 30, BODY_WIDTH, 70);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mTitleText.setFont("default-30-bold.font");
            mTitleText.setColor(Color.CYAN);
            mTitleText.setShadow(true);

            start += 40;

            mSongText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mSongText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mSongText.setFont("default-18-bold.font");
            mSongText.setShadow(true);

            mDurationText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mDurationText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mDurationText.setFont("default-18-bold.font");
            mDurationText.setShadow(true);

            start += 20;

            mAlbumText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mAlbumText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mAlbumText.setFont("default-18-bold.font");
            mAlbumText.setShadow(true);

            mYearText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mYearText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mYearText.setFont("default-18-bold.font");
            mYearText.setShadow(true);

            start += 20;

            mArtistText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mArtistText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mArtistText.setFont("default-18-bold.font");
            mArtistText.setShadow(true);

            mGenreText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            mGenreText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mGenreText.setFont("default-18-bold.font");
            mGenreText.setShadow(true);

            mStars = new BView[5];
            for (int i = 0; i < 5; i++) {
                mStars[i] = new BView(normal, BORDER_LEFT + (i * 40), height - SAFE_TITLE_V - 200, 34, 34, true);
                mStars[i].setResource(mStarIcon, RSRC_IMAGE_BESTFIT);
                mStars[i].setTransparency(0.6f);
            }

            list = new DefaultOptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 80, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Play");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            updateView();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            final Audio audio = currentAudio();
            if (audio != null) {
                setPainting(false);
                try {

                    clearCover();

                    File file = new File(audio.getPath());
                    String name = Tools.extractName(file.getName());
                    mTitleText.setValue(name);
                    mSongText.setValue("Song: " + Tools.trim(audio.getTitle(), 40));
                    mDurationText.setValue("Duration: " + mTimeFormat.format(new Date(audio.getDuration())));
                    mAlbumText.setValue("Album: " + Tools.trim(audio.getAlbum(), 40));
                    mYearText.setValue("Year: " + String.valueOf(audio.getDate()));
                    mArtistText.setValue("Artist: " + Tools.trim(audio.getArtist(), 40));
                    mGenreText.setValue("Genre: " + audio.getGenre());

                    setRating();

                    updateHints();

                    final MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory)
                            .getAppContext().getConfiguration();
                    setPainting(false);
                    try {
                        if (mCoverThread != null && mCoverThread.isAlive())
                            mCoverThread.interrupt();
                    }
                    finally
                    {
                        setPainting(true);
                    }
                    mCoverThread = new Thread() {
                        public void run() {
                            try {
                                java.awt.Image image = Mp3File.getCover(audio, musicConfiguration.isUseAmazon(),
                                        musicConfiguration.isUseFile());
                                if (image != null) {
                                    synchronized(this)
                                    {
                                        mCover.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                        mCover.setVisible(true);
                                        mCover.setTransparency(1.0f);
                                        mCover.setTransparency(0.0f, mAnim);
                                        getBApp().flush();
                                    }
                                }
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex, "Could retrieve cover");
                            }
                        }
                        
                        public void interrupt()
                        {
                            synchronized (this)
                            {
                                super.interrupt();
                            }
                        }
                    };
                    mCoverThread.start();

                } finally {
                    setPainting(true);
                }
            }
        }

        private void clearCover() {
            Audio audio = currentAudio();
            if (audio != null) {
                mCover.setVisible(false);
                if (mCover.resource != null)
                    mCover.resource.remove();
            }
        }

        public boolean handleExit() {
            clearCover();
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            Audio audio = currentAudio();
            switch (code) {
            case KEY_THUMBSDOWN:
                if (audio != null && audio.getRating() > 0) {
                    getBApp().play("thumbsdown.snd");
                    getBApp().flush();
                    try {
                        audio.setRating(Math.max(audio.getRating() - 1, 0));
                        AudioManager.updateAudio(audio);
                    } catch (Exception ex) {
                        Tools.logException(Music.class, ex);
                    }
                    setRating();
                } else {
                    getBApp().play("bonk.snd");
                    getBApp().flush();
                }
                return true;
            case KEY_THUMBSUP:
                if (audio != null && audio.getRating() < 5) {
                    getBApp().play("thumbsup.snd");
                    getBApp().flush();
                    try {
                        audio.setRating(Math.min(audio.getRating() + 1, 5));
                        AudioManager.updateAudio(audio);
                    } catch (Exception ex) {
                        Tools.logException(Music.class, ex);
                    }
                    setRating();
                } else {
                    getBApp().play("bonk.snd");
                    getBApp().flush();
                }
                return true;
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
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getNextPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
                }
            }
        }

        private void setRating() {
            Audio audio = currentAudio();
            if (audio != null) {
                for (int i = 0; i < 5; i++) {
                    if (i < audio.getRating())
                        mStars[i].setTransparency(0.0f);
                    else
                        mStars[i].setTransparency(0.6f);
                }
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("play")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        getBApp().push(new PlayerScreen((Music) getBApp(), mTracker), TRANSITION_LEFT);
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
                    NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
                    if (nameFile != null) {
                        return getAudio(nameFile.getFile().getCanonicalPath());
                    }
                } catch (Exception ex) {
                    Tools.logException(Music.class, ex);
                }
            }
            return null;
        }

        private SimpleDateFormat mTimeFormat;

        private Resource mAnim = getResource("*2000");

        private BView mCover;

        private BText mTitleText;

        private BText mSongText;

        private BText mArtistText;

        private BText mAlbumText;

        private BText mDurationText;

        private BText mYearText;

        private BText mGenreText;

        private Tracker mTracker;

        private BView[] mStars;

        private Thread mCoverThread;
    }

    public class PlayerScreen extends DefaultScreen {

        public PlayerScreen(Music app, Tracker tracker) {
            super(app, true);

            mTracker = tracker;

            app.setTracker(tracker);

            setTitle(" ");

            MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory).getAppContext()
            .getConfiguration();
            if (musicConfiguration.isShowImages())
                setFooter("Press INFO for lyrics, 0 for images");
            else
                setFooter("Press INFO for lyrics");

            getPlayer().startTrack();
        }

        private void updatePlayer() {
            new Thread() {
                public void run() {
                    MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory)
                            .getAppContext().getConfiguration();
                    //ClassicSkin classicSkin = new ClassicSkin(musicConfiguration.getSkin());
                    if (mClassicSkin != null) {
                        mBusy.setVisible(true);
                        synchronized(this)
                        {
                            setPainting(false);
                            try {
                                player = mClassicSkin.getMain(PlayerScreen.this);
                                previousControl = mClassicSkin.getPreviousControl(player);
                                playControl = mClassicSkin.getPlayControl(player);
                                pauseControl = mClassicSkin.getPauseControl(player);
                                stopControl = mClassicSkin.getStopControl(player);
                                nextControl = mClassicSkin.getNextControl(player);
                                ejectControl = mClassicSkin.getEjectControl(player);
                                title = mClassicSkin.getTitle(player);
                     
                                stereo = mClassicSkin.getStereoActive(player);
                                mono = mClassicSkin.getMonoPassive(player);
                     
                                sampleRate = mClassicSkin.getSampleRate(player, "44");
                                bitRate = mClassicSkin.getBitRate(player, " 96");
                     
                                stopIcon = mClassicSkin.getStopIcon(player);
                                stopIcon.setVisible(false);
                                playIcon = mClassicSkin.getPlayIcon(player);
                                playIcon.setVisible(false);
                                pauseIcon = mClassicSkin.getPauseIcon(player);
                                pauseIcon.setVisible(false);
                     
                                repeat = mClassicSkin.getRepeatActive(player);
                                shuffle = mClassicSkin.getShufflePassive(player);
    
                                positionControl = mClassicSkin.getPosition(player, 0);
    
                                seconds1 = mClassicSkin.getSeconds1(player);
                                seconds2 = mClassicSkin.getSeconds2(player);
                                minutes1 = mClassicSkin.getMinutes1(player);
                                minutes2 = mClassicSkin.getMinutes2(player);
                                
                                if (mLastTitle!=null)
                                    setTitleText(mLastTitle);
                                
                                playPlayer();
                                
                                mPlaying = true;
                                
                                mBusy.setVisible(false);
                                
                            } finally {
                                setPainting(true);
                            }
                        }
                        getBApp().flush();
                    }
                }
                
                public void interrupt()
                {
                    synchronized (this)
                    {
                        super.interrupt();
                    }
                }
            }.start();
        }

        private void updateTime(int seconds) {
            int secondD = 0, second = 0, minuteD = 0, minute = 0;
            int minutes = (int) Math.floor(seconds / 60);
            int hours = (int) Math.floor(minutes / 60);
            minutes = minutes - hours * 60;
            seconds = seconds - minutes * 60 - hours * 3600;
            if (seconds < 10) {
                secondD = 0;
                second = seconds;
            } else {
                secondD = ((int) seconds / 10);
                second = ((int) (seconds - (((int) seconds / 10)) * 10));
            }
            if (minutes < 10) {
                minuteD = 0;
                minute = minutes;
            } else {
                minuteD = ((int) minutes / 10);
                minute = ((int) (minutes - (((int) minutes / 10)) * 10));
            }

            setPainting(false);
            try {
                seconds1.setImage(second);
                seconds2.setImage(secondD);
                minutes1.setImage(minute);
                minutes2.setImage(minuteD);
            }
            finally
            {
                setPainting(true);
            }
        }

        private void setTitleText(String text) {
            text = Tools.extractName(text);

            if (!text.toUpperCase().equals(title.getText()))
            {
                setPainting(false);
                try {
                    title.setText(text);
                }
                finally
                {
                    setPainting(true);
                }
            }
            
            mLastTitle = text;
        }

        public void stopPlayer() {
            if (stopIcon!=null)
            {
                setPainting(false);
                try {
                    stopIcon.setVisible(true);
                    playIcon.setVisible(false);
                    pauseIcon.setVisible(false);
                    positionControl.setPosition(0);
                } finally {
                    setPainting(true);
                }
            }
        }

        public void playPlayer() {
            if (stopIcon!=null)
            {
                setPainting(false);
                try {
                    stopIcon.setVisible(false);
                    playIcon.setVisible(true);
                    pauseIcon.setVisible(false);
                } finally {
                    setPainting(true);
                }
            }
        }

        public void pausePlayer() {
            if (stopIcon!=null)
            {
                if (getPlayer().getState() != Player.STOP) {
                    setPainting(false);
                    try {
                        stopIcon.setVisible(false);
                        playIcon.setVisible(false);
                        if (getPlayer().getState() == Player.PAUSE) {
                            pauseIcon.setVisible(false);
                            playIcon.setVisible(true);
                        } else {
                            pauseIcon.setVisible(true);
                        }
                    } finally {
                        setPainting(true);
                    }
                }
            }
        }

        public void nextPlayer() {
            if (stopIcon!=null)
            {
                setPainting(false);
                try {
                    stopIcon.setVisible(false);
                    playIcon.setVisible(true);
                    pauseIcon.setVisible(false);
                } finally {
                    setPainting(true);
                }
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            getBApp().flush();
            updatePlayer();

            mScreenSaver = new ScreenSaver(this);
            mScreenSaver.start();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            mPlaying = false;
            setPainting(false);
            try {
                stopPlayer();

                if (mScreenSaver != null && mScreenSaver.isAlive()) {
                    mScreenSaver.interrupt();
                    mScreenSaver = null;
                }
                if (player != null)
                {
                    setPainting(false);
                    try {
                        player.setVisible(false);
                        player.remove();
                    } finally {
                        setPainting(true);
                    }
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
            case KEY_PAUSE:
                if (pauseControl!=null)
                    pauseControl.setSelected(true);
                pausePlayer();
                break;
            case KEY_PLAY:
                if (playControl!=null)
                    playControl.setSelected(true);
                playPlayer();
                break;
            case KEY_CHANNELUP:
                //getBApp().play("select.snd");
                //getBApp().flush();
                if (nextControl!=null)
                    nextControl.setSelected(true);
                break;
            case KEY_CHANNELDOWN:
                //getBApp().play("select.snd");
                //getBApp().flush();
                if (previousControl!=null)
                    previousControl.setSelected(true);
                break;
            case KEY_SLOW:
                if (stopControl!=null)
                    stopControl.setSelected(true);
                stopPlayer();
                break;
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_INFO:
                getBApp().play("select.snd");
                getBApp().flush();
                LyricsScreen lyricsScreen = new LyricsScreen((Music) getBApp(), mTracker);
                getBApp().push(lyricsScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            case KEY_NUM0:
                MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory).getAppContext()
                .getConfiguration();
                if (musicConfiguration.isShowImages())
                {
                    getBApp().play("select.snd");
                    getBApp().flush();
                    ImagesScreen imagesScreen = new ImagesScreen((Music) getBApp(), mTracker);
                    getBApp().push(imagesScreen, TRANSITION_LEFT);
                    getBApp().flush();
                    return true;
                }
                else
                    return false;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public boolean handleKeyRelease(int code, long rawcode) {
            switch (code) {
            case KEY_PAUSE:
                if (pauseControl!=null)
                        pauseControl.setSelected(false);
                break;
            case KEY_PLAY:
                if (playControl!=null)
                        playControl.setSelected(false);
                break;
            case KEY_CHANNELUP:
                if (nextControl!=null)
                        nextControl.setSelected(false);
                break;
            case KEY_CHANNELDOWN:
                if (previousControl!=null)
                    previousControl.setSelected(false);
                break;
            case KEY_SLOW:
                if (stopControl!=null)
                    stopControl.setSelected(false);
                break;
            case KEY_ENTER:
                if (ejectControl!=null)
                    ejectControl.setSelected(false);
                break;
            }
            return super.handleKeyRelease(code, rawcode);
        }

        public boolean handleAction(BView view, Object action) {
            NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
            if (action.equals("ready")) {
                if (mPlaying) {
                    setTitleText(nameFile.getName());
                    playPlayer();
                }
                else
                    mLastTitle = nameFile.getName();
                return true;
            } else if (action.equals("playing")) {
                if (mPlaying) {
                    if (getPlayer().getTotal() != 0) {
                        int value = (int) Math.round(getPlayer().getCurrentPosition() / (float) getPlayer().getTotal()
                                * 100);
                        positionControl.setPosition(value);
                        updateTime(getPlayer().getCurrentPosition() / 1000);
                    }

                    int value = getPlayer().getBitrate();
                    String newValue = Integer.toString(value);
                    if (value < 100)
                        newValue = " " + newValue;
                    bitRate.setText(newValue);
                }

                return true;
            } else if (action.equals("stopped")) {
                if (mPlaying) {
                    stopPlayer();
                    setTitleText(" ");
                    updateTime(0);
                }
                return true;
            }

            return super.handleAction(view, action);
        }

        public boolean handleEvent(HmeEvent event) {
            switch (event.opcode) {
            case EVT_KEY: {
                if (title!=null && title.handleEvent(event))
                    return true;
                break;
            }
            }
            return super.handleEvent(event);
        }

        Audio mAudio;

        // when did the last key press occur
        long lastKeyPress;

        View player;

        ImageControl previousControl;

        ImageControl playControl;

        ImageControl pauseControl;

        ImageControl stopControl;

        ImageControl nextControl;

        ImageControl ejectControl;

        ScrollTextControl title;

        View stereo;

        View mono;

        TextControl bitRate;

        TextControl sampleRate;

        View stopIcon;

        View playIcon;

        View pauseIcon;

        View repeat;

        View shuffle;

        PositionControl positionControl;

        ImageView seconds1;

        ImageView seconds2;

        ImageView minutes1;

        ImageView minutes2;

        private Tracker mTracker;

        private ScreenSaver mScreenSaver;

        private boolean mPlaying;
        
        private String mLastTitle = "";
    }

    private static Audio getAudio(String path) {
        Audio audio = null;
        try {
            List list = AudioManager.findByPath(path);
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(Music.class, ex);
        }

        if (audio == null) {
            try {
                audio = (Audio) MediaManager.getMedia(path);
                AudioManager.createAudio(audio);
            } catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
        }
        return audio;
    }

    public class LyricsScreen extends DefaultScreen {
        private BList list;

        public LyricsScreen(Music app, Tracker tracker) {
            super(app, "Lyrics", false);

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
            list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
            list.add("Press SELECT to go back");
            setFocusDefault(list);
        }

        public void updateLyrics() {
            setPainting(false);
            try {
                if (mLyricsThread != null && mLyricsThread.isAlive())
                    mLyricsThread.interrupt();
            }
            finally
            {
                setPainting(true);
            }
            NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
            Audio audio = null;
            try {
                List list = AudioManager.findByPath(nameFile.getFile().getCanonicalPath());
                if (list != null && list.size() > 0) {
                    audio = (Audio) list.get(0);
                }
            } catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
            if (audio.getLyrics()!=null && audio.getLyrics().length()>0)
            {
                setPainting(false);
                try {
                    mBusy.setVisible(false);
                    getBApp().flush();
                    scrollText.setVisible(true);
                    scrollText.setText(audio.getLyrics());
                    getBApp().flush();
                }
                finally
                {
                    setPainting(true);
                }
            }
            else
            {
                final Audio lyricsAudio = audio;
    
                mLyricsThread = new Thread() {
                    public void run() {
                        try {
                            String lyrics = Lyrics.getLyrics(lyricsAudio.getTitle(), lyricsAudio.getArtist());
                            if (lyrics == null || lyrics.trim().length() == 0)
                            {
                                lyrics = "Lyrics not found";
                            }
                            else
                            {
                                synchronized(this)
                                {
                                    try
                                    {
                                        lyricsAudio.setLyrics(lyrics);
                                        AudioManager.updateAudio(lyricsAudio);
                                    }
                                    catch (Exception ex)
                                    {
                                        Tools.logException(Music.class, ex, "Could not update lyrics");                    
                                    }
                                }
                            }
                            synchronized(this)
                            {
                                setPainting(false);
                                try {
                                    mBusy.setVisible(false);
                                    getBApp().flush();
                                    scrollText.setVisible(true);
                                    scrollText.setText(lyrics);
                                    getBApp().flush();
                                }
                                finally
                                {
                                    setPainting(true);
                                }
                            }
                        } catch (Exception ex) {
                            Tools.logException(Music.class, ex, "Could retrieve lyrics");
                        }
                    }
                    
                    public void interrupt()
                    {
                        synchronized (this)
                        {
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
            }
            finally
            {
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

        public ImagesScreen(Music app, Tracker tracker) {
            super(app, "Images", true);

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
            list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
            list.add("Press SELECT to go back");
            setFocusDefault(list);
        }

        public void updateImage() {
            NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
            Audio audio = null;
            try {
                List list = AudioManager.findByPath(nameFile.getFile().getCanonicalPath());
                if (list != null && list.size() > 0) {
                    audio = (Audio) list.get(0);
                }
            } catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
            final Audio lyricsAudio = audio;

            mImageThread = new Thread() {
                public void run() {
                    try {
                        synchronized(this)
                        {
                            mBusy.setVisible(true);
                            getBApp().flush();
                        }
                        
                        if (mResults==null || mResults.size()==0)
                        {
                            mResults = Yahoo.getImages("\""+lyricsAudio.getArtist()+"\" music");
                            mPos = 0;
                        }
                        if (mResults.size() == 0)
                        {
                            synchronized(this)
                            {
                                setPainting(false);
                                try {
                                    mBusy.setVisible(false);
                                    getBApp().flush();
                                }
                                finally
                                {
                                    setPainting(true);
                                }
                            }                            
                            return;
                        }
                        
                        NameValue nameValue = (NameValue)mResults.get(mPos);
                        BufferedImage image = Tools.getImage(new URL(nameValue.getValue()), -1, -1);
                        
                        if (image!=null)
                        {
                            synchronized(this)
                            {
                                setPainting(false);
                                try {
                                    if (mImageView.resource!=null)
                                        mImageView.resource.remove();
                                    mUrlText.setValue(nameValue.getName());
                                    mImageView.setVisible(true);
                                    mImageView.setTransparency(1f);
                                    mImageView.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                    mImageView.setTransparency(0f, getResource("*500"));
                                    image.flush();
                                    image = null;
                                }
                                finally
                                {
                                    setPainting(true);
                                }
                            }
                        }
                        else
                        {
                            mResults.remove(mPos);
                        }
                        
                    } catch (Exception ex) {
                        Tools.logException(Music.class, ex, "Could not retrieve image");
                        mResults.remove(mPos);
                    }
                    finally
                    {
                        synchronized(this)
                        {
                            setPainting(false);
                            try {
                                mPosText.setValue(String.valueOf(mPos+1)+" of "+String.valueOf(mResults.size()));
                                mBusy.setVisible(false);
                            }
                            finally
                            {
                                setPainting(true);
                            }
                            getBApp().flush();
                        }                        
                    }
                }
                
                public void interrupt()
                {
                    synchronized (this)
                    {
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
            }
            finally
            {
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
                if (mResults!=null && mResults.size()>0)
                {
                    getBApp().play("pageup.snd");
                    getBApp().flush();
                    mPos = mPos - 1;
                    if (mPos==-1)
                        mPos = mResults.size()-1;
                }
                updateImage();
                return true;
            case KEY_CHANNELDOWN:
                if (mResults!=null && mResults.size()>0)
                {
                    getBApp().play("pagedown.snd");
                    getBApp().flush();
                    mPos = mPos + 1;
                    if (mPos==mResults.size())
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
                    synchronized(this)
                    {
                        mPlayerScreen.setTransparency(0.9f, getResource("*60000"));
                    }
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex2) {
                    Tools.logException(Music.class, ex2);
                }
            }
        }
        
        public void interrupt()
        {
            synchronized (this)
            {
                super.interrupt();
            }
        }

        private PlayerScreen mPlayerScreen;
    }

    public static class MusicFactory extends AppFactory {

        public MusicFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            MusicConfiguration musicConfiguration = (MusicConfiguration) getAppContext().getConfiguration();
            
            mClassicSkin = new ClassicSkin(musicConfiguration.getSkin());
        }
    }
    
    private static ClassicSkin mClassicSkin;
}