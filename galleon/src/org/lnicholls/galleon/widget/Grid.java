package org.lnicholls.galleon.widget;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */

import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.tivo.hme.bananas.BHighlight;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;

/*
 * Based on TiVo HME Bananas BList by Adam Doppelt
 */

public class Grid extends BView {

    private static final Logger log = Logger.getLogger(Grid.class.getName());

    public final static String ANIM = "*100";

    /**
     * Creates a new Grid instance. To avoid drawing partial rows, the list height should be a multiple of the
     * rowHeight.
     * 
     * @param parent
     *            parent
     * @param x
     *            x
     * @param y
     *            y
     * @param width
     *            width
     * @param height
     *            height
     * @param text
     *            the text to be displayed
     */
    public Grid(BView parent, int x, int y, int width, int height, String text) {
        this(parent, x, y, width, height, width / 3, width / 3, true, true);
    }

    /**
     * Creates a new Grid instance. To avoid drawing partial rows, the list height should be a multiple of the
     * rowHeight.
     * 
     * @param parent
     *            parent
     * @param x
     *            x
     * @param y
     *            y
     * @param width
     *            width
     * @param height
     *            height
     * @param cellWidth
     *            the cell width
     * @param cellHeight
     *            the cell heigth
     * @param visible
     *            if true, make the view visibile
     */
    public Grid(BView parent, int x, int y, int width, int height, int cellWidth, int cellHeight) {
        this(parent, x, y, width, height, cellWidth, cellHeight, true, true);
    }

    /**
     * Creates a new Grid instance. To avoid drawing partial rows, the list height should be a multiple of the
     * rowHeight.
     * 
     * @param parent
     *            parent
     * @param x
     *            x
     * @param y
     *            y
     * @param width
     *            width
     * @param height
     *            height
     * @param cellWidth
     *            the cell width
     * @param cellHeight
     *            the cell heigth
     * @param visible
     *            if true, make the view visibile
     */
    public Grid(BView parent, int x, int y, int width, int height, int cellWidth, int cellHeight, boolean visible) {
        this(parent, x, y, width, height, cellHeight, cellWidth, true, true);
    }

    /**
     * Creates a new Grid instance. To avoid drawing partial rows, the list height should be a multiple of the
     * rowHeight.
     * 
     * @param parent
     *            parent
     * @param x
     *            x
     * @param y
     *            y
     * @param width
     *            width
     * @param height
     *            height
     * @param cellWidth
     *            the cell width
     * @param cellHeight
     *            the cell heigth
     * @param visible
     *            if true, make the view visibile
     * @param hints
     *            if true, add hints
     */
    public Grid(BView parent, int x, int y, int width, int height, int cellWidth, int cellHeight, boolean visible,
            boolean hints) {
        super(parent, x, y, width, height, visible);
        mCellWidth = cellWidth;
        mCellHeight = cellHeight;
        mGridRows = new LinkedList();
        mGridRowViews = new LinkedList();

        mNumColumns = width / cellWidth;

        if (hints) {
            BHighlights h = getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);
        }

