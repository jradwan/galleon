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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.NameFile;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.hme.winamp.ClassicSkin;
import org.lnicholls.hme.winamp.ImageControl;
import org.lnicholls.hme.winamp.ImageView;
import org.lnicholls.hme.winamp.PositionControl;
import org.lnicholls.hme.winamp.ScrollTextControl;
import org.lnicholls.hme.winamp.TextControl;

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

    private MusicScreen mMusicScreen;

    protected void init(Context context) {
        super.init(context);

        mFolderIcon = getResource("folder.png");

        mCDIcon = getResource("cd.png");

        mMusicScreen = new MusicScreen(this);

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
            super(app, "Music");

            mTracker = tracker;

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
                    new Thread() {
                        public void run() {
                            try {
                                Audio audio = getAudio(nameFile.getFile().getCanonicalPath());

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
                    if (mCoverThread != null && mCoverThread.isAlive())
                        mCoverThread.interrupt();
                    mCoverThread = new Thread() {
                        public void run() {
                            try {
                                java.awt.Image image = Mp3File.getCover(audio, musicConfiguration.isUseAmazon(),
                                        musicConfiguration.isUseFile());
                                if (image != null) {
                                    mCover.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                    mCover.setVisible(true);
                                    mCover.setTransparency(1.0f);
                                    mCover.setTransparency(0.0f, mAnim);
                                    mCover.flush();
                                }
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex, "Could retrieve cover");
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

            getPlayer().startTrack();

            playPlayer();
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

        private void setTitleText(String text) {
            text = Tools.extractName(text);

            if (!text.toUpperCase().equals(title.getText()))
                title.setText(text);
        }

        public void stopPlayer() {
            stopIcon.setVisible(true);
            playIcon.setVisible(false);
            pauseIcon.setVisible(false);
            positionControl.setPosition(0);
        }

        public void playPlayer() {
            stopIcon.setVisible(false);
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);
        }

        public void pausePlayer() {
            if (getPlayer().getState() != Player.STOP) {
                stopIcon.setVisible(false);
                playIcon.setVisible(false);
                if (getPlayer().getState() == Player.PAUSE) {
                    pauseIcon.setVisible(false);
                    playIcon.setVisible(true);
                } else {
                    pauseIcon.setVisible(true);
                }
            }
        }

        public void nextPlayer() {
            stopIcon.setVisible(false);
            playIcon.setVisible(true);
            pauseIcon.setVisible(false);
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
                if (mScreenSaver != null && mScreenSaver.isAlive()) {
                    mScreenSaver.interrupt();
                    mScreenSaver = null;
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
                break;
            case KEY_CHANNELDOWN:
                getBApp().play("select.snd");
                getBApp().flush();
                previousControl.setSelected(true);
                break;
            case KEY_SLOW:
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
            NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
            if (action.equals("ready")) {
                setTitleText(nameFile.getName());
                playPlayer();
                return true;
            } else if (action.equals("playing")) {
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

                return true;
            } else if (action.equals("stopped")) {
                stopPlayer();
                setTitleText(" ");
                updateTime(0);
                return true;
            }

            return super.handleAction(view, action);
        }

        public boolean handleEvent(HmeEvent event) {
            switch (event.opcode) {
            case EVT_KEY: {
                if (title.handleEvent(event))
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

    private class ScreenSaver extends Thread {
        public ScreenSaver(PlayerScreen playerScreen) {
            mPlayerScreen = playerScreen;
        }

        public void run() {
            while (true) {
                try {
                    sleep(1000 * 5 * 60);
                    mPlayerScreen.setTransparency(0.9f, getResource("*60000"));
                } catch (InterruptedException ex) {
                    return;
                } catch (Exception ex2) {
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
    }
}