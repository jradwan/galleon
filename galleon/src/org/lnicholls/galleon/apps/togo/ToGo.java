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
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.swing.ImageIcon;

import com.tivo.hme.bananas.*;

import org.jdom.Element;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.togo.Show;
import org.lnicholls.galleon.togo.ToGoList;
import org.lnicholls.galleon.util.*;

import com.tivo.hme.sdk.*;

/**
 * Based on TiVo Bananas sample code by Carl Haynes
 */

public class ToGo extends BApplication {
    
    public final static String TITLE = "ToGo";
    
    private ToGoList togoList = new ToGoList();
    
    private Resource mYellowIcon;

    private Resource mYellowExclamationIcon;

    private Resource mWhiteIcon;

    private Resource mGreenIcon;

    private Resource mRedIcon;

    private Resource mBlueIcon;
    
    private Resource mEmptyIcon;    
    
    protected void init(Context context) 
    {
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
    
    public boolean handleAction(BView view, Object action) 
    {
        if (action.equals("pop")) {
            pop();
            return true;
        }        
        return super.handleAction(view, action);
    }
    
    public class DefaultScreen extends BScreen
    {       
        public DefaultScreen(ToGo app)
        {
            super(app);
            
            below.setResource("cinemabackground.jpg");    
            
            mTitle = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V, (width-(SAFE_TITLE_H*2)), 54);
            mTitle.setValue(" ");
            mTitle.setColor(Color.yellow);
            mTitle.setShadow(Color.black, 3);
            mTitle.setFlags(RSRC_HALIGN_CENTER);
            mTitle.setFont("default-48.font");
        }
        
        public void setTitle(String value)
        {
            mTitle.setValue(value);
        }
        
        private BText mTitle;
        
    }
    
    public class ToGoMenuScreen extends DefaultScreen
    {
        private TGList list;
        
        public ToGoMenuScreen(ToGo app)
        {
            super(app);
            setTitle("Now Playing List");
            
            list = new TGList(this.normal, SAFE_TITLE_H+10, (height-SAFE_TITLE_V)-290, width - ((SAFE_TITLE_H*2)+32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP,   A_RIGHT+13, A_TOP    - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT+13, A_BOTTOM + 30);
            
            ArrayList recordings = (ArrayList)app.togoList.load(Show.STATUS_RECORDED);
            Show[] showArray = (Show[])recordings.toArray(new Show[0]);
            Arrays.sort(showArray, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Show show1 = (Show) o1;
                    Show show2 = (Show) o2;
                    
                    return -show1.getDateRecorded().compareTo(show2.getDateRecorded());
                }
            });            
            
            for (int i=0;i<showArray.length;i++)
            {
                Show show = (Show) showArray[i];
                list.add(new ToGoScreen(app,show));
            }
            
