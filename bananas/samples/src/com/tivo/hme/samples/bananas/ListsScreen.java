//////////////////////////////////////////////////////////////////////
//
// File: ListsScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import com.tivo.hme.bananas.*;

/**
 * A Screen to demonstrate a three ways to display lists.
 * 
 * 1) A standard list that has an icon on each row.
 * 2) A list which has an icon that follows the bar around.
 * 3) A list that hangs to the right.
 *
 * @author Carl Haynes
 */
public class ListsScreen extends BananasSampleScreen
{
    /**
     * Constructor 
     */
    public ListsScreen(BApplication app)
    {
        super(app);
        
        // 
        // create a standard list
        //
        
        StandardList standardList = new StandardList(getNormal(), SAFE_TITLE_H, 150, 175, 280, 40);
        String [] s1 = {"This", "is", "a", "standard", "list"};
        standardList.add(s1);
        setFocusDefault(standardList);
        
        // 
        // Create a list that has an icon that folows the bar around
        //
        
        IconList iconList = new IconList(getNormal(), SAFE_TITLE_H+180, 150, getWidth()-((SAFE_TITLE_H*2)+320), 280, 40);
        String[] s2 =  {"This list","has an", "icon", "that", "follows", "the",
                        "bar.", "Also,", "the", "list", "is", "quite", "long",
                        "and", "has", "many", "elements", "that", "flow",
                        "down", "in", "a", "pleasing", "manner."};
        BHighlights h = iconList.getHighlights();
        h.setPageHint(H_PAGEUP,   A_RIGHT+13, A_TOP    - 25);
        h.setPageHint(H_PAGEDOWN, A_RIGHT+13, A_BOTTOM + 30);
        iconList.add(s2);
        
        // 
        // create a list that hangs to the right - add elements one at a time,
        // just for kicks.
        //
        
        RightHangingList customBarList = new RightHangingList(getNormal(), getWidth()-(SAFE_TITLE_H+140), 150, 140, 280, 40);
        customBarList.add("This");
        customBarList.add("list");
        customBarList.add("hangs");
        customBarList.add("right");
    }
  
    /**
     * Title of the screen
     */
    public String toString() 
    {
        return "BList";
    }
    
    /**
     * A simple standard list that displays an icon and a string
     * 
     * @author Carl Haynes
     */
    static class StandardList extends BList 
    {
        /**
         * Constructor
         */
        public StandardList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", H_RIGHT);
        }

        /**
         * Create a row that has an icon and text.
         */
        protected void createRow(BView parent, int index)
        {
            BView icon = new BView(parent, 20, 0, 34, parent.getHeight());
            icon.setResource("star.png");
            BText text = new BText(parent, 60, 0, parent.getWidth()-70, parent.getHeight());
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setShadow(true);
            
            //
            // set the value of the row to be the text that was 
            // passed in through add()
            //
            
            text.setValue(get(index));
        }        
    }
    
    /**
     * This list has an icon that follows the bar to make it appear as if it is
     * a highlight.
     *
     * @author Carl Haynes
     */
    static class IconList extends BList 
    {
        BView iconView;       
        
        /**
         * Constructor
         */
        public IconList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, H_RIGHT);
        }

        /**
         * Create a row that displays the text, if its the first row, create the
         * icon view that will move around with the hilight.
         */
        protected void createRow(BView parent, int index)
        {
            BText text = new BText(parent, 55, 0, parent.getWidth()-40, parent.getHeight());
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index));
            text.setShadow(true);
            
            // if this is the first row and we haven't yet created the icon
            // view, the go ahead and create it
            if (index == 0 && iconView == null) {
                iconView = new BView(this, 30, parent.getY()+3, 26, 40);
                iconView.setResource("musicnote.png");
            }
        }
        
        /**
         * Whenever the focused row changes we get called twice, 
         * once for the row that loses focus and once for the row 
         * that gains focus
         */
        public boolean handleFocus(boolean isGained, BView gained, BView lost) 
        {    
            //
            // We really only care when we gain focus and when we do, we set the
            // icon to be on the row that just gained focus
            //
            // If we gain focus and the previous focus was not this list then we
            // jump the icon directly to the new focused item, otherwise we
            // animate it at the same speed as the bar so that it looks like it
            // is part of the bar.
            //
            
            if (isGained && gained.getParent() == this) {
                if (lost.getParent() == this) {
                    iconView.setLocation(30, gained.getY()+3, this.getResource("*100"));
                } else {
                    //
                    // gaining focus from another widget
                    //
                    
                    iconView.setLocation(30, gained.getY()+3);
                }               
            }
            iconView.setVisible(true);
            
            return super.handleFocus(isGained, gained, lost);
        }
    }
    
    /**
     * A List that hangs right
     * 
     * @author Carl Haynes
     */
    static class RightHangingList extends BList 
    {
        /**
         * Constructor
         */
        public RightHangingList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);

            setBarAndArrows(BAR_DEFAULT, BAR_HANG, H_LEFT, null);
        }

        /** 
         * Create a row that displays text.
         */
        protected void createRow(BView parent, int index)
        {
            BText text = new BText(parent, 20, 0, parent.getWidth()-40, parent.getHeight());
            text.setFlags(RSRC_HALIGN_RIGHT);
            text.setValue(get(index));
            text.setShadow(true);
        }        
    }
}
