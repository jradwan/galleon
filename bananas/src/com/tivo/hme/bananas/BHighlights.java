//////////////////////////////////////////////////////////////////////
//
// File: BHighlights.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import java.util.*;
import com.tivo.hme.sdk.*;

/**
 * A set of highlights. The highlights can be associated with a specific view,
 * or shared between multiple views (i.e. - the highlights for the rows in a
 * list widget).
 * 
 * @author      Adam Doppelt
 */
public class BHighlights implements IBananas, IHmeProtocol
{
    /**
     * The list of highlights.
     */
    private BHighlight head;

    /**
     * The highlights layout.
     */
    private IHighlightsLayout layout;

    /**
     * Create a new set of highlights, using the given layout.
     *
     * @param layout a description for how the highlights should be laid out
     * refreshed
     */
    public BHighlights(IHighlightsLayout layout)
    {
        this.layout = layout;
    }

    /**
     * @return The layout manager for the highlights
     */
    public IHighlightsLayout getLayout()
    {
    	return layout;
    }
    
    /**
     * Adds a highlight. If a highlight with the same name already exists, it is
     * removed.
     * 
     * @param h the highlight to set
     */
    public void set(BHighlight h)
    {
        remove(h.getName());
        h.next = head;
        head = h;
    }
    
    /**
     * Get a highlight.
     * 
     * @param name the name of the highlight to access
     */
    public BHighlight get(String name)
    {
        for (BHighlight h = head; h != null; h = h.next) {
            if (h.getName() == name) {
                return h;
            }
        }

        // try again interned
        name = name.intern();
        for (BHighlight h = head; h != null; h = h.next) {
            if (h.getName() == name) {
                return h;
            }
        }
        return null;
    }

    /**
     * Remove a highlight.
     * 
     * @param name the name of the highlights to remove
     */
    public void remove(String name)
    {
        name = name.intern();
        for (BHighlight h = head, prev = null; h != null; prev = h, h = h.next) {
            if (h.getName() == name) {
                if (prev != null) {
                    prev.next = h.next;
                } else {
                    head = h.next;
                }
                h.remove();
                break;
            }
        }
    }

    /**
     * Get an enumeration of all the highlights.
     */
    public Enumeration elements()
    {
        return new Enum(head);
    }

    /**
     * Set whether or not the highlight should be visible when refreshed. The
     * visible param is one of the H_VIS_XXXX constants:
     * 
     * <ul>
     * <li>H_VIS_FOCUS : visible if this view has focus
     * <li>H_VIS_TRUE : always visible
     * <li>H_VIS_FALSE : never visible
     * </ul>
     *
     * @param name the name of the highlight to update
     * @param visible the new visibility value
     */
    public void setVisible(String name, int visible)
    {
        BHighlight h = get(name);
        if (h != null) {
            h.setVisible(visible);
            refresh();
        }
    }

    /**
     * Refresh all highlights to reflect the current state of affairs, without
     * animation.
     */
    public void refresh()
    {
        refresh(null);
    }

    /**
     * Refresh all highlights to reflect the current state of affairs.
     * 
     * @param animation if non-null, refresh using this animation
     */
    public void refresh(Resource animation)    
    {
        if (head == null) {
            return;
        }

        BScreen screen = layout.getScreen();
        BRect rect = getRect();
                for (BHighlight h = head; h != null; h = h.next) {
            h.refresh(this, screen, rect, animation);
        }
    }

    //
    // highlight helpers
    //

    /**
     * Set a whispering arrow. The arrow will be placed at the given
     * coordinates and will use the given action.
     * 
     * @param name one of H_UP, H_DOWN, H_LEFT, H_RIGHT
     * @param anchorX one of the A_XXXX constants, plus an optional delta
     * @param anchorY one of the A_XXXX constants, plus an optional delta
     * @param action the action to fire when that key is pressed
     */
    public void setWhisperingArrow(String name, int anchorX, int anchorY, Object action)
    {
        if (action == null) {
            throw new RuntimeException("arrow action cannot be null");
        }

        BPoint p = getBApp().getSkin().getAnchor(name, getRect(), anchorX, anchorY);
        set(new BHighlight(name, action, p.x, p.y));
    }

