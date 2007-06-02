//////////////////////////////////////////////////////////////////////
//
// File: BView.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;

/**
 * A bananas view.
 * 
 * @author      Adam Doppelt
 */
public class BView extends View implements IBananas, IHighlightsLayout
{
    /**
     * If true, this view is capable of receiving focus. This is used by the
     * focus manager when automatically moving the focus in response to an arrow
     * key.
     */
    private boolean isFocusable;

    /**
     * Highlights associated with the view.
     * XXX protected
     */
    private BHighlights highlights;

    /**
     * Constructor.
     */
    public BView(BView parent, int x, int y, int width, int height)
    {
        this(parent, x, y, width, height, true);
    }

    /**
     * Constructor.
     */
    public BView(BView parent, int x, int y, int width, int height, boolean visible)
    {
        super(parent, x, y, width, height, visible);
    }

    /**
     * Private constructor for use by app.normal and app.below.
     */
    BView(View root)
    {
        super(root, 0, 0, root.getWidth(), root.getHeight());
    }

    //
    // accessors
    //

    /**
     * @return true if the view is focusable
     */
    public boolean isFocusable()
    {
    	return isFocusable;
    }
    
    /**
     * Get the app that contains the view.
     */
    public BApplication getBApp()
    {
        return (BApplication)getApp();
    }

    /**
     * Get the screen that contains the view.
     */
    public BScreen getScreen()
    {
        View v = this;
        while (true) {
            if (v instanceof BScreen) {
                return (BScreen)v;
            }
            v = v.getParent();
        }
    }

    /**
     * Returns true if this view has focus.
     */
    public boolean hasFocus()
    {
        return (getScreen().getFocus() == this);
    }

    /**
     * Indicate whether or not this view is capable of receiving focus. This is
     * used by the focus manager when automatically moving the focus in response
     * to an arrow key.
     */
    public void setFocusable(boolean isFocusable)
    {
        this.isFocusable = isFocusable;
    }

    /**
     * Get the highlights.
     */
    public BHighlights getHighlights()
    {
        if (highlights == null) {
            synchronized (this) {
                if (highlights == null) {
                    highlights = new BHighlights(this);
                }
            }
        }
        return highlights;
    }
    
    /**
     * Set the highlights
     */
    public void setHighlights( BHighlights highlights )
    {
    	this.highlights = highlights;
    }

    //
    // geometry
    //

    /**
     * Convert view origin to screen space.
     */
    public BPoint toScreen()
    {
        return toScreen(new BPoint(-getTranslationX(), -getTranslationY()));
    }

    /**
     * Convert point from view space to screen space.
     */
    public BPoint toScreen(BPoint p)
    {
        int x = p.x;
        int y = p.y;        
        View v = this;
        while (!(v instanceof BScreen)) {
            x += v.getTranslationX() + v.getX();
            y += v.getTranslationY() + v.getY();
            v = v.getParent();
        }
        return new BPoint(x, y);
    }

    /**
     * Convert view bounds to screen space;
     */
    public BRect toScreenBounds()
    {
        return toScreenBounds(new BRect(-getTranslationX(), -getTranslationY(), getWidth(), getHeight()));
    }

    /**
     * Convert point from view space to screen space.
     */
    public BRect toScreenBounds(BRect r)
    {
        int x = r.x;
        int y = r.y;        
        View v = this;
        while (!(v instanceof BScreen)) {
            x += v.getTranslationX() + v.getX();
            y += v.getTranslationY() + v.getY();
            v = v.getParent();
        }
        return new BRect(x, y, r.width, r.height);
    }
    
    /**
     * Return true if this view is a direct ancestor of the given child.
     */
    public boolean isAncestorOf(View child)
    {
        while (child != null) {
            if (child == (View)this) {
                return true;
            }
            child = child.getParent();
        }
        return false;
    }

    //
    // events
    //

    /**
     * Handle an event. Return true when the event is consumed.
     */
    public boolean handleEvent(HmeEvent event)
    {
        switch (event.getOpCode()) {
          case BEVT_ACTION: {
              BEvent.Action a = (BEvent.Action) event;
              return handleAction(a.getView(), a.getAction());
          }
          case BEVT_FOCUS: {
              BEvent.Focus f = (BEvent.Focus) event;
              return handleFocus(f.isGained(), f.getGained(), f.getLost());
          }
        }
        return super.handleEvent(event);
    }

    /**
     * Handle an action. Return true when the action is consumed.
     * 
     * @param view the view that generated the action
     * @param action the action string
     */
    public boolean handleAction(BView view, Object action)
    {
        return false;
    }

    /**
     * Handle a change in focus. Return true when the event is consumed.
     *
     * @param isGained if true, this is the "gained focus" event. Otherwise this
     * is the "lost focus" event
     * @param gained the view gaining focus
     * @param lost the view losing focus
     */
    public boolean handleFocus(boolean isGained, BView gained, BView lost)
    {
        return false;
    }

    /**
     * Handle a key press. Return true when the key is consumed. The default
     * behavior will turn arrow key presses into the corresponding actions if
     * highlights are present.
     */
    public boolean handleKeyPress(int code, long rawcode)
    {
        if (highlights != null && code >= KEY_UP && code <= KEY_RIGHT) {
            BHighlight h = highlights.get(ARROW_NAMES[code - KEY_UP]);
            if (h != null && h.getAction() != null) {
                postEvent(new BEvent.Action(this, h.getAction()));
                return true;
            }
        }
        return false;
    }

    protected void toString(StringBuffer buf)
    {
        if (isFocusable) {
            buf.append(",focusable");
        }
        super.toString(buf);
    }

    //
    // IHighlightsLayout
    //

    /**
     * Return the highlight bounds, which in this case is the view bounds.
     */
    public BRect getHighlightBounds()
    {
        return toScreenBounds();
    }
    
    /**
     * Return the focus bounds, which in this case is the view bounds.
     */
    public BRect getFocusBounds()
    {
    	return toScreenBounds();
    }

    /**
     * Return true if a particular highlight should be visible.
     */
    public boolean getHighlightIsVisible(int visible)
    {
        if (this.getVisible() && getTransparency() == 0f) {
            switch (visible) {
              case H_VIS_FOCUS: return hasFocus();
              case H_VIS_TRUE:  return true;
            }
        }
        return false;
    }
}
