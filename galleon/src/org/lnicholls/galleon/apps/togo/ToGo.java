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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;

import javax.swing.ImageIcon;

import com.tivo.hme.bananas.*;
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
        
        mYellowIcon = getResource("yellowball.gif");
        mYellowExclamationIcon = getResource("yellowball!.gif");
        mWhiteIcon = getResource("whiteball.gif");
        mGreenIcon = getResource("greenball.gif");
        mRedIcon = getResource("redball.gif");
        mBlueIcon = getResource("blueball.gif");
        mEmptyIcon = getResource("empty.gif");        
        
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
            mTitle.setFlags(RSRC_VALIGN_TOP);
            mTitle.setFont("default-48.font");
        }
        
        public void setTitle(String value)
        {
            mTitle.setValue(this.toString());
        }
        
        private BText mTitle;
        
    }
    
    public class ToGoMenuScreen extends DefaultScreen
    {
        private TGList list;
        
        public ToGoMenuScreen(ToGo app)
        {
            super(app);
            setTitle(toString());
            
            list = new TGList(this.normal, SAFE_TITLE_H+10, (height-SAFE_TITLE_V)-290, width - ((SAFE_TITLE_H*2)+32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP,   A_RIGHT+13, A_TOP    - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT+13, A_BOTTOM + 30);
            
            ArrayList recordings = (ArrayList)app.togoList.load();
            Iterator recordedIterator = recordings.iterator();
            while (recordedIterator.hasNext()) {
                Show show = (Show) recordedIterator.next();
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
            public TGList(BView parent, int x, int y, int width, int height, int rowHeight)
            {
                super(parent, x, y, width, height, rowHeight);
                
                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index)
            {   
                BView icon = new BView(parent, 10, 8, 16, 16);
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
                
                BText text = new BText(parent, 30, 4, parent.width-40, parent.height - 4);
                text.setShadow(true);
                text.setFlags(RSRC_HALIGN_LEFT);
                text.setValue(get(index).toString());
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

        
        public ToGoScreen(ToGo app, Show show)
        {
            super(app);
            
            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm");
            mTimeFormat = new SimpleDateFormat();
            mTimeFormat.applyPattern("H:mm");
            mCalendar = new GregorianCalendar();
            mNumberFormat = new DecimalFormat("###,###");            
            
            
            mShow = show;
            setTitle(toString());
            
            int top = SAFE_TITLE_V + 100;
            final int border_left = SAFE_TITLE_H + 32;
            final int text_width = width - ((SAFE_TITLE_H*2)+32);
            
            
            //
            // A wrapped line of text over a darkened area.
            //

            BView bg = new BView (normal, border_left, top, text_width, 60);
            bg.setResource(new Color(25, 25, 50));
            bg.setTransparency(.5f);
            
            BText wrapText = new BText(normal, border_left, top, text_width, 60);
            wrapText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP);
            wrapText.setFont("default-18.font");
            wrapText.setValue(show.getDescription());
            
            top += 65;            
            
            //
            // A line of text left aligned.
            //
            
            BText boldText = new BText(normal, border_left, top, text_width, 30);
            boldText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            boldText.setFont("default-18.font");
            boldText.setShadow(true);
            boldText.setValue(mShow.getEpisode());
     
            //
            // A line of text right aligned.
            //
            
            BText rightAligned = new BText(normal, border_left, top, text_width, 30);
            rightAligned.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            rightAligned.setFont("default-18.font");
            rightAligned.setShadow(true);
            rightAligned.setValue(show.getChannel() + " " + show.getStation());
     
            top += 35;
            
            //
            // A line of text left aligned.
            //
            
            mCalendar.setTime(mShow.getDateRecorded());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            BText plainSample = new BText(normal, border_left, top, text_width, 30);
            plainSample.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            plainSample.setFont("default-18.font");
            plainSample.setShadow(true);
            plainSample.setValue(mDateFormat.format(mCalendar.getTime()));
            
            
            //
            // A line of text right aligned.
            //
            
            mCalendar.setTime(show.getDateRecorded());
            mCalendar.set(GregorianCalendar.MINUTE, (mCalendar.get(GregorianCalendar.MINUTE) * 60
                    + mCalendar.get(GregorianCalendar.SECOND) + 30) / 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            BText rightAligned2 = new BText(normal, border_left, top, text_width, 30);
            rightAligned2.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            rightAligned2.setFont("default-18.font");
            rightAligned2.setShadow(true);
            rightAligned2.setValue(mShow.getQuality());            
            
            top += 35;
            
            
            int duration = Math.round(show.getDuration() / 1000 / 60 + 0.5f);
            mCalendar.setTime(new Date(Math.round((show.getDuration() / 1000 / 60 + 0.5f) / 10) * 10));
            mCalendar.set(GregorianCalendar.HOUR_OF_DAY, duration / 60);
            mCalendar.set(GregorianCalendar.MINUTE, duration % 60);
            mCalendar.set(GregorianCalendar.SECOND, 0);
            
            BText plainSample2 = new BText(normal, border_left, top, text_width, 30);
            plainSample2.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            plainSample2.setFont("default-18.font");
            plainSample2.setShadow(true);
            plainSample2.setValue(mTimeFormat.format(mCalendar.getTime()));            
            
            //
            // A line of text width a custom color.
            //
            
            statusText = new BText(normal, border_left, top, text_width, 30);
            statusText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            statusText.setFont("default-18-bolditalic.font");
            statusText.setColor(new Color(150, 100, 100));
            statusText.setShadow(true);
            statusText.setValue(show.getStatusString());

            top += 35;
            
            //
            // a left hanging button
            //
            
            //recordButton = new BButton(normal, SAFE_TITLE_H, top, 300, 30);
            //recordButton.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, null, null, null, H_DOWN, false);
            //recordButton.setResource(createText("default-24.font", Color.white, "Download to PC"));
            //setRecordButtonText();
            
            recordButton = new BButton(normal, SAFE_TITLE_H, top, 300, 30);
            recordButton.setResource(createText("default-24.font", Color.white, "Download to PC"));
            recordButton.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, H_DOWN, true);
            setRecordButtonText();            
                    
            top += 60;            

            //
            // Return to main menu button, set a left arrow highlight to call action
            // "pop" when selected.
            //
            
            BButton button = new BButton(normal, SAFE_TITLE_H, (height-SAFE_TITLE_V)-50, 300, 30);
            button.setResource(createText("default-24.font", Color.white, "Return to ToGo menu"));
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP, null, true);
            setFocusDefault(button);
        }
        
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_SELECT:
                if (recordButton.hasFocus())
                {
                  postEvent(new BEvent.Action(this, "record"));            
                  return true;
                }
                else
                {
                  postEvent(new BEvent.Action(this, "pop"));            
                  return true;
                }
            }
            return super.handleKeyPress(code, rawcode);
        }
        
        private void setRecordButtonText()
        {
            recordButton.clearResource();
            Boolean status = new Boolean(mShow.getStatus()==Show.STATUS_PENDING || mShow.getStatus()==Show.STATUS_USER_SELECTED || mShow.getStatus()==Show.STATUS_DOWNLOADING);
            if (status.booleanValue())
                recordButton.setResource(createText("default-24.font", Color.white, "Cancel Download"));
            else
                recordButton.setResource(createText("default-24.font", Color.white, "Download to PC"));
            recordButton.flush();
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
        
        private Show mShow;
        
        private BText statusText;
        
        private BButton recordButton;
    }    
}