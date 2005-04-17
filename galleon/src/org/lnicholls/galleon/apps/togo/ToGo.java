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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.TiVo;
import org.lnicholls.galleon.togo.ToGoThread;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class ToGo extends DefaultApplication {

    private static Logger log = Logger.getLogger(ToGo.class.getName());

    public final static String TITLE = "ToGo";
    
    private Resource mMenuBackground;
    
    private Resource mInfoBackground;    

    private Resource mYellowIcon;

    private Resource mYellowExclamationIcon;

    private Resource mWhiteIcon;

    private Resource mGreenIcon;

    private Resource mRedIcon;

    private Resource mBlueIcon;

    private Resource mEmptyIcon;

    private ToGoScreen mToGoScreen;

    protected void init(Context context) {
        super.init(context);
        
        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mYellowIcon = getSkinImage("menu", "expiresSoon");
        mYellowExclamationIcon = getSkinImage("menu", "expired");
        mWhiteIcon = getSkinImage("menu", "info");
        mGreenIcon = getSkinImage("menu", "saveUntilDelete");
        mRedIcon = getSkinImage("menu", "recording");
        mBlueIcon = getSkinImage("menu", "suggestion");
        mEmptyIcon = getSkinImage("menu", "empty");
        
        mToGoScreen = new ToGoScreen(this);

        push(new ToGoMenuScreen(this), TRANSITION_NONE);
    }

    public class ToGoMenuScreen extends DefaultMenuScreen {
        public ToGoMenuScreen(ToGo app) {
            super(app, "Now Playing List");
            
            below.setResource(mMenuBackground);

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/dd");
            mCalendar = new GregorianCalendar();

            int start = TOP - 30;
            int offset = 30;

            ToGoConfiguration togoConfiguration = (ToGoConfiguration) ((ToGoFactory) context.factory).getAppContext()
                    .getConfiguration();
            BText countText = new BText(normal, BORDER_LEFT + offset, start, BODY_WIDTH - 2 * offset, 20);
            countText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            countText.setFont("default-18.font");
            countText.setColor(Color.GREEN);
            countText.setShadow(true);

            BText lengthText = new BText(normal, BORDER_LEFT + offset, start, BODY_WIDTH - 2 * offset, 20);
            lengthText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            lengthText.setFont("default-18.font");
            lengthText.setColor(Color.GREEN);
            lengthText.setShadow(true);

            start += 20;

            BText sizeText = new BText(normal, BORDER_LEFT + offset, start, BODY_WIDTH - 2 * offset, 20);
            sizeText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            sizeText.setFont("default-18.font");
            sizeText.setColor(Color.GREEN);
            sizeText.setShadow(true);

            BText availableText = new BText(normal, BORDER_LEFT + offset, start, BODY_WIDTH - 2 * offset, 20);
            availableText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            availableText.setFont("default-18.font");
            availableText.setColor(Color.GREEN);
            availableText.setShadow(true);

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
                    if (video.getStatus() != Video.STATUS_RECORDING && video.getStatus() != Video.STATUS_DOWNLOADED) {
                        mMenuList.add(mMenuList.size(), video);
                        totalCount = totalCount + 1;
                        totalTime = totalTime + video.getDuration();
                        totalSize = totalSize + video.getSize();
                    }
                }
            } catch (HibernateException ex) {
                log.error("Getting recordings failed", ex);
            }

            if (togoConfiguration.isShowStats()) {
                List tivos = Server.getServer().getTiVos();
                Iterator iterator = tivos.iterator();
                while (iterator.hasNext()) {
                    TiVo tivo = (TiVo) iterator.next();
                    totalCapacity = totalCapacity + tivo.getCapacity();
                }
    
                long available = (totalCapacity * 1024) - (totalSize / (1024 * 1024));
                if (available < 0)
                    available = 0;
                countText.setValue("Total: " + String.valueOf(totalCount));
                SimpleDateFormat timeFormat = new SimpleDateFormat();
                timeFormat.applyPattern("H:mm");
                int duration = (int) Math.rint(totalTime / 1000 / 60.0);
                mCalendar.set(GregorianCalendar.HOUR_OF_DAY, (duration / 60));
                mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);
                lengthText.setValue("Length: " + timeFormat.format(mCalendar.getTime()));
                DecimalFormat numberFormat = new DecimalFormat("###,###");
                sizeText.setValue("Size: " + numberFormat.format(totalSize / (1024 * 1024)) + " MB");
                availableText.setValue("Available: " + numberFormat.format(available) + " MB");
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                mToGoScreen.setList(mMenuList);
                getBApp().push(mToGoScreen, TRANSITION_LEFT);
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            Video video = (Video) mMenuList.get(index);
            if (video.getIcon() != null) {
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
            if (video.getStatus() == Video.STATUS_DOWNLOADING)
                name.setFont("default-24-italic.font");
            else
                name.setFont("default-24.font");
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(video.getTitle(), 29));

            mCalendar.setTime(video.getDateRecorded());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);

            BText date = new BText(parent, parent.width - 100 - parent.height, 4, 100, parent.height - 4);
            date.setShadow(true);
            date.setFlags(RSRC_HALIGN_RIGHT);
            date.setValue(mDateFormat.format(mCalendar.getTime()));
        }

        protected SimpleDateFormat mDateFormat;

        protected GregorianCalendar mCalendar;
    }

    public class ToGoScreen extends DefaultScreen {
        protected SimpleDateFormat mDateFormat;

        protected SimpleDateFormat mTimeFormat;

        protected GregorianCalendar mCalendar;

        protected DecimalFormat mNumberFormat;

        private BList list;

        public ToGoScreen(ToGo app) {
            super(app, "Program", true);
            
            below.setResource(mInfoBackground);

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm a");
            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("H:mm");
            mCalendar = new GregorianCalendar();
            mNumberFormat = new DecimalFormat("###,###");

            int start = TOP;

            int location = 40;
            icon = new BView(normal, BORDER_LEFT, start + 3, 30, 30);

            titleText = new BText(normal, BORDER_LEFT + location, start, BODY_WIDTH - 40, 40);
            titleText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
            titleText.setFont("default-36.font");
            titleText.setShadow(true);

            start += 45;

            descriptionText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 90);
            descriptionText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            descriptionText.setFont("default-18-bold.font");
            descriptionText.setShadow(true);

            start += 85;

            dateText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            dateText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            dateText.setFont("default-18.font");
            dateText.setShadow(true);

            durationText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            durationText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            durationText.setFont("default-18.font");
            durationText.setShadow(true);

            start += 20;

            ratingText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            ratingText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            ratingText.setFont("default-18.font");
            ratingText.setShadow(true);

            videoText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            videoText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            videoText.setFont("default-18.font");
            videoText.setShadow(true);

            start += 20;

            genreText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            genreText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            genreText.setFont("default-18.font");
            genreText.setShadow(true);

            sizeText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            sizeText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            sizeText.setFont("default-18.font");
            sizeText.setShadow(true);

            start += 30;

            statusText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            statusText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            statusText.setFont("default-24-bolditalic.font");
            //statusText.setColor(new Color(150, 100, 100));
            statusText.setColor(Color.ORANGE);
            statusText.setShadow(true);

            start += 35;

            statusBarBg = new BView(normal, width - SAFE_TITLE_H - BODY_WIDTH / 3, start, BODY_WIDTH / 3, 30);
            statusBarBg.setResource(Color.WHITE);
            statusBarBg.setTransparency(.5f);
            statusBarBg.setVisible(false);
            statusBar = new BView(normal, width - SAFE_TITLE_H - BODY_WIDTH / 3 + 2, start + 2, BODY_WIDTH / 3 - 4,
                    30 - 4);
            statusBar.setResource(Color.GREEN);
            statusBar.setVisible(false);
            /*
             * speedText = new BText(normal, width - SAFE_TITLE_H - text_width/3, start , text_width/3, 30);
             * speedText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER | IHmeProtocol.RSRC_VALIGN_CENTER);
             * speedText.setFont("default-18-bold.font"); speedText.setColor(Color.BLACK);
             * speedText.setShadow(Color.DARK_GRAY,1); speedText.setValue(" "); speedText.setVisible(false);
             */

            list = new DefaultOptionList(this.normal, SAFE_TITLE_H, (height - SAFE_TITLE_V) - 80,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Save to computer");
            list.add("Don't do anything");

            setFocusDefault(list);
        }

        private void updateText() {
            setPainting(false);
            try {
                int location = 40;
                Video video = getVideo();
                if (icon.resource != null)
                    icon.setResource(mEmptyIcon);
                if (video.getIcon() != null) {
                    if (video.getIcon().equals("in-progress-recording"))
                        icon.setResource(mRedIcon);
                    else if (video.getIcon().equals("expires-soon-recording"))
                        icon.setResource(mYellowIcon);
                    else if (video.getIcon().equals("expired-recording"))
                        icon.setResource(mYellowExclamationIcon);
                    else if (video.getIcon().equals("save-until-i-delete-recording"))
                        icon.setResource(mGreenIcon);
                    else {
                        icon.setResource(mEmptyIcon);
                        location = 0;
                    }
                } else
                    location = 0;
                titleText.setLocation(BORDER_LEFT + location, TOP);

                titleText.setValue(video.getTitle() == null ? "" : Tools.trim(video.getTitle(), 30));

                mCalendar.setTime(video.getOriginalAirDate() == null ? new Date() : video.getOriginalAirDate());
                mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                        + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);

                String description = null;
                if (video.getEpisodeTitle() != null && video.getEpisodeTitle().length() != 0) {
                    description = video.getEpisodeTitle() + " (" + mCalendar.get(Calendar.YEAR) + ")";
                    if (video.getDescription() != null)
                        description = description + " " + video.getDescription();
                } else {
                    if (video.getDescription() == null)
                        description = "(" + mCalendar.get(Calendar.YEAR) + ")";
                    else
                        description = video.getDescription() + " (" + mCalendar.get(Calendar.YEAR) + ") ";
                }

                descriptionText.setValue(description);

                mCalendar.setTime(video.getDateRecorded() == null ? new Date() : video.getDateRecorded());
                mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                        + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);

                dateText.setValue(mDateFormat.format(mCalendar.getTime()) + " - " + video.getChannelMajorNumber() + " "
                        + video.getStation());

                int duration = (int) Math.rint(video.getDuration() / 1000 / 60.0);
                mCalendar.set(GregorianCalendar.HOUR_OF_DAY, (duration / 60));
                mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);

                durationText.setValue("Duration: " + mTimeFormat.format(mCalendar.getTime()));

                ratingText.setValue("Rated: " + (video.getRating() == null ? "N/A" : video.getRating()));

                videoText.setValue("Video: " + video.getRecordingQuality());

                genreText.setValue("Genre: " + Tools.trim(video.getProgramGenre(), 40));

                sizeText.setValue("Size: " + mNumberFormat.format(video.getSize() / (1024 * 1024)) + " MB");

                statusText.setValue(video.getStatusString());

                if (video.getStatus() == Video.STATUS_DOWNLOADING || video.getStatus() == Video.STATUS_DOWNLOADED) {
                    statusBarBg.setVisible(true);
                    statusBar.setVisible(true);
                    //speedText.setVisible(true);

                    if (video.getDownloadTime() > 0) {
                        long rate = (video.getDownloadSize() / 1024) / video.getDownloadTime();
                        statusText.setValue(video.getStatusString() + ": " + rate + " KB/Sec");
                        //speedText.setValue(rate+" KB/Sec");
                        if (video.getStatus() == Video.STATUS_DOWNLOADED) {
                            statusBar.setSize(statusBarBg.width - 4, statusBar.height);
                        } else {
                            float barFraction = video.getDownloadSize() / (float) video.getSize();
                            if ((statusBarBg.width - 4) * barFraction < 1)
                                statusBar.setSize(1, statusBar.height);
                            else
                                statusBar.setSize((int) (barFraction * (statusBarBg.width - 4)), statusBar.height);
                        }
                    } else {
                        statusText.setValue(video.getStatusString() + ": " + "0 KB/Sec");
                        //speedText.setValue("0 KB/Sec");
                        statusBar.setVisible(false);
                    }
                } else {
                    statusBarBg.setVisible(false);
                    statusBar.setVisible(false);
                    //speedText.setVisible(false);
                }

                Boolean status = new Boolean(video.getStatus() == Video.STATUS_RULE_MATCHED
                        || video.getStatus() == Video.STATUS_USER_SELECTED
                        || video.getStatus() == Video.STATUS_DOWNLOADING);
                if (status.booleanValue())
                    list.set(0, "Don't save to computer");
                else
                    list.set(0, "Save to computer");
            } finally {
                setPainting(true);
            }

            if (ToGoScreen.this.getApp().context != null)
                flush();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            setPainting(false);
            try {
                if (mUpdateThread != null && mUpdateThread.isAlive())
                    mUpdateThread.interrupt();
            } finally {
                setPainting(true);
            }

            mUpdateThread = new Thread() {
                public void run() {
                    int counter = 0;
                    while (true) {
                        try {
                            synchronized (this) {
                                updateText();
                            }

                            if (counter++ < 10)
                                sleep(1000 * 5);
                            else
                                sleep(1000 * 10);
                        } catch (InterruptedException ex) {
                            return;
                        } // handle silently for waking up
                        catch (Exception ex2) {
                            Tools.logException(ToGoThread.class, ex2);
                            return;
                        }
                    }
                }

                public void interrupt() {
                    synchronized (this)
                    {
                        super.interrupt();
                    }
                }
            };
            mUpdateThread.start();
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            setPainting(false);
            try {
                if (mUpdateThread != null && mUpdateThread.isAlive())
                    mUpdateThread.interrupt();
            } finally {
                setPainting(true);
            }
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
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_CHANNELUP:
                getBApp().play("pageup.snd");
                getBApp().flush();
                getPrevPos();
                updateText();
                return true;
            case KEY_CHANNELDOWN:
                getBApp().play("pagedown.snd");
                getBApp().flush();
                getNextPos();
                updateText();
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public void getNextPos() {
            if (mList != null) {
                int pos = mList.getFocus() + 1;
                if (mList.getFocus() == mList.size() - 1)
                    pos = 0;
                mList.setFocus(pos, false);
            }
        }

        public void getPrevPos() {
            if (mList != null) {
                int pos = mList.getFocus() - 1;
                if (mList.getFocus() == 0)
                    pos = mList.size() - 1;
                mList.setFocus(pos, false);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("record")) {
                Video video = getVideo();
                Boolean status = new Boolean(video.getStatus() == Video.STATUS_RULE_MATCHED
                        || video.getStatus() == Video.STATUS_USER_SELECTED
                        || video.getStatus() == Video.STATUS_DOWNLOADING);
                if (status.booleanValue()) {
                    video.setStatus(Video.STATUS_USER_CANCELLED);
                    getBApp().play("thumbsdown.snd");
                } else {
                    video.setStatus(Video.STATUS_USER_SELECTED);
                    getBApp().play("thumbsup.snd");
                }

                try {
                    log.debug("video: " + video.toString());
                    Server.getServer().updateVideo(video);
                    video = VideoManager.retrieveVideo(video.getId());
                } catch (Exception ex) {
                    log.error("Video update failed", ex);
                }

                updateText();

                return true;
            }

            return super.handleAction(view, action);
        }

        public void setList(BList list) {
            mList = list;
        }

        private synchronized Video getVideo() {
            Video video = (Video) mList.get(mList.getFocus());
            try {
                return VideoManager.retrieveVideo(video.getId());
            } catch (Exception ex) {
                log.error("Video retrieve failed", ex);
            }
            return video;
        }

        private BList mList;

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

    public static class ToGoFactory extends AppFactory {

        public ToGoFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
        }
    }
}