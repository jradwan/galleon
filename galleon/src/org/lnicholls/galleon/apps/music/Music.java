package org.lnicholls.galleon.apps.music;

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

import java.io.*;
import java.awt.*;
import java.awt.image.*;
import java.net.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.*;
import java.util.Iterator;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.stream.*;

import javax.swing.ImageIcon;

import com.tivo.hme.bananas.*;

import net.sf.hibernate.HibernateException;

import org.jdom.Element;
import org.lnicholls.galleon.app.*;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.togo.ToGoThread;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppDescriptor;
import org.lnicholls.galleon.apps.togo.ToGo.ToGoScreen.OptionList;
import org.lnicholls.galleon.apps.weather.WeatherData.Forecasts;
import org.lnicholls.galleon.database.*;
import org.lnicholls.galleon.media.*;

import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.share.*;
import com.tivo.hme.http.server.*;

import org.apache.log4j.Logger;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Based on TiVo Bananas sample code by Carl Haynes
 */

public class Music extends BApplication {

    private static Logger log = Logger.getLogger(Music.class.getName());

    public final static String TITLE = "Music";

    protected void init(Context context) {
        super.init(context);

        push(new MusicMenuScreen(this), TRANSITION_NONE);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public class DefaultScreen extends BScreen {
        public DefaultScreen(Music app) {
            super(app);

            below.setResource("background.jpg");

            mTitle = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V, (width - (SAFE_TITLE_H * 2)), 54);
            mTitle.setValue(" ");
            mTitle.setColor(Color.yellow);
            mTitle.setShadow(Color.black, 3);
            mTitle.setFlags(RSRC_HALIGN_CENTER);
            mTitle.setFont("default-48.font");
        }

        public void setTitle(String value) {
            mTitle.setValue(value);
        }

        private BText mTitle;

    }

    public class MusicMenuScreen extends DefaultScreen {
        private TGList list;

        public MusicMenuScreen(Music app) {
            super(app);
            setTitle("Music");

            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            MusicConfiguration musicConfiguration =  (MusicConfiguration)((MusicFactory) context.factory).getAppContext().getConfiguration();
            
            for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
                NameValue nameValue = (NameValue) i.next();
                list.add(new PathScreen(app, nameValue.getName(), nameValue.getValue()));
            }

            setFocusDefault(list);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BScreen screen = (BScreen) (list.get(list.getFocus()));
                getBApp().push(screen, TRANSITION_LEFT);
                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                getBApp().setActive(false);
                return true;
            }

            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "Music";
        }
    }
    
    public class TGList extends BList {
        public TGList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            if (index == 4)
                icon.setResource(getResource("alerticon.png"));
            else
                icon.setResource(getResource("icon.png"));

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(get(index).toString());
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "push"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public class PathScreen extends DefaultScreen {
        private BList list;

        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H + 256;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public PathScreen(Music app, String name, String path) {
            super(app);

            setTitle(name);
            mPath = path;
            mName = name;
            
            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);
            
            FileSystemContainer fileSystemContainer = new FileSystemContainer(path);
            List files = fileSystemContainer.getItems();
            Iterator iterator = files.iterator();
            while (iterator.hasNext())
            {
                NameValue nameValue = (NameValue)iterator.next();
                File file = new File(nameValue.getValue());
                if (file.isDirectory())
                    list.add("> "+nameValue.getName());
                else
                {
                    Audio audio = (Audio)MediaManager.getMedia(nameValue.getValue());
                    list.add(new MusicScreen(app, audio.getTitle(), nameValue.getValue()));
                }
            }
            setFocusDefault(list);
        }
        
        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                BScreen screen = (BScreen) (list.get(list.getFocus()));
                getBApp().push(screen, TRANSITION_LEFT);
                return true;
            }
            return super.handleAction(view, action);
        }        

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return mName;
        }
        
        private String mPath;
        private String mName;

    }

    public class MusicScreen extends DefaultScreen {
        private BList list;

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public MusicScreen(Music app, String name, String path) {
            super(app);

            setTitle(name);
            mPath = path;
            mName = name;
            
            int start = top;
            
            Audio audio = (Audio)MediaManager.getMedia(mPath);
            
            BText titleText = new BText(normal, border_left, start, text_width, 20);
            titleText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
            titleText.setFont("default-18-bold.font");
            titleText.setShadow(true);
            titleText.setValue(audio.getTitle());
            
            start+= 30;
            
            BText artistText = new BText(normal, border_left, start, text_width, 20);
            artistText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
            artistText.setFont("default-18-bold.font");
            artistText.setShadow(true);
            artistText.setValue(audio.getArtist());
            
            start+= 30;
            
            BText albumText = new BText(normal, border_left, start, text_width, 20);
            albumText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
            albumText.setFont("default-18-bold.font");
            albumText.setShadow(true);
            albumText.setValue(audio.getAlbum());
            
            start+= 30;
            
            BText yearText = new BText(normal, border_left, start, text_width, 20);
            yearText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
            yearText.setFont("default-18-bold.font");
            yearText.setShadow(true);
            yearText.setValue(String.valueOf(audio.getDate()));
            
            start+= 30;
            
            BText genreText = new BText(normal, border_left, start, text_width, 20);
            genreText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
            genreText.setFont("default-18-bold.font");
            genreText.setShadow(true);
            genreText.setValue(audio.getGenre());
            
            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            return super.handleEnter(arg, isReturn);
        }
        
        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return mName;
        }
        
        private String mPath;
        private String mName;
    }
