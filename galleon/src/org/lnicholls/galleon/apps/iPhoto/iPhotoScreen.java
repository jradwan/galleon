package org.lnicholls.galleon.apps.iPhoto;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.SimpleDateFormat;

import org.lnicholls.galleon.apps.photos.Photos;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.media.JpgFile;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.LabelText;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;


public class iPhotoScreen extends DefaultScreen {
    private BList list;
    public iPhotoScreen(iPhoto app) {
        super(app, true);
        setFooter("Press ENTER for options");
        getBelow().setResource(getApp().getInfoBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        setTitle("Photo");
        mDateFormat = new SimpleDateFormat();
        mDateFormat.applyPattern("EEE MMM d, yyyy hh:mm a");
        int start = BORDER_TOP;
        int thumb_size = isHighDef() ? 400 : 200;
        mThumbnail = new BView(getBelow(), getWidth() - SAFE_TITLE_H - thumb_size - 10,
                getHeight() - SAFE_TITLE_V - thumb_size, thumb_size,
                thumb_size, false);
        mTitleText = new LabelText(getNormal(), BORDER_LEFT, start,
                BODY_WIDTH, 30, true);
        mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP
                | RSRC_VALIGN_TOP);
        mTitleText.setFont("system-24-bold.font");
        mTitleText.setColor(Color.CYAN);
        mTitleText.setShadow(true);
        start += 50;
        mTakenText = new LabelText(getNormal(), BORDER_LEFT, start,
                BODY_WIDTH, 20, true);
        mTakenText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
        mTakenText.setFont("default-18-bold.font");
        mTakenText.setShadow(true);
        mImportedText = new LabelText(getNormal(), BORDER_LEFT, start,
                BODY_WIDTH, 20, true);
        mImportedText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
        mImportedText.setFont("default-18-bold.font");
        mImportedText.setShadow(true);
        start += 20;
        mModifiedText = new LabelText(getNormal(), BORDER_LEFT, start,
                BODY_WIDTH, 20, true);
        mModifiedText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
        mModifiedText.setFont("default-18-bold.font");
        mModifiedText.setShadow(true);
        // XXX add original size text ...
        mStars = new BView[5];
        for (int i = 0; i < 5; i++) {
            mStars[i] = new BView(getNormal(), BORDER_LEFT + (i * 40),
                    getHeight() - SAFE_TITLE_V - 200, 34, 34,
                    true);
            mStars[i].setResource(getApp().getStarIcon(), RSRC_IMAGE_BESTFIT);
            mStars[i].setTransparency(0.6f);
        }
        list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10,
                (getHeight() - SAFE_TITLE_V) - 150,
                (int) Math.round((getWidth() - (SAFE_TITLE_H * 2)) / 2.5),
                150, 35);
        list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        list.add("View photo");
        list.add("View slideshow");
        list.add("Rotate photo");
        list.add("Don't do anything");
        setFocusDefault(list);
    }
    
    public iPhoto getApp() {
        return (iPhoto)super.getApp();
    }
    
    public iPhotoConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    
    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        updateView();
        return super.handleEnter(arg, isReturn);
    }
    private void updateView() {
        final Image image = currentImage();
        if (image != null) {
            try {
                setPainting(false);
                clearThumbnail();
                File file = new File(image.getPath());
                String name = Tools.extractName(file.getName());
                mTitleText.setValue(name);
                mTakenText.setValue("Taken: "
                        + mDateFormat.format(image.getDateCreated()));
                mImportedText.setValue("Imported: "
                        + mDateFormat.format(image.getDateCaptured()));
                mModifiedText.setValue("Modified: "
                        + mDateFormat.format(image.getDateModified()));
                setRating();
                updateHints();
                final iPhotoConfiguration imagesConfiguration = getConfiguration();
                if (mThumbnailThread != null && mThumbnailThread.isAlive())
                    mThumbnailThread.interrupt();
                mThumbnailThread = new Thread() {
                    public void run() {
                        try {
                            BufferedImage thumbnail = JpgFile
                                    .getThumbnail(image);
                            if (thumbnail != null) {
                                synchronized (mThumbnail) {
                                    if (mThumbnail.getID() != -1) {
                                        synchronized (this) {
                                            if (image.getRotation() != 0) {
                                                thumbnail = ImageManipulator
                                                        .rotate(
                                                                thumbnail,
                                                                mThumbnail
                                                                .getWidth(),
                                                                mThumbnail
                                                                        .getHeight(),
                                                                image
                                                                        .getRotation());
                                            }
                                            mThumbnail.setResource(
                                                    createImage(thumbnail),
                                                    RSRC_IMAGE_BESTFIT);
                                            mThumbnail.setVisible(true);
                                            mThumbnail.setTransparency(1.0f);
                                            mThumbnail.setTransparency(0.0f, mAnim);
                                            mThumbnail.flush();
                                            Tools.clearResource(mThumbnail);
                                        }
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            Tools.logException(iPhoto.class, ex,
                                    "Could retrieve thumbnail");
                        }
                    }
                    public void interrupt() {
                        synchronized (this) {
                            super.interrupt();
                        }
                    }
                };
                mThumbnailThread.start();
            } finally {
                setPainting(true);
            }
        }
    }
    private void clearThumbnail() {
        Image image = currentImage();
        if (image != null) {
            mThumbnail.setVisible(false);
            if (mThumbnail.getResource() != null)
            {
                mThumbnail.getResource().flush();
                mThumbnail.getResource().remove();
            }
        }
    }
    public boolean handleExit() {
        try {
            setPainting(false);
            if (mThumbnailThread != null && mThumbnailThread.isAlive())
                mThumbnailThread.interrupt();
            clearThumbnail();
            System.gc();
        } finally {
            setPainting(true);
        }
        return super.handleExit();
    }
    public boolean handleKeyPress(int code, long rawcode) {
        Image image = currentImage();
        switch (code) {
        case KEY_THUMBSDOWN:
            if (image != null && image.getRating() > 0) {
                getBApp().play("thumbsdown.snd");
                getBApp().flush();
                try {
                    image.setRating(Math.max(image.getRating() - 1, 0));
                    ImageManager.updateImage(image);
                } catch (Exception ex) {
                    Tools.logException(iPhoto.class, ex);
                }
                setRating();
            } else {
                getBApp().play("bonk.snd");
                getBApp().flush();
            }
            return true;
        case KEY_THUMBSUP:
            if (image != null && image.getRating() < 5) {
                getBApp().play("thumbsup.snd");
                getBApp().flush();
                try {
                    image.setRating(Math.min(image.getRating() + 1, 5));
                    ImageManager.updateImage(image);
                } catch (Exception ex) {
                    Tools.logException(iPhoto.class, ex);
                }
                setRating();
            } else {
                getBApp().play("bonk.snd");
                getBApp().flush();
            }
            return true;
        case KEY_SELECT:
        case KEY_RIGHT:
            if (list.getFocus() == 2) {
                postEvent(new BEvent.Action(this, "rotate"));
                return true;
            } else if (list.getFocus() == 0) {
                postEvent(new BEvent.Action(this, "play"));
                return true;
            } else if (list.getFocus() == 1) {
                postEvent(new BEvent.Action(this, "slideshow"));
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
            getBApp().push(new OptionsScreen((iPhoto) getBApp()),
                    TRANSITION_LEFT);
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }
    public void getNextPos() {
        if (mTracker != null) {
            int pos = mTracker.getNextPos();
            Item nameFile = (Item) mTracker.getList().get(pos);
            while (nameFile.isFolder()) {
                pos = mTracker.getNextPos();
                nameFile = (FileItem) mTracker.getList().get(pos);
            }
        }
    }
    public void getPrevPos() {
        if (mTracker != null) {
            int pos = mTracker.getPrevPos();
            Item nameFile = (Item) mTracker.getList().get(pos);
            while (nameFile.isFolder()) {
                pos = mTracker.getPrevPos();
                nameFile = (FileItem) mTracker.getList().get(pos);
            }
        }
    }
    private void setRating() {
        Image image = currentImage();
        if (image != null) {
            for (int i = 0; i < 5; i++) {
                if (i < image.getRating())
                    mStars[i].setTransparency(0.0f);
                else
                    mStars[i].setTransparency(0.6f);
            }
        }
    }
    public boolean handleAction(BView view, Object action) {
        if (action.equals("rotate")) {
            getBApp().play("select.snd");
            getBApp().flush();
            DefaultApplication application = (DefaultApplication) getApp();
            if (!application.isDemoMode())
            {
                Image image = currentImage();
                try {
                    int rotation = 0;
                    if (image.getRotation() != 0)
                        rotation = image.getRotation();
                    rotation = rotation + 90;
                    image.setRotation(rotation % 360);
                    ImageManager.updateImage(image);
                } catch (Exception ex) {
                    Tools.logException(iPhoto.class, ex);
                }
                updateView();
            }
            return true;
        } else if (action.equals("play")) {
            getBApp().play("select.snd");
            getBApp().flush();
            new Thread() {
                public void run() {
                    getBApp().push(
                            new SlideshowScreen((iPhoto) getBApp(),
                                    mTracker, false), TRANSITION_LEFT);
                    getBApp().flush();
                }
            }.start();
            return true;
        } else if (action.equals("slideshow")) {
            getBApp().play("select.snd");
            getBApp().flush();
            new Thread() {
                public void run() {
                    getBApp().push(
                            new SlideshowScreen((iPhoto) getBApp(),
                                    mTracker, true), TRANSITION_LEFT);
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
    private Image currentImage() {
        if (mTracker != null) {
            try {
                FileItem nameFile = (FileItem) mTracker.getList().get(
                        mTracker.getPos());
                if (nameFile != null) {
                    return Photos.getImage(((File) nameFile.getValue())
                            .getCanonicalPath());
                }
            } catch (Exception ex) {
                Tools.logException(iPhoto.class, ex);
            }
        }
        return null;
    }
    private SimpleDateFormat mDateFormat;
    private Resource mAnim = getResource("*1000");
    private BView mThumbnail;
    private LabelText mTitleText;
    private LabelText mTakenText;
    private LabelText mImportedText;
    private LabelText mModifiedText;
    private Tracker mTracker;
    private BView[] mStars;
    private Thread mThumbnailThread;
}
