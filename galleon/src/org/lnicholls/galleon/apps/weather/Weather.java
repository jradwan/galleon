package org.lnicholls.galleon.apps.weather;

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
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.apps.togo.ToGo.ToGoScreen.OptionList;
import org.lnicholls.galleon.apps.weather.WeatherData.Forecasts;
import org.lnicholls.galleon.database.*;

import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.share.*;
import com.tivo.hme.http.server.*;

import org.apache.log4j.Logger;

import org.apache.commons.beanutils.PropertyUtils;

/**
 * Based on TiVo Bananas sample code by Carl Haynes
 */

public class Weather extends BApplication {

    private static Logger log = Logger.getLogger(Weather.class.getName());

    public final static String TITLE = "Weather";

    protected void init(Context context) {
        super.init(context);

        push(new WeatherMenuScreen(this), TRANSITION_NONE);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            return true;
        }
        return super.handleAction(view, action);
    }

    public class DefaultScreen extends BScreen {
        public DefaultScreen(Weather app) {
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

    public class WeatherMenuScreen extends DefaultScreen {
        private TGList list;

        public WeatherMenuScreen(Weather app) {
            super(app);
            setTitle("Weather");

            list = new TGList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 290, width
                    - ((SAFE_TITLE_H * 2) + 32), 280, 35);
            BHighlights h = list.getHighlights();
            h.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
            h.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

            WeatherData weatherData = ((WeatherFactory) context.factory).getWeatherData();
            System.out.println(weatherData);

            list.add(new CurrentConditionsScreen(app, weatherData));
            list.add(new ForecastScreen(app, weatherData));
            list.add(new LocalRadarScreen(app, weatherData));
            list.add(new NationalRadarScreen(app, weatherData));
            //list.add(new AlertsScreen(app, weatherData));

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
            return "ToGo";
        }

        public class TGList extends BList {
            protected SimpleDateFormat mDateFormat;

            protected GregorianCalendar mCalendar;

            public TGList(BView parent, int x, int y, int width, int height, int rowHeight) {
                super(parent, x, y, width, height, rowHeight);

                mDateFormat = new SimpleDateFormat();
                mDateFormat.applyPattern("EEE M/dd");
                mCalendar = new GregorianCalendar();

                setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
            }

            protected void createRow(BView parent, int index) {
                BView icon = new BView(parent, 10, 3, 30, 30);
                /*
                 * Video video = ((ToGoScreen) get(index)).getVideo(); if
                 * (video.getIcon().equals("in-progress-recording")) icon.setResource(mRedIcon); else if
                 * (video.getIcon().equals("expires-soon-recording")) icon.setResource(mYellowIcon); else if
                 * (video.getIcon().equals("expired-recording")) icon.setResource(mYellowExclamationIcon); else if
                 * (video.getIcon().equals("save-until-i-delete-recording")) icon.setResource(mGreenIcon); else
                 * icon.setResource(mEmptyIcon);
                 */

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
    }

    public class CurrentConditionsScreen extends DefaultScreen {
        private BList list;

        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H + 256;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public CurrentConditionsScreen(Weather app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle("Current Conditions");

            int start = top;

            icon = new BView(normal, SAFE_TITLE_H, SAFE_TITLE_V + 30, 256, 256);
            icon.setResource("NA.png");

            Resource font = createFont("casual.ttf", Font.BOLD, 60);
            temperatureText = new BText(normal, border_left, SAFE_TITLE_V + 70, text_width - 70, 70);
            temperatureText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            temperatureText.setFont(font);
            temperatureText.setColor(Color.GREEN);
            temperatureText.setShadow(Color.black, 3);

            conditionsText = new BText(normal, SAFE_TITLE_H, SAFE_TITLE_V + 280, 256, 80);
            conditionsText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
            conditionsText.setFont("default-24-bold.font");
            conditionsText.setColor(new Color(127, 235, 192));
            conditionsText.setShadow(true);
            conditionsText.setValue("Snowing");

            start += 70;

            BText labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("UV Index:");

            uvIndexText = new BText(normal, border_left, start, text_width, 30);
            uvIndexText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            uvIndexText.setFont("default-18-bold.font");
            uvIndexText.setShadow(true);

            start += 25;

            labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("Wind:");

            windText = new BText(normal, border_left, start, text_width, 30);
            windText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            windText.setFont("default-18-bold.font");
            windText.setShadow(true);

            start += 25;

            labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("Humidity:");

            humidityText = new BText(normal, border_left, start, text_width, 30);
            humidityText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            humidityText.setFont("default-18-bold.font");
            humidityText.setShadow(true);

            start += 25;

            labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("Pressure:");

            pressureText = new BText(normal, border_left, start, text_width, 30);
            pressureText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            pressureText.setFont("default-18-bold.font");
            pressureText.setShadow(true);

            start += 25;

            labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("Dew Point:");

            dewPointText = new BText(normal, border_left, start, text_width, 30);
            dewPointText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            dewPointText.setFont("default-18-bold.font");
            dewPointText.setShadow(true);

            start += 25;

            labelText = new BText(normal, border_left, start, text_width, 30);
            labelText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
            labelText.setFont("default-18-bold.font");
            labelText.setShadow(true);
            labelText.setValue("Visibility:");

            visibilityText = new BText(normal, border_left, start, text_width, 30);
            visibilityText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
            visibilityText.setFont("default-18-bold.font");
            visibilityText.setShadow(true);

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateText();
        }

        private void updateText() {
            temperatureText.setValue(mWeatherData.getCurrentConditions().getTemperature());
            conditionsText.setValue(mWeatherData.getCurrentConditions().getConditions());
            icon.setResource(pad(mWeatherData.getCurrentConditions().getIcon()) + ".png");
            uvIndexText.setValue(mWeatherData.getCurrentConditions().getUltraVioletIndex() + " "
                    + mWeatherData.getCurrentConditions().getUltraVioletDescription());
            windText.setValue("From " + mWeatherData.getCurrentConditions().getWindDescription() + " at "
                    + mWeatherData.getCurrentConditions().getWindSpeed() + " " + mWeatherData.getSpeedUnit());
            humidityText.setValue(mWeatherData.getCurrentConditions().getHumidity() + "%");
            pressureText.setValue(mWeatherData.getCurrentConditions().getBarometricPressure() + " "
                    + mWeatherData.getPressureUnit() + ".");
            dewPointText.setValue(mWeatherData.getCurrentConditions().getDewPoint() + "\u00BA"
                    + mWeatherData.getTemperatureUnit());
            visibilityText.setValue(mWeatherData.getCurrentConditions().getVisibility() + " "
                    + mWeatherData.getDistanceUnit() + ".");
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            try {
                updateText();
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
            return "Current Conditions";
        }

        private BView icon;

        private BText temperatureText;

        private BText conditionsText;

        private BText uvIndexText;

        private BText windText;

        private BText humidityText;

        private BText pressureText;

        private BText dewPointText;

        private BText visibilityText;

        WeatherData mWeatherData;
    }

    public class ForecastScreen extends DefaultScreen {
        private BList list;

        private final int top = SAFE_TITLE_V + 80;

        private final int border_left = SAFE_TITLE_H;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        public ForecastScreen(Weather app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle("Forecast");

            int dayWidth = text_width / 5;

            for (int i = 0; i < 5; i++) {
                int start = top;

                dayText[i] = new BText(normal, border_left + dayWidth * i, start, dayWidth, 20);
                dayText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                dayText[i].setFont("default-18-bold.font");
                dayText[i].setShadow(true);

                start = start + 20;
                
                icon[i] = new BView(normal, border_left + dayWidth * i, start, dayWidth, dayWidth);
                icon[i].setResource("NA.png");

                start = start + dayWidth;

                hiText[i] = new BText(normal, border_left + dayWidth * i, start, dayWidth, 30);
                hiText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                hiText[i].setFont("default-24-bold.font");
                hiText[i].setShadow(true);

                start = start + 30;

                loText[i] = new BText(normal, border_left + dayWidth * i, start, dayWidth, 20);
                loText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                loText[i].setFont("default-18-bold.font");
                loText[i].setShadow(true);

                start = start + 20;

                descriptionText[i] = new BText(normal, border_left + dayWidth * i, start, dayWidth, 60);
                descriptionText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
                descriptionText[i].setFont("default-18-bold.font");
                descriptionText[i].setColor(new Color(127, 235, 192));
                descriptionText[i].setShadow(true);
            }

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateText();
        }

        private void updateText() {

            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();

            int counter = 0;
            Iterator iterator = forecasts.getForecast();
            while (iterator.hasNext()) {
                WeatherData.Forecast forecast = (WeatherData.Forecast) iterator.next();
                WeatherData.Part dayPart = forecast.getDayForecast();
                java.awt.Image image = Tools.getResourceAsImage(getClass(), pad(dayPart.getIcon()) + ".png")
                        .getScaledInstance(text_width / 5, text_width / 5, java.awt.Image.SCALE_SMOOTH);
                image = Tools.getImage(image);
                icon[counter].setResource(image);

                dayText[counter].setValue(forecast.getDescription());
                hiText[counter].setValue(forecast.getHigh());
                loText[counter].setValue(forecast.getLow());
                descriptionText[counter].setValue(dayPart.getDescription());

                counter = counter + 1;
            }
        }

        private String pad(String value) {
            if (value.length() == 0)
                return "00";
            if (value.length() == 1)
                return "0" + value;
            return value;
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            try {
                updateText();
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
            return "Forecast";
        }

        private BView[] icon = new BView[5];

        private BText[] dayText = new BText[5];

        private BText[] hiText = new BText[5];

        private BText[] loText = new BText[5];

        private BText[] descriptionText = new BText[5];

        WeatherData mWeatherData;
    }
    
    public class LocalRadarScreen extends DefaultScreen {
        private BList list;

        public LocalRadarScreen(Weather app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H*2), height - (SAFE_TITLE_V*2));

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateImage();
        }

        private void updateImage() {
            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();
            
            try {
                if (mWeatherData.getLocalRadar()!=null)
                {
                    java.awt.Image cached = Tools.retrieveCachedImage(new URL(mWeatherData.getLocalRadar()));
                    if (cached!=null)
                    {
                        cached = cached.getScaledInstance(image.width, image.height, java.awt.Image.SCALE_SMOOTH);
                        cached = Tools.getImage(cached);
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

        public NationalRadarScreen(Weather app, WeatherData data) {
            super(app);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H*2), height - (SAFE_TITLE_V*2));

            list = new OptionList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 50,
                    (width - (SAFE_TITLE_H * 2)) / 2, 90, 35);
            list.add("Return to menu");

            setFocusDefault(list);

            updateImage();
        }

        private void updateImage() {
            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();
            
            try {
                if (mWeatherData.getNationalRadar()!=null)
                {
                    java.awt.Image cached = Tools.retrieveCachedImage(new URL(mWeatherData.getNationalRadar()));
                    if (cached!=null)
                    {
                        cached = cached.getScaledInstance(image.width, image.height, java.awt.Image.SCALE_SMOOTH);
                        cached = Tools.getImage(cached);
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

    public static class WeatherFactory extends Factory {
        WeatherData weatherData = null;
        
        static int counter = 0;

        /**
         * Create the factory - scan the folder.
         */
        protected void init(ArgumentList args) {
            weatherData = new WeatherData("Nashua", "NH", "03060");
        }
/*        
        public InputStream getStream(String uri) throws IOException
        {
            System.out.println("uri="+uri);
            return super.getStream(uri);    
        }

        public void handleHTTP(HttpRequest http, String uri) throws IOException {
            if (uri.equals("icon.png")) {
                ImageInputStream in = null;
                try {
                    InputStream is = getClass().getResourceAsStream("/"+pad(String.valueOf(counter++)) + ".png");
                    if (is != null) {
                        in = ImageIO.createImageInputStream(is);
                    }
                    else
                    {
                        super.handleHTTP(http, uri);
                        return;
                    }
                } catch (IOException e) {
                    http.reply(404, e.getMessage());
                    return;
                }

                try {
                    long offset = 0;
                    String range = http.get("Range");
                    if ((range != null) && range.startsWith("bytes=") && range.endsWith("-")) {
                        try {
                            offset = Long.parseLong(range.substring(6, range.length() - 1));
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }

                    if (!http.getReplied()) {
                        if (offset == 0) {
                            http.reply(200, "Media follows");
                        } else {
                            http.reply(206, "Partial Media follows");
                        }
                    }
                    
                    String ct = null;
                    if (uri.endsWith(".mpeg") || uri.endsWith(".mpg")) {
                        ct = "video/mpeg";
                    } else if (uri.endsWith(".mp3")) {
                        ct = "audio/mp3";
                    }
                    if (ct != null) {
                        http.addHeader("Content-Type", ct);
                    }

                    if (offset > 0) {
                        long total = in.length();
                        http.addHeader("Content-Range", "bytes " + offset + "-" + (total-1) + "/" + total);
                        //in.skip(offset);
                    }
                        
                    addHeaders(http, uri);

                    if (http.get("http-method").equalsIgnoreCase("HEAD")) {
                        return;
                    }
                    
                    OutputStream out = http.getOutputStream(in.length());
                    byte data[] = new byte[IHttpConstants.TCP_BUFFER_SIZE];
                    int n;
                    while ((n = in.read(data, 0, data.length)) > 0) {
                        out.write(data, 0, n);
                    }
                } finally {
                    in.close();
                }
            }
            else
                super.handleHTTP(http, uri);
        }
        */

        public WeatherData getWeatherData() {
            return weatherData;
        }
    }
    

    private static String pad(String value) {
        if (value.length() == 0)
            return "00";
        if (value.length() == 1)
            return "0" + value;
        return value;
    }
}