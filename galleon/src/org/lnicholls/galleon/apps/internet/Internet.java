package org.lnicholls.galleon.apps.internet;

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
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.Grid;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class Internet extends DefaultApplication {

    private static Logger log = Logger.getLogger(Internet.class.getName());

    private final static Runtime runtime = Runtime.getRuntime();

    public final static String TITLE = "Internet";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mFolderIcon;

    private Resource mLargeFolderIcon;

    private Resource mItemIcon;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mLargeFolderIcon = getSkinImage("menu", "gridFolder");
        mItemIcon = getSkinImage("menu", "item");

        InternetConfiguration internetConfiguration = (InternetConfiguration) ((InternetFactory) getContext()
                .getFactory()).getAppContext().getConfiguration();

        Tracker tracker = new Tracker(internetConfiguration.getUrls(), 0);

        push(new PathScreen(this, tracker), TRANSITION_NONE);
    }

    public class PGrid extends Grid {
        public PGrid(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);
            mThreads = new Vector();
        }

        public void createCell(final BView parent, int row, int column, boolean selected) {
            ArrayList photos = (ArrayList) get(row);
            if (column < photos.size()) {
                final NameValue nameValue = (NameValue) photos.get(column);
                // TODO Handle: Photos[#1,uri=null] handleApplicationError(4,view 1402 not found)
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            synchronized (this) {
                                parent.setResource(Color.GRAY);
                                parent.setTransparency(0.5f);
                                parent.flush();
                            }

                            Image image = null;
                            synchronized (this) {
                                image = getImage(nameValue.getValue(), false);
                            }

                            if (image != null) {
                                synchronized (this) {
                                    parent.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                    parent.setTransparency(0.0f);
                                    parent.flush();
                                }
                            }
                        } catch (Throwable ex) {
                            log.error(ex);
                        } finally {
                            mThreads.remove(this);
                        }
                    }

                    public void interrupt() {
                        synchronized (this) {
                            super.interrupt();
                        }
                    }
                };
                mThreads.add(thread);
                thread.start();
            }
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

        public void shutdown() {
            try {
                setPainting(false);
                Iterator iterator = mThreads.iterator();
                while (iterator.hasNext()) {
                    Thread thread = (Thread) iterator.next();
                    if (thread.isAlive()) {
                        thread.interrupt();
                    }
                }
                mThreads.clear();
            } finally {
                setPainting(true);
            }
        }

        private Vector mThreads;
    }

    public class PathScreen extends DefaultScreen {
        private PGrid grid;

        public PathScreen(Internet app, Tracker tracker) {
            super(app);

            getBelow().setResource(mMenuBackground);

            setTitle("Internet");

            mTracker = tracker;

            int w = getWidth() - 2 * BORDER_LEFT;
            int h = getHeight() - TOP - SAFE_TITLE_V - 2*PAD;
            grid = new PGrid(this.getNormal(), BORDER_LEFT, BORDER_TOP, w, h, h / 3);
            BHighlights highlights = grid.getHighlights();
            highlights.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            highlights.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            setFocusDefault(grid);

            mBusy = new BView(getNormal(), SAFE_TITLE_H, SAFE_TITLE_V, 32, 32);
            mBusy.setResource(mBusyIcon);
            mBusy.setVisible(false);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                if (grid.getFocus() != -1) {
                    Object object = grid.get(grid.getFocus());
                    getBApp().play("select.snd");
                    getBApp().flush();

                    mTop = grid.getTop();

                    ArrayList photos = (ArrayList) object;
                    final NameValue nameValue = (NameValue) photos.get(grid.getPos() % 3);
                    new Thread() {
                        public void run() {
                            try {
                                ImageScreen imageScreen = new ImageScreen((Internet) getBApp());
                                mTracker.setPos(grid.getPos());
                                imageScreen.setTracker(mTracker);

                                getBApp().push(imageScreen, TRANSITION_LEFT);
                                getBApp().flush();
                            } catch (Exception ex) {
                                Tools.logException(Internet.class, ex);
                            }
                        }
                    }.start();

                    return true;
                }
            } else if (action.equals("play")) {
                Object object = grid.get(grid.getFocus());
                getBApp().play("select.snd");
                getBApp().flush();

                mTop = grid.getTop();

                ArrayList photos = (ArrayList) object;
                final NameValue nameValue = (NameValue) photos.get(grid.getPos() % 3);
                new Thread() {
                    public void run() {
                        try {
                            ImageScreen imageScreen = new ImageScreen((Internet) getBApp());
                            mTracker.setPos(grid.getPos());
                            //getBApp().push(new SlideshowScreen((Internet) getBApp(), mTracker), TRANSITION_LEFT);
                            //getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Internet.class, ex);
                        }
                    }
                }.start();

                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {

            if (grid.size() == 0) {
                try {
                    setPainting(false);
                    //mBusy.setVisible(true);

                    ArrayList photos = new ArrayList();
                    Iterator iterator = mTracker.getList().iterator();
                    while (iterator.hasNext()) {
                        NameValue nameValue = (NameValue) iterator.next();
                        photos.add(nameValue);
                        if (photos.size() == 3) {
                            grid.add(photos);
                            photos = new ArrayList();
                        }
                    }
                    if (photos.size() > 0)
                        grid.add(photos);
                    //mBusy.setVisible(false);
                    //mBusy.flush();

                    grid.setTop(mTop);
                    grid.setPos(mTracker.getPos());
                } catch (Exception ex) {
                    Tools.logException(Internet.class, ex);
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
            case KEY_PLAY:
                postEvent(new BEvent.Action(this, "play"));
                return true;
            }

            return super.handleKeyPress(code, rawcode);
        }

        private Tracker mTracker;

        private int mTop;
    }

    public class ImageScreen extends DefaultScreen {

        public ImageScreen(Internet app) {
            super(app, null, null, false);

            //getBelow().setResource(mInfoBackground);

            mImage = new BView(this, BORDER_LEFT, SAFE_TITLE_V, BODY_WIDTH, BODY_HEIGHT - 20, true);

            setFooter("Press PLAY to reload");
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            updateView(false);
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            stopReload();
            return super.handleExit();
        }

        private void updateView(boolean reload) {
            final Image image = currentImage(reload);
            if (image != null) {
                try {
                    setPainting(false);

                    mImage.setResource(createImage(image), RSRC_IMAGE_BESTFIT);

                } finally {
                    setPainting(true);
                }
            }
            flush();
        }

        private void startReload() {
            if (mImageThread != null && mImageThread.isAlive())
                mImageThread.interrupt();

            mImageThread = new Thread() {
                public void run() {
                    try {
                        while (true) {
                            synchronized (this) {
                                updateView(true);
                            }
                            sleep(1000);
                        }
                    } catch (Exception ex) {
                        Tools.logException(Internet.class, ex, "Could not retrieve internet image");
                    }
                }

                public void interrupt() {
                    synchronized (this) {
                        super.interrupt();
                    }
                }
            };
            mImageThread.start();
            mPlaying = true;
            setFooter("Press PAUSE to stop reloading");
        }

        private void stopReload() {
            if (mImageThread != null && mImageThread.isAlive())
                mImageThread.interrupt();
            mImageThread = null;
            mPlaying = false;
            setFooter("Press PLAY to reload");
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SLOW:
            case KEY_PAUSE:
                if (mPlaying)
                    stopReload();
                else
                    startReload();
                break;
            case KEY_PLAY:
                startReload();
                break;
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_CHANNELUP:
                stopReload();
                getBApp().play("pageup.snd");
                getBApp().flush();
                getPrevPos();
                updateView(false);
                return true;
            case KEY_CHANNELDOWN:
                stopReload();
                getBApp().play("pagedown.snd");
                getBApp().flush();
                getNextPos();
                updateView(false);
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
            }
        }

        public void getPrevPos() {
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
            }
        }

        public void setTracker(Tracker value) {
            mTracker = value;
        }

        private Image currentImage(boolean reload) {
            if (mTracker != null) {
                try {
                    NameValue nameValue = (NameValue) mTracker.getList().get(mTracker.getPos());
                    if (nameValue != null) {
                        return getImage(nameValue.getValue(), reload);
                    }
                } catch (Exception ex) {
                    Tools.logException(Internet.class, ex);
                }
            }
            return null;
        }

        private BView mImage;

        private Tracker mTracker;

        private Thread mImageThread;

        private boolean mPlaying;
    }

    private static Image getImage(String address, boolean reload) {
        try {
            URL url = new URL(address);
            if (reload)
                Tools.cacheImage(url, address);

            return Tools.retrieveCachedImage(url);
        } catch (Exception ex) {
            Tools.logException(Internet.class, ex);
        }
        return null;
    }

    public static class InternetFactory extends AppFactory {

        public InternetFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            InternetConfiguration internetConfiguration = (InternetConfiguration) getAppContext().getConfiguration();

            Server.getServer().scheduleShortTerm(new ReloadTask(new ReloadCallback() {
                public void reload() {
                    try {
                    	log.debug("Internet");
                        updateImages();
                    } catch (Exception ex) {
                        log.error("Could not download internet images", ex);
                    }
                }
            }), internetConfiguration.getReload());
        }

        public void setAppContext(AppContext appContext) {
            super.setAppContext(appContext);

            updateImages();
        }

        private void updateImages() {
            final InternetConfiguration internetConfiguration = (InternetConfiguration) getAppContext()
                    .getConfiguration();

            new Thread() {
                public void run() {
                    Iterator iterator = internetConfiguration.getUrls().iterator();
                    while (iterator.hasNext()) {
                        NameValue nameValue = (NameValue) iterator.next();

                        try {
                            URL url = new URL(nameValue.getValue());
                            Tools.cacheImage(url, nameValue.getValue());
                        } catch (Exception ex) {
                            Tools.logException(Internet.class, ex);
                        }

                    }
                }
            }.start();
        }
    }
}