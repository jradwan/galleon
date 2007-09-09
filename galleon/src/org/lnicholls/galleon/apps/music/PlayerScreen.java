package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.sdk.HmeEvent;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.ScreenSaver;
import org.lnicholls.galleon.util.ScreenSaverFactory;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultPlayer;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.MusicScreenSaver;
import org.lnicholls.galleon.widget.MusicPlayer;
import org.lnicholls.galleon.widget.DefaultApplication.Player;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;
import org.lnicholls.galleon.winamp.WinampPlayer;


public class PlayerScreen extends DefaultScreen implements ScreenSaverFactory {

    // private WinampPlayer player;

    private DefaultPlayer player;

    private Tracker mTracker;

    private MusicScreenSaver screenSaver;

    public PlayerScreen(Music app, Tracker tracker) {
        super(app, true);

        getBelow().setResource(app.getPlayerBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        boolean sameTrack = false;
        Audio currentAudio = app.getCurrentAudio();
        Tracker currentTracker = app.getTracker();
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

        setFooter("Press INFO for lyrics, REPLAY to return to this screen");

        if (!sameTrack || app.getPlayer().getState() == Player.STOP)
            app.getPlayer().startTrack();
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public ScreenSaver getScreenSaver() {
        return screenSaver;
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
                        if (musicPlayerConfiguration.getPlayer().equals(MusicPlayerConfiguration.CLASSIC)) {
                            int y;
                            if (isHighDef()) {
                                y = getSafeTitleVertical() + 20;
                            } else {
                                y = getSafeTitleHorizontal();
                            }
                            player = new MusicPlayer(PlayerScreen.this, isHighDef(), getContentX(),
                                    y, getContentWidth(),
                                    BODY_HEIGHT, false, getApp(), mTracker);
                        } else {
                            player = new WinampPlayer(PlayerScreen.this, 0, 0, PlayerScreen.this.getWidth(),
                                    PlayerScreen.this.getHeight(), false, getApp(), mTracker);
                        }
                        player.updatePlayer();
                        player.setVisible(true);
                    } finally {
                        setPainting(true);
                    }
                }
                setFocusDefault(player);
                setFocus(player);
                mBusy.setVisible(false);

                MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                        .getMusicPlayerConfiguration();
                if (musicPlayerConfiguration.isScreensaver()) {
                    screenSaver = new MusicScreenSaver();
                    if (player instanceof MusicPlayer) {
                        ((MusicPlayer)player).setScreenSaver(screenSaver);
                    }
                }
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
            if (player != null) {
                player.stopPlayer();
                player.setVisible(false);
                player.flush();
                player.remove();
                player = null;
            }
        } finally {
            setPainting(true);
        }
        return super.handleExit();
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_INFO:
        case KEY_NUM0:
            getBApp().play("select.snd");
            getBApp().flush();
            LyricsScreen lyricsScreen = new LyricsScreen((Music) getBApp(), mTracker);
            getBApp().push(lyricsScreen, TRANSITION_LEFT);
            getBApp().flush();
            return true;
        /*
         * case KEY_NUM0: MusicConfiguration musicConfiguration =
         * (MusicConfiguration) ((MusicFactory) getContext().getFactory())
         * .getAppContext().getConfiguration(); MusicPlayerConfiguration
         * musicPlayerConfiguration =
         * Server.getServer().getMusicPlayerConfiguration(); if
         * (musicPlayerConfiguration.isShowImages()) {
         * getBApp().play("select.snd"); getBApp().flush(); ImagesScreen
         * imagesScreen = new ImagesScreen((Music) getBApp(), mTracker);
         * getBApp().push(imagesScreen, TRANSITION_LEFT); getBApp().flush();
         * return true; } else return false;
         */
        }

        return super.handleKeyPress(code, rawcode);
    }
}
