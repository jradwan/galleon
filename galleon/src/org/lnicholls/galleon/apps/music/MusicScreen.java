package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BView;
import java.io.File;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.MusicInfo;
import org.lnicholls.galleon.widget.MusicOptionsScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class MusicScreen extends DefaultScreen {

    private BList list;

    private MusicInfo mMusicInfo;

    private Tracker mTracker;

    public MusicScreen(Music app) {
        super(app, "Song", true);

        setFooter("Press ENTER for options");

        getBelow().setResource(app.getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        mMusicInfo = new MusicInfo(this.getNormal(), BORDER_LEFT, TOP, BODY_WIDTH, BODY_HEIGHT, true);

        list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 80,
                (int) Math.round((getWidth() - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
        list.add("Play");
        list.add("Don't do anything");

        setFocusDefault(list);
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        getBelow().setResource(getApp().getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        updateView();

        return super.handleEnter(arg, isReturn);
    }

    private void updateView() {
        Audio audio = currentAudio();
        if (audio != null) {
            Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
            mMusicInfo.setAudio(audio, nameFile.getName());
        }
    }

    public boolean handleExit() {
        mMusicInfo.flush();
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
        case KEY_PLAY:
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
        case KEY_ENTER:
            getBApp().push(new MusicOptionsScreen((Music) getBApp(), getApp().getInfoBackground()), TRANSITION_LEFT);
            return true;
        case KEY_REPLAY:
            if (((Music) getBApp()).getTracker() != null) {
                new Thread() {
                    public void run() {
                        getBApp().push(new PlayerScreen((Music) getBApp(), ((Music) getBApp()).getTracker()),
                                TRANSITION_LEFT);
                        getBApp().flush();
                    }
                }.start();
            }
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
        if (action.equals("play") || action.equals("push")) {

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
                Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
                if (nameFile != null && nameFile.getValue() != null) {
                    if (nameFile.isFile())
                        return Music.getAudio(((File) nameFile.getValue()).getCanonicalPath());
                    else
                        return Music.getAudio((String) nameFile.getValue());
                }
            } catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
        }
        return null;
    }
}
