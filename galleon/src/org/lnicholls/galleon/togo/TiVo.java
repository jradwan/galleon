package org.lnicholls.galleon.togo;

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

import java.util.*;

public class TiVo {

    public TiVo() {

    }

    public TiVo(String name, String address) {
        mName = name;
        mAddress = address;
        mNumShows = 0;
    }

    public String getName() {
        return mName;
    }

    public void setName(String value) {
        mName = value;
    }

    public String getAddress() {
        return mAddress;
    }

    public void setAddress(String value) {
        mAddress = value;
    }
    
    public Date getLastChangedDate() {
        return mLastChangedDate;
    }

    public void setLastChangedDate(Date date) {
        mLastChangedDate = date;
    }    
    
    public int getNumShows() {
        return mNumShows;
    }

    public void setNumShows(int value) {
        mNumShows = value;
    }    
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append("Name=" + mName + '\n');
            buffer.append("Address=" + mAddress + '\n');
            buffer.append("LastChangedDate=" + mLastChangedDate + '\n');
            buffer.append("NumShows=" + mNumShows + '\n');
        }
        return buffer.toString();
    }    

    private String mName;

    private String mAddress;
    
    private Date mLastChangedDate = new Date(0);
    
    private int mNumShows = 0;
}