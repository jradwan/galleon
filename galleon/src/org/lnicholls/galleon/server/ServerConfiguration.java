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
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
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

    public void setConfiguredPort(int port) {
        configuredPort = port;
    }

    public int getConfiguredPort() {
        return configuredPort;
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
        return mIPAddress;
    }

    public void setNetMask(String netMask) {
        try {
            StringTokenizer tokens = new StringTokenizer(netMask, ".");
            if (tokens.countTokens() == 4) {
                boolean valid = true;
                for (int i = 0; i < 4; i++) {
                    String token = tokens.nextToken();
                    int value = Integer.parseInt(token);
                    if (value < 0 || value > 255) {
                        valid = false;
                        break;
                    }
                }
                if (valid) {
                    mNetMask = netMask;
                    return;
                }
            }
        } catch (Exception ex) {
            Tools.logException(ServerConfiguration.class, ex);
        }
        mNetMask = "";
    }

    public String getNetMask() {
        return mNetMask;
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

    //  Used by AudioPlaylistTiVoItem to determine if proxy should be used for streaming stations
    public void setUseStreamingProxy(boolean useStreamingProxy) {
        mUseStreamingProxy = useStreamingProxy;
    }

    public boolean getUseStreamingProxy() {
        return mUseStreamingProxy;
    }

    public void setUseTiVoBeacon(boolean value) {
        mUseTiVoBeacon = value;
    }

    public boolean getUseTiVoBeacon() {
        return mUseTiVoBeacon;
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

    private String mVersion = Tools.getVersion();

    private String mName;

    private int mReload = 60;

    private int configuredPort = 8081;

    private String mIPAddress;

    private String mNetMask;

    private boolean mShuffleItems = true; // defaults to true, the same shuffle style as the TiVo HMO server.

    private boolean mGenerateThumbnails = false; // defaults to false, thumbnails are not generated at startup.

    private boolean mUseStreamingProxy = true; // defaults to true, proxy is used.

    private boolean mUseTiVoBeacon = true;

    private String mRecordingsPath = "";

    private String mMediaAccessKey = "";

    private List mTiVos;
}