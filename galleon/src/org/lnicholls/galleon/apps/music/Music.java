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
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.music.FileSystemContainer.NameFile;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.ThumbnailManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.togo.ToGoThread;
import org.lnicholls.galleon.util.Amazon;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.hme.winamp.ClassicSkin;
import org.lnicholls.hme.winamp.ImageControl;
import org.lnicholls.hme.winamp.ImageView;
import org.lnicholls.hme.winamp.PositionControl;
import org.lnicholls.hme.winamp.ScrollTextControl;
import org.lnicholls.hme.winamp.TextControl;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.http.server.HttpRequest;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.StreamResource;
import com.tivo.hme.sdk.View;
import com.tivo.hme.util.ArgumentList;
import com.tivo.hme.util.Mp3Duration;

public class Music extends BApplication {

    private static Logger log = Logger.getLogger(Music.class.getName());

    public final static String TITLE = "Music";

    private Resource mBackground;

    private Resource mIcon;

    private Resource mBusyIcon;

    private Resource mBusy2Icon;

    private Resource mFolderIcon;

    private Resource mCDIcon;

    private MusicScreen mMusicScreen;

    public static final int INIT = 0;

    public static final int OPEN = 1;

    public static final int PLAY = 2;

    public static final int PAUSE = 3;

    public static final int STOP = 4;

    private int mPlayerState = STOP;

    protected void init(Context context) {
        super.init(context);

        mBackground = getResource("background.jpg");

        mIcon = getResource("icon.png");

        mBusyIcon = getResource("busy.gif");

        mBusy2Icon = getResource("busy2.gif");

        mFolderIcon = getResource("folder.png");

        mCDIcon = getResource("cd.png");

        mMusicScreen = new MusicScreen(this);

        push(new MusicMenuScreen(this), TRANSITION_NONE);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public class DefaultScreen extends BScreen {
        public DefaultScreen(Music app) {
            super(app);

            below.setResource(mBackground);

            mTitle = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V, (width - (SAFE_TITLE_H * 2)), 54);
            mTitle.setValue(toString());
            mTitle.setColor(Color.yellow);
            mTitle.setShadow(Color.black, 3);
            mTitle.setFlags(RSRC_HALIGN_CENTER);
            mTitle.setFont("default-48.font");
        }

        public void setTitle(String value) {
            mTitle.setValue(value);
        }

        public String toString() {
            return "Music";
        }

        private BText mTitle;

    }

    public class MusicMenuScreen extends DefaultScreen {
        private TGList list;

        public MusicMenuScreen(Music app) {
            super(app);
            setTitle("Music");

            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory)
                    .getAppContext().getConfiguration();

            for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                list.add(new NameFile(nameValue.getName(), new File(nameValue.getValue())));
            }

