package org.lnicholls.galleon.apps.photos;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;
import java.awt.image.BufferedImage;
import java.io.File;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class SlideshowScreen extends DefaultScreen {
    private Resource mAnim = getResource("*100");
    Image mImage;
    View mPhoto;
    Slideshow mSlideshow;
    // when did the last key press occur
    long lastKeyPress;
    private Tracker mTracker;
    private boolean mShowSlideshow;
    
    public SlideshowScreen(Photos app, Tracker tracker) {
        this(app, tracker, true);
    }
    public SlideshowScreen(Photos app, Tracker tracker,
            boolean showSlideshow) {
        super(app, null, null, false);
        mTracker = tracker;
        mShowSlideshow = showSlideshow;
        setTitle(" ");
        PhotosConfiguration imagesConfiguration = getConfiguration();
        if (imagesConfiguration.isUseSafe())
            mPhoto = new View(getBelow(), SAFE_ACTION_H, SAFE_ACTION_V,
                    getWidth() - 2 * SAFE_ACTION_H, getHeight()
                    - 2 * SAFE_ACTION_V);
        else
            mPhoto = new View(getBelow(), 0, 0, getWidth(), getHeight());
    }
    
    public Photos getApp() {
        return (Photos)super.getApp();
    }
    
    public PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    
    private void updateView() {
        final Image image = currentImage();
        if (image != null) {
            try {
                setPainting(false);
                // clearImage();
                final File file = new File(image.getPath());
                updateHints();
                new Thread() {
                    public void run() {
                        try {
                            BufferedImage photo = null;
                            try {
                                photo = Tools.ImageIORead(file);
                            } catch (OutOfMemoryError ex) {
                                Tools.logMemory();
                                photo = Tools.ImageIORead(file);
                            }
                            if (photo != null) {
                                photo = (BufferedImage) Tools.getImage(photo);
                                BufferedImage scaled = ImageManipulator
                                        .getScaledImage(photo, mPhoto
                                                .getWidth(),
                                        mPhoto.getHeight());
                                if (image.getRotation() != 0) {
                                    scaled = ImageManipulator.rotate(
                                            scaled, mPhoto.getWidth(),
                                            mPhoto.getHeight(),
                                            image.getRotation());
                                }
                                if (scaled != null) {
                                    mPhoto.setResource(createImage(scaled),
                                            RSRC_IMAGE_BESTFIT);
                                    mPhoto.setVisible(true);
                                    // mPhoto.setTransparency(1);
                                    // mPhoto.setTransparency(0, mAnim);
                                    getBApp().flush();
                                    scaled.flush();
                                    scaled = null;
                                    Tools.clearResource(mPhoto);
                                }
                            }
                        } catch (Throwable ex) {
                            Tools.logMemory();
                            Tools.logException(Photos.class, ex,
                                    "Could not retrieve image: "
                                    + file.getAbsolutePath());
                        }
                    }
                }.start();
            } finally {
                setPainting(true);
            }
        }
    }
    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        updateView();
        if (mShowSlideshow) {
            mSlideshow = new Slideshow(this);
            mSlideshow.start();
        }
        return super.handleEnter(arg, isReturn);
    }
    private void clearImage() {
        Image image = currentImage();
        if (image != null) {
            mPhoto.setVisible(false);
            if (mPhoto.getResource() != null)
            {
                mPhoto.getResource().flush();
                mPhoto.getResource().remove();
            }
            getBApp().flush();
        }
    }
    public boolean handleExit() {
        try {
            setPainting(false);
            clearImage();
            if (mSlideshow != null && mSlideshow.isAlive()) {
                mSlideshow.interrupt();
                mSlideshow = null;
            }
            System.gc();
        } finally {
            setPainting(true);
        }
        return super.handleExit();
    }
    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_UP:
            code = KEY_CHANNELUP;
            getApp().handleKeyPress(code, rawcode);
            return true;
        case KEY_DOWN:
            code = KEY_CHANNELDOWN;
            getApp().handleKeyPress(code, rawcode);
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
    public void getNextPos() {
        if (mTracker != null && mTracker.getList().size() > 0) {
            int pos = mTracker.getNextPos();
            Item nameFile = (Item) mTracker.getList().get(pos);
            while (nameFile.isFolder()) {
                pos = mTracker.getNextPos();
                nameFile = (FileItem) mTracker.getList().get(pos);
            }
        }
    }
    public void getPrevPos() {
        if (mTracker != null && mTracker.getList().size() > 0) {
            int pos = mTracker.getPrevPos();
            Item nameFile = (Item) mTracker.getList().get(pos);
            while (nameFile.isFolder()) {
                pos = mTracker.getPrevPos();
                nameFile = (FileItem) mTracker.getList().get(pos);
            }
        }
    }
    protected Image currentImage() {
        if (mTracker != null && mTracker.getList().size() > 0) {
            try {
                FileItem nameFile = (FileItem) mTracker.getList().get(
                        mTracker.getPos());
                if (nameFile != null) {
                    return Photos.getImage(((File) nameFile.getValue())
                            .getCanonicalPath());
                }
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            }
        }
        return null;
    }
}
