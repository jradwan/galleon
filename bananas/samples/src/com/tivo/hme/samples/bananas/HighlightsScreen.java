//////////////////////////////////////////////////////////////////////
//
// File: HighlightsScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * A Screen to demonstrate how items can share a set of highlights.  This is the
 * way a list works.
 * 
 * We create a custom IHighlightsLayout implements to control where the
 * highlights are set.
 *
 * @author Carl Haynes
 */
public class HighlightsScreen extends BananasSampleScreen
{
    BHighlights highlights;
    
    BText items[] = new BText[6];
    int focused;
    
    /**
     * Constructor
     */
    public HighlightsScreen(BApplication app)
    {
        super(app);
        
        final int top = 145;
        final int border_left = SAFE_TITLE_H+20;
        
        //
        // Create 6 items, slightly offset from each other, we will move the
        // highlights between these items.
        //
        
        for (int i = 0 ; i < 3 ; i ++) {
            items[i] = new BText(this.getNormal(), border_left + (i*40), top + (i*40), 200, 40);
            items[i].setValue("item " + (i+1));
            items[i].setFlags(RSRC_HALIGN_LEFT);
            items[i].setFocusable(true);
      
            
            items[i+3] = new BText(this.getNormal(), border_left + (i*40), (top+160) + (i*40), 200, 40);
            items[i+3].setValue("item " + (i+4));
            items[i+3].setFlags(RSRC_HALIGN_LEFT);
            items[i+3].setFocusable(true);           
        }
        
        //
        // Draw some text in middle of the items .
        //
        
        BText txt = new BText(getNormal(), SAFE_TITLE_H+20, top+120, getWidth()-((SAFE_TITLE_H*2+20)), 30);
        txt.setValue("Highlights are shared.");
        txt.setFont("default-24-bolditalic.font");
        txt.setColor(new Color(150, 150, 0));
        txt.setShadow(true);
        txt.setFlags(RSRC_HALIGN_LEFT);
              
        //
        // Setup the highlights.
        //

        // Set the focus to the first item - since our highlights object depends
        // on focus being set. Otherwise we'd use setFocusDefault.

        setFocus(items[0]); 

        highlights = new BHighlights(new Layout());
        highlights.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, true);
        highlights.refresh(getResource("*500"));
    }

    /**
     * Handle up and down actions by setting the focus to a new item and
     * refreshing the highlights, which will cause the highlights to move to the
     * newly focused item.
     */
    public boolean handleAction(BView view, Object action) 
    {
        if (action.equals(H_DOWN)) {
            if (++focused > items.length-1) {
                focused = 5;
            }
        } else if (action.equals(H_UP)) {
            if (--focused < 0) {
                focused = 0;
            }
        } else {
            return super.handleAction(view, action);
        }
 
        setFocus(items[focused]);
        highlights.refresh(getResource("*300"));
        return true;
    }

    /**
     * Key presses are handled by posting an event that will get handled by
     * handleAction().
     */
    public boolean handleKeyPress(int code, long rawcode)
    {
        Object action = null;
        switch (code) {
          case KEY_UP:    action = H_UP ;  break;
          case KEY_DOWN:  action = H_DOWN; break;
          case KEY_LEFT:  action = "pop";  break;
        }
        if (action != null) {
            postEvent(new BEvent.Action(this, action));
            return true;
        }
        return false;
    }
    
    /**
     * Title of screen.
     */
    public String toString() 
    {
        return "Shared Highlights";
    }
        
    //
    // custom highlights layout - highlights follow the focus
    //

    class Layout implements IHighlightsLayout
    {
        public BScreen getScreen()
        {
            return HighlightsScreen.this;
        }
        
        public BRect getHighlightBounds()
        {
            return getFocus().toScreenBounds();
        }
        
        public boolean getHighlightIsVisible(int visible)
        {
            return getFocus().getHighlightIsVisible(visible);
        }
    }
}
