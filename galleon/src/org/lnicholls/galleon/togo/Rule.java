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

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;

public class Rule {

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

    public boolean match(Show show) {
        if (mValue.length() == 0)
            return true;
        if (mCriteria.equals(CRITERIA_TITLE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getTitle().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getTitle().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getTitle().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getTitle().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_DESCRIPTION)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getDescription().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getDescription().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getDescription().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getDescription().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_EPISODE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getEpisode().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getEpisode().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getEpisode().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getEpisode().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_CHANNEL)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getChannel().toLowerCase().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getChannel().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getChannel().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getChannel().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_STATION)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getStation().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getStation().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getStation().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getStation().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_RATING)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getRating().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getRating().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getRating().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getRating().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_QUALITY)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getQuality().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getQuality().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getQuality().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getQuality().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_GENRE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getGenre().toLowerCase().indexOf(mValue) != -1);
        } else if (mCriteria.equals(CRITERIA_TYPE)) {
            if (mComparison.equals(COMPARISON_CONTAINS))
                return (show.getGenre().toLowerCase().indexOf(mValue) != -1);
            else if (mComparison.equals(COMPARISON_EQUALS))
                return (show.getGenre().toLowerCase().equals(mValue));
            else if (mComparison.equals(COMPARISON_STARTS_WITH))
                return (show.getGenre().toLowerCase().startsWith(mValue));
            else if (mComparison.equals(COMPARISON_ENDS_WITH))
                return (show.getGenre().toLowerCase().endsWith(mValue));
        } else if (mCriteria.equals(CRITERIA_DATE)) {
            try {
                DateFormat dateFormat = new SimpleDateFormat();
                Date date = dateFormat.parse(mValue);
                if (mComparison.equals(COMPARISON_EQUALS))
                    return (show.getDateRecorded().equals(date));
                else if (mComparison.equals(COMPARISON_MORE_THAN))
                    return (show.getDateRecorded().after(date));
                else if (mComparison.equals(COMPARISON_LESS_THAN))
                    return (show.getDateRecorded().before(date));
            } catch (ParseException ex) {
                Tools.logException(Rule.class, ex);
            }

        } else if (mCriteria.equals(CRITERIA_DURATION)) {
            try {
                int duration = Integer.parseInt(mValue) / 1000;
                if (mComparison.equals(COMPARISON_EQUALS))
                    return show.getDuration() == duration;
                else if (mComparison.equals(COMPARISON_MORE_THAN))
                    return show.getDuration() > duration;
                else if (mComparison.equals(COMPARISON_LESS_THAN))
                    return show.getDuration() < duration;
            } catch (NumberFormatException ex) {
                Tools.logException(Rule.class, ex);
            }
        } else if (mCriteria.equals(CRITERIA_SIZE)) {
            long size = Long.parseLong(mValue) / (1024 * 1024);
            if (mComparison.equals(COMPARISON_EQUALS))
                return show.getSize() == size;
            else if (mComparison.equals(COMPARISON_MORE_THAN))
                return show.getSize() > size;
            else if (mComparison.equals(COMPARISON_LESS_THAN))
                return show.getSize() < size;
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