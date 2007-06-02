//////////////////////////////////////////////////////////////////////
//
// File: TransitionsScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * A Screen to demonstrate the three screen transitions available in Bananas.
 *
 * @author Carl Haynes
 */
public class TransitionsScreen extends BananasSampleScreen
{
    TransitionList list;
    BackScreen backScreen;
    
    /**
     * Constructor
     */
    public TransitionsScreen(BApplication app)
    {
        super(app);
        
        //
        // Create a description paragraph for our screen.
        //
        
        BText desc = new BText(getNormal(), SAFE_TITLE_H+32, SAFE_TITLE_V+100, getWidth()-((SAFE_TITLE_H*2)+32), 80);
        desc.setVisible(true);
        desc.setValue("There are three types of transitions which can be used between screens: slide left; cross fade; and none. Each of these are demonstrated below.");
        desc.setFont("default-24.font");
        desc.setFlags(RSRC_TEXT_WRAP | RSRC_HALIGN_LEFT);
        
        //
        // Create a list with the three possible transitions.
        //
        
        list = new TransitionList(getNormal(), SAFE_TITLE_H, (getHeight()-SAFE_TITLE_V)-120, 300, 120, 40);
        list.add("TRANSITION_LEFT");
        list.add("TRANSITION_FADE");
        list.add("TRANSITION_NONE");
        
        setFocusDefault(list);
    }
    
    /**
     * 
     */
    public boolean handleAction(BView view, Object action) 
    {
        if (action.equals("push")) {
            //
            // We haven't created the "back" screen, do so now.
            //
            
            if (backScreen == null) {
                backScreen = new BackScreen(getBApp());
            }
            
            //
            // Do transition based on which row was selected.
            //
            
            switch(list.getFocus()) {
              case 0:
                getBApp().push(backScreen, TRANSITION_LEFT);
                return true;
              case 1:
                getBApp().push(backScreen, TRANSITION_FADE);
                return true;
              case 2:
                getBApp().push(backScreen, TRANSITION_NONE);
                return true;
            }
        } 
        
        return super.handleAction(view, action);
    }
    
    /**
     * This is the title that will show up in the main menu.
     */
    public String toString() 
    {
        return "Transitions";
    }
    
    /**
     * A simple list that displays the text set for each row.
     */
    static class TransitionList extends BList 
    {
        /**
         * Constructor
         */
        public TransitionList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            
            //
            // Add a bar that will hang off the left edge of the screen and is
            // the width of the list.
            //
            // In addition, add a right arrow highlight with the action of
            // "push".
            //
            
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", "push");
        }
        
        /** 
         * 
         */
        protected void createRow(BView parent, int index)
        {
            BText text = new BText(parent, 20, 4, parent.getWidth()-40, parent.getHeight() - 4);
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index));       
        }

        /**
         * Treat select like a "push" action on the list row.
         */
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_SELECT:
                postEvent(new BEvent.Action(this, "push"));                
                break;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }
    
    /**
     * A very simple screen that just has one button to allow you to return to
     * the previous screen.
     *
     * @author Carl Haynes
     */
    static class BackScreen extends BScreen 
    {
        BButton button;
        
        /**
         * Constructor
         */
        public BackScreen(BApplication app)
        {
            super(app);
            
            //
            // Set the background color.
            //
            
            getBelow().setResource(new Color(50, 100, 100));
            
            //
            // Create a button.
            //
            
            button = new BButton(getNormal(), SAFE_TITLE_H, (getHeight()-SAFE_TITLE_V)-30, 300, 30);
            button.setResource(createText("default-24.font", Color.white, "Return to transitions menu"));
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
            setFocus(button);            
        }

        //
        // Pop on select.
        //
        
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_SELECT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }
}
