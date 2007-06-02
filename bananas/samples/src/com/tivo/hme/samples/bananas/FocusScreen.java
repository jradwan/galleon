//////////////////////////////////////////////////////////////////////
//
// File: FocusScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * A Screen to demonstrate the way focus is handled. 
 * 
 * Included is a list and several buttons of various sizes placed on the screen
 * to demonstrate the way in which focus is handled.
 *
 * @author Carl Haynes
 */
public class FocusScreen extends BananasSampleScreen
{
    /**
     * Constructor
     */
    public FocusScreen(BApplication app)
    {
        super(app);

        //
        // set a description paragraph of the screen
        //
        
        BText desc = new BText(getNormal(), SAFE_TITLE_H+32, SAFE_TITLE_V+80, getWidth()-((SAFE_TITLE_H*2)+32), 80);
        desc.setVisible(true);
        desc.setValue("Focus management is based on location and handled automatically when highlights are set.");
        desc.setFont("default-24.font");
        desc.setFlags(RSRC_TEXT_WRAP | RSRC_HALIGN_LEFT);
        
        final int top = 225;
        
        //
        // Create a simple list
        //
        
        FocusList list  = new FocusList(getNormal(), SAFE_TITLE_H, top, 150, 200, 40);
        list.add("This");
        list.add("is");
        list.add("a");
        list.add("list");
        setFocusDefault(list);
          
        //
        // top left button
        //
        
        BButton buttonTL = new BButton(getNormal(), SAFE_TITLE_H+175, top, 150, 30);
        buttonTL.setResource(createText("default-24.font", Color.white, "Button 1"));
        buttonTL.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, H_RIGHT, null, H_DOWN, false);
        
        //
        // Top right button
        //
        
        BButton buttonTR = new BButton(getNormal(), (getWidth()-SAFE_TITLE_H) - 150, top+25, 150, 30);
        buttonTR.setResource(createText("default-24.font", Color.white, "Button 2"));
        buttonTR.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, null, null, H_DOWN, false);
       
        //
        // middle left button
        //
        
        BButton buttonML = new BButton(getNormal(), SAFE_TITLE_H+175, top+50, 150, 30);
        buttonML.setResource(createText("default-24.font", Color.white, "Button 3"));
        buttonML.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, H_RIGHT, H_UP, H_DOWN, false);
        
        //
        // a long button
        //
        
        BButton buttonLong = new BButton(getNormal(), SAFE_TITLE_H+175, top+100, getWidth()-((SAFE_TITLE_H*2)+175), 30);
        buttonLong.setResource(createText("default-24.font", Color.white, "Button 4"));
        buttonLong.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, null, H_UP, H_DOWN, false);

        //
        // bottom left button
        //
        
        BButton buttonBL = new BButton(getNormal(), SAFE_TITLE_H+175, top+150, 150, 30);
        buttonBL.setResource(createText("default-24.font", Color.white, "Button 5"));
        buttonBL.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, H_RIGHT, H_UP, null, false);        
 
        //
        // bottom right button
        //
        
        BButton buttonBR = new BButton(getNormal(), (getWidth()-SAFE_TITLE_H)-155, top+150, 150, 30);
        buttonBR.setResource(createText("default-24.font", Color.white, "Button 6"));
        buttonBR.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, null, H_UP, null, false);        
    }
     
    /**
     * Title of the screen
     */
    public String toString() 
    {
        return "Focus";
    }
    
    /**
     * A Simple list that just displays the text set for each row
     * 
     * @author Carl Haynes
     */
    static class FocusList extends BList 
    {
        /**
         * Constructor
         */
        public FocusList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", H_RIGHT);
            getHighlights().setWhisperingArrow(H_UP, A_CENTER, A_TOP, H_UP);
        }

        /**
         * Create the row by displaying the text
         */
        protected void createRow(BView parent, int index)
        {
            BText text = new BText(parent, 20, 0, parent.getWidth()-40, parent.getHeight());
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index));
        }        
    }
}