            setFocusDefault(list);
        }
        
        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BScreen screen = (BScreen)(list.get(list.getFocus()));
                getBApp().push(screen, TRANSITION_LEFT);            
                return true;
            }        
           return super.handleAction(view, action);
        }
        
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_LEFT:
                getBApp().setActive(false);
                return true;
            }
        
            return super.handleKeyPress(code, rawcode);
        }
        
        public String toString() 
        {
            return "ToGo";
        }
        
        public class TGList extends BList 
        {
            protected SimpleDateFormat mDateFormat;
            
            protected GregorianCalendar mCalendar;
            
            public TGList(BView parent, int x, int y, int width, int height, int rowHeight)
            {
                super(parent, x, y, width, height, rowHeight);
                
                mDateFormat = new SimpleDateFormat();
                mDateFormat.applyPattern("EEE M/dd");
                mCalendar = new GregorianCalendar();
                
                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index)
            {   
                BView icon = new BView(parent, 10, 3, 30, 30);
                Show show = ((ToGoScreen)get(index)).getShow();
                if (show.getIcon().equals("in-progress-recording"))
                    icon.setResource(mRedIcon);
                else if (show.getIcon().equals("expires-soon-recording"))
                    icon.setResource(mYellowIcon);
                else if (show.getIcon().equals("expired-recording"))
                    icon.setResource(mYellowExclamationIcon);
                else if (show.getIcon().equals("save-until-i-delete-recording"))
                    icon.setResource(mGreenIcon);                
                else
                    icon.setResource(mEmptyIcon);
                
                BText name = new BText(parent, 50, 4, parent.width-40, parent.height - 4);
                name.setShadow(true);
                name.setFlags(RSRC_HALIGN_LEFT);
                name.setValue(show.getTitle());
                
                mCalendar.setTime(show.getDateRecorded());
                mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                        + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
                mCalendar.set(GregorianCalendar.SECOND, 0);
                
                BText date = new BText(parent, parent.width-100-parent.height, 4, 100, parent.height - 4);
                date.setShadow(true);
                date.setFlags(RSRC_HALIGN_RIGHT);
                date.setValue(mDateFormat.format(mCalendar.getTime()));
            }

            public boolean handleKeyPress(int code, long rawcode) 
            {
                switch (code) {
                  case KEY_SELECT:
                    postEvent(new BEvent.Action(this, "push"));
                    return true;
                }
                return super.handleKeyPress(code, rawcode);
            }
        }
    }
    
    public class ToGoScreen extends DefaultScreen
    {
        protected SimpleDateFormat mDateFormat;

        protected SimpleDateFormat mTimeFormat;

        protected GregorianCalendar mCalendar;

        protected DecimalFormat mNumberFormat;
        
        private BList list;

        
        public ToGoScreen(ToGo app, Show show)
        {
            super(app);
            
            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mma");
            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("H:mm");
            mCalendar = new GregorianCalendar();
            mNumberFormat = new DecimalFormat("###,###");            
            
            
            mShow = show;
            setTitle("Program");
            
            int top = SAFE_TITLE_V + 100;
            final int border_left = SAFE_TITLE_H + 20;
            final int text_width = width - ((SAFE_TITLE_H*2)+32);
            
            int location = 40;
            BView icon = new BView(normal, border_left, top+3, 30, 30);
            if (show.getIcon().equals("in-progress-recording"))
                icon.setResource(mRedIcon);
            else if (show.getIcon().equals("expires-soon-recording"))
                icon.setResource(mYellowIcon);
            else if (show.getIcon().equals("expired-recording"))
                icon.setResource(mYellowExclamationIcon);
            else if (show.getIcon().equals("save-until-i-delete-recording"))
                icon.setResource(mGreenIcon);                
            else
            {
                icon.setResource(mEmptyIcon);
                location = 0;
            }
            
            BText titleText = new BText(normal, border_left+location, top, text_width-40, 40);
            titleText.setFlags(RSRC_HALIGN_LEFT|RSRC_VALIGN_TOP);
            titleText.setFont("default-36.font");
            titleText.setShadow(true);
            titleText.setValue(mShow.getTitle());
            
            //
            // A wrapped line of text over a darkened area.
            //
            
            top += 45;

            /*
            BView bg = new BView (normal, border_left, top, text_width, 80);
            bg.setResource(new Color(25, 25, 50));
            bg.setTransparency(.5f);
            */
            
            BText wrapText = new BText(normal, border_left, top, text_width, 80);
            wrapText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            wrapText.setFont("default-18.font");
            String description = show.getEpisode();
            // episode (date) description (cc,stereo)
            if (description.length()!=0)
                description = description + ". " +mShow.getDescription();
            else
                description = mShow.getDescription();
            wrapText.setShadow(true);
            wrapText.setValue(description);
            
            top += 85;            
            
            mCalendar.setTime(mShow.getDateRecorded());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            BText plainSample = new BText(normal, border_left, top, text_width, 30);
            plainSample.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            plainSample.setFont("default-18.font");
            plainSample.setShadow(true);
            plainSample.setValue(mDateFormat.format(mCalendar.getTime())+" "+show.getChannel() + " " + show.getStation());            

            
            int duration = Math.round(show.getDuration() / 1000 / 60 + 0.5f);
            mCalendar.setTime(new Date(Math.round((show.getDuration() / 1000 / 60 + 0.5f) / 10) * 10));
            mCalendar.set(GregorianCalendar.HOUR_OF_DAY, duration / 60);
            mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            BText rightAligned = new BText(normal, border_left, top, text_width, 30);
            rightAligned.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            rightAligned.setFont("default-18.font");
            rightAligned.setShadow(true);
            rightAligned.setValue("Duration: "+mTimeFormat.format(mCalendar.getTime()));
     
            top += 20;
            
            BText plainSample2 = new BText(normal, border_left, top, text_width, 30);
            plainSample2.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            plainSample2.setFont("default-18.font");
            plainSample2.setShadow(true);
            plainSample2.setValue("Rated: "+show.getRating());
            
            BText rightAligned2 = new BText(normal, border_left, top, text_width, 30);
            rightAligned2.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            rightAligned2.setFont("default-18.font");
            rightAligned2.setShadow(true);
            rightAligned2.setValue("Video: "+mShow.getQuality());            
            
            top += 20;
            
            BText plainSample3 = new BText(normal, border_left, top, text_width, 30);
            plainSample3.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            plainSample3.setFont("default-18.font");
            plainSample3.setShadow(true);
            plainSample3.setValue(show.getGenre());
            
            top += 30;
            
            //
            // A line of text width a custom color.
            //
            
            statusText = new BText(normal, border_left, top, text_width, 30);
            statusText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            statusText.setFont("default-24-bolditalic.font");
            statusText.setColor(new Color(150, 100, 100));
            statusText.setShadow(true);
            statusText.setValue(show.getStatusString());

            top += 35;
            
            list = new OptionList(this.normal, SAFE_TITLE_H+10, (height-SAFE_TITLE_V)-80, (width - (SAFE_TITLE_H*2))/2, 90, 35);
            list.add("Save to computer");
            list.add("Don't do anything");
            
            setFocusDefault(list);
        }
        
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_SELECT:
              case KEY_RIGHT:
                if (list.getFocus()==0)
                {
                  postEvent(new BEvent.Action(this, "record"));            
                  return true;
                }
                else
                {
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
        
        private void setRecordButtonText()
        {
            Boolean status = new Boolean(mShow.getStatus()==Show.STATUS_PENDING || mShow.getStatus()==Show.STATUS_USER_SELECTED || mShow.getStatus()==Show.STATUS_DOWNLOADING);
            if (status.booleanValue())
                list.set(0, "Don't save to computer");
                //recordButton.setResource(createText("default-24.font", Color.white, "Cancel Download"));
            else
                list.set(0, "Save to computer");
                //recordButton.setResource(createText("default-24.font", Color.white, "Download to PC"));
        }
        
        public boolean handleAction(BView view, Object action) 
        {
            if (action.equals("record")) {
                Boolean status = new Boolean(mShow.getStatus()==Show.STATUS_PENDING || mShow.getStatus()==Show.STATUS_USER_SELECTED || mShow.getStatus()==Show.STATUS_DOWNLOADING);
                if (status.booleanValue())
                {
                    mShow.setStatus(Show.STATUS_USER_CANCELLED);
                    getBApp().play("thumbsdown.snd");
                }
                else    
                {
                    mShow.setStatus(Show.STATUS_USER_SELECTED);
                    getBApp().play("thumbsup.snd");
                }
                
                statusText.setValue(mShow.getStatusString());
                setRecordButtonText();
                
                // TODO Fix this
                boolean found = false;
                ArrayList downloads = ((ToGo)this.getBApp()).togoList.load();
                Iterator recordedIterator = downloads.iterator();
                while (recordedIterator.hasNext())
                {
                    Show downloaded = (Show)recordedIterator.next();
                    
                    if (downloaded.equals(mShow))
                    {
                        downloaded.setStatus(mShow.getStatus());
                        found = true;
                        break;
                    }
                }
                if (!found)
                    downloads.add(mShow);
                ((ToGo)this.getBApp()).togoList.save(downloads);
                
                return true;
            } 
            
            return super.handleAction(view, action);
        }
        
        public Show getShow()
        {
            return mShow;
        }
        
        public String toString() 
        {
            return mShow.getTitle();
        }
        
        public class OptionList extends BList 
        {
            public OptionList(BView parent, int x, int y, int width, int height, int rowHeight)
            {
                super(parent, x, y, width, height, rowHeight);
                
                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index)
            {   
                BText text = new BText(parent, 10, 4, parent.width-40, parent.height - 4);
                text.setShadow(true);
                text.setFlags(RSRC_HALIGN_LEFT);
                text.setValue(get(index).toString());
            }

            public boolean handleKeyPress(int code, long rawcode) 
            {
                switch (code) {
                  case KEY_SELECT:
                    if (list.getFocus()==1)
                      postEvent(new BEvent.Action(this, "pop"));
                    else
                        postEvent(new BEvent.Action(this, "record"));
                    return true;
                }
                return super.handleKeyPress(code, rawcode);
            }
        }
        
        
        private Show mShow;
        
        private BText statusText;
    }    
}