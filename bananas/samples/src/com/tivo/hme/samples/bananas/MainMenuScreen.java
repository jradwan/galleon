//////////////////////////////////////////////////////////////////////
//
// File: MainMenuScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import com.tivo.hme.bananas.*;

/**
 * The MainMenu Screen, this is the first screen that the user will see and
 * presents a list of screens, each demonstrating a feature of the Bananas
 * toolkit.
 *
 * @author Carl Haynes
 */
public class MainMenuScreen extends BananasSampleScreen
{
    SampleList list;   
    
    /**
     * Constructor
     * 
     * 
     * @param parent
     * @param x
     * @param y
     * @param width
     * @param height
     */
    public MainMenuScreen(BApplication app)
    {
        super(app);
        
        //
        // create the list, each row in the list is a screen.
        //
        
        list = new SampleList(this.getNormal(), SAFE_TITLE_H+10, (getHeight()-SAFE_TITLE_V)-290, 300, 280, 35);
        list.add(new ListsScreen(getBApp()));
        list.add(new TextScreen(getBApp()));
        list.add(new ButtonsScreen(getBApp()));
        list.add(new KeyboardScreen(getBApp()));
        list.add(new TransitionsScreen(getBApp()));
        list.add(new FocusScreen(getBApp()));
        list.add(new SoundsScreen(getBApp()));
        list.add(new HighlightsScreen(getBApp()));
        
        //
        // Must set focus to the list so that bar and highlights will show up.
        //
        
        setFocusDefault(list);
    }
    
    /**
     * Hand an action
     */
    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            //
            // Go to the screen currently selected in the list.
            //

            BScreen screen = (BScreen)(list.get(list.getFocus()));
            getBApp().push(screen, TRANSITION_LEFT);            
            return true;
        }        
       return super.handleAction(view, action);
    }
    
    /**
     * Listen for the left key.
     */
    public boolean handleKeyPress(int code, long rawcode) 
    {
        switch (code) {
          case KEY_LEFT:
            //
            // Kill the application.
            //
            
            getBApp().setActive(false);
            return true;
        }
    
        return super.handleKeyPress(code, rawcode);
    }
    
    /**
     * Screen title.
     */
    public String toString() 
    {
        return "Bananas Central";
    }
    
    /**
     * A list which displays the text of the object passed in to each row.  In
     * this particular case each row is a screen and we know that each screen
     * has a toString() method so we use that as the title.
     *
     * @author Carl Haynes
     */
    static class SampleList extends BList 
    {
        /**
         * Constructor
         */
        public SampleList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            
            //
            // Add a bar that will hang off the left edge of the screen and is
            // the width of the list.
            //
            // In addition, add a right arrow highlight with the action of
            // "push".
            //
            
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        /**
         * create a row that displays text
         */
        protected void createRow(BView parent, int index)
        {   
            //
            // the row is a simple text item, aligned left, and is the name of
            // the screen
            //
            
            BText text = new BText(parent, 10, 4, parent.getWidth()-40, parent.getHeight() - 4);
            text.setShadow(true);
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index).toString());
        }

        /**
         * Transform SELECT key into a push action.
         */
        public boolean handleKeyPress(int code, long rawcode) 
        {
            switch (code) {
              case KEY_SELECT:
                //
                // Go to the screen currently selected in the list.
                //
                postEvent(new BEvent.Action(this, "push"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }
}