/*    

    public class LocalRadarScreen extends DefaultScreen {
        private BList list;

        public LocalRadarScreen(Music app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H * 2), height
                    - (SAFE_TITLE_V * 2));

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateImage();
        }

        private void updateImage() {
            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();

            try {
                if (mWeatherData.getLocalRadar() != null) {
                    java.awt.Image cached = Tools.retrieveCachedImage(new URL(mWeatherData.getLocalRadar()));
                    if (cached != null) {
                        //cached = cached.getScaledInstance(image.width, image.height, java.awt.Image.SCALE_SMOOTH);
                        //cached = Tools.getImage(cached);
                        image.setResource(cached);
                        return;
                    }
                }
            } catch (MalformedURLException ex) {
                log.error("Could not update weather local radar", ex);
            }

            below.setResource("background.jpg");
            image.setResource("NA.png");
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            try {
                updateImage();
            } catch (Exception ex) {
                log.error("Could not update weather text", ex);
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "Local Radar";
        }

        private BView image;

        WeatherData mWeatherData;
    }

    public class NationalRadarScreen extends DefaultScreen {
        private BList list;

        public NationalRadarScreen(Music app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H * 2), height
                    - (SAFE_TITLE_V * 2));

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateImage();
        }

        private void updateImage() {
            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();

            try {
                if (mWeatherData.getNationalRadar() != null) {
                    java.awt.Image cached = Tools.retrieveCachedImage(new URL(mWeatherData.getNationalRadar()));
                    if (cached != null) {
                        image.setResource(cached);
                        return;
                    }
                }
            } catch (MalformedURLException ex) {
                log.error("Could not update weather local radar", ex);
            }
            below.setResource("background.jpg");
            image.setResource("NA.png");
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            try {
                updateImage();
            } catch (Exception ex) {
                log.error("Could not update weather text", ex);
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "National Radar";
        }

        private BView image;

        WeatherData mWeatherData;
    }

    public class AlertsScreen extends DefaultScreen {
        private BList list;

        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public AlertsScreen(Music app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle("Alerts");

            int start = top;

            headlineText = new BText(normal, border_left, start, text_width, 30);
            headlineText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
            headlineText.setFont("default-24-bold.font");
            headlineText.setColor(new Color(150, 100, 100));
            headlineText.setShadow(Color.black, 3);

            start += 35;

            descriptionText = new BText(normal, border_left, start, text_width, 80);
            descriptionText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
            descriptionText.setFont("default-24-bold.font");
            descriptionText.setColor(new Color(127, 235, 192));
            descriptionText.setShadow(true);
            descriptionText.setValue(" ");

            BText copyrightText = new BText(normal, SAFE_TITLE_H, height - SAFE_TITLE_V - 18,
                    (width - (SAFE_TITLE_H * 2)), 20);
            copyrightText.setValue("weather.gov");
            copyrightText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
            copyrightText.setFont("default-18.font");

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2.5), 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateText();
        }

        private void updateText() {
            Iterator iterator = mWeatherData.getAlerts();
            if (iterator.hasNext()) {
                WeatherData.Alert alert = (WeatherData.Alert) iterator.next();
                headlineText.setValue(alert.getHeadline());
                descriptionText.setValue(alert.getDescription());
            }
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            try {
                updateText();
            } catch (Exception ex) {
                log.error("Could not update alerts text", ex);
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
            case KEY_RIGHT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_LEFT:
                // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "Alerts";
        }

        private BText headlineText;

        private BText descriptionText;

        WeatherData mWeatherData;
    }
*/
    public class OptionList extends BList {
        public OptionList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);

            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        protected void createRow(BView parent, int index) {
            BText text = new BText(parent, 10, 4, parent.width - 40, parent.height - 4);
            text.setShadow(true);
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setValue(get(index).toString());
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public static class MusicFactory extends AppFactory {

        public MusicFactory(AppContext appContext){
            super(appContext);
        }
        
        protected void init(ArgumentList args) {
            super.init(args);
            MusicConfiguration weatherConfiguration = (MusicConfiguration)getAppContext().getConfiguration();
        }
    }
}