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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeEventHandler;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.StreamResource;
import com.tivo.hme.sdk.HmeEvent.ResourceInfo;

public class DefaultApplication extends BApplication {

    private static final Logger log = Logger.getLogger(DefaultApplication.class.getName());

    public static String TRACKER = "org.lnicholls.galleon.widget.DefaultApplication.Tracker";

    protected Resource mBusyIcon;

    protected Resource mStarIcon;

    protected void init(Context context) {
        super.init(context);

        mBusyIcon = getSkinImage(null, "busy");
        mStarIcon = getSkinImage(null, "star");

        mCallbacks = new ArrayList();

        mPlayer = new Player(this);
    }

    public Resource getStarIcon() {
        return mStarIcon;
    }

    public Resource getBusyIcon() {
        return mBusyIcon;
    }

    protected Resource getSkinImage(String screen, String key) {
        ByteArrayOutputStream baos = Server.getServer().getSkin().getImage(this.getClass().getName(), screen, key);
        if (mLastObject != null && mLastResource != null) {
            if (mLastObject == baos) {
                return mLastResource;
            }
        }
        mLastObject = baos;

        if (baos != null) {
            /*
             * if (image.getWidth() > 640 || image.getHeight() > 480) image = ImageManipulator.getScaledImage(image,
             * 640, 480);
             */
            try {
                mLastResource = createImage(baos.toByteArray());
                return mLastResource;
            } catch (Exception ex) {
                Tools.logException(DefaultApplication.class, ex);
            }
        }
        mLastResource = createImage(Tools.getDefaultImage());
        return mLastResource;
    }

    protected void dispatchEvent(HmeEvent event) {
        switch (event.opcode) {
        case EVT_KEY:
            HmeEvent.Key e = (HmeEvent.Key) event;
            if (handleCallback(e))
                return;
        }
        super.dispatchEvent(event);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_ENTER:
            if (mCurrentTrackerContext != null) {
                play("thumbsup.snd");
                Tools.savePersistentValue(TRACKER, mCurrentTrackerContext);
            }
            return true;
        case KEY_LEFT:
            play("pageup.snd");
            flush();
            setActive(false); // TODO Make default just pop
            return true;
        case KEY_PAUSE:
            soundPlayed = true;
            mPlayer.pauseTrack();
            return true;
        case KEY_PLAY:
            mPlayer.playTrack();
            return true;
        case KEY_CHANNELUP:
            play("select.snd");
            flush();
            mPlayer.getPrevPos();
            mPlayer.startTrack();
            return true;
        case KEY_CHANNELDOWN:
            play("select.snd");
            flush();
            mPlayer.getNextPos();
            mPlayer.startTrack();
            return true;
        case KEY_SLOW:
            mPlayer.stopTrack();
            break;
        case KEY_CLEAR:
            setActive(false);
            break;
        }
        return super.handleKeyPress(code, rawcode);
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

