package org.lnicholls.galleon.skin;

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
import java.io.*;
import java.net.*;
import java.util.*;import java.awt.image.*;

import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;

/**
 * A konfabulator widget interpreter 
 *
 * @author      Leon Nicholls
 */
public class Konfabulator extends Application
{
    public static WidgetLoader mWidgetLoader;    public static View mWindow;
    View mContent;    Widget mWidget;
    
    /**
     * Create the app.
     */
    public void init(Context context)
    {        mWidgetLoader = ((KonfabulatorFactory)getContext().getFactory()).getWidgetLoader();
        //root.setResource("myloop.mpg");                mContent = new View(getRoot(), SAFE_ACTION_H / 2, SAFE_ACTION_V / 2,
                getRoot().getWidth() - SAFE_ACTION_H,
                getRoot().getHeight() - SAFE_ACTION_V);                mWidget = new Widget(mWidgetLoader);        mWindow = mWidget.getWindow(mContent);                           
    }        public void close()    {
        if (mWidget!=null)
            mWidget.close();    }
        /**
     * Handle events from the mp3 stream.
     */
    public boolean handleEvent(HmeEvent event)
    {
	switch (event.getOpCode()) {
          case EVT_KEY: {
              //if (title.handleEvent(event))              break;            }
          //
          // The stream will send resouce info events while it plays. The SDK
          // will automatically generate a synthetic RSRC_STATUS event whenever
          // the status of the stream changes.
          //
          // If the track finishes, start playing the next one.
          //
          
	  case StreamResource.EVT_RSRC_INFO: {
	      HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo)event;	      if (info.getStatus() == RSRC_STATUS_PLAYING) {
	        String pos = (String)info.getMap().get("pos");	        if (pos!=null)
	        {	            try
                    {	                StringTokenizer tokenizer = new StringTokenizer(pos,"/");
	                if (tokenizer.countTokens()==2)
	                {	                    String current = tokenizer.nextToken();
	                    String total = tokenizer.nextToken();	                    	                    int value = (int) Math.round(Float.parseFloat(current)/Integer.parseInt(total)*100);
	                    //positionControl.setPosition(value);	                    //updateTime(Integer.parseInt(current)/1000);
                        }                    }
	            catch (Exception ex) {}                        
	        }	        String bitrate = (String)info.getMap().get("bitrate");
	        if (bitrate!=null)	        {
	            try
	            {	                int value = (int) Math.round(Float.parseFloat(bitrate)/1024);
	                String newValue = Integer.toString(value);
	                if (value<100)
	                    newValue = " " + newValue;	                //bitRate.setText(newValue);	            }
	            catch (Exception ex) {}
	        }    	      }          
	      return true;          }	  case StreamResource.EVT_RSRC_STATUS: {
	      HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo)event;	      if (info.getStatus()>= RSRC_STATUS_CLOSED) {
                  // the track finished - what next?
                  
	      }
	      return true;	  }
	}
	return super.handleEvent(event);
    }    
    /**
     * All events received by the app are sent through dispatchEvent. This is a
     * convenient place to listen for ALL key presses since they hit this method
     * regardless of which view has the focus.
     */
    protected void dispatchEvent(HmeEvent event)
    {
        //System.out.println(event);        switch (event.getOpCode()) {
          case EVT_KEY:
              HmeEvent.Key e = (HmeEvent.Key)event;
              if (e.getAction()==KEY_PRESS)              {
                switch (e.getCode()){
                  case KEY_PAUSE:                        mWidget.pause();
                    break;                  case KEY_PLAY:                        mWidget.play();
                    break;                                    case KEY_LEFT:                        mWidget.left(5);
                    break;
                  case KEY_RIGHT:                        mWidget.right(5);
                    break;                                                                              case KEY_CHANNELDOWN:                        mWidget.resetScore();
                    break;                                      case KEY_SLOW:                        mWidget.start();
                    break;                                      case KEY_SELECT:                        mWidget.start();
                    break;                                                                 
                }               }
              else                           if (e.getAction()==KEY_RELEASE)              {                switch (e.getCode()){                  case KEY_PAUSE:
                    break;                  case KEY_PLAY:
                    break;                                  
                  case KEY_CHANNELUP:
                    break;                                      case KEY_CHANNELDOWN:
                    break;                    
                  case KEY_SLOW:
                    break;                                        
                  case KEY_ENTER:                        setActive(false);
                    break;                                                             }               }                            else                           if (e.getAction()==KEY_REPEAT)              {                switch (e.getCode()){                  case KEY_LEFT:                        mWidget.left(10);
                    break;
                  case KEY_RIGHT:                        mWidget.right(10);
                    break;                                                                            }               }                 
            break;
        }
        super.dispatchEvent(event);
    }

    public static class KonfabulatorFactory extends Factory
    {
        WidgetLoader widgetLoader = null;

        /**
         * Create the factory - scan the folder.
         */
	protected void init(ArgumentList args)
	{
            try {
                args.checkForIllegalFlags();
                if (args.getRemainingCount() != 1) {
                    usage();
                }
            } catch (ArgumentList.BadArgumentException e) {
                usage();
            }

            String file = args.shift();
            if (!new File(file).exists()) {
                System.out.println("Widget not found: " + file);
                usage();
            }
	                widgetLoader = new WidgetLoader(file);
	}		public WidgetLoader getWidgetLoader() { return widgetLoader; }

        /**
         * Print usage and exit.
         */
        void usage()
        {
            System.err.println("Usage: Konfabulator widget");
            System.err.println("For example 'Konfabulator Satsuki.widget' ");
            System.exit(1);
        }
    }}
