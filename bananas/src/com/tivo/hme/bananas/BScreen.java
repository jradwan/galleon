//////////////////////////////////////////////////////////////////////
//
// File: BScreen.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;

/**
 * A screen. An application consists of one or more screens that are pushed and
 * popped. Eacn screen has three planes that can contain views:
 * <ul>
 * <li>above - contains highlights such as arrows and other decorations
 * <li>normal - contains normal widgets such as buttons, text, lists
 * <li>below - contains highlights such as bars and other backgrounds
 * </ul>
 * 
 * @author      Adam Doppelt
 */
public class BScreen extends BView
{
    /**
     * The layer above the widgets. This is a good place to put whispering
     * arrows.
     */
    private BView above;
    
    /**
     * The layer for widgets.
     */
    private BView normal;

    /**
     * The layer below the widgets. This is a good place to put highlight bars.
     */
    private BView below;

    /**
     * A helper for managing focus.
     */
    private BFocusMgr focusMgr;

    /**
     * The view that currently has focus.
     */
    private BView focus;

    /**
     * If set, this view gets focus when the screen is entered. This view will
     * also get focus if the focused object is removed.
     */
    private BView focusDefault;

    /**
     * Constructor.
     *
     * @param app the containing application
     */
    public BScreen(BApplication app)
    {
        super(app.getNormal(), 0, 0, app.getWidth(), app.getHeight(), false);
        below  = new BView(this);
        normal = new BView(this);
        above  = new BView(this);
        focusMgr = new BFocusMgr();
    }

    //
    // focus management
    //

    /**
     * @return the view that currently has focus
     */
    public BView getFocus()
    {
    	return focus;
    }
    
    /**
     * Set the focus to a new view. Both the old and new focus receive a
     * BEvent.Focus event. Note that painting is disabled for the duration of
     * the call to setFocus.
     *
     * @param focus thew new focus
     */
    public void setFocus(BView focus)
    {
        setPainting(false);
        try {
            if (this.focus != focus) {
                BView oldFocus = this.focus;
                this.focus = focus;
                if (oldFocus != null) {
                    oldFocus.postEvent(new BEvent.Focus(false, focus, oldFocus));
                }
                if (focus != null) {
                    focus.postEvent(new BEvent.Focus(true, focus, oldFocus));
                }
            }
        } finally {
            setPainting(true);
        }
    }

    /**
     * Set the default focus. If set, it gets focus when the screen is
     * entered. This view will also get focus if the focused object is removed.
     *
     * @param focusDefault the view that should receive focus by default
     */
    public void setFocusDefault(BView focusDefault)
    {
        this.focusDefault = focusDefault;
    }

    /**
     * Get the default focus.
     */
    public BView getFocusDefault()
    {
        return focusDefault;
    }

    //
    // event handlers
    //

    /**
     * Handle an event. Return true when the event is consumed.
     */
    public boolean handleEvent(HmeEvent event)
    {
        switch (event.getOpCode()) {
          case BEVT_SCREEN_ENTER: {
              BEvent.ScreenEnter e = (BEvent.ScreenEnter)event;
              return handleEnter(e.getArg(), e.isReturn());
          }
          case BEVT_SCREEN_EXIT:
            return handleExit();
        }
        return super.handleEvent(event);
    }

    /**
     * Handle a screen enter. The default behavior will set the focus to the
     * default if this isn't a return. Return true when the event is consumed.
     *
     * @param arg the argument passed to push/pop
     * @param isReturn if true, this screen is being entered because another
     * screen was popped
     */
    public boolean handleEnter(Object arg, boolean isReturn)
    {
        if (!isReturn && focusDefault != null) {
            setFocus(focusDefault);
        }
        return false;
    }

    /**
     * Handle a screen exit. Return true when the event is consumed.
     */
    public boolean handleExit()
    {
        return false;        
    }

    /**
     * Handle an action event. The default handler understands "left", "right",
     * "up" and "down" and will attempt to move the focus in that direction.
     * Return true when the action is consumed.
     * 
     * @param view the view that generated the action
     * @param action the action string
     */
    public boolean handleAction(BView view, Object action)
    {
        for (int i = ARROW_NAMES.length; --i >= 0; ) {
            if (ARROW_NAMES[i].equals(action)) {
                BView newFocus = focusMgr.followArrow(view, KEY_UP + i);
                if (newFocus != null) {
                    setFocus(newFocus);
                    return true;
                }
                break;
            }
        }
        return super.handleAction(view, action);
    }

    /**
     * Handle a focus change. By default, the screen will refresh highlights of
     * the view that is being affected. Return true when the event is consumed.
     *
     * @param isGained if true, this is the "gained focus" event. Otherwise this
     * is the "lost focus" event
     * @param gained the view gaining focus
     * @param lost the view losing focus
     */
    public boolean handleFocus(boolean isGained, BView gained, BView lost)
    {
        BView view = isGained ? gained : lost;
        if (view.getHighlights() != null) {
            view.getHighlights().refresh();
        }
        return true;
    }
    
    // accessors
    
    /**
     * @return The layer above the widgets. This is a good place to put whispering arrows.
     */
    public BView getAbove()
    {
    	return above;
    }
    
    /**
     * @return The layer for widgets.
     */
    public BView getNormal()
    {
    	return  normal;
    }
    
    /**
     * @return The layer below the widgets. This is a good place to put highlight bars.
     */
    public BView getBelow()
    {
    	return below;
    }
}
