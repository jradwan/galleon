package org.lnicholls.galleon.apps.togo;

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

import java.awt.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.*;
import java.util.Iterator;
import java.util.List;

import javax.swing.ImageIcon;

import com.tivo.hme.bananas.*;

import net.sf.hibernate.HibernateException;

import org.jdom.Element;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.togo.ToGoThread;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.database.*;

import com.tivo.hme.sdk.*;

import org.apache.log4j.Logger;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Based on TiVo Bananas sample code by Carl Haynes
 */

public class ToGo extends BApplication {

    private static Logger log = Logger.getLogger(ToGo.class.getName());

    public final static String TITLE = "ToGo";

    private Resource mYellowIcon;

    private Resource mYellowExclamationIcon;

    private Resource mWhiteIcon;

    private Resource mGreenIcon;

    private Resource mRedIcon;

    private Resource mBlueIcon;

    private Resource mEmptyIcon;
    
    protected void init(Context context) {
        super.init(context);

        mYellowIcon = getResource("yellowball.png");
        mYellowExclamationIcon = getResource("yellowball!.png");
        mWhiteIcon = getResource("whiteball.png");
        mGreenIcon = getResource("greenball.png");
        mRedIcon = getResource("redball.png");
        mBlueIcon = getResource("blueball.png");
        mEmptyIcon = getResource("empty.png");

        push(new ToGoMenuScreen(this), TRANSITION_NONE);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public class DefaultScreen extends BScreen {
        public DefaultScreen(ToGo app) {
            super(app);

            below.setResource("cinemabackground.jpg");

            mTitle = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V, (width - (SAFE_TITLE_H * 2)), 54);
            mTitle.setValue(" ");
            mTitle.setColor(Color.yellow);
            mTitle.setShadow(Color.black, 3);
            mTitle.setFlags(RSRC_HALIGN_CENTER);
            mTitle.setFont("default-48.font");
        }

        public void setTitle(String value) {
            mTitle.setValue(value);
        }

        private BText mTitle;

    }

    public class ToGoMenuScreen extends DefaultScreen {
        private TGList list;

        public ToGoMenuScreen(ToGo app) {
            super(app);
            setTitle("Now Playing List");
            
            int top = SAFE_TITLE_V + 50;
            int border_left = SAFE_TITLE_H + 20;
            int text_width = width - ((SAFE_TITLE_H * 2) + 82);
            
            int start = top;
            
            BText countText = new BText(normal, border_left, start, text_width, 20);
            countText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            countText.setFont("default-18.font");
            countText.setColor(Color.GREEN);
            countText.setShadow(true);

            BText lengthText = new BText(normal, border_left, start, text_width, 20);
            lengthText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            lengthText.setFont("default-18.font");
            lengthText.setColor(Color.GREEN);
            lengthText.setShadow(true);

            start += 20;

            BText sizeText = new BText(normal, border_left, start, text_width, 20);
            sizeText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            sizeText.setFont("default-18.font");
            sizeText.setColor(Color.GREEN);
            sizeText.setShadow(true);

            BText availableText = new BText(normal, border_left, start, text_width, 20);
            availableText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            availableText.setFont("default-18.font");
            availableText.setColor(Color.GREEN);
            availableText.setShadow(true);
            

            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);
            
            int totalCount = 0;
            int totalTime = 0;
            long totalSize = 0;
            int totalCapacity = 0;

            // TODO Update list
            try {
                List recordings = VideoManager.listAll();
                Video[] videoArray = (Video[]) recordings.toArray(new Video[0]);
                Arrays.sort(videoArray, new Comparator() {
                    public int compare(Object o1, Object o2) {
                        Video video1 = (Video) o1;
                        Video video2 = (Video) o2;

                        return -video1.getDateRecorded().compareTo(video2.getDateRecorded());
                    }
                });

                for (int i = 0; i < videoArray.length; i++) {
                    Video video = (Video) videoArray[i];
                    if (video.getStatus()!=Video.STATUS_RECORDING)
                    {
                        list.add(new ToGoScreen(app, video));
                        totalCount = totalCount + 1;
                        totalTime = totalTime + video.getDuration();
                        totalSize = totalSize + video.getSize();
                    }
                }
            } catch (HibernateException ex) {
                log.error("Getting recordings failed", ex);
            }
            
