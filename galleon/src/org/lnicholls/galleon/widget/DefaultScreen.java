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

import java.awt.Color;

import org.apache.log4j.Logger;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BHighlight;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;

public class DefaultScreen extends BScreen {

    private static final Logger log = Logger.getLogger(DefaultScreen.class.getName());

    static final class HintsView extends BView {
        public HintsView(BView parent, int x, int y, int width, int height, boolean visible) {
            super(parent, x, y, width, height, true);

            setFocusable(true);
            BHighlights h = getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT, A_TOP);
            h.setPageHint(H_PAGEDOWN, A_RIGHT, A_BOTTOM);

            BHighlight pageup = h.get(H_PAGEUP);
            BHighlight pagedown = h.get(H_PAGEDOWN);
            if (pageup != null && pagedown != null) {
                pageup.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                pagedown.setVisible(H_VIS_TRUE); // : H_VIS_FALSE);
                h.refresh();
            }
        }

        public boolean getHighlightIsVisible(int visible) {
            return visible == H_VIS_TRUE;
        }
    }

    public DefaultScreen(BApplication app) {
        this(app, "background.jpg");
    }

    public DefaultScreen(BApplication app, String background) {
        this(app, background, false);
    }

    public DefaultScreen(BApplication app, boolean hints) {
        this(app, "background.jpg", hints);
    }

    public DefaultScreen(BApplication app, String background, boolean hints) {
        super(app);

        setBackground(background);

        if (hints)
            mHints = new HintsView(above, SAFE_TITLE_H, SAFE_TITLE_V, width - 2 * SAFE_TITLE_H, height - 2
                    * SAFE_TITLE_V, true);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            this.getBApp().pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_LEFT:
            getBApp().play("pageup.snd");
            getBApp().flush();
            getBApp().setActive(false); // TODO Make default just pop
            return true;
        }

        return super.handleKeyPress(code, rawcode);
    }

    public void setTitle(String value) {
        if (value != null) {
            if (mTitle == null) {
                mTitle = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V, (width - (SAFE_TITLE_H * 2)), 50);
                mTitle.setValue(" ");
                mTitle.setColor(Color.yellow);
                mTitle.setShadow(Color.black, 3);
                mTitle.setFlags(RSRC_HALIGN_CENTER);
                mTitle.setFont("default-48.font");
            }

            mTitle.setValue(value);
        }
    }

    public void setFooter(String value) {
        if (value != null) {
            if (mFooter == null) {
                mFooter = new BText(normal, SAFE_TITLE_H, height - SAFE_TITLE_V - 18, (width - (SAFE_TITLE_H * 2)), 20);
                mFooter.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
                mFooter.setFont("default-18.font");
            }

            mFooter.setValue(value);
        }
    }

    public void setBackground(String value) {
        if (value != null)
            below.setResource(value);
    }

    private BText mTitle;

    private BText mFooter;

    private HintsView mHints;
}

