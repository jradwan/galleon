package org.lnicholls.galleon.skins;

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

import org.apache.commons.lang.builder.ToStringBuilder;

public class Descriptor {
    
    public Descriptor()
    {
        mImages = new ArrayList();
    }
    
    public void getImages(java.util.List value) {
        mImages = value;
    }

    public java.util.List getImages() {
        return mImages;
    }
    
    public Image getImage(String id, int resolution)
    {
        Image defaultImage = null;
        for (int i = 0; i < mImages.size(); i++) {
            Image image = (Image) mImages.get(i);
            if (image.getId().toLowerCase().equals(id.toLowerCase())) {
                if (defaultImage == null && image.getResolution() == 0) {
                    defaultImage = image;
                }
                if (image.getResolution() == resolution) {
                    return image;
                }
            }
        }
        return defaultImage;
    }

    public void addImage(Image value) {
        mImages.add(value);
    }
    
    public static class Image {
        public Image() {

        }

        public String getSource() {
            return mSource;
        }

        public void setSource(String value) {
            mSource = value;
        }

        public String getId() {
            return mId;
        }

        public void setId(String value) {
            mId = value;
        }

        public int getResolution() {
            return mResolution;
        }

        public void setResolution(int value) {
            mResolution = value;
        }

        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        private String mSource;

        private String mId;
        
        private int mResolution;
    }    
    
    private List mImages;
}
