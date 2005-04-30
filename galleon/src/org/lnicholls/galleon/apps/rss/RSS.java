package org.lnicholls.galleon.apps.rss;

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
import java.awt.Image;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Date;
import java.io.*;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.ScrollText;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

public class RSS extends DefaultApplication {

    private static Logger log = Logger.getLogger(RSS.class.getName());

    public final static String TITLE = "RSS";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mViewerBackground;

    private Resource mFolderIcon;

    private Resource mItemIcon;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mViewerBackground = getSkinImage("viewer", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mItemIcon = getSkinImage("menu", "item");

        push(new RSSMenuScreen(this), TRANSITION_NONE);
    }

    public class RSSMenuScreen extends DefaultMenuScreen {
        public RSSMenuScreen(RSS app) {
            super(app, "RSS");

            below.setResource(mMenuBackground);
            
            RSSConfiguration rssConfiguration = (RSSConfiguration) ((RSSFactory) context.factory).getAppContext()
                    .getConfiguration();
            List feeds = rssConfiguration.getFeeds();
            NameValue[] feedArray = (NameValue[]) feeds.toArray(new NameValue[0]);
            Arrays.sort(feedArray, new Comparator() {
                public int compare(Object o1, Object o2) {
                    NameValue nameValue1 = (NameValue) o1;
                    NameValue nameValue2 = (NameValue) o2;

                    return -nameValue1.getName().compareTo(nameValue2.getName());
                }
            });

            for (int i = 0; i < feedArray.length; i++) {
                NameValue nameValue = (NameValue) feedArray[i];
                List stories = (List) ((RSSFactory) context.factory).mChannels.get(nameValue.getValue());
                if (stories != null && stories.size() > 0)
                    mMenuList.add(nameValue);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                NameValue nameValue = (NameValue) mMenuList.get(mMenuList.getFocus());

                List stories = (List) ((RSSFactory) context.factory).mChannels.get(nameValue.getValue());
                RSSFeedMenuScreen rssFeedMenuScreen = new RSSFeedMenuScreen((RSS) getBApp(), nameValue,
                        stories);
                getBApp().push(rssFeedMenuScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            icon.setResource(mFolderIcon);

            NameValue nameValue = (NameValue) mMenuList.get(index);
            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(nameValue.getName(), 40));
        }
    }

    public class RSSFeedMenuScreen extends DefaultMenuScreen {
        public RSSFeedMenuScreen(RSS app, NameValue nameValue, List list) {
            super(app, null);

            below.setResource(mMenuBackground);
            
            Image image = image = Tools.retrieveCachedImage(nameValue.getValue());
            if (image !=null) {
                mImage = new BView(below, SAFE_TITLE_H + (this.BODY_WIDTH - image.getWidth(null))/2, SAFE_TITLE_V, image.getWidth(null), image.getHeight(null));
                mImage.setResource(createImage(image));    
            }
            else
                setTitle(nameValue.getName());

            for (int i = 0; i < list.size(); i++) {
                ItemIF item = (ItemIF) list.get(i);
                mMenuList.add(item);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                ItemIF item = (ItemIF) mMenuList.get(mMenuList.getFocus());

                RSSScreen rssScreen = new RSSScreen((RSS) getBApp(), item);
                getBApp().push(rssScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            icon.setResource(mItemIcon);

            ItemIF item = (ItemIF) mMenuList.get(index);
            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(item.getTitle()), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
        
        private BView mImage;
    }

    public class RSSScreen extends DefaultScreen {

        public RSSScreen(RSS app, ItemIF item) {
            super(app, cleanHTML(item.getTitle()), false);

            below.setResource(mInfoBackground);

            int start = TOP;
            
            mScrollText = new ScrollText(normal, SAFE_TITLE_H, TOP + 30, BODY_WIDTH - 10, height - 2 * SAFE_TITLE_V - 175,
                    cleanHTML(item.getDescription()));

            /*mList = new DefaultOptionList(this.normal, SAFE_TITLE_H, (height - SAFE_TITLE_V) - 40,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            mList.add("Back to menu");
            setFocusDefault(mList);
            */
            
            BButton button = new BButton(normal, SAFE_TITLE_H + 10, (height-SAFE_TITLE_V)-55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 35);
            button.setResource(createText("default-24.font", Color.white, "Return to menu"));
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
            setFocus(button);
        }
        
        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_UP:
            case KEY_DOWN:
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                return mScrollText.handleKeyPress(code, rawcode);
            }
            return super.handleKeyPress(code, rawcode);
        }        

        private BList mList;

        private ScrollText mScrollText;
    }