        refresh();
    }

    public void refresh() {
        BScreen screen = getScreen();
        Resource anim = mAnimate ? getResource(ANIM) : null;
        mAnimate = false;
        int size = mGridRows.size();

        screen.setPainting(false);
        try {
            mTop = Math.max(Math.min(mTop, size - mVisibleRows), 0);
            
            int max = Math.min(mTop + mVisibleRows, size) - 1;
            if (mFocused < mTop) {
                mTop = Math.max(0, mFocused);
            } else if (mFocused > max) {
                int end = Math.min(mFocused + 1, size);
                mTop = Math.max(end - mVisibleRows, 0);
            }            

            int popMin = Math.max(mTop - mVisibleRows, 0);
            int popMax = Math.min(mTop + 2 * mVisibleRows, mGridRows.size());

            int fixMin, fixMax;
            if (mDirty < mTop) {
                fixMin = Math.max(mDirty - mVisibleRows, 0);
                fixMax = popMax;
            } else {
                fixMin = popMin;
                fixMax = Math.min(mDirty + 2 * mVisibleRows, mGridRows.size());
            }
            mDirty = mTop;

            for (int index = fixMin; index < fixMax; ++index) {
                if (index < popMin || index >= popMax) {
                    BView v = (BView) mGridRowViews.get(index);
                    if (v != null) {
                        v.remove();
                        mGridRowViews.set(index, null);
                    }
                } else {
                    getRow(index);
                }
            }

            setTranslation(0, -mTop * mCellHeight, anim);

            BHighlights h = getHighlights();
            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible((mTop > 0) ? H_VIS_TRUE : H_VIS_FALSE);
                pagedown.setVisible((mTop + mVisibleRows < mGridRowViews.size()) ? H_VIS_TRUE : H_VIS_FALSE);
                h.refresh(anim);
            }
            
            /*
            if (mFocused != -1) {
                h = getRowHighlights();
                BHighlight up = h.get(H_UP);
                BHighlight down = h.get(H_DOWN);
                if (up != null && down != null) {
                    up.setVisible((mFocused > 0) ? H_VIS_FOCUS : H_VIS_FALSE);
                    down.setVisible((mFocused < size - 1) ? H_VIS_FOCUS : H_VIS_FALSE);
                }
                rowHighlights.refresh(anim);
            } else if (isAncestorOf(screen.focus)) {
                if (size > 0) {
                    setFocus(0, false);
                } else {
                    screen.setFocus(screen.getFocusDefault());
                }
            } 
            */           

        } finally {
            screen.setPainting(true);
        }
    }

    public boolean handleKeyPress(int code, long rawcode) {
        final int pagesize = mVisibleRows - 1;
        int newfocus = -1;
        int newtop = mTop;
        int newColumn = mColumn;

        switch (code) {
        case KEY_LEFT:
            newColumn = mColumn - 1;
            break;
        case KEY_RIGHT:
            newColumn = mColumn + 1;
            break;
        case KEY_UP:
            newtop = mTop - 1;
            break;
        case KEY_DOWN:
            newtop = mTop + 1;
            break;
        case KEY_CHANNELUP:
            newtop = mTop - pagesize;
            break;
        case KEY_CHANNELDOWN:
            newtop = mTop + pagesize;
            break;
        default:
            return super.handleKeyPress(code, rawcode);
        }

        int max = mGridRows.size() - 1;
        newtop = Math.max(Math.min(newtop, max - pagesize), 0);
        if (newtop == mTop) {
            return false;
        }

        newColumn = newColumn % mNumColumns;
        if (newColumn == mColumn) {
            return false;
        }

        getBApp().playSoundForKey(code, true, true);
        if (newtop != mTop) {
            mAnimate = true;
            setTop(newtop);
            refresh();
        }

        if (newtop != mTop) {
            mAnimate = true;
            setColumn(newColumn);
            refresh();
        }
        return true;
    }

    public boolean handleKeyRepeat(int code, long rawcode) {
        switch (code) {
        case KEY_UP:
        case KEY_DOWN:
        case KEY_CHANNELUP:
        case KEY_CHANNELDOWN:
            return handleKeyPress(code, rawcode);
        }
        return super.handleKeyRepeat(code, rawcode);
    }

    public void clear() {
        for (Iterator iterator = mGridRowViews.iterator(); iterator.hasNext();) {
            BView view = (BView) iterator.next();
            if (view != null) {
                view.remove();
            }
        }
        mGridRows.clear();
        mGridRowViews.clear();

        refresh();
    }

    private BView getRow(int index) {
        BView view = (BView) mGridRowViews.get(index);
        if (view == null) {
            view = new BView(this, 0, index * mCellHeight, width, mCellHeight);
            createRow(view, index);
            mGridRowViews.set(index, view);
        } else {
            view.setLocation(0, index * mCellHeight);
        }
        return view;
    }

    private void createRow(BView parent, int row) {
        int cells = mGridRows.size() % mNumColumns;
        for (int i = 0; i < cells; i++) {
            BView view = new BView(parent, i * mCellWidth, 0, mCellWidth, mCellHeight);
            createCell(view, row, i, i==mColumn);
        }
    }

    public void createCell(BView parent, int row, int column, boolean selected) {
        parent.setResource(getResource("icon.png"));
    }
    
    public void add(Object element)
    {
        add(mGridRows.size(),element);
    }
    
    public void add(int index, Object element)
    {
        mGridRows.add(index, element);
        mGridRowViews.add(index, null);
        touch(index, 1);
    }
    
    void touch(int index, int len)
    {
        if (index <= mFocused) {
            mFocused += len;
        }

        if (len > 0) {
            if (index <= mTop) {
                setTop(mTop + len);
            } else {
                mDirty += len;
            }
        } else if (index < mTop) {
            setTop(mTop + len);
        }
        refresh();
    }    

    public void setTop(int ntop) {
        mDirty = mTop;
        mTop = ntop;
        mFocused = Math.min(Math.max(mFocused, 0), mGridRows.size() - 1);
    }

    public void setColumn(int value) {
        mDirty = mTop;
        mColumn = value;
    }

    public int getCellHeight() {
        return mCellHeight;
    }

    public int getCellWidth() {
        return mCellWidth;
    }

    public void setAnimate(boolean value) {
        mAnimate = value;
    }

    public boolean getAnimate() {
        return mAnimate;
    }
    
    public int getFocus()
    {
        return mFocused;
    }
    
    public void setFocus(int index, boolean animate)
    {
        int size = mGridRows.size();
        if (size > 0) {
            mAnimate = animate;
            mFocused = Math.min(Math.max(index, 0), size - 1);
            getScreen().setFocus(getRow(mFocused));
            mAnimate = false;
        }
    }    

    private LinkedList mGridRows;

    private LinkedList mGridRowViews;

    private int mCellWidth;

    private int mCellHeight;

    private int mVisibleRows;

    private int mTop;

    private int mDirty;

    private int mColumn;

    private int mNumColumns;
    
    protected int mFocused;

    boolean mAnimate;
}