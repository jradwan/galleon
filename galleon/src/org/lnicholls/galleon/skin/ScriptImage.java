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


import java.awt.*;import java.awt.Color;
import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;import org.mozilla.javascript.*;public class ScriptImage extends ScriptableObject {

    final static Button mediaTrackerComp = new Button();
    
     private static Runtime runtime = Runtime.getRuntime();
     
     private static Hashtable ImageCache = new Hashtable();
     
     // The zero-argument constructor used by Rhino runtime to create instances
     public ScriptImage() 
     { 
     }
     
     public void setView(View view)
     {
        mView = view;
     }
     
     // Method jsConstructor defines the JavaScript constructor
     public void jsConstructor() { }
 
     // The class name is defined by the getClassName method
     public String getClassName() { return "Image"; }
 
     public String jsGet_src() { return mSrc; }
     public void jsSet_src(String src) { 
        if (!mSrc.equals(src)) 
        {
            mSrc = src; 
            Konfabulator.mWindow.getApp().getRoot().setPainting(false);
            try
            {
                //logMemory();
                
                // Get the image
                Image cachedImage = (Image)ImageCache.get(mSrc);
                if (cachedImage==null)
                {
                    Image orignalImage = (Image)Konfabulator.mWidgetLoader.getResource(mSrc);
                    if (orignalImage!=null)
                    {
                        cachedImage = orignalImage.getScaledInstance(orignalImage.getWidth(null), orignalImage.getHeight(null), Image.SCALE_SMOOTH);
                        try {
                            MediaTracker mt = new MediaTracker(mediaTrackerComp);
                            mt.addImage(cachedImage, 0);
                            mt.waitForAll();
                        } catch(InterruptedException e) {
                        }
                        ImageCache.put(mSrc,cachedImage);
                        //image.flush();
                        //image = null;
                    }    
                    else
                        return;
                }
                else
                {
                    //System.out.println("Found cached image for: "+mSrc);
                }
                
                // Get the view
                if (mView==null)
                {
                    //System.out.println("Creating ImageWrapper: "+mHOffset+","+mVOffset+","+cachedImage.getWidth(null)+","+cachedImage.getHeight(null)+" ("+mSrc+")");
                    mView = new View(Konfabulator.mWindow,mHOffset,mVOffset,cachedImage.getWidth(null),cachedImage.getHeight(null));
                }
                else
                {
                    //System.out.println("Found view for: "+mSrc);
                }
                
                mView.setResource(cachedImage);
                mView.setBounds(mHOffset,mVOffset,cachedImage.getWidth(null),cachedImage.getHeight(null));
                float transparency = 1 - (mOpacity/255f);
                mView.setTransparency(transparency);
            }
            catch (Exception ex) 
            {
                ex.printStackTrace();
            }            
            finally
            {
                Konfabulator.mWindow.getApp().getRoot().setPainting(true);
            }
        }            
     }
     
    public int jsGet_hOffset() { return mHOffset; }
    public void jsSet_hOffset(int value) 
    { 
        if (mHOffset != value)
        { 
            mHOffset = value; 
            if (mView!=null)
            {
                //System.out.println("jsSet_hOffset:"+value+","+mVOffset+","+mView.width+","+mView.height+" ("+mSrc+")");
                Resource anim = mView.getResource("*100");
                mView.setBounds(value,mVOffset,mView.getWidth(),mView.getHeight());//,anim);
            }
        }            
    }            
    public int jsGet_vOffset() { return mVOffset; }
    public void jsSet_vOffset(int value) 
    { 
        if (mVOffset != value)
        { 
            mVOffset = value; 
            if (mView!=null)
            {
                //System.out.println("jsSet_vOffset:"+mHOffset+","+value+","+mView.width+","+mView.height+" ("+mSrc+")");
                mView.setBounds(mHOffset,value,mView.getWidth(),mView.getHeight());
            }        
        }            
    }
    public int jsGet_opacity() { return mOpacity; }
    public void jsSet_opacity(int value) 
    { 
        if (mOpacity != value)
        { 
            mOpacity = value; 
            if (mView!=null)
            {
                float transparency = 1 - (value/255f);
                if (transparency<1)
                {
                    mView.setVisible(true);
                    mView.setTransparency(transparency);
                }   
                else 
                    mView.setVisible(false);
            }
        }                    
    }
    public String jsGet_onMouseUp() { return mOnMouseUp; }
    public void jsSet_onMouseUp(String value) { mOnMouseUp = value; }                        
     
    private static void logMemory() {
        System.out.println("Max Memory: " + runtime.maxMemory());
        System.out.println("Total Memory: " + runtime.totalMemory());
        System.out.println("Free Memory: " + runtime.freeMemory());
    }     
 
     private View mView;
     private String mSrc = "";
     private int mHOffset = 0;
     private int mVOffset = 0;
     private int mOpacity = 255;
     private String mOnMouseUp = "";
 }