    public static class RSSFactory extends AppFactory {

        public RSSFactory(AppContext appContext) {
            super(appContext);
            
            Server.getServer().scheduleShortTerm(new ReloadTask(new ReloadCallback() {
                public void reload() {
                    try {
                        updateChannels();
                    } catch (Exception ex) {
                        log.error("Could not download stations", ex);
                    }
                }
            }), 5);            
        }

        public void setAppContext(AppContext appContext) {
            super.setAppContext(appContext);

            updateChannels();
        }

        private void updateChannels() {
            RSSConfiguration rssConfiguration = (RSSConfiguration) getAppContext().getConfiguration();
            Iterator iterator = rssConfiguration.getFeeds().iterator();
            while (iterator.hasNext()) {
                NameValue nameValue = (NameValue) iterator.next();
                List stories = (List) mChannels.get(nameValue.getValue());
                if (stories == null) {
                    stories = new ArrayList();
                    mChannels.put(nameValue.getValue(), stories);
                }

                try {
                    String last = Tools.loadPersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "date");
                    String difference = Tools.loadPersistentValue(this.getClass().getName() + "." + nameValue.getValue() +"." + "ttl");
                    String content = null;
                    
                    int time = 60;
                    if (difference!=null)
                    {
                        try
                        {
                            time = Integer.parseInt(difference);
                        }
                        catch (Exception ex){}
                    }
                    
                    if (last !=null)
                    {
                        Date lastTime = new Date(last);
                        Date current = new Date();
                        if ((current.getTime()-lastTime.getTime())/(1000*60) > time)
                        {
                            content = Tools.getPage(new URL(nameValue.getValue()));
                            
                            Tools.savePersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "date", new Date().toString());
                            Tools.savePersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "content", content);
                        }
                        else
                        {
                            content = Tools.loadPersistentValue(this.getClass().getName() + "." + nameValue.getValue() +"." + "content");
                        }
                    }
                    else
                    {
                        content = Tools.getPage(new URL(nameValue.getValue()));
                        
                        Tools.savePersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "date", new Date().toString());
                        Tools.savePersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "content", content);
                    }
                    
                    if (content!=null)
                    {
                        ChannelBuilderIF builder = new ChannelBuilder();
                        ChannelIF channel = FeedParser.parse(builder, new ByteArrayInputStream((content.getBytes())));
    
                        if (channel.getItems().size()>0)
                        {
                            stories.clear();
                            
                            int count = 0;
                            Iterator chs = channel.getItems().iterator();
                            while (chs.hasNext()) {
                                ItemIF item = (ItemIF) chs.next();
                                stories.add(item);
                            }
        
                            if (channel.getImage() != null && Tools.retrieveCachedImage(nameValue.getValue())==null) {
                                Tools.cacheImage(channel.getImage().getLocation(), nameValue.getValue());
                            }
                        }
                        
                        int ttl = channel.getTtl();
                        if (ttl==0 | ttl == -1)
                            ttl = 60;
                        
                        Tools.savePersistentValue(this.getClass().getName() + "." + nameValue.getValue() + "." + "ttl", String.valueOf(ttl));
                    }
                } catch (Exception ex) {
                    Tools.logException(RSS.class, ex, "Could not reload " + nameValue.getValue());
                }
            }
        }

        protected void init(ArgumentList args) {
            super.init(args);
        }

        private static Hashtable mChannels = new Hashtable();
    }
    
    private static String cleanHTML(String data) {
        String result = "";
        data = data.replaceAll("\n", " ");
        int pos1 = data.indexOf("<");
        if (pos1 != -1) {
            while (pos1 != -1) {
                int pos2 = data.indexOf(">");
                if (pos2 == -1) {
                    result = result + data;
                    break;
                }
                result = result + data.substring(0, pos1);
                data = data.substring(pos2 + 1);
                pos1 = data.indexOf("<");
            }
        } else
            result = data;
        return StringEscapeUtils.unescapeHtml(result);
    }
}