        private int mPos = -1;
    }

    public static class Player implements IHmeEventHandler, IHmeProtocol {
        public static final int PLAY = 0;

        public static final int PAUSE = 1;

        public static final int STOP = 2;

        public Player(DefaultApplication defaultApplication) {
            mDefaultApplication = defaultApplication;
        }

        public void playTrack(String url) {
            //System.out.println("playTrack:"+url);
            if (mStreamResource != null) {
                //System.out.println("playTrack: remove");
                mStreamResource.removeHandler(this);
                mStreamResource.remove();
                //mStreamResource.close();
                mDefaultApplication.flush();
            }
            mPlayerState = PLAY;
            mStreamResource = mDefaultApplication.createStream(url, "audio/mp3", true);
            log.debug("mStreamResource=" + mStreamResource.getID());
            mStreamResource.addHandler(this);
        }

        public void playTrack() {
            //System.out.println("playTrack:");
            if (mPlayerState != PLAY) {
                if (mStreamResource != null) {
                    mPlayerState = PLAY;
                    mStreamResource.play();
                } else
                    startTrack();
            }
        }

        public void startTrack() {
            //System.out.println("startTrack:");
            if (mTracker != null) {
                try {
                    if (mTracker.getPos() == -1)
                        getNextPos();
                    Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
                    Audio audio = null;
                    if (nameFile.isFile()) {
                        File file = (File) nameFile.getValue();
                        audio = getAudio(file.getCanonicalPath());
                    } else
                        audio = getAudio((String) nameFile.getValue());

                    if (audio != null) {
                        setTitle("");
                        stopTrack();
                        String url = mDefaultApplication.getContext().base.toString();
                        try {
                            if (nameFile.isFile())
                                url += URLEncoder.encode(mDefaultApplication.hashCode() + "/" + audio.getId() + ".mp3",
                                        "UTF-8");
                            else
                                url += URLEncoder.encode(mDefaultApplication.hashCode() + "/" + audio.getId()
                                        + ".http.mp3", "UTF-8");
                        } catch (UnsupportedEncodingException ex) {
                            Tools.logException(DefaultApplication.class, ex, url);
                        }
                        try {
                            audio.setPlayCount(audio.getPlayCount() + 1);
                            audio.setDatePlayed(new Date());
                            AudioManager.updateAudio(audio);
                        } catch (Exception ex) {
                            Tools.logException(DefaultApplication.class, ex);
                        }
                        
                        mCurrentAudio = audio;

                        playTrack(url);
                    }
                } catch (Exception ex) {
                    Tools.logException(DefaultApplication.class, ex);
                }
            }
        }

        public void pauseTrack() {
            //System.out.println("pauseTrack:");
            if (mPlayerState != STOP) {
                if (mPlayerState == PAUSE) {
                    playTrack();
                } else {
                    if (mStreamResource != null) {
                        mPlayerState = PAUSE;
                        mStreamResource.pause();
                    }
                }
            }
        }

        public void stopTrack() {
            //System.out.println("stopTrack:");
            if (mPlayerState != STOP) {
                if (mStreamResource != null) {
                    mPlayerState = STOP;

                    mStreamResource.removeHandler(this);
                    mStreamResource.remove();
                    //mStreamResource.close();
                    mStreamResource = null;
                    mDefaultApplication.flush();
                    reset();
                }
            }
        }

        public void postEvent(HmeEvent event) {
            //System.out.println("postEvent:");
            // TODO Implement listeners
            HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo) event;
            switch (event.opcode) {
            case StreamResource.EVT_RSRC_INFO:
                if (info.status == RSRC_STATUS_PLAYING) {
                    String pos = (String) info.map.get("pos");
                    if (pos != null) {
                        try {
                            StringTokenizer tokenizer = new StringTokenizer(pos, "/");
                            if (tokenizer.countTokens() == 2) {
                                mCurrentPosition = Integer.parseInt(tokenizer.nextToken());
                                mTotal = Integer.parseInt(tokenizer.nextToken());
                            }
                        } catch (Exception ex) {
                        }
                    }
                    String bitrate = (String) info.map.get("bitrate");
                    if (bitrate != null) {
                        try {
                            mBitrate = (int) Math.round(Float.parseFloat(bitrate) / 1024);
                        } catch (Exception ex) {
                        }
                    }
                }
                if (mDefaultApplication.getCurrentScreen().focus != null)
                    mDefaultApplication.getCurrentScreen().focus.handleEvent(new BEvent.Action(mDefaultApplication
                            .getCurrentScreen().focus, ResourceInfo.statusToString(info.status)));
                return;
            case StreamResource.EVT_RSRC_STATUS:
                if (info.status == RSRC_STATUS_PLAYING) {
                    if (mDefaultApplication.getCurrentScreen().focus != null)
                        mDefaultApplication.getCurrentScreen().focus.handleEvent(new BEvent.Action(mDefaultApplication
                                .getCurrentScreen().focus, "ready"));
                } else if (info.status >= RSRC_STATUS_CLOSED) {
                    //System.out.println("postEvent:RSRC_STATUS_CLOSED");
                    if (mPlayerState != STOP) {
                        stopTrack();

                        reset();

                        getNextPos();
                        startTrack();

                        if (mDefaultApplication.getCurrentScreen().focus != null)
                            mDefaultApplication.getCurrentScreen().focus.handleEvent(new BEvent.Action(
                                    mDefaultApplication.getCurrentScreen().focus, "stopped"));
                    }
                }
                return;
            }
        }

        public void setTitle(String value) {
            mTitle = value;
            if (mDefaultApplication.getCurrentScreen().focus != null)
                mDefaultApplication.getCurrentScreen().focus.handleEvent(new BEvent.Action(mDefaultApplication
                        .getCurrentScreen().focus, "update"));
        }

        public void getNextPos() {
            //System.out.println("getNextPos:");
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
            //System.out.println("getPrevPos:");
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                Item nameFile = (Item) mTracker.getList().get(pos);
                while (nameFile.isFolder() || nameFile.isPlaylist()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (Item) mTracker.getList().get(pos);
                }
            }
        }

        private void reset() {
            //System.out.println("reset:");
            mCurrentPosition = 0;
            mTotal = 0;
            mBitrate = 0;
            mTitle = "";
        }

        public void setTracker(Tracker tracker) {
            if (mTracker != null)
                stopTrack();

            mTracker = tracker;
        }

        public Tracker getTracker() {
            return mTracker;
        }

        public int getCurrentPosition() {
            return mCurrentPosition;
        }

        public int getTotal() {
            return mTotal;
        }

        public int getBitrate() {
            return mBitrate;
        }

        public int getState() {
            return mPlayerState;
        }

        public String getTitle() {
            return mTitle;
        }
        
        public Audio getCurrentAudio() {
            return mCurrentAudio;
        }

        private DefaultApplication mDefaultApplication;

        private StreamResource mStreamResource;

        private Tracker mTracker;

        private int mPlayerState = STOP;

        private int mCurrentPosition;

        private int mTotal;

        private int mBitrate;

        private String mTitle = "";
        
        private Audio mCurrentAudio;
    }

    private static Audio getAudio(String path) {
        Audio audio = null;
        try {
            List list = AudioManager.findByPath(path);
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(DefaultApplication.class, ex);
        }

        if (audio == null) {
            try {
                audio = (Audio) MediaManager.getMedia(path);
                AudioManager.createAudio(audio);
            } catch (Exception ex) {
                Tools.logException(DefaultApplication.class, ex);
            }
        }
        return audio;
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public boolean handleCallback(HmeEvent event) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            Callback callback = (Callback) mCallbacks.get(i);
            if (callback.handleEvent(event)) {
                mCallbacks.remove(i);
                return true;
            }
        }

        return false;
    }

    public Player getPlayer() {
        return mPlayer;
    }

    public void setTracker(Tracker tracker) {
        mPlayer.setTracker(tracker);
        List list = tracker.getList();
        if (list.size() > 0) {
            Item nameFile = (Item) list.get(0);
            if (nameFile.isFile()) {
                File file = (File) nameFile.getValue();
                //Tools.savePersistentValue(DefaultApplication.TRACKER, file.getParent());
            }
        }
    }

    public Tracker getTracker() {
        return mPlayer.getTracker();
    }
    
    public Audio getCurrentAudio() {
        return mPlayer.getCurrentAudio();
    }

    public void setCurrentTrackerContext(String value) {
        mCurrentTrackerContext = value;
    }

    public boolean handleApplicationError(int errorCode, String errorText) {
        log.debug(this + " handleApplicationError(" + errorCode + "," + errorText + ")");
        return true;
    }

    /*
     * protected void writeStream(String filename) throws IOException { InputStream in = getApp().getStream(filename);
     * try { while (true) { int count = in.read(buf, 0, buf.length); if (count < 0) { break; } context.out.write(buf, 0,
     * count); } } finally { try { in.close(); } catch (IOException e) { } } }
     */

    // TODO Need to handle multiple apps
    private Player mPlayer;

    private ArrayList mCallbacks;

    private String mCurrentTrackerContext;

    private ByteArrayOutputStream mLastObject;

    private Resource mLastResource;
}