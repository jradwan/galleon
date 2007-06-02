//////////////////////////////////////////////////////////////////////
//
// File: BButton.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * A button widget. The button must be populated with other views in order to
 * customize the appearance.
 *
 * @author      Adam Doppelt
 */
public class BButton extends BView
{
    /**
     * Creates a new BButton instance.
     *
     * @param parent parent
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     */
    public BButton(BView parent, int x, int y, int width, int height)
    {
        this(parent, x, y, width, height, true);
    }
    
    /**
     * Creates a new BButton instance.
     *
     * @param parent parent
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     * @param visible if true, make the view visibile
     */
    public BButton(BView parent, int x, int y, int width, int height, boolean visible)
    {
	super(parent, x, y, width, height, visible);
        setFocusable(true);
    }

    /**
     * Fill the button with a bar and arrows.
     *
     * @param action_left if not null, the button will contain a whispering left
     * arrow that fires this action.
     * @param action_right if not null, the button will contain a whispering
     * right arrow that fires this action.
     */
    public void setBarAndArrows(Object action_left, Object action_right)
    {
        setBarAndArrows(action_left, action_right, true);
    }

    /**
     * Fill the button with a bar and arrows.
     *
     * @param action_left if not null, the button will contain a whispering left
     * arrow that fires this action.
     * @param action_right if not null, the button will contain a whispering
     * right arrow that fires this action.
     * @param inside if true, the arrows will be placed inside the
     * button. Otherwise they will be placed outside.
     */
    public void setBarAndArrows(Object action_left, Object action_right, boolean inside)
    {
        setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT,
                        action_left, action_right,
                        null, null, inside);
    }
    
    /**
     * Fill the button with a bar and arrows.
     *
     * @param action_left if not null, the button will contain a whispering left
     * arrow that fires this action.
     * @param action_right if not null, the button will contain a whispering
     * right arrow that fires this action.
     * @param action_up if not null, the button will contain a whispering up
     * arrow that fires this action.
     * @param action_down if not null, the button will contain a whispering down
     * arrow that fires this action.
     * @param inside if true, the arrows will be placed inside the
     * button. Otherwise they will be placed outside.
     */
    public void setBarAndArrows(Object action_left, Object action_right,
                                Object action_up, Object action_down,
                                boolean inside)
    {
        setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT,
                        action_left, action_right,
                        action_up, action_down, inside);
    }

    /**
     * Fill the button with a bar and arrows.
     *
     * @param bar_left If BAR_HANG, the bar extends off the side of the
     * screen. If BAR_DEFAULT, the bar touches the edge of the button. Otherwise
     * this is an offset from 0.
     * @param bar_right If BAR_HANG, the bar extends off the side of the
     * screen. If BAR_DEFAULT, the bar touches the edge of the button. If
     * negative, this is an offset from the right edge. If positive this is an
     * offset from the left edge.
     * @param action_left if not null, the button will contain a whispering left
     * arrow that fires this action.
     * @param action_right if not null, the button will contain a whispering
     * right arrow that fires this action.
     * @param action_up if not null, the button will contain a whispering up
     * arrow that fires this action.
     * @param action_down if not null, the button will contain a whispering down
     * arrow that fires this action.
     * @param inside if true, the arrows will be placed inside the
     * button. Otherwise they will be placed outside.
     */
    public void setBarAndArrows(int bar_left, int bar_right,
                                Object action_left, Object action_right,
                                Object action_up, Object action_down,
                                boolean inside)
    {
        BHighlights h = getHighlights();
        
        // create bar, left, right highlights
        h.setBarAndArrows(bar_left, bar_right,
                          action_left, action_right,
                          inside);
        // make the bar highlight always visible
        h.setVisible(H_BAR, H_VIS_TRUE);

        //
        // up/down arrows
        //

        BSkin skin = getBApp().getSkin();
        int whi_h = skin.get(H_UP).getHeight();
        int bar_padv = skin.get(H_BAR).getInt("padv", 4);
            
        if (action_up != null) {
            h.setWhisperingArrow(H_UP, A_CENTER,
                                 A_TOP - whi_h - bar_padv, action_up);
        } else {
            h.remove(H_UP);
        }
        
        if (action_down != null) {
            h.setWhisperingArrow(H_DOWN, A_CENTER,
                                 A_TOP + getHeight() + bar_padv, action_down);
        } else {
            h.remove(H_DOWN);
        }
    }
}
