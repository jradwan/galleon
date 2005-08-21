package org.lnicholls.galleon.server;

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

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.togo.Rule;
import org.lnicholls.galleon.util.Tools;

public class ServerConfiguration implements Serializable {

    private static Logger log = Logger.getLogger(ServerConfiguration.class.getName());

    public ServerConfiguration() {
        try {
            System.setProperty("http.agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");
        } catch (Exception ex) {
            Tools.logException(Server.class, ex);
        }
        mTiVos = new ArrayList();
        mRules = new ArrayList();
    }

    public void setVersion(String version) {
        mVersion = version;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setName(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    public void setReload(int reload) {
        mReload = reload;
    }

    public int getReload() {
        return mReload;
    }

    public void setPort(int port) {
        mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    public void setIPAddress(String ipaddress) {
        if (ipaddress.length() > 0) {
            try {
                InetAddress address = InetAddress.getByName(ipaddress);
                mIPAddress = ipaddress;
                return;
            } catch (UnknownHostException ex) {
            }
        }
        mIPAddress = "";
    }

    public String getIPAddress() {
        if (mIPAddress==null)
        	return Tools.getLocalIpAddress();
    	return mIPAddress;
    }

    public void setShuffleItems(boolean shuffleItems) {
        mShuffleItems = shuffleItems;
    }

    public boolean getShuffleItems() {
        return mShuffleItems;
    }

    // Used by ImageTiVoContainer to determine if thumbnails should be generated at startup
    public void setGenerateThumbnails(boolean generateThumbnails) {
        mGenerateThumbnails = generateThumbnails;
    }

    public boolean getGenerateThumbnails() {
        return mGenerateThumbnails;
    }

    public void setRecordingsPath(String value) {
        mRecordingsPath = value;
    }

    public String getRecordingsPath() {
        return mRecordingsPath;
    }

    public void setMediaAccessKey(String value) {
        mMediaAccessKey = value;
    }

    public String getMediaAccessKey() {
        return mMediaAccessKey;
    }

    public List getTiVos() {
        return mTiVos;
    }

    public void setTiVos(List tivos) {
        mTiVos = tivos;
    }

    public boolean addTiVo(TiVo tivo) {
        Iterator iterator = mTiVos.iterator();
        while (iterator.hasNext()) {
            TiVo known = (TiVo) iterator.next();
            if (known.getAddress().equals(tivo.getAddress()))
                return false;
        }

        mTiVos.add(tivo);
        return true;
    }

    public List getRules() {
        return mRules;
    }

    public void setRules(List rules) {
        mRules = rules;
    }

    public boolean addRule(Rule rule) {
        mRules.add(rule);
        return true;
    }
    
    public void setSkin(String value) {
        mSkin = value;
    }

    public String getSkin() {
        return mSkin;
    }
    
    public void setDebug(boolean value) {
        mDebug = value;
    }

    public boolean isDebug() {
        return mDebug;
    }    
    
    public void setHandleTimeout(boolean value) {
        mHandleTimeout = value;
    }

    public boolean isHandleTimeout() {
        return mHandleTimeout;
    }    
    
    public void setMusicPlayerConfiguration(MusicPlayerConfiguration value) {
        mMusicPlayerConfiguration = value;
    }

    public MusicPlayerConfiguration getMusicPlayerConfiguration() {
        return mMusicPlayerConfiguration;
    }
    
    public void setDataConfiguration(DataConfiguration value) {
        mDataConfiguration = value;
    }

    public DataConfiguration getDataConfiguration() {
        return mDataConfiguration;
    }
    
    private String mVersion = Tools.getVersion();

    private String mName;

    private int mReload = 60;

    private int mPort = 7288;

    private String mIPAddress;

    private boolean mShuffleItems = true; // defaults to true, the same shuffle style as the TiVo HMO server.

    private boolean mGenerateThumbnails = false; // defaults to false, thumbnails are not generated at startup.

    private String mRecordingsPath = "";

    private String mMediaAccessKey = "";

    private List mTiVos;

    private List mRules;
    
    private String mSkin = "";
    
    private boolean mDebug;
    
    private boolean mHandleTimeout = true;
    
    private MusicPlayerConfiguration mMusicPlayerConfiguration = new MusicPlayerConfiguration();
    
    private DataConfiguration mDataConfiguration = new DataConfiguration();
}