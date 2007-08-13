package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.io.File;
import java.util.Iterator;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Playlist;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.MusicOptionsScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;


public class PathScreen extends DefaultMenuScreen {

    private Tracker mTracker;

    private boolean mFirst;

    public PathScreen(Music app, Tracker tracker) {
        this(app, tracker, false);
    }

    public PathScreen(Music app, Tracker tracker, boolean first) {
        super(app, "Music");

        setFooter("Press ENTER for options, 1 for Jukebox");

        getBelow().setResource(app.getMenuBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        mTracker = tracker;
        mFirst = first;

        Iterator iterator = mTracker.getList().iterator();
        while (iterator.hasNext()) {
            Item nameFile = (Item) iterator.next();
            mMenuList.add(nameFile);
        }
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        mFocus = mTracker.getPos();
        mTracker = (Tracker)mTracker.clone();
        return super.handleEnter(arg, isReturn);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            if (mMenuList.size() > 0) {
                load();
                final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.isFolder()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                File file = (File) nameFile.getValue();
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(file
                                        .getCanonicalPath());
                                //((DefaultApplication) getBApp()).setCurrentTrackerContext(file.getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer
                                        .getItemsSorted(FileFilters.audioDirectoryFilter), 0);
                                PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex);
                            }
                        }
                    }.start();
                } else {
                    if (nameFile.isPlaylist()) {
                        try {
                            mTracker.setPos(mMenuList.getFocus());
                            File file = (File) nameFile.getValue();
                            Playlist playlist = (Playlist) MediaManager.getMedia(file.getCanonicalPath());
                            if (playlist != null && playlist.getList() != null) {
                                Tracker tracker = new Tracker(playlist.getList(), 0);
                                PathScreen pathScreen = new PathScreen((Music) getBApp(), tracker);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    } else {
                        new Thread() {
                            public void run() {
                                try {
                                    mTracker.setPos(mMenuList.getFocus());
                                    MusicScreen musicScreen = new MusicScreen(getApp());
                                    musicScreen.setTracker(mTracker);

                                    getBApp().push(musicScreen, TRANSITION_LEFT);
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
        } else if (action.equals("play")) {
            if (mMenuList.size() > 0) {
                load();
                final Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
                if (nameFile.isFolder()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                File file = (File) nameFile.getValue();
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(file
                                        .getCanonicalPath(), true);
                                //((DefaultApplication) getBApp()).setCurrentTrackerContext(file.getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer
                                        .getItems(FileFilters.audioDirectoryFilter), 0);

                                MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                                        .getMusicPlayerConfiguration();
                                tracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
                                getBApp().push(new PlayerScreen((Music) getBApp(), tracker), TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Music.class, ex);
                            }
                        }
                    }.start();
                } else if (nameFile.isPlaylist()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(mMenuList.getFocus());
                                File file = (File) nameFile.getValue();
                                Playlist playlist = (Playlist) MediaManager.getMedia(file.getCanonicalPath());
                                if (playlist != null && playlist.getList() != null) {
                                    Tracker tracker = new Tracker(playlist.getList(), 0);
                                    MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                                            .getMusicPlayerConfiguration();
                                    tracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
                                    getBApp().push(new PlayerScreen((Music) getBApp(), tracker), TRANSITION_LEFT);
                                    getBApp().flush();
                                }
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
        }
        return super.handleAction(view, action);
    }

    protected void createRow(BView parent, int index) {
        Music app = getApp();
        int parentWidth = parent.getWidth();
        int parentHeight = parent.getHeight();
        BView icon = new BView(parent, 9, (parentHeight-32)/2, 32, 32);
        Item nameFile = (Item) mMenuList.get(index);
        String filename = nameFile.getName();
        if (nameFile.isFolder()) {
            icon.setResource(app.getFolderIcon());
            if (filename.endsWith(".lnk"))
                filename = filename.substring(0, filename.indexOf(".lnk"));
        } else {
            if (nameFile.isPlaylist())
                icon.setResource(app.getPlaylistIcon());
            else {
                File file = (File) nameFile.getValue();
                try {
                    Audio audio = Music.getAudio(file.getCanonicalPath());
                    filename = audio.getTrack() + ". " + audio.getTitle();
                } catch (Exception ex) {
                }
                icon.setResource(app.getCdIcon());
            }
        }

        BText name = new BText(parent, 50, 4, parentWidth - 40, parentHeight - 4);
        if (isHighDef()) {
            name.setFont("default-32.font");
        }
        name.setShadow(true);
        name.setFlags(RSRC_HALIGN_LEFT);
        name.setValue(Tools.trim(Tools.clean(filename), 40));
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_NUM1:
        case KEY_THUMBSUP:
            try
            {
                Item nameFile = (Item) (mMenuList.get(mMenuList.getFocus()));
                File file = (File) nameFile.getValue();
                // TODO Playlists?
                if (nameFile.isFolder() || nameFile.isFile()) {
                    getApp().setCurrentTrackerContext(file.getCanonicalPath());

                    mMenuList.flash();
                    return super.handleKeyPress(code, rawcode);
                }
            }
            catch (Exception ex) {
                Tools.logException(Music.class, ex);
            }
            break;
        case KEY_LEFT:
            if (!mFirst) {
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            break;
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
}