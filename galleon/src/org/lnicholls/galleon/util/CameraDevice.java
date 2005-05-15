package org.lnicholls.galleon.util;

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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.media.Buffer;
import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;
import javax.media.ControllerEvent;
import javax.media.ControllerListener;
import javax.media.Format;
import javax.media.Manager;
import javax.media.NoPlayerException;
import javax.media.Player;
import javax.media.control.FormatControl;
import javax.media.control.FrameGrabbingControl;
import javax.media.format.VideoFormat;
import javax.media.util.BufferToImage;

import org.apache.log4j.Logger;

/*
 * Based on code from TimCam (http://timcam.sourceforge.net) by  Tim Carr
 * 
 */

public class CameraDevice {

    private static final Logger log = Logger.getLogger(CameraDevice.class.getName());

    public CameraDevice() {

    }

    public void startPlayer() throws IOException, NoPlayerException {
        initializeDevice();
        if (mChosenFormat != -1) {
            mFormatControl.setFormat(mAvailableFormats[mChosenFormat]);
        }
        mPlayer.start();
        waitForDevice();
        mFrameGrabbingControl = (FrameGrabbingControl) mPlayer.getControl("javax.media.control.FrameGrabbingControl");
    }

    public void stopPlayer() throws NoPlayerException, IOException {
        mPlayer.stop();
        waitForDevice();
        mPlayer.close();
        mPlayer.deallocate();
        mPlayer = null;
    }

    private void initializeDevice() throws NoPlayerException, IOException {
        mPlayer = Manager.createPlayer(mCaptureDeviceInfo.getLocator());
        mPlayer.realize();
        mPlayer.addControllerListener(new ControllerListener() {
            public void controllerUpdate(ControllerEvent ce) {
                synchronized (stateLock) {
                    stateLock.notifyAll();
                }
            }
        });

        mPlayer.prefetch();
        waitForDevice();
        mFormatControl = (FormatControl) mPlayer.getControl("javax.media.control.FormatControl");
    }

    private void waitForDevice() {
        while (mPlayer.getState() != mPlayer.getTargetState()) {
            synchronized (stateLock) {
                try {
                    stateLock.wait();
                } catch (InterruptedException ie) {

                }
            }
        }
    }

    public void setDevice(String device){
        try
        {
            mCaptureDeviceInfo = CaptureDeviceManager.getDevice(device);
            mAvailableFormats = mCaptureDeviceInfo.getFormats();
        }
        catch (Exception ex)
        {
            Tools.logException(CameraDevice.class, ex, "Could not create device: "+device);
        }
    }

    public Image getSnapshot() {
        try {
            Buffer bufferFrame = null;
            BufferToImage bufferToImage = null;
            Image image = null;

            do {
                bufferFrame = mFrameGrabbingControl.grabFrame();
            } while (bufferFrame.getLength() == 0);
            bufferToImage = new BufferToImage((VideoFormat) bufferFrame.getFormat());
            return bufferToImage.createImage(bufferFrame);
        } catch (Exception ex) {
            Tools.logException(CameraDevice.class, ex, "Could not create snapshot");
        }
        return null;
    }

    public static List getCameras() {
        Vector videoDevices = CaptureDeviceManager.getDeviceList(new VideoFormat(null));
        ArrayList cameras = new ArrayList();
        for (int i = 0; i < videoDevices.size(); i++) {
            CaptureDeviceInfo cdi = (CaptureDeviceInfo) videoDevices.elementAt(i);
            cameras.add(cdi.getName());
        }
        return cameras;
    }

    private FrameGrabbingControl mFrameGrabbingControl;

    private FormatControl mFormatControl;

    private CaptureDeviceInfo mCaptureDeviceInfo;

    private Player mPlayer;

    private Format[] mAvailableFormats;

    protected int mChosenFormat = -1;

    private Object stateLock = new Object();
}