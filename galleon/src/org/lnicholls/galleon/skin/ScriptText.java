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


import java.awt.*;import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;import org.mozilla.javascript.*;public class ScriptText extends ScriptableObject {
     // The zero-argument constructor used by Rhino runtime to create instances
     public ScriptText() 
     { 
     }
     
     public void setView(View view)
     {
        mView = view;
     }     
     
     // Method jsConstructor defines the JavaScript constructor
     public void jsConstructor() { }
 
     // The class name is defined by the getClassName method
     public String getClassName() { return "Text"; }
 
     public String jsGet_name() { return mName; }
     public void jsSet_name(String value) { mName = value; }
     public String jsGet_font() { return mFontName; }
     public void jsSet_font(String value) { mFontName = value; }
     public String jsGet_style() { return mStyle; }
     public void jsSet_style(String value) { mStyle = value; }
     public int jsGet_size() { return mSize; }
     public void jsSet_size(int value) { mSize = value+2; }
     public String jsGet_data() { return mData; }
     public void jsSet_data(String value) 
     { 
        if (!mData.equals(value))
        {
            mData = value; 
            Konfabulator.mWindow.getApp().root.setPainting(false);
            try
            {
                // Get the font
                Font font = createFont();
                int lineHeight = FontUtil.getLineHeight(font);
                //System.out.println("lineHeight="+lineHeight);
                int dataWidth = FontUtil.getStringWidth(font,mData);
                //System.out.println("dataWidth="+dataWidth);
                
                int x = mHOffset;
                int y = mVOffset;
                if (mAlignment.equalsIgnoreCase("center"))
                {
                    x = x - dataWidth/2;
                    y = y - lineHeight/2;
                }
                else
                if (mAlignment.equalsIgnoreCase("right"))
                {
                    x = x - dataWidth;
                    y = y - lineHeight/2;
                }
                else
                {
                    y = y - lineHeight/2;
                }
                
                // Get the view
                if (mView==null)
                {
                    //System.out.println("Creating ScriptText: "+x+","+y+","+dataWidth+","+lineHeight);
                    mView = new View(Konfabulator.mWindow,x,y,dataWidth,lineHeight);
                }
                else
                {
                    //System.out.println("Found view for: "+mData);
                    mView.setBounds(x,y,dataWidth,lineHeight);
                }
                
                Resource text = mView.createText(mView.createFont(getFontFile(), getFontStyle(), mSize), getColor(), mData);
                //Resource text = mView.createText(mView.createFont(getFontFile(), getFontStyle(), mSize), Color.white, mData);
                
                mView.setResource(text);
                //mView.setBounds(mHOffset,mVOffset,dataWidth,lineHeight);                    
                float transparency = 1 - (mOpacity/255f);
                mView.setTransparency(transparency);
                //mView.setResource(Color.white);
            }
            catch (Exception ex) 
            {
                ex.printStackTrace();
            }            
            finally
            {
                Konfabulator.mWindow.getApp().root.setPainting(true);
            }
        }            
     }     
     
     public String jsGet_color() { return mColor; }
     public void jsSet_color(String value) { mColor = value; }
     public String jsGet_alignment() { return mAlignment; }
     public void jsSet_alignment(String value) { mAlignment = value; }
     public int jsGet_hOffset() { return mHOffset; }
        public void jsSet_hOffset(int value) 
        { 
            if (mHOffset!=value)
            {
                mHOffset = value; 
                if (mView!=null)
                {
                    Font font = createFont();
                    int lineHeight = FontUtil.getLineHeight(font);
                    int dataWidth = FontUtil.getStringWidth(font,mData);
                    int x = mHOffset;
                    int y = mVOffset;
                    if (mAlignment.equalsIgnoreCase("center"))
                    {
                        x = x - dataWidth/2;
                        y = y - lineHeight/2;
                    }            
            
                    mView.setBounds(x,mView.y,mView.width,mView.height);
                }
            }                
        }            
        public int jsGet_vOffset() { return mVOffset; }
        public void jsSet_vOffset(int value) 
        { 
            if (mVOffset!=value)
            {
                mVOffset = value; 
                if (mView!=null)
                {
                    Font font = createFont();
                    int lineHeight = FontUtil.getLineHeight(font);
                    int dataWidth = FontUtil.getStringWidth(font,mData);
                    int x = mHOffset;
                    int y = mVOffset;
                    if (mAlignment.equalsIgnoreCase("center"))
                    {
                        x = x - dataWidth/2;
                        y = y - lineHeight/2;
                    }                        
                    mView.setBounds(mView.x,y,mView.width,mView.height);
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

     public int jsGet_zOrder() { return mZOrder; }
     public void jsSet_zOrder(int value) { mZOrder = value; }
     
     private Font createFont()
     {
        InputStream in = Konfabulator.class.getClassLoader().getResourceAsStream("org/lnicholls/hme/konfabulator/"+getFontFile());        try         {
            Font originalFont = Font.createFont(Font.TRUETYPE_FONT, in);            return originalFont.deriveFont(getFontStyle(), mSize);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                in.close();
            } catch (IOException e) {
            }
        }                    return null;                         
     }
     
     private int getFontStyle()
     {
        if (mStyle.equalsIgnoreCase("plain"))
            return Font.PLAIN;
        else
        if (mStyle.equalsIgnoreCase("bold"))
            return Font.BOLD;
        else                
        if (mStyle.equalsIgnoreCase("italic"))
            return Font.ITALIC;
        return Font.PLAIN;
     }
     
     private String getFontFile()
     {
        if (mFontName.equalsIgnoreCase("helvetica"))
            return "default.ttf";
        return "default.ttf";    
     }
     
     private Color getColor()
     {
        try
        {
            return Color.decode(mColor);    
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }    
        return Color.white;
     }
     
     public String toString() { return mName; }
 
     private String mName = "";
     private String mData = "";
     private String mColor = "#000000";
     private String mAlignment = "center";
     private String mFontName = "helvetica";
     private String mStyle = "plain";
     private int mSize = 10;
     private int mHOffset = 0;
     private int mVOffset = 0;
     private int mOpacity = 255;
     private int mZOrder = 0;
     
     private View mView;
}