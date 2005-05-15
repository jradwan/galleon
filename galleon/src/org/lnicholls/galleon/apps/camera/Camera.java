package org.lnicholls.galleon.apps.camera;

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

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.util.CameraDevice;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultScreen;

import EDU.oswego.cs.dl.util.concurrent.Callable;
import EDU.oswego.cs.dl.util.concurrent.TimedCallable;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class Camera extends DefaultApplication {

    private static Logger log = Logger.getLogger(Camera.class.getName());

    public final static String TITLE = "Camera";

    private Resource mMenuBackground;

    private Resource mItemIcon;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mItemIcon = getSkinImage(null, "icon");

        CameraConfiguration cameraConfiguration = (CameraConfiguration) ((CameraFactory) context.factory)
                .getAppContext().getConfiguration();

        if (cameraConfiguration.getCameras().size() == 1) {
            CameraDevice camera = new CameraDevice();
            camera.setDevice(((NameValue) cameraConfiguration.getCameras().get(0)).getValue());
            push(new CameraScreen(this, camera), TRANSITION_NONE);
        } else
            push(new CameraMenuScreen(this), TRANSITION_NONE);
    }

    public class CameraMenuScreen extends DefaultMenuScreen {
        public CameraMenuScreen(Camera app) {
            super(app, "Camera");

            below.setResource(mMenuBackground);

            CameraConfiguration cameraConfiguration = (CameraConfiguration) ((CameraFactory) context.factory)
                    .getAppContext().getConfiguration();

            for (Iterator i = cameraConfiguration.getCameras().iterator(); i.hasNext(); /* Nothing */) {
                NameValue camera = (NameValue) i.next();
                mMenuList.add(camera);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();

                new Thread() {
                    public void run() {
                        try {
                            CameraDevice camera = new CameraDevice();
                            camera.setDevice(((NameValue) mMenuList.get(mMenuList.getFocus())).getValue());
                            getBApp().push(new CameraScreen((Camera) getApp(), camera), TRANSITION_NONE);
                            getBApp().flush();
                        } catch (Exception ex) {
                            Tools.logException(Camera.class, ex);
                        }
                    }
                }.start();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameValue camera = (NameValue) mMenuList.get(index);
            icon.setResource(mItemIcon);

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(camera.getName(), 40));
        }

    }

    public class CameraScreen extends DefaultScreen {

        public CameraScreen(Camera app, CameraDevice camera) {
            super(app, null, false);

            mCamera = camera;
        }

        private void update() {
            BufferedImage image = null;
            try {
                Image cam = mCamera.getSnapshot();

                int w = cam.getWidth(null);
                int h = cam.getHeight(null);
                int[] pixels = new int[w * h];
                PixelGrabber pg = new PixelGrabber(cam, 0, 0, w, h, pixels, 0, w);
                try {
                    pg.grabPixels();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }

                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                image.setRGB(0, 0, w, h, pixels, 0, w);

            } catch (Exception ex) {
                Tools.logException(Camera.class, ex);
            }

            try {
                setPainting(false);
                if (image != null) {
                    if (image.getWidth() > 640 || image.getHeight() > 480) {
                        BufferedImage scaled = ImageManipulator.getScaledImage(image, 640, 480);
                        image.flush();
                        image = null;

                        normal.setResource(createImage(scaled), RSRC_IMAGE_BESTFIT);
                        scaled.flush();
                        scaled = null;
                    } else {
                        normal.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                        image.flush();
                        image = null;
                    }
                }
            } finally {
                setPainting(true);
            }
            normal.flush();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (mCameraThread != null && mCameraThread.isAlive())
                mCameraThread.interrupt();

            mCameraThread = new Thread() {
                public void run() {
                    try {
                        setFooter("Initializing");
                        flush();
                        
                        TimedCallable timedCallable = new TimedCallable(new Callable() {
                            public synchronized Object call() throws Exception {
                                mCamera.startPlayer();
                                return Boolean.TRUE;
                            }
                        }, 1000 * 10);
                        Object result = timedCallable.call();
                        timedCallable = null;

                        if (result != null) {
                            setFooter(" ");
                            while (true) {
                                synchronized (this) {
                                    update();
                                }
                                sleep(500);
                            }
                        }
                        else
                        {
                            setTitle("Error");
                            below.setResource(mMenuBackground);
                        }
                    } catch (Exception ex) {
                        Tools.logException(Camera.class, ex, "Could not retrieve camera");
                        try {
                            mCamera.stopPlayer();
                        } catch (Exception ex2) {
                        }
                    }
                }

                public void interrupt() {
                    synchronized (this) {
                        super.interrupt();
                    }
                }
            };
            mCameraThread.start();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            if (mCameraThread != null && mCameraThread.isAlive())
                mCameraThread.interrupt();
            mCameraThread = null;
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            CameraConfiguration cameraConfiguration = (CameraConfiguration) ((CameraFactory) context.factory)
                    .getAppContext().getConfiguration();
            switch (code) {
            case KEY_LEFT:
                if (cameraConfiguration.getCameras().size() > 1) {
                    postEvent(new BEvent.Action(this, "pop"));
                    return true;
                }
            }
            return super.handleKeyPress(code, rawcode);
        }

        private Thread mCameraThread;

        private CameraDevice mCamera;
    }

    public static class CameraFactory extends AppFactory {

        public CameraFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);

            CameraConfiguration cameraConfiguration = (CameraConfiguration) getAppContext().getConfiguration();
        }
    }

    //private static CameraDevice mCamera;
}