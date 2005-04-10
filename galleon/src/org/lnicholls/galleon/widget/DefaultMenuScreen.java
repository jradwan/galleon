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

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BView;

public class DefaultMenuScreen extends DefaultScreen {

    public DefaultMenuScreen(DefaultApplication app, String title) {
        super(app);
        setTitle(title);

        mMenuList = new MenuList(this, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                - ((SAFE_TITLE_H * 2) + 32), 280, 35);
        BHighlights h = mMenuList.getHighlights();
        h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
        h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

        setFocusDefault(mMenuList);
    }

    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        mMenuList.init();
        return super.handleEnter(arg, isReturn);
    }

    public boolean handleExit() {
        mMenuList.clearViews();
        return super.handleExit();
    }

    public void load() {
        if (mMenuList.getFocus()!=-1)
        {
            BView row = mMenuList.getRow(mMenuList.getFocus());
            BView icon = (BView) row.children[0];
            icon.setResource(((DefaultApplication) getApp()).mBusy2Icon);
            icon.flush();
    
            getBApp().play("select.snd");
            getBApp().flush();
        }
    }

    protected void createRow(BView parent, int index) {

    }

    public class MenuList extends DefaultList {
        public MenuList(DefaultMenuScreen defaultMenuScreen, int x, int y, int width, int height, int rowHeight) {
            super(defaultMenuScreen.normal, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            mDefaultMenuScreen = defaultMenuScreen;
        }

        protected void createRow(BView parent, int index) {
            mDefaultMenuScreen.createRow(parent, index);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_PLAY:
                postEvent(new BEvent.Action(this, "play"));
                return true;
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "push"));
                return true;
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                boolean result = super.handleKeyPress(code, rawcode);
                if (!result) {
                    getBApp().play("bonk.snd");
                    getBApp().flush();
                }
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public int getTop() {
            return top;
        }

        DefaultMenuScreen mDefaultMenuScreen;
    }

    protected MenuList mMenuList;
}