package org.lnicholls.galleon.apps.photos;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.io.File;
import java.util.Iterator;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;


public class PhotosMenuScreen extends DefaultMenuScreen {
    public PhotosMenuScreen(Photos app) {
        super(app, "Photos");
        setFooter("Press ENTER for options");
        getBelow().setResource(app.getMenuBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        PhotosConfiguration imagesConfiguration = getConfiguration();
        for (Iterator i = imagesConfiguration.getPaths().iterator(); i
                .hasNext(); /* Nothing */) {
            NameValue nameValue = (NameValue) i.next();
            mMenuList.add(new FolderItem(nameValue.getName(), new File(
                    nameValue.getValue())));
        }
    }
    
    public Photos getApp() {
        return (Photos)super.getApp();
    }
    
    public PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            if (mMenuList.size() > 0) {
                load();
                new Thread() {
                    public void run() {
                        try {
                            FileItem nameFile = (FileItem) (mMenuList
                                    .get(mMenuList.getFocus()));
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
                return true;
            }
        } else if (action.equals("play")) {
            load();
            new Thread() {
                public void run() {
                    try {
                        FileItem nameFile = (FileItem) (mMenuList
                                .get(mMenuList.getFocus()));
                        File file = (File) nameFile.getValue();
                        FileSystemContainer fileSystemContainer = new FileSystemContainer(
                                file.getCanonicalPath(),
                                true);
                        Tracker tracker = new Tracker(fileSystemContainer
                        .getItemsSorted(FileFilters.imageDirectoryFilter),
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
            return true;
        }
        return super.handleAction(view, action);
    }
    protected void createRow(BView parent, int index) {
        BView icon = new BView(parent, 9, 2, 32, 32);
        Item nameFile = (Item) mMenuList.get(index);
        if (nameFile.isFolder()) {
            icon.setResource(getApp().getFolderIcon());
            icon.flush();
        } else {
            icon.setResource(getApp().getCameraIcon());
            icon.flush();
        }
        BText name = new BText(parent, 50, 4, parent.getWidth() - 40,
                parent.getHeight() - 4);
        name.setShadow(true);
        name.setFlags(RSRC_HALIGN_LEFT);
        name.setValue(Tools.trim(nameFile.getName(), 40));
    }
    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_PLAY:
            postEvent(new BEvent.Action(this, "play"));
            return true;
        case KEY_ENTER:
            getBApp().push(new OptionsScreen((Photos) getBApp()),
                    TRANSITION_LEFT);
        }
        return super.handleKeyPress(code, rawcode);
    }
}