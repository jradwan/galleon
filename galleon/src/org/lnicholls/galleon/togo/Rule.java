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

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.io.Serializable;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.util.Tools;

public class Rule implements Serializable {

    private static Logger log = Logger.getLogger(Rule.class.getName());

    private String mType;

    private String mCode;

    private Date mDateRecorded;

    private int mDuration;

    private long mSize;

    public static String CRITERIA_TITLE = "title";

    public static String CRITERIA_DESCRIPTION = "description";

    public static String CRITERIA_EPISODE = "episode";

    public static String CRITERIA_CHANNEL = "channel";

    public static String CRITERIA_STATION = "station";

    public static String CRITERIA_RATING = "rating";

    public static String CRITERIA_QUALITY = "quality";

    public static String CRITERIA_GENRE = "genre";

    public static String CRITERIA_TYPE = "type";

    public static String CRITERIA_DATE = "date";

    public static String CRITERIA_DURATION = "duration";

    public static String CRITERIA_SIZE = "size";

    public static String COMPARISON_EQUALS = "equals";

    public static String COMPARISON_CONTAINS = "contains";

    public static String COMPARISON_STARTS_WITH = "startsWith";

    public static String COMPARISON_ENDS_WITH = "endsWith";

    public static String COMPARISON_MORE_THAN = "moreThan";

    public static String COMPARISON_LESS_THAN = "lessThan";

    public Rule() {
        mCriteria = "";
        mComparison = "";
        mValue = "";
        mDownload = false;
    }

    public Rule(String criteria, String comparison, String value, boolean download) {
        mCriteria = criteria;
        mComparison = comparison;
        mValue = value.toLowerCase().trim();
        mDownload = download;
    }

    public String getCriteria() {
        return mCriteria;
    }

    public void setCriteria(String value) {
        mCriteria = value;
    }

    public String getComparison() {
        return mComparison;
    }

    public void setComparison(String value) {
        mComparison = value;
    }

    public String getValue() {
        return mValue;
    }

    public void setValue(String value) {
        mValue = value.toLowerCase().trim();
    }

    public boolean getDownload() {
        return mDownload;
    }

    public void setDownload(boolean value) {
        mDownload = value;
    }

    public boolean match(Video video) {
        if (mValue.length() == 0)
            return true;
        if (mCriteria.equals(CRITERIA_TITLE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getTitle().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getTitle().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getTitle().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getTitle().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_DESCRIPTION)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getDescription().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getDescription().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getDescription().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getDescription().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_EPISODE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getEpisodeTitle().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getEpisodeTitle().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getEpisodeTitle().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getEpisodeTitle().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_CHANNEL)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getChannel().toLowerCase().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getChannel().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getChannel().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getChannel().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_STATION)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getStation().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getStation().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getStation().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getStation().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_RATING)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getRating().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getRating().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getRating().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getRating().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_QUALITY)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getRecordingQuality().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getRecordingQuality().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getRecordingQuality().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getRecordingQuality().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_GENRE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getProgramGenre().toLowerCase().indexOf(mValue) != -1);
        } else if (mCriteria.equals(CRITERIA_TYPE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (video.getProgramGenre().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (video.getProgramGenre().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (video.getProgramGenre().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (video.getProgramGenre().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_DATE)) {
            try {
                DateFormat dateFormat = new SimpleDateFormat();
                Date date = dateFormat.parse(mValue);
                if (mComparison.equals(COMPARISON_EQUALS))
                    return (video.getDateRecorded().equals(date));
                else if (mComparison.equals(COMPARISON_MORE_THAN))
                    return (video.getDateRecorded().after(date));
                else if (mComparison.equals(COMPARISON_LESS_THAN))
                    return (video.getDateRecorded().before(date));
            } catch (ParseException ex) {
                Tools.logException(Rule.class, ex);
            }

        } else if (mCriteria.equals(CRITERIA_DURATION)) {
            try {
                int duration = Integer.parseInt(mValue) / 1000;
                if (mComparison.equals(COMPARISON_EQUALS))
                    return video.getDuration() == duration;
                else if (mComparison.equals(COMPARISON_MORE_THAN))
                    return video.getDuration() > duration;
                else if (mComparison.equals(COMPARISON_LESS_THAN))
                    return video.getDuration() < duration;
            } catch (NumberFormatException ex) {
                Tools.logException(Rule.class, ex);
            }
        } else if (mCriteria.equals(CRITERIA_SIZE)) {
            long size = Long.parseLong(mValue) / (1024 * 1024);
            if (mComparison.equals(COMPARISON_EQUALS))
                return video.getSize() == size;
            else if (mComparison.equals(COMPARISON_MORE_THAN))
                return video.getSize() > size;
            else if (mComparison.equals(COMPARISON_LESS_THAN))
                return video.getSize() < size;
        }
        return false;
    }

    public String getCriteriaString() {
        if (mCriteria.equals(CRITERIA_TITLE))
            return "Title";
        else if (mCriteria.equals(CRITERIA_DESCRIPTION))
            return "Description";
        else if (mCriteria.equals(CRITERIA_EPISODE))
            return "Episode";
        else if (mCriteria.equals(CRITERIA_CHANNEL))
            return "Channel";
        else if (mCriteria.equals(CRITERIA_STATION))
            return "Station";
        else if (mCriteria.equals(CRITERIA_RATING))
            return "Rating";
        else if (mCriteria.equals(CRITERIA_QUALITY))
            return "Quality";
        else if (mCriteria.equals(CRITERIA_GENRE))
            return "Genre";
        else if (mCriteria.equals(CRITERIA_TYPE))
            return "Type";
        else if (mCriteria.equals(CRITERIA_DATE))
            return "Date";

        return "";
    }

    public String getComparisonString() {
        if (mComparison.equals(COMPARISON_EQUALS))
            return "Equals";
        else if (mComparison.equals(COMPARISON_CONTAINS))
            return "Contains";
        else if (mComparison.equals(COMPARISON_STARTS_WITH))
            return "Starts With";
        else if (mComparison.equals(COMPARISON_ENDS_WITH))
            return "Ends With";
        else if (mComparison.equals(COMPARISON_MORE_THAN))
            return "More Than";
        else if (mComparison.equals(COMPARISON_LESS_THAN))
            return "Less Than";

        return "";
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append("Criteria=" + mCriteria + '\n');
            buffer.append("Comparison=" + mComparison + '\n');
            buffer.append("Value=" + mValue + '\n');
            buffer.append("Download=" + mDownload + '\n');
        }
        return buffer.toString();
    }

    private String mCriteria;

    private String mComparison;

    private String mValue;

    private boolean mDownload;
}