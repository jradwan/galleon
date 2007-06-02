//////////////////////////////////////////////////////////////////////
//
// File: BHighlight.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;

/**
 * A highlight used to decorate a view. A highlight is usually a whispering
 * arrow, a background bar, etc. BHighlight is really two parts - a recipe for
 * creating a highlight and the view that is the actual highlight on screen.<p>
 *
 * The highlight doesn't know anything about the view that it is highlighting,
 * and can theoretically be shared between views. For example, all of the rows
 * in a list might reuse the same set of highlights.<p>
 *
 * Highlights appear either above or below the screen's normal plane. This is
 * accomplished by placing the highlight into the screen's above or below plane.
 * 
 * @author      Adam Doppelt
 */
public class BHighlight implements IBananas
{
    /**
     * The actual highlight on screen.
     */
    private BView view;

    /**
     * The name of the highlight - this will be passed to BSkin when it comes
     * time to create the highlight.
     */
    private String name;

    /**
     * The action that this highlight should trigger. This is only used for
     * highlights which correspond to key codes. For example, a "right"
     * arrow key might trigger a "push" action.
     */
    private Object action;

    /**
     * The coords where the highlight should draw, relative to the view being
     * highlighted.
     */
    private int dx, dy;

    /**
     * If the highlight is capable of stretching (i.e. - a highlight bar), this
     * is the highlight's width. Otherwise this is -1.
     */
    private int width;

    /**
     * If true, this highlight should appear in the above plane of the
     * screen. Otherwise it will appear below. The default is true.
     */
    private boolean isAbove;

    /**
     * One of the H_VIS_XXXX constants - indicates whether or not the
     * highlight should be visible.
     */
    private int visible;

    /**
     * Linked list of highlights.
     * Note: Package access so that BHighlights can put it in a linked list
     */
    BHighlight next;
    
    /**
     * Create a new highlight.
     * 
     * @param name the name of the highlight - this will be passed to BSkin when
     * it comes time to create the highlight
     * @param action the action that this highlight should trigger - only used
     * for highlights which correspond to key codes
     * @param dx the x coord where the highlight should draw, relative to the
     * view being highlighted
     * @param dy the y coord where the highlight should draw, relative to the
     * view being highlighted
     */
    public BHighlight(String name, Object action, int dx, int dy)
    {
        this.name = name.intern();
        this.action = action;
        this.dx = dx;
        this.dy = dy;
        this.width = -1;
        this.isAbove = true;
        this.visible = H_VIS_FOCUS;
    }

    /**
     * Refresh the highlight to make it match the correct appearance. If the
     * highlight should be visible it will be created, shown, and placed in
     * correct location. If the highlight should NOT be visible it will be
     * hidden.
     * 
     * @param h the highlights that contain this highlight
     * @param s the screen containing the highlights
     * @param r the rectangle on screen to be highlighted
     * @param animation if non-null the highlight will be adjusted with this
     * animation
     */
    public void refresh(BHighlights h, BScreen s, BRect r, Resource animation)
    {
        int x = r.x + dx;
        int y = r.y + dy;

        if (h.getLayout().getHighlightIsVisible(visible)) {
            if (view == null) {
                // create the view
                BSkin skin = s.getBApp().getSkin();
                BView parent = isAbove ? s.getAbove() : s.getBelow();

                // skin it (and maybe stretch it)
                if (width == -1) {
                    view = skin.createSkin(parent, name, x, y);
                } else {
                    view = skin.createSkinStretch(parent, name, x, y, width);
                }
            } else {
                // show the existing view
                view.setVisible(true);
                view.setLocation(x, y, animation);  
                if (width != -1 && width != view.getWidth()) {
                    view.setSize(width, view.getHeight(), animation);
                }
            }
        } else if (view != null) {
            // hide it
            view.setVisible(false, animation);
            view.setLocation(x, y, animation);            
        }
    }

    /**
     * Remove the highlight.
     */
    public void remove()
    {
        if (view != null) {
            view.remove();
        }
    }
    
    //
    // accessors
    //

    /**
     * @return the view for the actual highlight
     */
    public BView getView()
    {
    	return view;
    }

    /**
     * Set the view for the highlight.
     */   
    protected void setView(BView view){
        this.view = view;
    }
 
    /**
     * @return the name of the highlight - this will be passed to BSkin when it comes
     * time to create the highlight.
     */
    public String getName()
    {
    	return name;
    }
    
    /**
     * @return The action that this highlight should trigger. This is only used for
     * highlights which correspond to key codes. For example, a "right" arrow key 
     * might trigger a "push" action.
     */
    public Object getAction()
    {
    	return action;
    }
    
    /**
     * Set whether the highlight should draw above or below normal views.
     * 
     * @param isAbove if true, the highlight will be placed in the screen's above
     * layer. Otherwise it'll go into below.
     */
    public void setAbove(boolean isAbove)
    {
        this.isAbove = isAbove;
    }

    /**
     * Set whether or not the highlight should stretch to a certain width. This
     * is used for stretchable backgrounds.
     *
     * @param width the new width
     */
    public void setStretchWidth(int width)
    {
        this.width = width;
    }

    /**
     * Set whether or not the highlight should be visible when refreshed. The
     * visible param is one of the H_VIS_XXXX constants:
     * 
     * <ul>
     * <li>H_VIS_FOCUS : visible if the highlighted view has focus
     * <li>H_VIS_TRUE : always visible
     * <li>H_VIS_FALSE : never visible
     * </ul>
     * 
     * @param visible the new visibility value
     */
    public void setVisible(int visible)
    {
        this.visible = visible;
    }

    public String toString()
    {
        return "highlight " + name + "," + action;
    }
    /**
     * Get the highlight X coordinate offset
     * 
     * @return x axis offset where the highlight should draw, relative to the view being highlighted.
     */
    protected int getDx() {
    	return dx;
    }
    /**
     * Get the highlight Y coordinate offset
     * 
     * @return y axis offset where the highlight should draw, relative to the view being highlighted.
     */
    protected int getDy() {
    	return dy;
    }
}
