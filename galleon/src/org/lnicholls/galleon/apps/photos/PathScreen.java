package org.lnicholls.galleon.apps.photos;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BView;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;


public class PathScreen extends DefaultScreen {
    private PGrid grid;
    public PathScreen(Photos app, Tracker tracker) {
        this(app, tracker, false);
    }
    public PathScreen(Photos app, Tracker tracker, boolean first) {
        super(app);
        getBelow().setResource(getApp().getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        setFooter("Press ENTER for options");
        setTitle("Photos");
        mTracker = tracker;
        mFirst = first;
        int w = getContentWidth();
        int h = getContentHeight();
        grid = new PGrid(app, this.getNormal(), getContentX(), getContentY(), w, h,
                (int) (h / 2.9));
        BHighlights highlights = grid.getHighlights();
        highlights.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
        highlights.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);
        setFocusDefault(grid);
        mBusy = new BView(getNormal(), SAFE_TITLE_H, SAFE_TITLE_V, 32, 32);
        mBusy.setResource(getApp().getBusyIcon());
        mBusy.setVisible(false);
    }
    
    public Photos getApp() {
        return (Photos)super.getApp();
    }
    
    public PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    
    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            if (grid.getFocus() != -1) {
                Object object = grid.get(grid.getFocus());
                getBApp().play("select.snd");
                getBApp().flush();
                mTop = grid.getTop();
                ArrayList photos = (ArrayList) object;
                final Item nameFile = (Item) photos.get(grid.getPos() % 3);
                if (nameFile.isFolder()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(grid.getPos());
                                File file = (File) nameFile.getValue();
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(
                                        file
                                        .getCanonicalPath());
                                Tracker tracker = new Tracker(
                                        fileSystemContainer
                                                .getItemsSorted(FileFilters.imageDirectoryFilter),
                                        0);
                                PathScreen pathScreen = new PathScreen(
                                        (Photos) getBApp(), tracker);
                                getBApp().push(pathScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Photos.class, ex);
                            }
                        }
                    }.start();
                } else {
                    new Thread() {
                        public void run() {
                            try {
                                PhotosScreen photosScreen = new PhotosScreen(
                                        (Photos) getBApp());
                                mTracker.setPos(grid.getPos());
                                photosScreen.setTracker(mTracker);
                                getBApp().push(photosScreen,
                                        TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Photos.class, ex);
                            }
                        }
                    }.start();
                }
                return true;
            }
        } else if (action.equals("play")) {
            Object object = grid.get(grid.getFocus());
            getBApp().play("select.snd");
            getBApp().flush();
            mTop = grid.getTop();
            ArrayList photos = (ArrayList) object;
            final Item nameFile = (Item) photos.get(grid.getPos() % 3);
            if (nameFile.isFolder()) {
                new Thread() {
                    public void run() {
                        try {
                            mTracker.setPos(grid.getPos());
                            File file = (File) nameFile.getValue();
                            FileSystemContainer fileSystemContainer = new FileSystemContainer(
                                    file
                                    .getCanonicalPath(), true);
                            Tracker tracker = new Tracker(
                                    fileSystemContainer
                                            .getItems(FileFilters.imageDirectoryFilter),
                                    0);
                            PhotosConfiguration imagesConfiguration = getConfiguration();
                            tracker.setRandom(imagesConfiguration
                                    .isRandomPlayFolders());
                            getBApp().push(
                                    new SlideshowScreen((Photos) getBApp(),
                                            tracker), TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Photos.class, ex);
                        }
                    }
                }.start();
            } else {
                new Thread() {
                    public void run() {
                        try {
                            PhotosScreen photosScreen = new PhotosScreen(
                                    (Photos) getBApp());
                            mTracker.setPos(grid.getPos());
                            getBApp().push(
                                    new SlideshowScreen((Photos) getBApp(),
                                            mTracker), TRANSITION_LEFT);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Photos.class, ex);
                        }
                    }
                }.start();
            }
            return true;
        }
        return super.handleAction(view, action);
    }
    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        if (grid.size() == 0) {
            try {
                setPainting(false);
                // mBusy.setVisible(true);
                ArrayList photos = new ArrayList();
                Iterator iterator = mTracker.getList().iterator();
                while (iterator.hasNext()) {
                    Item nameFile = (Item) iterator.next();
                    photos.add(nameFile);
                    if (photos.size() == 3) {
                        grid.add(photos);
                        photos = new ArrayList();
                    }
                }
                if (photos.size() > 0)
                    grid.add(photos);
                // mBusy.setVisible(false);
                // mBusy.flush();
                grid.setTop(mTop);
                grid.setPos(mTracker.getPos());
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            } finally {
                setPainting(true);
            }
        }
        return super.handleEnter(arg, isReturn);
    }
    public boolean handleExit() {
        grid.shutdown();
        mTop = grid.getTop();
        grid.clear();
        return super.handleExit();
    }
    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_LEFT:
            if (!mFirst) {
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            break;
        case KEY_PLAY:
            postEvent(new BEvent.Action(this, "play"));
            return true;
        case KEY_ENTER:
            getBApp().push(new OptionsScreen((Photos) getBApp()),
                    TRANSITION_LEFT);
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }
    private Tracker mTracker;
    private int mTop;
    private boolean mFirst;
}