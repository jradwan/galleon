package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import java.awt.Color;
import java.io.File;
import java.util.List;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.util.Lyrics;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.ScrollText;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class LyricsScreen extends DefaultScreen {
    private BList list;

    private ScrollText scrollText;

    private Thread mLyricsThread;

    private Tracker mTracker;

    public LyricsScreen(Music app, Tracker tracker) {
        super(app, "Lyrics", false);

        getBelow().setResource(app.getLyricsBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        mTracker = tracker;

        // setFocusDefault(scrollText);

        // setFooter("lyrc.com.ar");
        //setFooter("lyrictracker.com");
        setFooter("autolyrics.com");

        mBusy.setVisible(true);

        Object font;
        int buttonHeight;
        if (isHighDef()) {
            buttonHeight = 60;
            font = "default-32.font";
        } else {
            buttonHeight = 35;
            font = "default-24.font";
        }
        BButton button = new BButton(getNormal(), getContentX(), getContentBottom() - buttonHeight - 10, 
                Math.round((getWidth() - (getSafeTitleHorizontal() * 2)) / 2), buttonHeight);
        button.setResource(createText(font, Color.white, "Return to player"));
        button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);

        scrollText = new ScrollText(getNormal(), getContentX(), getContentY(), getContentWidth(), 
                getContentHeight() - buttonHeight - 25, "");
        scrollText.setVisible(false);
        
        setFocus(button);
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public void updateLyrics() {
        try {
            setPainting(false);
            if (mLyricsThread != null && mLyricsThread.isAlive())
                mLyricsThread.interrupt();
        } finally {
            setPainting(true);
        }
        Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
        Audio audio = null;
        try {
			audio = AudioManager.findByItem(nameFile);
        } catch (Exception ex) {
            Tools.logException(Music.class, ex);
        }
        if (audio.getLyrics() != null && audio.getLyrics().length() > 0) {
            try {
                setPainting(false);
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
                                    Tools.logException(Music.class, ex, "Could not update lyrics");
                                }
                            }
                        }
                        synchronized (this) {
                            try {
                                setPainting(false);
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
                        Tools.logException(Music.class, ex, "Could retrieve lyrics");
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
        try {
            setPainting(false);
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
}
