package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.io.File;
import java.util.Iterator;
import org.lnicholls.galleon.apps.music.Music.MusicFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.MusicOptionsScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class MusicMenuScreen extends DefaultMenuScreen {
    public MusicMenuScreen(Music app) {
        super(app, "Music");

        setFooter("Press ENTER for options");

        getBelow().setResource(app.getMenuBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) app.getFactory()).getAppContext()
                .getConfiguration();

        for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
            NameValue nameValue = (NameValue) i.next();
            mMenuList.add(new FolderItem(nameValue.getName(), new File(nameValue.getValue())));
        }
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            if (mMenuList.size() > 0) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
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
                return true;
            }
        } else if (action.equals("play")) {
            load();
            new Thread() {
                public void run() {
                    try {
                        FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
                        File file = (File) nameFile.getValue();
                        FileSystemContainer fileSystemContainer = new FileSystemContainer(file.getCanonicalPath(),
                                true);
                        //((DefaultApplication) getBApp()).setCurrentTrackerContext(file.getCanonicalPath());
                        Tracker tracker = new Tracker(fileSystemContainer
                                .getItemsSorted(FileFilters.audioDirectoryFilter), 0);

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
            return true;
        }
        return super.handleAction(view, action);
    }

    protected void createRow(BView parent, int index) {
        try
        {
        Music app = getApp();
        BView icon = new BView(parent, 9, 2, 32, 32);
        Item nameFile = (Item) mMenuList.get(index);
        String filename = nameFile.getName();
        if (nameFile.isFolder()) {
            icon.setResource(app.getFolderIcon());
            if (filename.endsWith(".lnk"))
                filename = filename.substring(0, filename.indexOf(".lnk"));
        } else {
            File file = (File) nameFile.getValue();
            Audio audio = Music.getAudio(file.getCanonicalPath());
            filename = audio.getTrack() + ". " + audio.getTitle();
            icon.setResource(app.getCdIcon());
        }

        BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
        name.setShadow(true);
        name.setFlags(RSRC_HALIGN_LEFT);
        name.setValue(Tools.trim(Tools.clean(filename), 40));
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
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
        case KEY_PLAY:
            postEvent(new BEvent.Action(this, "play"));
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
}

