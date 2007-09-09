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

import java.util.List;

import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BRect;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.View;

/*
 * Based on TiVo HME Bananas BList by Adam Doppelt
 */

public abstract class Grid extends BList {
	public static final int ROWS_HD = 5;
	public static final int COLUMNS_HD = 5;
	public static final int ROWS_SD = 3;
	public static final int COLUMNS_SD = 3;
	
    /**
     * Creates a new BList instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param rowHeight
     *            the height of each row contained in the list.
     */
    public Grid(BView parent, int x, int y, int width, int height, int rowHeight) {
        this(parent, x, y, width, height, rowHeight, true, false);
    }

    /**
     * Creates a new BList instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param rowHeight
     *            the height of each row contained in the list.
     * @param visible
     *            if true, make the view visibile
     */
    public Grid(BView parent, int x, int y, int width, int height, int rowHeight, boolean visible) {
    	this(parent, x, y, width, height, rowHeight, visible, false);
    }
    /**
     * Creates a new BList instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param rowHeight
     *            the height of each row contained in the list.
     * @param visible
     *            if true, make the view visible
     * @param HiDef
     *            if true, lay out the grid for a High Definition screen
     */
    public Grid(BView parent, int x, int y, int width, int height, int rowHeight, boolean visible, boolean HiDef) {
        super(parent, x, y, width, height, rowHeight, visible);
        mRowCount = HiDef ? ROWS_HD : ROWS_SD;
        mColumnCount = HiDef ? COLUMNS_HD : COLUMNS_SD;
        	
        mMarker = new View(this, 0, 0,
                           HiDef ? width / COLUMNS_HD : width / COLUMNS_SD,
                           rowHeight);
        mMarker.setResource(createImage("org/lnicholls/galleon/widget/marker.png")); // TODO Make configurable
    }

    /**
     * Create a row for the given element. For example, you could create a view with the given parent and then set its
     * resource to be a text resource based on the element at index.
     * 
     * @param parent
     *            use this as the parent for your new view
     * @param index
     *            the index for the row
     * @return the new row
     */
    protected void createRow(BView parent, int index) {
    	final int PADDING = 3;
        for (int i = 0; i < mColumnCount; i++) {
            BView view = new BView(parent, i * parent.getWidth() / mColumnCount + PADDING, 0,
            					   parent.getWidth() / mColumnCount - 2 * PADDING, getRowHeight() - 2 * PADDING);
            createCell(view, index, i, index == getFocus());
        }
    }

    public abstract void createCell(BView parent, int row, int column, boolean selected);

    public boolean handleKeyPress(int code, long rawcode) {
        try {
            if (size() > 0) {
                List cells = (List) get(getFocus());
                int newColumn = mColumn;

                switch (code) {
                case KEY_RIGHT:
                    newColumn = Math.max(Math.min(mColumn + 1, cells.size() - 1), 0);
                    if (mColumn == newColumn) {
                        getBApp().play("bonk.snd");
                        getBApp().flush();
                        return false;
                    } else {
                        mColumn = newColumn;
                        getBApp().play("updown.snd");
                        getBApp().flush();
                        return true;
                    }
                case KEY_LEFT:
                    newColumn = Math.max(Math.min(mColumn - 1, cells.size() - 1), 0);
                    if (mColumn == newColumn) {
                        return false;
                    } else {
                        mColumn = newColumn;
                        getBApp().play("updown.snd");
                        getBApp().flush();
                        return true;
                    }
                case KEY_ADVANCE:
                    if (getFocus() == (size() - 1)) {
                        getBApp().play("pageup.snd");
                        getBApp().flush();
                        setFocus(0, false);
                    } else {
                        getBApp().play("pagedown.snd");
                        getBApp().flush();
                        setFocus(size() - 1, false);
                    }
                    return true;
                default:
                    return super.handleKeyPress(code, rawcode);
                }
            } else
                return super.handleKeyPress(code, rawcode);
        } finally {
            if (size() > 0) {
                List cells = (List) get(getFocus());
                mColumn = Math.max(Math.min(mColumn, cells.size() - 1), 0);
            }
            updateMarker();
        }
    }

    private void updateMarker() {
        BRect markerBounds = new BRect(mColumn * (getWidth() / mColumnCount),
                                       getFocus() * getRowHeight(),
                                       getWidth() / mColumnCount,
                                       getRowHeight());
        mMarker.setBounds(markerBounds.x, markerBounds.y, markerBounds.width, markerBounds.height);
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

    public int getPos() {
        return getFocus() * mColumnCount + mColumn;
    }

    public void setPos(int value) {
        setFocus(value / mColumnCount, false);
        mColumn = value % mColumnCount;
        updateMarker();
        refresh();
    }
    
    public int getRowCount() {
    	return mRowCount;
    }
    public int getColumnCount() {
    	return mColumnCount;
    }

    private int mColumn;
    private int mColumnCount;
    private int mRowCount;
    

    private View mMarker;
}