            List tivos = Server.getServer().getTiVos();
            Iterator iterator = tivos.iterator();
            while (iterator.hasNext())
            {
                TiVo tivo = (TiVo)iterator.next();
                totalCapacity = totalCapacity + tivo.getCapacity();
            }
            
            countText.setValue("Count: "+String.valueOf(totalCount));
            SimpleDateFormat timeFormat = new SimpleDateFormat();
            timeFormat.applyPattern("H:mm");
            long duration = (long)Math.rint(totalTime / 1000 / 60.0);
            lengthText.setValue("Length: " + timeFormat.format(new Date(duration)));
            DecimalFormat numberFormat = new DecimalFormat("###,###");
            sizeText.setValue("Size: " + numberFormat.format(totalSize / (1024 * 1024)) + " MB");
            availableText.setValue("Available: " + numberFormat.format((totalCapacity*1024) - (totalSize / (1024 * 1024))) + " MB");

            setFocusDefault(list);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BScreen screen = (BScreen) (list.get(list.getFocus()));
                getBApp().push(screen, TRANSITION_LEFT);
                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                getBApp().setActive(false);
                return true;
            }

            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "ToGo";
        }

        public class TGList extends BList {
            protected SimpleDateFormat mDateFormat;

            protected GregorianCalendar mCalendar;

            public TGList(BView parent, int x, int y, int width, int height, int rowHeight) {
                super(parent, x, y, width, height, rowHeight);

                mDateFormat = new SimpleDateFormat();
                mDateFormat.applyPattern("EEE M/dd");
                mCalendar = new GregorianCalendar();

                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index) {
                BView icon = new BView(parent, 10, 3, 30, 30);
                Video video = ((ToGoScreen) get(index)).getVideo();
                if (video.getIcon()!=null)
                {
                    if (video.getIcon().equals("in-progress-recording"))
                        icon.setResource(mRedIcon);
                    else if (video.getIcon().equals("expires-soon-recording"))
                        icon.setResource(mYellowIcon);
                    else if (video.getIcon().equals("expired-recording"))
                        icon.setResource(mYellowExclamationIcon);
                    else if (video.getIcon().equals("save-until-i-delete-recording"))
                        icon.setResource(mGreenIcon);
                    else
                        icon.setResource(mEmptyIcon);
                }

                BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
                name.setShadow(true);
                if (video.getStatus()==Video.STATUS_DOWNLOADING)
                    name.setFont("default-24-italic.font");
                else
                    name.setFont("default-24.font");
                name.setFlags(RSRC_HALIGN_LEFT);
                name.setValue(trim(video.getTitle(),29));

                mCalendar.setTime(video.getDateRecorded());
                mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                        + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);

                BText date = new BText(parent, parent.width - 100 - parent.height, 4, 100, parent.height - 4);
                date.setShadow(true);
                date.setFlags(RSRC_HALIGN_RIGHT);
                date.setValue(mDateFormat.format(mCalendar.getTime()));
            }

            public boolean handleKeyPress(int code, long rawcode) {
                switch (code) {
                case KEY_SELECT:
                    postEvent(new BEvent.Action(this, "push"));
                    return true;
                }
                return super.handleKeyPress(code, rawcode);
            }
        }
    }
    
    public String trim(String value, int max)
    {
        if (value!=null && value.length()>max)
            return value.substring(0,max-3)+"...";
        return value;
    }

    public class ToGoScreen extends DefaultScreen {
        protected SimpleDateFormat mDateFormat;

        protected SimpleDateFormat mTimeFormat;

        protected GregorianCalendar mCalendar;

        protected DecimalFormat mNumberFormat;

        private BList list;
        
        private final int top = SAFE_TITLE_V + 100;
        private final int border_left = SAFE_TITLE_H + 20;
        private final int text_width = width - ((SAFE_TITLE_H * 2) + 32);

        public ToGoScreen(ToGo app, Video video) {
            super(app);

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm a");
            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("H:mm");
            mCalendar = new GregorianCalendar();
            mNumberFormat = new DecimalFormat("###,###");

            mVideo = video;
            
            setTitle("Program");
            
            int start = top;

            int location = 40;
            icon = new BView(normal, border_left, start + 3, 30, 30);

            titleText = new BText(normal, border_left + location, start, text_width - 40, 40);
            titleText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            titleText.setFont("default-36.font");
            titleText.setShadow(true);

            start += 45;

            descriptionText = new BText(normal, border_left, start, text_width, 90);
            descriptionText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            descriptionText.setFont("default-18-bold.font");
            descriptionText.setShadow(true);

            start += 85;

            dateText = new BText(normal, border_left, start, text_width, 30);
            dateText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            dateText.setFont("default-18.font");
            dateText.setShadow(true);

            durationText = new BText(normal, border_left, start, text_width, 30);
            durationText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            durationText.setFont("default-18.font");
            durationText.setShadow(true);

            start += 20;

            ratingText = new BText(normal, border_left, start, text_width, 30);
            ratingText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            ratingText.setFont("default-18.font");
            ratingText.setShadow(true);

            videoText = new BText(normal, border_left, start, text_width, 30);
            videoText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            videoText.setFont("default-18.font");
            videoText.setShadow(true);

            start += 20;

            genreText = new BText(normal, border_left, start, text_width, 30);
            genreText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            genreText.setFont("default-18.font");
            genreText.setShadow(true);
            
            sizeText = new BText(normal, border_left, start, text_width, 30);
            sizeText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            sizeText.setFont("default-18.font");
            sizeText.setShadow(true);

            start += 30;

            statusText = new BText(normal, border_left, start, text_width, 30);
            statusText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            statusText.setFont("default-24-bolditalic.font");
            //statusText.setColor(new Color(150, 100, 100));
            statusText.setColor(Color.ORANGE);
            statusText.setShadow(true);

            start += 35;
            
            statusBarBg = new BView (normal, width - SAFE_TITLE_H - text_width/3, start , text_width/3, 30);
            statusBarBg.setResource(Color.WHITE);
            statusBarBg.setTransparency(.5f);
            statusBarBg.setVisible(false);
            statusBar = new BView (normal, width - SAFE_TITLE_H - text_width/3 + 2, start + 2, text_width/3-4, 30 - 4);
            statusBar.setResource(Color.GREEN);
            statusBar.setVisible(false);
            /*
            speedText = new BText(normal, width - SAFE_TITLE_H - text_width/3, start , text_width/3, 30);
            speedText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER | IHmeProtocol.RSRC_VALIGN_CENTER);
            speedText.setFont("default-18-bold.font");
            speedText.setColor(Color.BLACK);
            speedText.setShadow(Color.DARK_GRAY,1);
            speedText.setValue(" ");
            speedText.setVisible(false);
            */

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 80,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Save to computer");
            list.add("Don't do anything");

            setFocusDefault(list);
            
            updateText();
        }
        
        private void updateText()
        {
            int location = 40;
            if (mVideo.getIcon()!=null)
            {
                if (mVideo.getIcon().equals("in-progress-recording"))
                    icon.setResource(mRedIcon);
                else if (mVideo.getIcon().equals("expires-soon-recording"))
                    icon.setResource(mYellowIcon);
                else if (mVideo.getIcon().equals("expired-recording"))
                    icon.setResource(mYellowExclamationIcon);
                else if (mVideo.getIcon().equals("save-until-i-delete-recording"))
                    icon.setResource(mGreenIcon);
                else {
                    icon.setResource(mEmptyIcon);
                    location = 0;
                }
            }
            else
                location = 0;
            titleText.setLocation(border_left + location, top);

            titleText.setValue(mVideo.getTitle()==null?"":trim(mVideo.getTitle(),30));

            mCalendar.setTime(mVideo.getOriginalAirDate()==null?new Date():mVideo.getOriginalAirDate());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);            

            String description = null;
            if (mVideo.getEpisodeTitle()!=null && mVideo.getEpisodeTitle().length()!=0)
            {
                description = mVideo.getEpisodeTitle() + " ("+ mCalendar.get(Calendar.YEAR)+")";
                if (mVideo.getDescription()!=null)
                    description = description + " "  +mVideo.getDescription();
            }
            else
            {
                if (mVideo.getDescription()==null)
                    description = "("+ mCalendar.get(Calendar.YEAR)+")";
                else
                    description = mVideo.getDescription()+ " ("+ mCalendar.get(Calendar.YEAR)+") ";
            }
            
            descriptionText.setValue(description);

            mCalendar.setTime(mVideo.getDateRecorded()==null?new Date():mVideo.getDateRecorded());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);

            dateText.setValue(mDateFormat.format(mCalendar.getTime()) + " - " + mVideo.getChannelMajorNumber() + " "
                    + mVideo.getStation());

            int duration = (int)Math.rint(mVideo.getDuration() / 1000 / 60.0);
            mCalendar.set(GregorianCalendar.HOUR_OF_DAY, (duration / 60));
            mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            durationText.setValue("Duration: " + mTimeFormat.format(mCalendar.getTime()));

            ratingText.setValue("Rated: " + (mVideo.getRating()==null?"N/A":mVideo.getRating()));

            videoText.setValue("Video: " + mVideo.getRecordingQuality());

            genreText.setValue("Genre: " + trim(mVideo.getProgramGenre(),40));
            
            sizeText.setValue("Size: " + mNumberFormat.format(mVideo.getSize() / (1024 * 1024)) + " MB");

            statusText.setValue(mVideo.getStatusString());
            
            if (mVideo.getStatus()==Video.STATUS_DOWNLOADING || mVideo.getStatus()==Video.STATUS_DOWNLOADED)
            {
                statusBarBg.setVisible(true);
                statusBar.setVisible(true);
                //speedText.setVisible(true);
                
                if (mVideo.getDownloadTime()>0)
                {
                    long rate = (mVideo.getDownloadSize()/1024)/mVideo.getDownloadTime();
                    statusText.setValue(mVideo.getStatusString()+": "+rate+" KB/Sec");
                    //speedText.setValue(rate+" KB/Sec");
                    if (mVideo.getStatus()==Video.STATUS_DOWNLOADED)
                    {
                        statusBar.setSize(statusBarBg.width-4,statusBar.height);
                    }
                    else
                    {
                        float barFraction = mVideo.getDownloadSize()/(float)mVideo.getSize();
                        if ((statusBarBg.width-4)*barFraction<1)
                            statusBar.setSize(1,statusBar.height);
                        else
                            statusBar.setSize((int)(barFraction*(statusBarBg.width-4)),statusBar.height);
                    }
                }
                else
                {
                    statusText.setValue(mVideo.getStatusString()+": "+"0 KB/Sec");
                    //speedText.setValue("0 KB/Sec");
                    statusBar.setVisible(false);
                }
            }
            else
            {
                statusBarBg.setVisible(false);
                statusBar.setVisible(false);
                //speedText.setVisible(false);
            }
            
            Boolean status = new Boolean(mVideo.getStatus() == Video.STATUS_RULE_MATCHED
                    || mVideo.getStatus() == Video.STATUS_USER_SELECTED
                    || mVideo.getStatus() == Video.STATUS_DOWNLOADING);
            if (status.booleanValue())
                list.set(0, "Don't save to computer");
            else
                list.set(0, "Save to computer");
            
            if (ToGoScreen.this.getApp().context != null)
                flush();
        }
        
        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (mUpdateThread!=null && mUpdateThread.isAlive())
                mUpdateThread.interrupt();
            
            mUpdateThread = new Thread() {
                public void run()
                {
                    int counter = 0;
                    while (true)
                    {
                        if (ToGoScreen.this.getApp().context == null)
                            return;
                        
                        try
                        {
                            Video updated = VideoManager.retrieveVideo(mVideo.getId());
                            PropertyUtils.copyProperties(mVideo, updated);
                            updateText();
                            if (counter++<10)
                                sleep(1000 * 5);
                            else
                                sleep(1000 * 10);
                        } catch (HibernateException ex) {
                            try
                            {
                                log.error("Video retrieve failed", ex);
                            }
                            catch( Exception ex2){}
                            return;
                        } catch (InterruptedException ex) {
                            return;
                        } // handle silently for waking up
                        catch (Exception ex2) {
                            Tools.logException(ToGoThread.class, ex2);
                            return;
                        }
                    }
                }
            };
            mUpdateThread.start();
            return super.handleEnter(arg, isReturn);
        }
        
        public boolean handleExit() {
            if (mUpdateThread!=null && mUpdateThread.isAlive())
                mUpdateThread.interrupt();
            mUpdateThread = null;
            return super.handleExit();
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                if (list.getFocus() == 0) {
                    postEvent(new BEvent.Action(this, "record"));
                    return true;
                } else {
                    postEvent(new BEvent.Action(this, "pop"));
                    return true;
                }
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("record")) {
                Boolean status = new Boolean(mVideo.getStatus() == Video.STATUS_RULE_MATCHED
                        || mVideo.getStatus() == Video.STATUS_USER_SELECTED
                        || mVideo.getStatus() == Video.STATUS_DOWNLOADING);
                if (status.booleanValue()) {
                    mVideo.setStatus(Video.STATUS_USER_CANCELLED);
                    getBApp().play("thumbsdown.snd");
                } else {
                    mVideo.setStatus(Video.STATUS_USER_SELECTED);
                    getBApp().play("thumbsup.snd");
                }

                try {
                    log.debug("video: "+mVideo.toString());
                    Server.getServer().updateVideo(mVideo);
                    mVideo = VideoManager.retrieveVideo(mVideo.getId());
                } catch (Exception ex) {
                    log.error("Video update failed", ex);
                }
                
                updateText();

                return true;
            }

            return super.handleAction(view, action);
        }

        public Video getVideo() {
            return mVideo;
        }

        public String toString() {
            return mVideo.getTitle();
        }

        public class OptionList extends BList {
            public OptionList(BView parent, int x, int y, int width, int height, int rowHeight) {
                super(parent, x, y, width, height, rowHeight);

                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index) {
                BText text = new BText(parent, 10, 4, parent.width - 40, parent.height - 4);
                text.setShadow(true);
                text.setFlags(RSRC_HALIGN_LEFT);
                text.setValue(get(index).toString());
            }

            public boolean handleKeyPress(int code, long rawcode) {
                switch (code) {
                case KEY_SELECT:
                    if (list.getFocus() == 1)
                        postEvent(new BEvent.Action(this, "pop"));
                    else
                        postEvent(new BEvent.Action(this, "record"));
                    return true;
                }
                return super.handleKeyPress(code, rawcode);
            }
        }

        private Video mVideo;

        private BText statusText;
        
        int location = 40;
        private BView icon;
        private BText titleText;
        private BText descriptionText;
        private BText dateText;
        private BText durationText;
        private BText ratingText;
        private BText videoText;
        private BText genreText;
        private BText sizeText;
        private BView statusBarBg;
        private BView statusBar;
        //private BText speedText;
        private Thread mUpdateThread;
    }
}