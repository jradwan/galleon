//////////////////////////////////////////////////////////////////////
//
// File: BList.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import java.util.*;
import com.tivo.hme.sdk.*;

/**
 * A list. BList contains a list of elements (Objects) and a list of rows that
 * correspond to those elements (Rows). BList will call createRow() when it
 * needs to populate a row. Subclasses must override createRow.<p>
 *
 * The list scrolls itself using setTranslation and will add/remove rows as
 * necessary. By default, BList will automatically handle KEY_UP, KEY_DOWN,
 * KEY_CHANNELUP, and KEY_CHANNELDOWN.<p>
 *
 * A single row within the list has focus. As the user moves up and down in the
 * list the focus will shift from row to row.<p>
 *
 * BList keeps track of which row is currently displayed at the "top" of the
 * list on screen. As the user scrolls down this value will increase. The list
 * will populate a set of rows around the top. Once views scroll beyond this
 * populated window they will be removed. The refresh() method is responsible
 * for updating the rows.<p>
 *
 * In addition to "top", the list contains a dirty marker which indicates other
 * rows that may need refreshing. For example, when the user scrolls down the
 * dirty value is set to the old top so that refresh() can remove the old rows
 * if necessary.
 *
 * @author      Adam Doppelt
 */
public abstract class BList extends BView
{
    /**
     * The animation used to move the highlight.
     */
    public final static String ANIM = "*100";

    /**
     * Array of elements.
     */
    private Vector elements;

    /**
     * Array of row views.
     */
    private Vector rows;
    
    /**
     * Height of each row.
     */
    private int rowHeight;

    /**
     * Number of visible rows.
     */
    private int nVisibleRows;    

    /**
     * A set of highlights to be shared across all rows.
     */
    private BHighlights rowHighlights;

    /**
     * The currently focused row.
     */
    private int focused;

    /**
     * The row that is currently being drawn at the top of the list.
     */
    private int top;

    /**
     * The dirty value is used to track which portion of the list is in need of
     * updating. It's interpretation is similar to top. Refresh uses the union
     * of the top/dirty windows to determine which rows require refreshing.
     */
    private int dirty;

    /**
     * If true, animate the next refresh.
     */
    private boolean animate;
    
    /**
     * Creates a new BList instance. To avoid drawing partial rows, the list
     * height should be a multiple of the rowHeight.
     *
     * @param parent parent
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     * @param rowHeight the height of each row contained in the list.
     */
    public BList(BView parent, int x, int y, int width, int height, int rowHeight)
    {
        this(parent, x, y, width, height, rowHeight, true);
    }
    
    /**
     * Creates a new BList instance. To avoid drawing partial rows, the list
     * height should be a multiple of the rowHeight.
     *
     * @param parent parent
     * @param x x
     * @param y y
     * @param width width
     * @param height height
     * @param rowHeight the height of each row contained in the list.
     * @param visible if true, make the view visibile
     */
    public BList(BView parent, int x, int y, int width, int height, int rowHeight, boolean visible)
    {
	super(parent, x, y, width, height, visible);

        this.elements = new Vector();
        this.rows = new Vector();
	this.rowHeight = rowHeight;
        this.nVisibleRows = height / rowHeight;
        this.rowHighlights = new BHighlights(new Layout());

        clear();
    }

    //
    // row highlights
    //

    public BHighlights getRowHighlights()
    {
        return rowHighlights;
    }

    public void setBarAndArrows(int bar_left, int bar_right,
                                Object action_left, Object action_right)
    {
        getRowHighlights().setBarAndArrows(bar_left, bar_right,
                                           action_left, action_right, true);
    }

    public int getTop(){
        return top;
    }
    
    public int getNVisibleRows(){
        return nVisibleRows;
    }

    //
    // subclasses should override this
    //

    /**
     * Create a row for the given element. For example, you could create a view
     * with the given parent and then set its resource to be a text resource
     * based on the element at index.
     *
     * @param parent use this as the parent for your new view
     * @param index the index for the row
     * @return the new row
     */
    protected abstract void createRow(BView parent, int index);
    
        
    //
    // accessors
    //

