package org.lnicholls.galleon.apps.podcasting;

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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.util.NameValue;

public class PodcastingConfiguration implements AppConfiguration {

    public String getName() {
        return mName;
    }

    public void setName(String value) {
        if (mName != null && !mName.equals(value))
            mModified = true;
        mName = value;
    }
    
    public List getDirectorys() {
        return mDirectories;
    }

    public void setDirectorys(List value) {
        mModified = true;
        mDirectories = value;
    }

    public void addDirectory(NameValue nameValue) {
        mModified = true;
        mDirectories.add(nameValue);
    }
    
    public List getSubscriptions() {
        if (mSubscriptions==null)
        {
            mSubscriptions = new ArrayList();
            mSubscriptions.add(new NameValue("iPodder.org","http://www.ipodder.org/discuss/reader$4.opml"));
            mSubscriptions.add(new NameValue("iPodderX Top Picks","http://directory.ipodderx.com/opml/iPodderX_Picks.opml"));
            mSubscriptions.add(new NameValue("iPodderX Most Popular","http://directory.ipodderx.com/opml/iPodderX_Popular.opml"));
            mSubscriptions.add(new NameValue("Podcast Alley Top 50","http://www.podcastalley.com/PodcastAlleyTop50.opml"));
            mSubscriptions.add(new NameValue("Podcast Alley 10 Newest","http://www.podcastalley.com/PodcastAlley10Newest.opml"));
            mSubscriptions.add(new NameValue("GigaDial 25 Latest","http://www.gigadial.net/public/opml/dial25.opml"));
            mSubscriptions.add(new NameValue("Sports Podcast Network","http://sportspodnet.com/opml/spn.opml"));
        }
        return mSubscriptions;
    }

    public void setSubscriptions(List value) {
        mModified = true;
        mSubscriptions = value;
    }

    public void addSubscription(NameValue nameValue) {
        mModified = true;
        mSubscriptions.add(nameValue);
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

    private String mName;
    
    private boolean mModified;

    private List mDirectories = new ArrayList();
    
    private List mSubscriptions;
}