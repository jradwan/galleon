package org.lnicholls.galleon.widget;

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
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.apps.music.Music;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication.Player;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;

public class MusicPlayer extends DefaultPlayer {

    private static Logger log = Logger.getLogger(MusicPlayer.class.getName());

    public MusicPlayer(DefaultScreen parent, int x, int y, int width, int height, boolean visible,
            DefaultApplication application, Tracker tracker) {
        super(parent, x, y, width, height, visible);

        mTracker = tracker;
        mApplication = application;

        mMusicInfo = new MusicInfo(this, 0, 0, width, height, true);

        mPlay = mApplication.getSkinImage("player", "play");
        mPause = mApplication.getSkinImage("player", "pause");
        mStop = mApplication.getSkinImage("player", "stop");
        mOverlay = mApplication.getSkinImage("player", "overlay");

        statusBarBg = new BView(this, 30, height - 30, width - 30 - 10 - 30, 30);
        statusBarBg.setResource(Color.BLACK);
        statusBarBg.setVisible(false);
        //statusBarBg.setTransparency(.5f);
        statusBar = new BView(this, 30 + 2, height - 30 + 5, width - 30 - 10 - 30, 30 - 10);
        statusBar.setResource(Color.GREEN);
        statusBar.setSize(1, statusBar.height);
        
        mShape = new BView(this, 0, height - 50, 500, 50);
        mShape.setResource(mOverlay);
        
        mButton = new BView(this, 0, height - 30, 30, 30);
        mButton.setResource(mPlay);

        mTimeText = new BText(this, 0, height - 45, width, 20);
        mTimeText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_BOTTOM);
        mTimeText.setFont("default-18-bold.font");
        mTimeText.setColor(Color.GREEN);
        mTimeText.setShadow(true);
    }

    public void updatePlayer() {
        final Audio audio = currentAudio();
        if (audio != null) {
            mMusicInfo.setAudio(audio);
            playPlayer();

            mPlaying = true;
        }
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
                Tools.logException(Music.class, ex);
            }
        }
        return null;
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

    private void updateTitle() {
        try {
            Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
            Audio audio = null;
            if (nameFile.isFile())
                audio = getAudio(((File) nameFile.getValue()).getCanonicalPath());
            else
                audio = getAudio((String) nameFile.getValue());

            mMusicInfo.setAudio(audio);
        } catch (Exception ex) {
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

        setPainting(false);
        try {
            //seconds1.setImage(second);
            //seconds2.setImage(secondD);
            //minutes1.setImage(minute);
            //minutes2.setImage(minuteD);
            mTimeText.setValue(minuteD+""+minute+":"+secondD+""+second);
        } finally {
            setPainting(true);
        }
    }

    public void stopPlayer() {
        //if (stopIcon != null) {
        if (true) {
            setPainting(false);
            try {
                mButton.setResource(mStop);
                //stopIcon.setVisible(true);
                //playIcon.setVisible(false);
                //pauseIcon.setVisible(false);
                //positionControl.setPosition(0);
                statusBar.setSize(1, statusBar.height);
            } finally {
                setPainting(true);
            }
            try {
                Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
                Audio audio = null;
                if (nameFile.isFile())
                    audio = getAudio(((File) nameFile.getValue()).getCanonicalPath());
                else
                    audio = getAudio((String) nameFile.getValue());

                if (mPlaying && audio != null) {
                    mMusicInfo.setAudio(audio);
                }
            } catch (Exception ex) {
            }
        }
    }

    public void playPlayer() {
        //if (stopIcon != null) {
        if (true) {
            setPainting(false);
            try {
                mButton.setResource(mPlay);
                //stopIcon.setVisible(false);
                //playIcon.setVisible(true);
                //pauseIcon.setVisible(false);
            } finally {
                setPainting(true);
            }
        }
    }

    public void pausePlayer() {
        //if (stopIcon != null) {
        if (true) {
            if (mApplication.getPlayer().getState() != Player.STOP) {
                setPainting(false);
                try {
                    //stopIcon.setVisible(false);
                    //playIcon.setVisible(false);
                    if (mApplication.getPlayer().getState() == Player.PAUSE) {
                        mButton.setResource(mPlay);
                        //pauseIcon.setVisible(false);
                        //playIcon.setVisible(true);
                    } else {
                        mButton.setResource(mPause);
                        //pauseIcon.setVisible(true);
                    }
                } finally {
                    setPainting(true);
                }
            }
        }
    }

    public void nextPlayer() {
        //if (stopIcon != null) {
        if (true) {
            setPainting(false);
            try {
                mButton.setResource(mPlay);
                //stopIcon.setVisible(false);
                //playIcon.setVisible(true);
                //pauseIcon.setVisible(false);
            } finally {
                setPainting(true);
            }
        }
    }

    public boolean handleKeyPress(int code, long rawcode) {
        if (transparency != 0.0f)
            setTransparency(0.0f);
        if (mMusicInfo.handleKeyPress(code, rawcode))
            return true;
        switch (code) {
        case KEY_PAUSE:
            //if (pauseControl != null)
            //    pauseControl.setSelected(true);
            pausePlayer();
            break;
        case KEY_PLAY:
            //if (playControl != null)
            //    playControl.setSelected(true);
            playPlayer();
            break;
        case KEY_CHANNELUP:
            //getBApp().play("select.snd");
            //getBApp().flush();
            //if (previousControl != null)
            //    previousControl.setSelected(true);
            break;
        case KEY_CHANNELDOWN:
            //getBApp().play("select.snd");
            //getBApp().flush();
            //if (nextControl != null)
            //    nextControl.setSelected(true);
            break;
        case KEY_SLOW:
            //if (stopControl != null)
            //    stopControl.setSelected(true);
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
            //if (pauseControl != null)
            //    pauseControl.setSelected(false);
            break;
        case KEY_PLAY:
            //if (playControl != null)
            //    playControl.setSelected(false);
            break;
        case KEY_CHANNELUP:
            //if (previousControl != null)
            //    previousControl.setSelected(false);
            break;
        case KEY_CHANNELDOWN:
            //if (nextControl != null)
            //    nextControl.setSelected(false);
            break;
        case KEY_SLOW:
            //if (stopControl != null)
            //    stopControl.setSelected(false);
            break;
        case KEY_ENTER:
            //if (ejectControl != null)
            //    ejectControl.setSelected(false);
            break;
        }
        return super.handleKeyRelease(code, rawcode);
    }

    private void setPosition(Audio audio, int value) {
        //speedText.setVisible(true);

        if (audio.getDuration() != -1) {
            //statusText.setValue(video.getStatusString() + ": " + rate + " KB/Sec");
            //speedText.setValue(rate+" KB/Sec");
            float barFraction = (float) value / 100.0f;
            if ((statusBarBg.width - 4) * barFraction < 1)
                statusBar.setSize(1, statusBar.height);
            else
                statusBar.setSize((int) (barFraction * (statusBarBg.width - 4)), statusBar.height);
        } else {
            //statusText.setValue(video.getStatusString() + ": " + "0 KB/Sec");
            //speedText.setValue("0 KB/Sec");
            statusBar.setSize(statusBarBg.width - 4, statusBar.height);
        }
    }

    public boolean handleAction(BView view, Object action) {
        Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
        if (action.equals("ready")) {
            if (mPlaying) {
                playPlayer();
            }
            try {
                Audio audio = null;
                if (nameFile.isFile()) {
                    audio = getAudio(((File) nameFile.getValue()).getCanonicalPath());
                } else
                    audio = getAudio((String) nameFile.getValue());
                if (mPlaying && audio != null) {
                    mMusicInfo.setAudio(audio);
                }
            } catch (Exception ex) {
                Tools.logException(MusicPlayer.class, ex);
            }
        } else if (action.equals("playing")) {
            if (mPlaying) {
                if (mApplication.getPlayer().getTotal() != 0) {
                    int value = (int) Math.round(mApplication.getPlayer().getCurrentPosition()
                            / (float) mApplication.getPlayer().getTotal() * 100);
                    try {
                        Audio audio = null;
                        if (nameFile.isFile())
                            audio = getAudio(((File) nameFile.getValue()).getCanonicalPath());
                        else
                            audio = getAudio((String) nameFile.getValue());
                        if (audio != null && audio.getDuration() != -1)
                            setPosition(audio, value);
                    } catch (Exception ex) {
                    }
                    updateTime(mApplication.getPlayer().getCurrentPosition() / 1000);
                }

                int value = mApplication.getPlayer().getBitrate();
                String newValue = Integer.toString(value);
                if (value < 100)
                    newValue = " " + newValue;
                //bitRate.setText(newValue);
            }

            return true;
        } else if (action.equals("stopped")) {
            if (mPlaying) {
                stopPlayer();
                mMusicInfo.setTitle(" ");
                updateTime(0);
            }
            return true;
        } else if (action.equals("update")) {
            if (mPlaying) {
                mMusicInfo.setTitle(mApplication.getPlayer().getTitle());
            } else
                mLastTitle = mApplication.getPlayer().getTitle();
            return true;
        }

        return super.handleAction(view, action);
    }

    public boolean handleEvent(HmeEvent event) {
        switch (event.opcode) {
        case EVT_KEY: {
            //if (title != null && title.handleEvent(event))
            //    return true;
            break;
        }
        }
        return super.handleEvent(event);
    }

    private DefaultApplication mApplication;

    private Audio mAudio;

    // when did the last key press occur
    private long lastKeyPress;

    private boolean mPlaying;

    private String mLastTitle = "";

    private Tracker mTracker;

    private MusicInfo mMusicInfo;

    private BView statusBarBg;

    private BView statusBar;

    private Resource mStop;

    private Resource mPlay;

    private Resource mPause;
    
    private Resource mOverlay;

    private BView mButton;
    
    private BView mShape;
    
    private BText mTimeText;
}