    /**
     * Get the row height.
     * 
     * @return row height
     */
    public int getRowHeight()
    {
        return rowHeight;
    }
    
    /**
     * Get the index of the currently focused row.
     *
     * @return the index of the focused row
     */
    public int getFocus()
    {
        return focused;
    }

    /**
     * Set the focus to a particular row.
     * 
     * @param index the row to focus
     * @param animate If true, animate the list as we move to the new row.
     */
    public void setFocus(int index, boolean animate)
    {
        int size = size();
        if (size > 0) {
            this.animate = animate;
            focused = Math.min(Math.max(index, 0), size - 1);
            getScreen().setFocus(getRow(focused));
            this.animate = false;
        }
    }

    //
    // helpers for updating the display
    //

    /**
     * Set the current top row. This doesn't take effect until the next
     * refresh().
     */
    public void setTop(int ntop)
    {
        setTop(ntop, focused);
    }
    public void setTop(int ntop, int index)
    {
        dirty = top;
        top = ntop;
        focused = Math.min(Math.max(index, 0), size() - 1);
    }
    
    /**
     * If necessary, scrolls the list to show the currently focused row and
     * creates new row views to wrap the elements that are currently visible.
     */
    public void refresh()
    {
        BScreen screen = getScreen();
        Resource anim = animate ? getResource(ANIM) : null;
        animate = false;
        int size = size();
        
        screen.setPainting(false);
        try {
            //
            // 1. update top so that focused is still visible
            //

            // 0 <= top <= size - nVisibleRows
            top = Math.max(Math.min(top, size - nVisibleRows), 0);

            int max = Math.min(top + nVisibleRows, size) - 1;
            if (focused < top) {
                top = Math.max(0, focused);
            } else if (focused > max) {
                int end = Math.min(focused + 1, size);
                top = Math.max(end - nVisibleRows, 0);
            }

            //
            // 2. determine which rows to update by combining dirty and top.
            //

            // popMin <==> popMax will be populated
            int popMin = Math.max(top - nVisibleRows, 0);
            int popMax = Math.min(top + 2 * nVisibleRows, size());
                
            // fixMin <==> fixMax will be "fixed" by either being populated
            // or removed
            
            int fixMin, fixMax;
            if (dirty < top) {
                fixMin = Math.max(dirty - nVisibleRows, 0);
                fixMax = popMax;
            } else {
                fixMin = popMin;
                fixMax = Math.min(dirty + 2 * nVisibleRows, size());
            }
            dirty = top;

            //
            // 3. fix rows
            //

            for (int index = fixMin; index < fixMax; ++index) {
                if (index < popMin || index >= popMax) {
                    BView v = (BView)rows.elementAt(index);
                    if (v != null) {
                        v.remove();
                        rows.setElementAt(null, index);
                    }
                } else {
                    getRow(index);
                }
            }

            //
            // 4. move to the new top.
            //
            // Note : it is very important to translate the list before updating
            // the highlights, because otherwise the highlights will appear in
            // the wrong location.
            //

            setTranslation(0, -top * rowHeight, anim);

            //
            // 5. refresh pageup/pagedown highlights
            //

            BHighlights h = getHighlights();
            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible((top > 0) ? H_VIS_TRUE : H_VIS_FALSE);
                pagedown.setVisible((top + nVisibleRows < rows.size()) ? H_VIS_TRUE : H_VIS_FALSE);
                h.refresh(anim);
            }
            
            //
            // 6. refresh row highlights if we still have focus
            //

            if (focused != -1) {
                h = getRowHighlights();
                BHighlight up = h.get(H_UP);
                BHighlight down = h.get(H_DOWN);
                if (up != null && down != null) {
                    up.setVisible((focused > 0) ? H_VIS_FOCUS : H_VIS_FALSE);
                    down.setVisible((focused < size - 1) ? H_VIS_FOCUS : H_VIS_FALSE);
                }
                rowHighlights.refresh(anim);
            } else if (isAncestorOf(screen.getFocus())) {
                //
                // 7. make sure that focus hasn't been removed
                //
                
                if (size > 0) {
                    setFocus(0, false);
                } else {
                    screen.setFocus(screen.getFocusDefault());
                }
            }
        } finally {
            screen.setPainting(true);
        }
    }

    //
    // event handlers
    //
    
    /**
     * Handle key presses. The list handles KEY_UP, KEY_DOWN, KEY_CHANNELUP and
     * KEY_CHANNELDOWN by default.
     */
    public boolean handleKeyPress(int code, long rawcode)
    {
        final int pagesize = nVisibleRows - 1;
        int newfocus = -1;
        int newtop = top;

	switch (code) {
          case KEY_UP:          newfocus = focused - 1;                       break;
          case KEY_DOWN:        newfocus = focused + 1;                       break;
	  case KEY_CHANNELUP:   newfocus = top; newtop = top - pagesize;      break;
          case KEY_CHANNELDOWN: newfocus = top + pagesize; newtop = newfocus; break;
          default:              return super.handleKeyPress(code, rawcode);
        }

        if (focused != -1) {
            int max = size() - 1;

            // normalize the newfocus/newtop values
            newfocus = Math.max(Math.min(newfocus, max), 0);
            if (newtop == top) {
                // check if we moved off the current page
                if (newfocus < top) {
                    newtop = newfocus - (pagesize - 1);
                } else if (newfocus > top + pagesize) {
                    newtop = newfocus - 1;
                }
            }
            newtop = Math.max(Math.min(newtop, max - pagesize), 0);

            if (newfocus == focused && newtop == top) {
                // no changes - bonk!
                return false;
            }

            // Play the sound manually because sometimes we change top without
            // changing the focus and by default the key is considered not
            // handled if the focus doesn't change (reasonable behavior most of
            // the time).
            getBApp().playSoundForKey(code, true, true);
            if (newtop != top) {
                animate = true;
                setTop(newtop, newfocus);
                refresh();
            }
            setFocus(newfocus, true);
        }
        return true;
    }

    /**
     * Handle key repeats for some keys.
     */
    public boolean handleKeyRepeat(int code, long rawcode)
    {
	switch (code) {
	  case KEY_UP:
	  case KEY_DOWN:
	  case KEY_CHANNELUP:
	  case KEY_CHANNELDOWN:
	    return handleKeyPress(code, rawcode);
	}
	return super.handleKeyRepeat(code, rawcode);
    }

    /**
     * Handle focus movement.
     */
    public boolean handleFocus(boolean isGained, BView gained, BView lost)
    {
        if (isGained) {
            if (gained == this) {
                // If someone tried to set the focus to the list, move to the
                // first element. This is just for convenience.
                setFocus(0, false);
            } else {
                // one of our rows received focus
                focused = gained.getY() / rowHeight;
                refresh();
            }
        } else if (gained.getParent() != this) {
            // If we're losing focus, call refresh(). The above check
            // (gained.parent != this) is an optimization to avoid calling
            // refresh() twice when the focus moves between rows in the list.
            refresh();
        }
        return true;
    }

    //
    // element accessors
    //

    /**
     * Returns the number of elements in the list.
     */
    public int size()
    {
        return elements.size();
    }

    /**
     * Returns true if the list contains element o.
     */
    public boolean contains(Object o)
    {
        return elements.contains(o);
    }

    /**
     * Add an object to the end of the list.
     */
    public void add(Object o)
    {
        add(size(), o);
    }

    /**
     * Add a group of objects to the end of the list.
     */
    public void add(Object a[])
    {
        add(size(), a);
    }

    /**
     * Add a group of objects to the end of the list.
     */
    public void add(Vector v)
    {
        add(size(), v);
    }

    /**
     * Find an object in the list.
     */
    public int indexOf(Object o)
    {
        return elements.indexOf(o);
    }

    /**
     * Find an object in the list, starting at the end.
     */
    public int lastIndexOf(Object o)
    {
        return elements.lastIndexOf(o);
    }

    /**
     * Remove an object from the list. Returns true if the object was removed.
     */
    public boolean remove(Object o)
    {
        int index = elements.indexOf(o);
        if (index == -1) {
            return false;
        }
        remove(index);
        return true;
    }

    /**
     * Clear the list.
     */
    public void clear()
    {
        for (Enumeration e = rows.elements(); e.hasMoreElements(); ) {
            BView v = (BView)e.nextElement();
            if (v != null) {
                // we have to remove() old views                
                v.remove();
            }
        }
        elements.clear();
        rows.clear();
        
        focused = -1;
        
        refresh();
    }

    /**
     * Get an object from the list.
     */
    public Object get(int index)
    {
        return elements.get(index);
    }

    /**
     * Get a row at the specific index, creating it if necessary.
     */
    public BView getRow(int index)
    {
        // Since this is our main accessor this a good place to make sure that
        // we move the row to the correct height.
        BView view = (BView)rows.get(index);
        if (view == null) {
            view = new BView(this, 0, index * rowHeight, getWidth(), rowHeight);
            view.setFocusable(true);
            view.setHighlights( rowHighlights );
            createRow(view, index);
            rows.set(index, view);
        } else {
            view.setLocation(0, index * rowHeight);
        }
        return view;
    }
    
    /**
     * Set an object in the list.
     * 
     * @param index which element to change
     * @param element the new element
     */
    public Object set(int index, Object element)
    {
        Object obj = elements.set(index, element);
        BView v = (BView)rows.set(index, null);
        if (v != null) {
            // we have to remove() old views
            v.remove();
        }

        // fix the focus if necessary
        if (v.hasFocus()) {
            getScreen().setFocus(getRow(index));
        }
        
        refresh();
        return obj;
    }

    /**
     * Add an object at a particular index.
     *
     * @param index where to insert the new element
     * @param element the new element
     */
    public void add(int index, Object element)
    {
        elements.add(index, element);
        rows.add(index, null);
        touch(index, 1);
    }

    /**
     * Add a group of objects at a particular index.
     *
     * @param index where to insert the new elements
     * @param a the new elements
     */
    public void add(int index, Object a[])
    {
        for (int i = a.length; i-- > 0;) {
            elements.add(index, a[i]);
            rows.add(index, null);
        }
        touch(index, a.length);
    }

    /**
     * Add a group of objects at a particular index.
     *
     * @param index where to insert the new elements
     * @param v the new elements
     */
    public void add(int index, Vector v)
    {
        for (int i = v.size(); i-- > 0;) {
            elements.add(index, v.elementAt(i));
            rows.add(index, null);
        }
        touch(index, v.size());
    }

    /**
     * Remove an element.
     *
     * @param index where to remove the element
     */
    public Object remove(int index)
    {
        Object o = elements.remove(index);
        BView v = (BView)rows.remove(index);
        if (v != null) {
            // we have to remove() old views            
            v.remove();
        }
        touch(index, -1);
        return o;
    }

    /**
     * A helper for adding/removing elements. This will update focused, top, and
     * dirty. It will then call refresh.
     *
     * @param index the index where the change occurred
     * @param len the number of items added (positive) or removed (negative)
     */
    void touch(int index, int len)
    {
        // move focused
        if (index <= focused) {
            focused += len;
        }

        // move top (and maybe dirty when items added)
        if (len > 0) {
            if (index <= top) {
                setTop(top + len);
            } else {
                // When elements are added above the top, rows at the bottom
                // of our populated window may need to be removed. Adjust dirty
                // upward to make this happen during refresh().
                dirty += len;
            }
        } else if (index < top) {
            setTop(top + len);
        }
        refresh();
    }

    //
    // Highlights layout for the rows
    //

    class Layout implements IHighlightsLayout
    {
        public BScreen getScreen()
        {
            return BList.this.getScreen();
        }
        
        public BRect getHighlightBounds()
        {
            BView row = getRow();
            if (row != null) {
                return row.getHighlightBounds();
            }
            return toScreenBounds(new BRect(0, 0, getWidth(), rowHeight));
        }
    
        public boolean getHighlightIsVisible(int visible)
        {
            BView row = getRow();
            return (row != null) ? row.getHighlightIsVisible(visible) : false;
        }

        protected BView getRow()
        {
            return (focused != -1) ? (BView)rows.elementAt(focused) : null;
        }
    }
}