            setFocusDefault(list);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                icon.setResource(mBusy2Icon);
                icon.flush();

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        try {
                            NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                            FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                    .getCanonicalPath());
                            Tracker tracker = new Tracker(fileSystemContainer.getItems(), 0);
                            PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                            getBApp().push(pathScreen, TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            log.error(ex);
                        }
                    }
                }.start();
                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (list.getFocus() >= 0) {
                NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                if (nameFile.getFile().isDirectory())
                    icon.setResource(mFolderIcon);
                else
                    icon.setResource(mCDIcon);
                icon.flush();
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                getBApp().setActive(false);
                return true;
            }

            return super.handleKeyPress(code, rawcode);
        }
    }

    public class TGList extends BList {
        public TGList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameFile nameFile = (NameFile) get(index);
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

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "push"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public int getTop() {
            return top;
        }
    }

    public class PathScreen extends DefaultScreen {
        private TGList list;

        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public PathScreen(Music app, Tracker tracker) {
            super(app);

            mTracker = tracker;

            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            setFocusDefault(list);

            mBusy = new BView(normal, SAFE_TITLE_H, SAFE_TITLE_V, 32, 32);
            mBusy.setResource(mBusyIcon);
            mBusy.setVisible(false);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                Object object = list.get(list.getFocus());
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                icon.setResource(mBusy2Icon);
                icon.flush();

                getBApp().play("select.snd");
                getBApp().flush();

                final NameFile nameFile = (NameFile) object;
                if (nameFile.getFile().isDirectory()) {
                    new Thread() {
                        public void run() {
                            try {
                                NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                        .getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer.getItems(), 0);
                                PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                log.error(ex);
                            }
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            try {
                                Audio audio = getAudio(nameFile.getFile().getCanonicalPath());

                                mMusicScreen.setName(nameFile.getName());
                                mMusicScreen.setAudio(audio);
                                mTracker.setPos(list.getFocus());
                                mMusicScreen.setTracker(mTracker);

                                getBApp().push(mMusicScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                log.error(ex);
                            }
                        }
                    }.start();
                }

                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (list.size() == 0) {
                setPainting(false);
                try {
                    mBusy.setVisible(true);

                    Iterator iterator = mTracker.getList().iterator();
                    while (iterator.hasNext()) {
                        NameFile nameFile = (NameFile) iterator.next();
                        list.add(nameFile);
                    }
                    mBusy.setVisible(false);
                    mBusy.flush();
                    list.setFocus(0, false);
                    list.flush();
                } catch (Exception ex) {
                    log.error(ex);
                } finally {
                    setPainting(true);
                }
                list.setTop(mTop);
                list.setFocus(mFocus, false);
            } else {
                if (list.getFocus() >= 0) {
                    NameFile nameFile = (NameFile) list.get(list.getFocus());
                    BView row = list.getRow(list.getFocus());
                    BView icon = (BView) row.children[0];
                    if (nameFile.getFile().isDirectory())
                        icon.setResource(mFolderIcon);
                    else
                        icon.setResource(mCDIcon);
                    icon.flush();
                }
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            mTop = list.getTop();
            mFocus = list.getFocus();
            list.clear();
            return super.handleExit();
        }

        private Tracker mTracker;

        private BView mBusy;

        private int mTop;

        private int mFocus;
    }

    public class MusicScreen extends DefaultScreen {

        private BList list;

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public MusicScreen(Music app) {
            super(app);

            setTitle("Song");

            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("mm:ss");

            int start = top;

            mCover = new BView(below, width - SAFE_TITLE_H - 200, height - SAFE_TITLE_V - 200, 200, 200, false);

            mTitleText = new BText(normal, border_left, start, text_width, 40);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mTitleText.setFont("default-36-bold.font");
            mTitleText.setColor(Color.CYAN);
            mTitleText.setShadow(true);

            start += 40;

            mSongText = new BText(normal, border_left, start, text_width, 20);
            mSongText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mSongText.setFont("default-18-bold.font");
            mSongText.setShadow(true);

            mDurationText = new BText(normal, border_left, start, text_width, 20);
            mDurationText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mDurationText.setFont("default-18-bold.font");
            mDurationText.setShadow(true);

            start += 20;

            mAlbumText = new BText(normal, border_left, start, text_width, 20);
            mAlbumText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mAlbumText.setFont("default-18-bold.font");
            mAlbumText.setShadow(true);

            mYearText = new BText(normal, border_left, start, text_width, 20);
            mYearText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mYearText.setFont("default-18-bold.font");
            mYearText.setShadow(true);

            start += 20;

            mArtistText = new BText(normal, border_left, start, text_width, 20);
            mArtistText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mArtistText.setFont("default-18-bold.font");
            mArtistText.setShadow(true);

            mGenreText = new BText(normal, border_left, start, text_width, 20);
            mGenreText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mGenreText.setFont("default-18-bold.font");
            mGenreText.setShadow(true);

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 80, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Play");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (mAudio != null) {
                mTitleText.setValue(Tools.trim(mName, 30));
                mSongText.setValue("Song: " + Tools.trim(mAudio.getTitle(), 40));
                mDurationText.setValue("Duration: " + mTimeFormat.format(new Date(mAudio.getDuration())));
                mAlbumText.setValue("Album: " + Tools.trim(mAudio.getAlbum(), 40));
                mYearText.setValue("Year: " + String.valueOf(mAudio.getDate()));
                mArtistText.setValue("Artist: " + Tools.trim(mAudio.getArtist(), 40));
                mGenreText.setValue("Genre: " + mAudio.getGenre());

                try {
                    java.awt.Image image = ThumbnailManager.findImageByKey(Mp3File.getKey(mAudio));
                    if (image == null)
                        image = Amazon.getAlbumImage(Mp3File.getKey(mAudio), mAudio.getArtist(), mAudio.getAlbum().equals(
                                Mp3File.DEFAULT_ALBUM) ? mAudio.getTitle() : mAudio.getAlbum());
                    if (image != null) {
                        mCover.setResource(image, RSRC_IMAGE_BESTFIT);
                        mCover.setVisible(true);
                        mCover.setTransparency(1.0f);
                        mCover.setTransparency(0.0f, mAnim);
                    }
                } catch (Exception ex) {
                    log.error("Could retrieve cover", ex);
                }
            }

            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            if (mAudio != null) {
                mCover.setVisible(false);
                if (mCover.resource != null)
                    mCover.resource.remove();
            }
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
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
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
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

        public void setName(String value) {
            mName = value;
        }

        public void setAudio(Audio value) {
            mAudio = value;
        }

        public void setTracker(Tracker value) {
            mTracker = value;
        }

        private SimpleDateFormat mTimeFormat;

        private Resource mAnim = getResource("*2000");

        private Audio mAudio;

        private String mName;

        private BView mCover;

        private BText mTitleText;

        private BText mSongText;

        private BText mArtistText;

        private BText mAlbumText;

        private BText mDurationText;

        private BText mYearText;

        private BText mGenreText;

        private Tracker mTracker;
    }
    
    public class OptionList extends BList {
        public OptionList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);

            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BText text = new BText(parent, 10, 4, parent.width - 40, parent.height - 4);
            text.setShadow(true);
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index).toString());
        }
    }

    public class PlayerScreen extends DefaultScreen {

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public PlayerScreen(Music app, Tracker tracker) {
            super(app);

            mTracker = tracker;

            setTitle(" ");

            track = new Track(this, 64, 10, 640 - 64 * 2, 60);

            MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) context.factory)
                    .getAppContext().getConfiguration();
            ClassicSkin classicSkin = new ClassicSkin(musicConfiguration.getSkin());
            if (classicSkin != null) {
                setPainting(false);
                try {
                    player = classicSkin.getMain(this);
    
                    previousControl = classicSkin.getPreviousControl(player);
                    playControl = classicSkin.getPlayControl(player);
                    pauseControl = classicSkin.getPauseControl(player);
                    stopControl = classicSkin.getStopControl(player);
                    nextControl = classicSkin.getNextControl(player);
                    ejectControl = classicSkin.getEjectControl(player);
    
                    title = classicSkin.getTitle(player);
    
                    stereo = classicSkin.getStereoActive(player);
                    mono = classicSkin.getMonoPassive(player);
    
                    sampleRate = classicSkin.getSampleRate(player, "44");
                    bitRate = classicSkin.getBitRate(player, " 96");
    
                    stopIcon = classicSkin.getStopIcon(player);
                    stopIcon.setVisible(false);
                    playIcon = classicSkin.getPlayIcon(player);
                    playIcon.setVisible(false);
                    pauseIcon = classicSkin.getPauseIcon(player);
                    pauseIcon.setVisible(false);
    
                    repeat = classicSkin.getRepeatActive(player);
                    shuffle = classicSkin.getShufflePassive(player);
    
                    positionControl = classicSkin.getPosition(player, 0);
    
                    seconds1 = classicSkin.getSeconds1(player);
                    seconds2 = classicSkin.getSeconds2(player);
                    minutes1 = classicSkin.getMinutes1(player);
                    minutes2 = classicSkin.getMinutes2(player);
                } finally {
                    setPainting(true);
                }
            }

            playPlayer();
        }

        void playCurrent() {
            play();
        }

        void play() {
            try {
                NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
                mAudio = getAudio(nameFile.getFile().getCanonicalPath());
            } catch (Exception ex) {
                log.error(ex);
            }

            if (mAudio != null) {
                track.closeTrack();
                String url = getApp().getContext().base.toString();
                try {
                    url += URLEncoder.encode(mAudio.getId().toString() + ".mp3", "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                track.playTrack(url);
                setTitleText(mAudio.getTitle()+" - "+mAudio.getArtist());
            }
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

            seconds1.setImage(second);
            seconds2.setImage(secondD);
            minutes1.setImage(minute);
            minutes2.setImage(minuteD);
        }

        private int counter = 0;

        private void setTitleText(String text) {
            if (text.toUpperCase().endsWith(".MP3"))
                text = text.substring(0, text.length() - 4);
            if (text.indexOf("/") != -1)
                text = text.substring(text.indexOf("/") + 1);

            title.setText(text);
        }

        public boolean handleEvent(HmeEvent event) {
            switch (event.opcode) {

            case EVT_KEY: {
                if (title.handleEvent(event))
                    return true;
                break;
            }

            case StreamResource.EVT_RSRC_INFO: {
                HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo) event;
                if (info.status == RSRC_STATUS_PLAYING) {
                    String pos = (String) info.map.get("pos");
                    if (pos != null) {
                        try {
                            StringTokenizer tokenizer = new StringTokenizer(pos, "/");
                            if (tokenizer.countTokens() == 2) {
                                String current = tokenizer.nextToken();
                                String total = tokenizer.nextToken();

                                int value = (int) Math.round(Float.parseFloat(current) / Integer.parseInt(total) * 100);
                                positionControl.setPosition(value);
                                updateTime(Integer.parseInt(current) / 1000);
                            }
                        } catch (Exception ex) {
                        }
                    }
                    String bitrate = (String) info.map.get("bitrate");
                    if (bitrate != null) {
                        try {
                            int value = (int) Math.round(Float.parseFloat(bitrate) / 1024);
                            String newValue = Integer.toString(value);
                            if (value < 100)
                                newValue = " " + newValue;
                            bitRate.setText(newValue);
                        } catch (Exception ex) {
                        }
                    }
                }
                return true;
            }
            case StreamResource.EVT_RSRC_STATUS: {
                HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo) event;
                if (info.status >= RSRC_STATUS_CLOSED) {
                    if (mPlayerState != STOP) {
                        positionControl.setPosition(0);

                        // if the user hasn't touched the list recently, move the
                        // selector to reflect the new track.
                        if (System.currentTimeMillis() - lastKeyPress > 5000) {
                            //list.select(index, true);
                        }

                        // now play the new track
                        getNextPos();
                        play();
                    }
                }
                return true;
            }
            }
            return super.handleEvent(event);
        }

        public void stopPlayer() {
            stopIcon.setVisible(true);
            playIcon.setVisible(false);
            pauseIcon.setVisible(false);
            positionControl.setPosition(0);

            mPlayerState = STOP;
            track.closeTrack();
        }

        public void playPlayer() {
            stopIcon.setVisible(false);
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);

            if (mPlayerState != PLAY) {
                track.playTrack();
                mPlayerState = PLAY;
                playCurrent();
            }
        }

        public void pausePlayer() {
            if (mPlayerState != STOP) {
                stopIcon.setVisible(false);
                playIcon.setVisible(false);
                if (mPlayerState == PAUSE) {
                    mPlayerState = PLAY;
                    track.playTrack();
                    pauseIcon.setVisible(false);
                    playIcon.setVisible(true);
                } else {
                    mPlayerState = PAUSE;
                    track.pauseTrack();
                    pauseIcon.setVisible(true);
                }
            }
        }

        public void nextPlayer() {
            stopIcon.setVisible(false);
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);

            if (mPlayerState != PLAY) {
                mPlayerState = PLAY;
                track.playTrack();
            }
        }

        private int getNextPos() {
            int pos = mTracker.getNextPos();
            NameFile nameFile = (NameFile) mTracker.getList().get(pos);
            while (nameFile.getFile().isDirectory()) {
                pos = mTracker.getNextPos();
                nameFile = (NameFile) mTracker.getList().get(pos);
            }
            return pos;
        }

        private int getPrevPos() {
            int pos = mTracker.getPrevPos();
            NameFile nameFile = (NameFile) mTracker.getList().get(pos);
            while (nameFile.getFile().isDirectory()) {
                pos = mTracker.getPrevPos();
                nameFile = (NameFile) mTracker.getList().get(pos);
            }
            return pos;
        }

        class Track extends View {
            View label;

            Track(View parent, int x, int y, int width, int height) {
                super(parent, x, y, width, height);
                label = new View(this, 0, 0, width, height);
                label.setVisible(false);
            }

            void playTrack(String url) {
                if (resource != null) {
                    resource.remove();
                    flush();
                }
                setResource(createStream(url, null, true));
            }

            void pauseTrack() {
                if (resource != null) {
                    ((StreamResource) resource).pause();
                }
            }

            void playTrack() {
                if (resource != null) {
                    ((StreamResource) resource).play();
                }
            }

            void closeTrack() {
                if (resource != null) {
                    ((StreamResource) resource).close();
                }
            }

            public boolean handleEvent(HmeEvent event) {
                if (label.resource != null)
                    label.resource.remove();
                label.setResource(createText("default-18-bold.font", Color.yellow, event.toString()),
                        RSRC_HALIGN_CENTER | RSRC_TEXT_WRAP);
                return super.handleEvent(event);
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            mScreenSaver = new ScreenSaver(this);
            mScreenSaver.start();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            setPainting(false);
            try {
                stopPlayer();
                if (mScreenSaver!=null && mScreenSaver.isAlive())
                {
                    mScreenSaver.interrupt();
                    mScreenSaver = null;
                }
            } finally {
                setPainting(true);
            }
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            if (transparency!=0.0f)
                setTransparency(0.0f);
            switch (code) {
            case KEY_PAUSE:
                pauseControl.setSelected(true);
                pausePlayer();
                break;
            case KEY_PLAY:
                playControl.setSelected(true);
                playPlayer();
                break;
            case KEY_CHANNELUP:
                getBApp().play("select.snd");
                getBApp().flush();
                nextControl.setSelected(true);
                getNextPos();
                play();
                break;
            case KEY_CHANNELDOWN:
                getBApp().play("select.snd");
                getBApp().flush();
                previousControl.setSelected(true);
                getPrevPos();
                play();
                break;
            case KEY_SLOW:
                stopControl.setSelected(true);
                stopPlayer();
                break;
            case KEY_ENTER:
                ejectControl.setSelected(true);
                setActive(false);
                break;
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public boolean handleKeyRelease(int code, long rawcode) {
            switch (code) {
            case KEY_PAUSE:
                pauseControl.setSelected(false);
                break;
            case KEY_PLAY:
                playControl.setSelected(false);
                break;
            case KEY_CHANNELUP:
                nextControl.setSelected(false);
                break;
            case KEY_CHANNELDOWN:
                previousControl.setSelected(false);
                break;
            case KEY_SLOW:
                stopControl.setSelected(false);
                break;
            case KEY_ENTER:
                ejectControl.setSelected(false);
                break;
            }
            return super.handleKeyRelease(code, rawcode);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("play")) {
                return true;
            }

            return super.handleAction(view, action);
        }

        Audio mAudio;

        Track track;

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
    }

    public static class Tracker {
        public Tracker(List list, int pos) {
            setList(list);
            setPos(pos);
        }

        public void setPos(int pos) {
            mPos = pos;
        }

        public int getPos() {
            return mPos;
        }

        public int getNextPos() {
            if (++mPos > (mList.size() - 1))
                mPos = 0;
            return mPos;
        }

        public int getPrevPos() {
            if (--mPos < 0)
                mPos = mList.size() - 1;
            return mPos;
        }

        public void setList(List list) {
            mList = list;
        }

        public List getList() {
            return mList;
        }

        private List mList = new ArrayList();

        private int mPos;
    }

    private static Audio getAudio(String path) {
        Audio audio = null;
        try {
            List list = AudioManager.findByPath(path);
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            log.error(ex);
        }

        if (audio == null) {
            try {
                audio = (Audio) MediaManager.getMedia(path);
                AudioManager.createAudio(audio);
            } catch (Exception ex) {
                log.error(ex);
            }
        }
        return audio;
    }
    
    private class ScreenSaver extends Thread
    {
        public ScreenSaver(PlayerScreen playerScreen)
        {
            mPlayerScreen = playerScreen;
        }
        
        public void run()
        {
            while (true)
            {
                try
                {
                    sleep(1000*5*60);
                    mPlayerScreen.setTransparency(0.9f,getResource("*60000"));
                } 
                catch (InterruptedException ex) {
                    return;
                } 
                catch (Exception ex2) {
                    Tools.logException(Music.class, ex2);
                }
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
        }

        public InputStream getStream(String uri) throws IOException {
            if (uri.toLowerCase().endsWith(".mp3")) {
                try {
                    String id = Tools.extractName(uri);
                    Audio audio = AudioManager.retrieveAudio(Integer.valueOf(id));
                    File file = new File(audio.getPath());
                    if (file.exists()) {
                        return new FileInputStream(file);
                    }
                } catch (Exception ex) {
                    log.error(uri, ex);
                }
            }

            return super.getStream(uri);
        }

        protected void addHeaders(HttpRequest http, String uri) throws IOException {
            if (uri.toLowerCase().endsWith(".mp3")) {
                InputStream tmp = getStream(uri);
                if (tmp != null) {
                    try {
                        http.addHeader(TIVO_DURATION, "" + Mp3Duration.getMp3Duration(tmp, tmp.available()));
                    } finally {
                        tmp.close();
                    }
                }
            }
            super.addHeaders(http, uri);
        }

    }
}