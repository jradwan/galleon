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

import org.apache.commons.lang.builder.ToStringBuilder;

public class MusicPlayerConfiguration implements Serializable {

    public MusicPlayerConfiguration() {

    }

    public String getSkin() {
        return mSkin;
    }

    public void setSkin(String value) {
        if (mSkin != null && !mSkin.equals(value))
            mModified = true;
        mSkin = value;
    }

    public void setUseFile(boolean value) {
        mUseFile = value;
    }

    public boolean isUseFile() {
        return mUseFile;
    }

    public void setUseAmazon(boolean value) {
        mUseAmazon = value;
    }

    public boolean isUseAmazon() {
        return mUseAmazon;
    }

    public void setShowImages(boolean value) {
        mShowImages = value;
    }

    public boolean isShowImages() {
        return mShowImages;
    }

    public void setModified(boolean value) {
        mModified = value;
    }

    public boolean isModified() {
        return mModified;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private String mSkin;

    private boolean mUseFile = true;

    private boolean mUseAmazon = true;

    private boolean mShowImages = false;

    private boolean mModified;
}