    /**
     * Set a bar.
     *
     * @param dx x offset, relative to 0,0 on the view
     * @param width bar width
     */
    public void setBar(int dx, int width)
    {
        BSkin.Element e = getBApp().getSkin().get(H_BAR);
        int y = (getRect().height - e.getHeight()) / 2;
        
        BHighlight h = new BHighlight(e.getName(), null, dx, y);
        h.setAbove(false);
        h.setStretchWidth(width);

        set(h);
    }

    /**
     * Set a pageup/down hint. The hint will be placed at the given coordinates.
     * 
     * @param name one of H_PAGEUP, H_PAGEDOWN
     * @param anchorX one of the A_XXXX constants, plus an optional delta
     * @param anchorY one of the A_XXXX constants, plus an optional delta
     */
    public void setPageHint(String name, int anchorX, int anchorY)
    {
        BPoint p = getBApp().getSkin().getAnchor(name, getRect(), anchorX, anchorY);

        BHighlight h = new BHighlight(name, null, p.x, p.y);
        h.setVisible(H_VIS_TRUE);
        set(h);
    }

    /**
     * Set the transparency for the entire set of highlights.
     */
    public void setTransparency(float transparency, Resource anim)
    {
        for (BHighlight h = head; h != null; h = h.next) {
            if (h.getView() != null) {
                h.getView().setTransparency(transparency, anim);
            }
        }
    }

    /**
     * Set the background bar and (optional) whispering left/right arrows.
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
     * @param inside if true, the arrows will be placed inside the
     * button. Otherwise they will be placed outside.
     */
    public void setBarAndArrows(int bar_left, int bar_right,
                                Object action_left, Object action_right,
                                boolean inside)
    {
        //
        // bar_left
        // |
        // |      whi_left
        // | padh  |
        // 
        //   ============================
        //  ==        
        // ==     /
        // ==     \
        //  ==     
        //   ============================
        // 
        //

        BApplication app = getBApp();
        BScreen screen = layout.getScreen();
        BRect rect = getRect();

        BSkin.Element e = app.getSkin().get(H_BAR);
        int cap = e.getInt("cap", 30);
        int padh = e.getInt("padh", 10);
        int w = app.getSkin().get(H_LEFT).getWidth();
        if ((w & 1) == 1) {
            ++w;
        }

        int originx = -rect.x;

        //
        // left
        //

        int whi_left;
        switch (bar_left) {
          case BAR_HANG:
            bar_left = originx - cap;
            whi_left = originx + SAFE_TITLE_H;
            break;
          default:
            whi_left = bar_left + (inside ? padh : -w);
            break;
        }
    
        //
        // right
        //
        
        int whi_right;
        switch (bar_right) {
          case BAR_HANG:
            bar_right = screen.getWidth() + originx;
            whi_right = bar_right - SAFE_TITLE_H - w;
            bar_right += cap;
            break;
          default:
            if (bar_right <= 0) {
                bar_right += rect.width;
            }
            whi_right = bar_right;
            if (inside) {
                whi_right -= padh + w;
            }
            break;
        }

        //
        // now set the highlights
        //
        
        setBar(bar_left, bar_right - bar_left);
        if (action_left != null) {
            setWhisperingArrow(H_LEFT,  A_LEFT + whi_left, 0, action_left);
        } else {
            remove(H_LEFT);
        }
        if (action_right != null) {
            setWhisperingArrow(H_RIGHT, A_LEFT + whi_right, 0, action_right);
        } else {
            remove(H_RIGHT);
        }
    }

    //
    // internal shortcut helpers
    //
    
    protected BApplication getBApp()
    {
        return layout.getScreen().getBApp();
    }

    protected BRect getRect()
    {
        return layout.getHighlightBounds();
    }
    
    //
    // Enum
    //
    
    static class Enum implements Enumeration
    {
        BHighlight list;
        Enum(BHighlight head)
        {
            list = head;
        }
        public boolean hasMoreElements()
        {
            return list != null;
        }
        public Object nextElement()
        {
            BHighlight h = list;
            if (h == null) {
                throw new NoSuchElementException();
            }
            list = list.next;
            return h;
        }
    }
}
