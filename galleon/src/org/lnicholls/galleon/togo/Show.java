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

import java.util.Date;

public class Show {

    public static int STATUS_DOWNLOADING = 1;

    public static int STATUS_DOWNLOADED = 2;

    public static int STATUS_PENDING = 4;

    public static int STATUS_USER_CANCELLED = 8;

    public static int STATUS_RECORDED = 16;

    public static int STATUS_RECORDING = 32;

    public static int STATUS_INCOMPLETE = 64;

    public static int STATUS_USER_SELECTED = 128;

    public static String SELECTION_USER = "user";

    public static String SELECTION_RULE = "rule";

    public Show() {
        mTitle = "";
        mDescription = "";
        mEpisode = "";
        mChannel = "";
        mStation = "";
        mRating = "";
        mQuality = "";
        mGenre = "";
        mType = "";
        mCode = "";
        mDateRecorded = new Date();
        mDuration = 0;
        mSize = 0;
        mSelection = SELECTION_RULE;
        mStatus = STATUS_RECORDED;
        mPath = "";
        mIcon = "";
        mSource = "";
    }

    public Show(String title, String description, String episode, String channel, String station, String rating,
            String quality, String genre, String type, String code, Date dateRecorded, int duration, int size,
            String selection, int status, String path, String icon) {
        mTitle = title;
        mDescription = description;
        mEpisode = episode;
        mChannel = channel;
        mStation = station;
        mRating = rating;
        mQuality = quality;
        mGenre = genre;
        mType = type;
        mCode = code;
        mDateRecorded = dateRecorded;
        mDuration = duration;
        mSize = size;
        mSelection = selection;
        mStatus = status;
        mPath = path;
        mIcon = icon;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String value) {
        mTitle = value;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String value) {
        mDescription = value;
    }

    public String getEpisode() {
        return mEpisode;
    }

    public void setEpisode(String value) {
        mEpisode = value;
    }

    public String getChannel() {
        return mChannel;
    }

    public void setChannel(String value) {
        mChannel = value;
    }

    public String getStation() {
        return mStation;
    }

    public void setStation(String value) {
        mStation = value;
    }

    public String getRating() {
        return mRating;
    }

    public void setRating(String value) {
        mRating = value;
    }

    public String getQuality() {
        return mQuality;
    }

    public void setQuality(String value) {
        mQuality = value;
    }

    public String getGenre() {
        return mGenre;
    }

    public void setGenre(String value) {
        mGenre = value;
    }

    public String getType() {
        return mType;
    }

    public void setType(String value) {
        mType = value;
    }

    public String getCode() {
        return mCode;
    }

    public void setCode(String value) {
        mCode = value;
    }

    public Date getDateRecorded() {
        return mDateRecorded;
    }

    public void setDateRecorded(Date value) {
        mDateRecorded = value;
    }

    public int getDuration() {
        return mDuration;
    }

    public void setDuration(int value) {
        mDuration = value;
    }

    public long getSize() {
        return mSize;
    }

    public void setSize(long value) {
        mSize = value;
    }

    public String getSelection() {
        return mSelection;
    }

    public void setSelection(String value) {
        mSelection = value;
    }

    public int getStatus() {
        return mStatus;
    }

    public void setStatus(int value) {
        mStatus = value;
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String value) {
        mPath = value;
    }
    
    public String getIcon() {
        return mIcon;
    }

    public void setIcon(String value) {
        mIcon = value;
    }    
    
    public String getSource() {
        return mSource;
    }

    public void setSource(String value) {
        mSource = value;
    }    

    public String getStatusString() {
        switch (mStatus) {
        case 1:
            return "Downloading";
        case 2:
            return "Downloaded";
        case 4:
            return "Pending Download";
        case 8:
            return "User Cancelled";
        case 16:
            return "Recorded";
        case 32:
            return "Recording";
        case 64:
            return "Incomplete";
        case 128:
            return "User Selected";
        }
        return "";
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append("Title=" + mTitle + '\n');
            buffer.append("Description=" + mDescription + '\n');
            buffer.append("Episode=" + mEpisode + '\n');
            buffer.append("Channel=" + mChannel + '\n');
            buffer.append("Station=" + mStation + '\n');
            buffer.append("Rating=" + mRating + '\n');
            buffer.append("Quality=" + mQuality + '\n');
            buffer.append("Genre=" + mGenre + '\n');
            buffer.append("Type=" + mType + '\n');
            buffer.append("Code=" + mCode + '\n');
            buffer.append("DateRecorded=" + mDateRecorded + '\n');
            buffer.append("Duration=" + mDuration + '\n');
            buffer.append("Size=" + mSize + '\n');
            buffer.append("Selection=" + mSelection + '\n');
            buffer.append("Status=" + mStatus + '\n');
            buffer.append("Path=" + mPath + '\n');
            buffer.append("Icon=" + mIcon + '\n');
            buffer.append("Source=" + mSource + '\n');
        }
        return buffer.toString();
    }
    
    public boolean equals(Object object)
    {
        Show show = (Show)object;
        return mDateRecorded.equals(show.mDateRecorded) && mChannel.equals(show.mChannel);
    }

    private String mTitle;

    private String mDescription;

    private String mEpisode;

    private String mChannel;

    private String mStation;

    private String mRating;

    private String mQuality;

    private String mGenre;

    private String mType;

    private String mCode;

    private Date mDateRecorded;

    private int mDuration;

    private long mSize;

    private String mSelection;

    private int mStatus;

    private String mPath;
    
    private String mIcon;
    
    private String mSource;
}