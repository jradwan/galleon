package org.lnicholls.galleon.apps.photos;

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

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.*;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.media.JpgFile;
import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.NameFile;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;
import org.lnicholls.galleon.widget.*;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlight;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;
import com.tivo.hme.util.ArgumentList;

public class Photos extends DefaultApplication {

    private static Logger log = Logger.getLogger(Photos.class.getName());
    
    private final static Runtime runtime = Runtime.getRuntime();

    public final static String TITLE = "Photos";

    private Resource mBackground;

    private Resource mIcon;

    private Resource mBusyIcon;

    private Resource mBusy2Icon;

    private Resource mFolderIcon;

    private Resource mCameraIcon;

    private Resource mStarIcon;

    private PhotosScreen mImagesScreen;

    protected void init(Context context) {
        super.init(context);
        
        mBackground = getResource("background.jpg");

        mIcon = getResource("icon.png");

        mBusyIcon = getResource("busy.gif");

        mBusy2Icon = getResource("busy2.gif");

        mFolderIcon = getResource("folder.png");

        mCameraIcon = getResource("camera.png");

        mStarIcon = getResource("star.png");

        mImagesScreen = new PhotosScreen(this);
        
        String path = Tools.loadPersistentValue(DefaultApplication.TRACKER);
        if (path!=null)
        {
            FileSystemContainer fileSystemContainer = new FileSystemContainer(path);
            Tracker tracker = new Tracker(fileSystemContainer.getItems(FileFilters.audioDirectoryFilter), 0);
            setTracker(tracker);
        }

        push(new PhotosMenuScreen(this), TRANSITION_NONE);
    }

    public class PhotosMenuScreen extends DefaultScreen {
        private PList list;
        private Grid grid;

        public PhotosMenuScreen(Photos app) {
            super(app);
            setTitle("Photos");

            list = new PList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);
            
            /*
            int w = width - ((SAFE_TITLE_H * 2) + 32);
            int h = 280;
            grid = new Grid(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, w, h, w/3, w/3);
            */

            PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) context.factory)
                    .getAppContext().getConfiguration();

            for (Iterator i = imagesConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                list.add(new NameFile(nameValue.getName(), new File(nameValue.getValue())));
                //grid.add(new NameFile(nameValue.getName(), new File(nameValue.getValue())));
            }

            setFocusDefault(list);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                icon.setResource(mBusy2Icon);
                icon.flush();

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        try {
                            NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                            FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                    .getCanonicalPath());
                            Tracker tracker = new Tracker(fileSystemContainer
                                    .getItems(FileFilters.imageDirectoryFilter), 0);
                            PathScreen pathScreen = new PathScreen((Photos) getBApp(), tracker);
                            getBApp().push(pathScreen, TRANSITION_LEFT);
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

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (list.getFocus() >= 0) {
                NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                if (nameFile.getFile().isDirectory())
                    icon.setResource(mFolderIcon);
                else
                    icon.setResource(mCameraIcon);
                icon.flush();
            }
            return super.handleEnter(arg, isReturn);
        }
    }

    public class PList extends BList {
        public PList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameFile nameFile = (NameFile) get(index);
            if (nameFile.getFile().isDirectory()) {
                icon.setResource(mFolderIcon);
            } else {
                icon.setResource(mCameraIcon);
            }

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(nameFile.getName(), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "push"));
                return true;
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                boolean result = super.handleKeyPress(code, rawcode);
                if (!result) {
                    getBApp().play("bonk.snd");
                    getBApp().flush();
                }
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public int getTop() {
            return top;
        }
    }

    public class PathScreen extends DefaultScreen {
        private PList list;

        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public PathScreen(Photos app, Tracker tracker) {
            super(app);

            setTitle("Photos");

            mTracker = tracker;

            list = new PList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            setFocusDefault(list);

            mBusy = new BView(normal, SAFE_TITLE_H, SAFE_TITLE_V, 32, 32);
            mBusy.setResource(mBusyIcon);
            mBusy.setVisible(false);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                Object object = list.get(list.getFocus());
                BView row = list.getRow(list.getFocus());
                BView icon = (BView) row.children[0];
                icon.setResource(mBusy2Icon);
                icon.flush();

                getBApp().play("select.snd");
                getBApp().flush();

                final NameFile nameFile = (NameFile) object;
                if (nameFile.getFile().isDirectory()) {
                    new Thread() {
                        public void run() {
                            try {
                                mTracker.setPos(list.getFocus());

                                NameFile nameFile = (NameFile) (list.get(list.getFocus()));
                                FileSystemContainer fileSystemContainer = new FileSystemContainer(nameFile.getFile()
                                        .getCanonicalPath());
                                Tracker tracker = new Tracker(fileSystemContainer
                                        .getItems(FileFilters.imageDirectoryFilter), 0);
                                PathScreen pathScreen = new PathScreen((Photos) getBApp(), tracker);
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
                                Image image = getImage(nameFile.getFile().getCanonicalPath());

                                mTracker.setPos(list.getFocus());
                                mImagesScreen.setTracker(mTracker);

                                getBApp().push(mImagesScreen, TRANSITION_LEFT);
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
            if (list.size() == 0) {
                setPainting(false);
                try {
                    mBusy.setVisible(true);

                    Iterator iterator = mTracker.getList().iterator();
                    while (iterator.hasNext()) {
                        NameFile nameFile = (NameFile) iterator.next();
                        list.add(nameFile);
                    }
                    mBusy.setVisible(false);
                    mBusy.flush();
                    list.setFocus(0, false);
                    list.flush();
                } catch (Exception ex) {
                    Tools.logException(Photos.class, ex);
                } finally {
                    setPainting(true);
                }
                list.setTop(mTop);
                list.setFocus(mTracker.getPos(), false);
            } else {
                if (list.getFocus() >= 0) {
                    NameFile nameFile = (NameFile) list.get(list.getFocus());
                    BView row = list.getRow(list.getFocus());
                    BView icon = (BView) row.children[0];
                    if (nameFile.getFile().isDirectory())
                        icon.setResource(mFolderIcon);
                    else
                        icon.setResource(mCameraIcon);
                    icon.flush();
                }
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            mTop = list.getTop();
            list.clear();
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        private Tracker mTracker;

        private BView mBusy;

        private int mTop;
    }

    public class PhotosScreen extends DefaultScreen {

        private BList list;

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public PhotosScreen(Photos app) {
            super(app, true);

            setTitle("Photo");

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm a");

            int start = top;

            mThumbnail = new BView(below, width - SAFE_TITLE_H - 210, height - SAFE_TITLE_V - 200, 200, 200, false);

            mTitleText = new BText(normal, border_left, start - 30, text_width, 70);
            mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            mTitleText.setFont("default-30-bold.font");
            mTitleText.setColor(Color.CYAN);
            mTitleText.setShadow(true);

            start += 40;

            mTakenText = new BText(normal, border_left, start, text_width, 20);
            mTakenText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mTakenText.setFont("default-18-bold.font");
            mTakenText.setShadow(true);

            mImportedText = new BText(normal, border_left, start, text_width, 20);
            mImportedText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            mImportedText.setFont("default-18-bold.font");
            mImportedText.setShadow(true);

            start += 20;

            mModifiedText = new BText(normal, border_left, start, text_width, 20);
            mModifiedText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            mModifiedText.setFont("default-18-bold.font");
            mModifiedText.setShadow(true);

            mStars = new BView[5];
            for (int i = 0; i < 5; i++) {
                mStars[i] = new BView(normal, border_left + (i * 40), height - SAFE_TITLE_V - 200, 34, 34, true);
                mStars[i].setResource(mStarIcon, RSRC_IMAGE_BESTFIT);
                mStars[i].setTransparency(0.6f);
            }

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 80, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("View slideshow");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            updateView();

            return super.handleEnter(arg, isReturn);
        }

        private void updateView() {
            final Image image = currentImage();
            if (image != null) {
                setPainting(false);
                try {

                    clearThumbnail();

                    File file = new File(image.getPath());
                    String name = Tools.extractName(file.getName());
                    mTitleText.setValue(name);
                    mTakenText.setValue("Taken: " + mDateFormat.format(image.getDateCreated()));
                    mImportedText.setValue("Imported: " + mDateFormat.format(image.getDateCaptured()));
                    mModifiedText.setValue("Modified: " + mDateFormat.format(image.getDateModified()));

                    setRating();

                    updateHints();

                    final PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) context.factory)
                            .getAppContext().getConfiguration();
                    new Thread() {
                        public void run() {
                            try {
                                BufferedImage thumbnail = JpgFile.getThumbnail(image);
                                if (thumbnail != null) {
                                    mThumbnail.setResource(createImage(thumbnail), RSRC_IMAGE_BESTFIT);
                                    mThumbnail.setVisible(true);
                                    mThumbnail.setTransparency(1.0f);
                                    mThumbnail.setTransparency(0.0f, mAnim);
                                    mThumbnail.flush();
                                }
                                else
                                {
                                    // TODO Move out as a utility function
                                    try {
                                        thumbnail = Tools.retrieveCachedImage(image.getPath());
                                        if (thumbnail != null) {
                                            mThumbnail.setResource(createImage(thumbnail), RSRC_IMAGE_BESTFIT);
                                            mThumbnail.setVisible(true);
                                            mThumbnail.setTransparency(1.0f);
                                            mThumbnail.setTransparency(0.0f, mAnim);
                                            mThumbnail.flush();
                                        }
                                        else
                                        {
                                            FileInputStream is = new FileInputStream(image.getPath());
                                            if (is != null) {
                                                BufferedImage photo = ImageIO.read(is);
    
                                                if (photo != null) {
                                                    photo = (BufferedImage) Tools.getImage(photo);
                                                    BufferedImage scaled = ImageManipulator.getScaledImage(photo, 200, 200);
                                                    mThumbnail.setResource(createImage(scaled), RSRC_IMAGE_BESTFIT);
                                                    mThumbnail.setVisible(true);
                                                    mThumbnail.setTransparency(1.0f);
                                                    mThumbnail.setTransparency(0.0f, mAnim);
                                                    mThumbnail.flush();
                                                    
                                                    Tools.cacheImage(scaled, 200, 200, image.getPath());
                                                }
                                            }
                                        }
                                    } catch (Exception ex) {
                                        Tools.logException(Photos.class, ex, "Could retrieve image");
                                    }                                    
                                }
                            } catch (Exception ex) {
                                Tools.logException(Photos.class, ex, "Could retrieve cover");
                            }
                        }
                    }.start();

                } finally {
                    setPainting(true);
                }
            }
        }

        private void updateHints() {
            BHighlights h = getHighlights();
            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                pagedown.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                h.refresh();
            }
        }

        private void clearThumbnail() {
            Image image = currentImage();
            if (image != null) {
                mThumbnail.setVisible(false);
                if (mThumbnail.resource != null)
                    mThumbnail.resource.remove();
            }
        }

        public boolean handleExit() {
            clearThumbnail();
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
                        Tools.logException(Photos.class, ex);
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
                        Tools.logException(Photos.class, ex);
                    }
                    setRating();
                } else {
                    getBApp().play("bonk.snd");
                    getBApp().flush();
                }
                return true;
            case KEY_SELECT:
            case KEY_RIGHT:
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
            }
            return super.handleKeyPress(code, rawcode);
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getNextPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
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
            if (action.equals("play")) {

                getBApp().play("select.snd");
                getBApp().flush();

                new Thread() {
                    public void run() {
                        getBApp().push(new SlideshowScreen((Photos) getBApp(), mTracker), TRANSITION_LEFT);
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
                    NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
                    if (nameFile != null) {
                        return getImage(nameFile.getFile().getCanonicalPath());
                    }
                } catch (Exception ex) {
                    Tools.logException(Photos.class, ex);
                }
            }
            return null;
        }

        private SimpleDateFormat mDateFormat;

        private Resource mAnim = getResource("*1000");

        private BView mThumbnail;

        private BText mTitleText;

        private BText mTakenText;

        private BText mImportedText;

        private BText mModifiedText;

        private Tracker mTracker;

        private BView[] mStars;
    }

    public class OptionList extends BList {
        public OptionList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);

            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BText text = new BText(parent, 10, 4, parent.width - 40, parent.height - 4);
            text.setShadow(true);
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index).toString());
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                return parent.handleKeyPress(code, rawcode);
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public class SlideshowScreen extends DefaultScreen {

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public SlideshowScreen(Photos app, Tracker tracker) {
            super(app, null, false);

            mTracker = tracker;

            //app.setTracker(tracker);

            setTitle(" ");

            mPhoto = new View(below, 0, 0, width, height);

            PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) context.factory)
                    .getAppContext().getConfiguration();
            //getPlayer().startTrack();
        }

        private void updateView() {
            final Image image = currentImage();
            if (image != null) {
                setPainting(false);
                try {

                    clearImage();

                    final File file = new File(image.getPath());

                    updateHints();

                    final PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) context.factory)
                            .getAppContext().getConfiguration();
                    new Thread() {
                        public void run() {
                            try {
                                FileInputStream is = new FileInputStream(file);
                                if (is != null) {
                                    BufferedImage photo = ImageIO.read(is);

                                    if (photo != null) {
                                        photo = (BufferedImage) Tools.getImage(photo);
                                        BufferedImage scaled = ImageManipulator.getScaledImage(photo, mPhoto.width, mPhoto.height);
                                        mPhoto.setResource(createImage(scaled), RSRC_IMAGE_BESTFIT);
                                        mPhoto.setVisible(true);
                                        mPhoto.setTransparency(1);
                                        mPhoto.setTransparency(0, mAnim);
                                        getBApp().flush();
                                        scaled.flush();
                                        scaled = null;
                                    }
                                }
                            } catch (Exception ex) {
                                Tools.logException(Photos.class, ex, "Could retrieve image");
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

            mSlideshow = new Slideshow(this);
            mSlideshow.start();
            return super.handleEnter(arg, isReturn);
        }

        private void clearImage() {
            Image image = currentImage();
            if (image != null) {
                mPhoto.setVisible(false);
                if (mPhoto.resource != null)
                    mPhoto.resource.remove();
            }
        }

        public boolean handleExit() {
            setPainting(false);
            try {
                clearImage();
                if (mSlideshow != null && mSlideshow.isAlive()) {
                    mSlideshow.interrupt();
                    mSlideshow = null;
                }
            } finally {
                setPainting(true);
            }
            return super.handleExit();
        }

        private void updateHints() {
            BHighlights h = getHighlights();
            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                pagedown.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                h.refresh();
            }
        }

        private void setTitleText(String text) {
            text = Tools.extractName(text);

            //if (!text.toUpperCase().equals(title.getText()))
            //    title.setText(text);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
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

        public boolean handleAction(BView view, Object action) {
            NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
            if (action.equals("ready")) {
                setTitleText(nameFile.getName());
                return true;
            } else if (action.equals("playing")) {

                return true;
            } else if (action.equals("stopped")) {
                setTitleText(" ");
                return true;
            }

            return super.handleAction(view, action);
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getNextPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                NameFile nameFile = (NameFile) mTracker.getList().get(pos);
                while (nameFile.getFile().isDirectory()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (NameFile) mTracker.getList().get(pos);
                }
            }
        }

        private Image currentImage() {
            if (mTracker != null) {
                try {
                    NameFile nameFile = (NameFile) mTracker.getList().get(mTracker.getPos());
                    if (nameFile != null) {
                        return getImage(nameFile.getFile().getCanonicalPath());
                    }
                } catch (Exception ex) {
                    Tools.logException(Photos.class, ex);
                }
            }
            return null;
        }

        private Resource mAnim = getResource("*100");

        Image mImage;

        View mPhoto;

        Slideshow mSlideshow;

        // when did the last key press occur
        long lastKeyPress;

        private Tracker mTracker;
    }

    private static Image getImage(String path) {
        Image image = null;
        try {
            List list = ImageManager.findByPath(path);
            if (list != null && list.size() > 0) {
                image = (Image) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(Photos.class, ex);
        }

        if (image == null) {
            try {
                image = (Image) MediaManager.getMedia(path);
                ImageManager.createImage(image);
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            }
        }
        return image;
    }

    private class Slideshow extends Thread {
        public Slideshow(SlideshowScreen slideshowScreen) {
            mSlideshowScreen = slideshowScreen;
        }

        public void run() {
            final PhotosConfiguration photosConfiguration = (PhotosConfiguration) ((PhotosFactory) context.factory)
                    .getAppContext().getConfiguration();

            Effect[] effects = new Effect[0];
            
            if (photosConfiguration.getEffect().equals(Effects.RANDOM) || photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
            {
                Collection list = Effects.getEffects();
                effects = (Effect[])list.toArray(effects);
            }
            else
            {
                ArrayList list = new ArrayList();
                list.add(Effects.getEffect(photosConfiguration.getEffect()));
                effects = (Effect[])list.toArray(effects);
            }

            BufferedImage photo = null;
            FileInputStream is = null;
            Image image = null;
            Random random = new Random();
            
            int currentEffect = 0;
            
            if (photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
                currentEffect = 0;
            else
            if (photosConfiguration.getEffect().equals(Effects.RANDOM))
            {
                currentEffect = random.nextInt(effects.length);
            }            

            while (true) {
                try {
                    sleep(1000 * photosConfiguration.getDisplayTime());
                    mSlideshowScreen.getNextPos();
                    image = mSlideshowScreen.currentImage();
                    if (image != null) {
                        File file = new File(image.getPath());
                        try {
                            is = new FileInputStream(file);
                            if (is != null) {
                                photo = ImageIO.read(is);
                                is.close();
                                is = null;

                                if (photo != null) {
                                    long startTime = System.currentTimeMillis();
                                    photo = (BufferedImage) Tools.getImage(photo);
                                    long estimatedTime = System.currentTimeMillis() - startTime;
                                    BufferedImage scaled = ImageManipulator.getScaledImage(photo, mSlideshowScreen.mPhoto.width,
                                            mSlideshowScreen.mPhoto.height);
                                    estimatedTime = System.currentTimeMillis() - startTime;
                                    if (photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
                                        currentEffect = (currentEffect + 1) % effects.length;
                                    else
                                    if (photosConfiguration.getEffect().equals(Effects.RANDOM))
                                    {
                                        currentEffect = random.nextInt(effects.length);
                                    }
                                    Effect effect = (Effect) effects[currentEffect];
                                    effect.setDelay(photosConfiguration.getTransitionTime() * 1000);
                                    effect.apply(mSlideshowScreen.mPhoto, scaled);
                                }
                            }
                        } catch (Exception ex) {
                            Tools.logException(Photos.class, ex, "Could retrieve image");
                        }
                    }
                    image = null;
                } catch (InterruptedException ex) {
                    return;
                } catch (OutOfMemoryError ex) {
                    System.gc();
                } catch (Exception ex2) {
                    Tools.logException(Photos.class, ex2);
                }
            }
        }

        private SlideshowScreen mSlideshowScreen;
    }
    
     public static class PhotosFactory extends AppFactory {

        public PhotosFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            PhotosConfiguration imagesConfiguration = (PhotosConfiguration) getAppContext().getConfiguration();
        }
    }
}