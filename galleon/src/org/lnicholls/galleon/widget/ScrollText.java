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

import java.awt.Font;
import java.awt.FontMetrics;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;

import com.tivo.hme.bananas.BHighlight;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;

/*
 * Based on TiVo HME Bananas BList by Adam Doppelt
 */

public class ScrollText extends BView {

    private static final Logger log = Logger.getLogger(ScrollText.class.getName());

    private static Font DEFAULT_FONT = null;
    static {
        try {
            DEFAULT_FONT = Font.createFont(Font.TRUETYPE_FONT, ScrollText.class.getClassLoader().getResourceAsStream(
                    ScrollText.class.getPackage().getName().replace('.', '/') + "/" + "FreeSans.ttf"));
        } catch (Exception ex) {
            log.error(ex);
        }
    }

    public final static String ANIM = "*100";

    /**
     * Creates a new ScrollText instance. To avoid drawing partial rows, the list height should be a multiple of the
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
    public ScrollText(BView parent, int x, int y, int width, int height, String text) {
        this(parent, x, y, width, height, text, DEFAULT_FONT.deriveFont(Font.PLAIN, 18), true, true); // TODO Configure
        // font
    }

    /**
     * Creates a new ScrollText instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param font
     *            the font for the text
     * @param visible
     *            if true, make the view visibile
     */
    public ScrollText(BView parent, int x, int y, int width, int height, String text, Font font) {
        this(parent, x, y, width, height, text, font, true, true);
    }

    /**
     * Creates a new ScrollText instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param font
     *            the font for the text
     * @param visible
     *            if true, make the view visibile
     */
    public ScrollText(BView parent, int x, int y, int width, int height, String text, Font font, boolean visible) {
        this(parent, x, y, width, height, text, font, true, true);
    }

    /**
     * Creates a new ScrollText instance. To avoid drawing partial rows, the list height should be a multiple of the
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
     * @param font
     *            the font for the text
     * @param visible
     *            if true, make the view visibile
     * @param hints
     *            if true, add hints
     */
    public ScrollText(BView parent, int x, int y, int width, int height, String text, Font font, boolean visible,
            boolean hints) {
        super(parent, x, y, width, height, visible);
        mTextLines = new LinkedList();
        mTextLineViews = new LinkedList();
        mFont = font;

        setText(text);

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
        int size = mTextLines.size();

        screen.setPainting(false);
        try {
            mTop = Math.max(Math.min(mTop, size - mVisibleRows), 0);

            int popMin = Math.max(mTop - mVisibleRows, 0);
            int popMax = Math.min(mTop + 2 * mVisibleRows, mTextLines.size());

            int fixMin, fixMax;
            if (mDirty < mTop) {
                fixMin = Math.max(mDirty - mVisibleRows, 0);
                fixMax = popMax;
            } else {
                fixMin = popMin;
                fixMax = Math.min(mDirty + 2 * mVisibleRows, mTextLines.size());
            }
            mDirty = mTop;

            for (int index = fixMin; index < fixMax; ++index) {
                if (index < popMin || index >= popMax) {
                    BView v = (BView) mTextLineViews.get(index);
                    if (v != null) {
                        v.remove();
                        mTextLineViews.set(index, null);
                    }
                } else {
                    getRow(index);
                }
            }

            setTranslation(0, -mTop * mRowHeight, anim);

            BHighlights h = getHighlights();
            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible((mTop > 0) ? H_VIS_TRUE : H_VIS_FALSE);
                pagedown.setVisible((mTop + mVisibleRows < mTextLineViews.size()) ? H_VIS_TRUE : H_VIS_FALSE);
                h.refresh(anim);
            }

        } finally {
            screen.setPainting(true);
        }
    }

    public boolean handleKeyPress(int code, long rawcode) {
        final int pagesize = mVisibleRows - 1;
        int newfocus = -1;
        int newtop = mTop;

        switch (code) {
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

        int max = mTextLines.size() - 1;
        newtop = Math.max(Math.min(newtop, max - pagesize), 0);
        if (newtop == mTop) {
            return false;
        }

        getBApp().playSoundForKey(code, true, true);
        if (newtop != mTop) {
            mAnimate = true;
            setTop(newtop);
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
        for (Iterator iterator = mTextLineViews.iterator(); iterator.hasNext();) {
            BView view = (BView) iterator.next();
            if (view != null) {
                view.remove();
            }
        }
        mTextLines.clear();
        mTextLineViews.clear();

        refresh();
    }

    private BView getRow(int index) {
        BView view = (BView) mTextLineViews.get(index);
        if (view == null) {
            view = new BView(this, 0, index * mRowHeight, width, mRowHeight);
            createText(view, index);
            mTextLineViews.set(index, view);
        } else {
            view.setLocation(0, index * mRowHeight);
        }
        return view;
    }

    private void createText(BView parent, int index) {
        BText text = new BText(parent, 0, 0, parent.width, parent.height);
        text.setFlags(RSRC_HALIGN_LEFT);
        text.setFont("default-" + mRowHeight + ".font");
        text.setShadow(true);

        text.setValue(mTextLines.get(index));
    }

    public void setTop(int ntop) {
        mDirty = mTop;
        mTop = ntop;
    }

    public void setText(String value) {
        mText = value;

        calculateText();
    }

    public String getText() {
        return mText;
    }

    public Font getFont() {
        return mFont;
    }

    public void setFont(Font value) {
        mFont = value;

        calculateText();
    }

    public int getRowHeight() {
        return mRowHeight;
    }

    public void setAnimate(boolean value) {
        mAnimate = value;
    }

    public boolean getAnimate() {
        return mAnimate;
    }

    private void calculateText() {
        clear();

        FontMetrics fontMetrics = Tools.getFontMetrics(mFont);
        mRowHeight = fontMetrics.getHeight();
        mVisibleRows = height / mRowHeight;

        String[] lines = Tools.layout(width, fontMetrics, mText);
        for (int i = 0; i < lines.length; i++) {
            int size = mTextLines.size();
            mTextLines.add(size, lines[i]);
            mTextLineViews.add(size, null);
        }

        mTop = 0;
        refresh();
    }

    private String mText;

    private Font mFont;

    private LinkedList mTextLines;

    private LinkedList mTextLineViews;

    private int mRowHeight;

    private int mVisibleRows;

    private int mTop;

    private int mDirty;

    boolean mAnimate;
}