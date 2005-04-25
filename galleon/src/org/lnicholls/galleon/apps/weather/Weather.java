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

import java.awt.Color;
import java.awt.Font;
import java.awt.image.BufferedImage;
import java.io.*;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import javax.imageio.*;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.ScrollText;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlight;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.http.server.HttpRequest;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class Weather extends DefaultApplication {

    private static Logger log = Logger.getLogger(Weather.class.getName());

    public final static String TITLE = "Weather";

    private int mCurrent = 0;

    private static WeatherScreen[] screens = new WeatherScreen[5];

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mAlertIcon;

    private Resource mIcon;

    private Resource mNA;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mAlertIcon = getSkinImage("menu", "alert");
        mIcon = getSkinImage("menu", "item");
        mNA = getSkinImage(null, "na");

        push(new WeatherMenuScreen(this), TRANSITION_NONE);

        /*
         * WeatherData weatherData = ((WeatherFactory) context.factory).getWeatherData();
         * 
         * screens[0] = new CurrentConditionsScreen(this, weatherData); screens[1] = new ForecastScreen(this,
         * weatherData); screens[2] = new LocalRadarScreen(this, weatherData); screens[3] = new
         * NationalRadarScreen(this, weatherData); screens[4] = new AlertsScreen(this, weatherData);
         * 
         * push(screens[0], TRANSITION_NONE);
         */
    }

    public class WeatherScreen extends DefaultScreen {
        public WeatherScreen(Weather app) {
            super(app);

            this.setFocusable(true);

            BHighlights h = getHighlights();
            h.setWhisperingArrow(H_LEFT, A_LEFT + SAFE_TITLE_H, A_BOTTOM - SAFE_TITLE_V, "pop");
            h.setWhisperingArrow(H_RIGHT, A_RIGHT - SAFE_TITLE_H, A_BOTTOM - SAFE_TITLE_V, "right");

            setFocusDefault(this);
        }

        public boolean getHighlightIsVisible(int visible) {
            return visible == H_VIS_TRUE;
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            BHighlights h = getHighlights();
            BHighlight right = h.get(H_RIGHT);
            BHighlight left = h.get(H_LEFT);
            if (right != null && left != null) {
                if (mCurrent == 4)
                    right.setVisible(H_VIS_FALSE);
                else
                    right.setVisible(H_VIS_TRUE);
                if (mCurrent == 0)
                    left.setVisible(H_VIS_FALSE);
                else
                    left.setVisible(H_VIS_TRUE);
            }
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("right")) {
                if (mCurrent == 4) {
                    getBApp().play("thumbsdown.snd");
                    return true;
                }
                mCurrent = mCurrent + 1;
                push(screens[mCurrent], TRANSITION_LEFT);
                return true;
            } else if (action.equals("pop")) {
                if (mCurrent == 0) {
                    getBApp().play("pageup.snd");
                    getBApp().flush();
                    getBApp().setActive(false);
                }
                --mCurrent;
                pop();
                return true;
            }
            return super.handleAction(view, action);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public class WeatherMenuScreen extends DefaultMenuScreen {

        public WeatherMenuScreen(Weather app) {
            super(app, "Weather");

            below.setResource(mMenuBackground);

            WeatherData weatherData = ((WeatherFactory) context.factory).getWeatherData();

            mMenuList.add(new CurrentConditionsScreen(app, weatherData));
            mMenuList.add(new ForecastScreen(app, weatherData));
            mMenuList.add(new LocalRadarScreen(app, weatherData));
            mMenuList.add(new NationalRadarScreen(app, weatherData));
            if (weatherData.hasAlerts())
                mMenuList.add(new AlertsScreen(app, weatherData));
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                BScreen screen = (BScreen) (mMenuList.get(mMenuList.getFocus()));
                getBApp().push(screen, TRANSITION_LEFT);
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            if (index == 4)
                icon.setResource(mAlertIcon);
            else
                icon.setResource(mIcon);

            BText name = new BText(parent, 50, 4, parent.width - 40, parent.height - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(mMenuList.get(index).toString());
        }
    }

    public class CurrentConditionsScreen extends DefaultScreen {
        private final int top = SAFE_TITLE_V + 100;

        private final int border_left = SAFE_TITLE_H + 256;

        private final int text_width = width - border_left - (SAFE_TITLE_H);

        private WeatherList list;

        public CurrentConditionsScreen(Weather app, WeatherData data) {
            super(app);

            below.setResource(mInfoBackground);

            mWeatherData = data;

            setTitle("Current Conditions");

            int start = top;

            icon = new BView(normal, SAFE_TITLE_H, SAFE_TITLE_V + 30, 256, 256);
            icon.setResource(mNA);

            Resource font = createFont("Dekadens.ttf", Font.BOLD, 60);
            temperatureText = new BText(normal, border_left, SAFE_TITLE_V + 70, text_width - 70, 70);
            temperatureText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
            temperatureText.setFont(font);
            temperatureText.setColor(new Color(127, 235, 192));
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

            setFooter("weather.com");

            list = new WeatherList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            list.add("Press SELECT to go back");
            setFocusDefault(list);

            updateText();
        }

        private void updateText() {
            temperatureText.setValue(mWeatherData.getCurrentConditions().getTemperature());
            conditionsText.setValue(mWeatherData.getCurrentConditions().getConditions());
            icon.setResource(getSkinImage(null, pad(mWeatherData.getCurrentConditions().getIcon())));
            uvIndexText.setValue(mWeatherData.getCurrentConditions().getUltraVioletIndex() + " "
                    + mWeatherData.getCurrentConditions().getUltraVioletDescription());
            if (mWeatherData.getCurrentConditions().getWindDescription().toLowerCase().equals("calm"))
                windText.setValue(mWeatherData.getCurrentConditions().getWindDescription());
            else
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
            case KEY_LEFT: // TODO Why never gets this code?
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
        private WeatherList list;

        public ForecastScreen(Weather app, WeatherData data) {
            super(app);

            below.setResource(mInfoBackground);

            mWeatherData = data;

            setTitle("Forecast");

            int gap = 6;

            int dayWidth = (BODY_WIDTH - 4 * gap) / 5;

            for (int i = 0; i < 5; i++) {
                int start = TOP;

                int x = (dayWidth + gap / 2) * i;

                dayText[i] = new BText(normal, BORDER_LEFT + x, start, dayWidth, 20);
                dayText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                dayText[i].setFont("default-18-bold.font");
                dayText[i].setShadow(true);

                start = start + 20;

                icon[i] = new BView(normal, BORDER_LEFT + x, start, dayWidth, dayWidth);
                icon[i].setResource(mNA);

                start = start + dayWidth;

                hiText[i] = new BText(normal, BORDER_LEFT + x, start, dayWidth, 30);
                hiText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                hiText[i].setFont("default-24-bold.font");
                hiText[i].setShadow(true);

                start = start + 30;

                loText[i] = new BText(normal, BORDER_LEFT + x, start, dayWidth, 20);
                loText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP);
                loText[i].setFont("default-18-bold.font");
                loText[i].setShadow(true);

                start = start + 20;

                descriptionText[i] = new BText(normal, BORDER_LEFT + x, start, dayWidth, 60);
                descriptionText[i].setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
                descriptionText[i].setFont("default-18-bold.font");
                descriptionText[i].setColor(new Color(127, 235, 192));
                descriptionText[i].setShadow(true);
            }

            setFooter("weather.com");

            list = new WeatherList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            list.add("Press SELECT to go back");
            setFocusDefault(list);

            updateText();
        }

        private void updateText() {

            WeatherData.Forecasts forecasts = mWeatherData.getForecasts();

            int counter = 0;
            Iterator iterator = forecasts.getForecast();
            while (iterator.hasNext()) {
                try
                {
                    WeatherData.Forecast forecast = (WeatherData.Forecast) iterator.next();
                    WeatherData.Part dayPart = forecast.getDayForecast();
                    WeatherData.Part nightPart = forecast.getNightForecast();
                    ByteArrayOutputStream baos = Server.getServer().getSkin().getImage(Weather.this.getClass().getName(), null, pad(dayPart.getIcon()));
                    java.awt.Image image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray())).getScaledInstance(BODY_WIDTH / 5, BODY_WIDTH / 5, java.awt.Image.SCALE_SMOOTH);
                    //BufferedImage image = (BufferedImage) getSkinImage(null, pad(dayPart.getIcon())).getScaledInstance(
                    //        BODY_WIDTH / 5, BODY_WIDTH / 5, java.awt.Image.SCALE_SMOOTH);
                    //java.awt.Image image = Tools.getResourceAsImage(getClass(), pad(dayPart.getIcon()) + ".png")
                    //        .getScaledInstance(BODY_WIDTH / 5, BODY_WIDTH / 5, java.awt.Image.SCALE_SMOOTH);
                    image = (BufferedImage) Tools.getImage(image);
                    //icon[counter].setResource(getSkinImage(null, pad(dayPart.getIcon())), RSRC_IMAGE_BESTFIT);
                    icon[counter].setResource(createImage(image));
                    
                    dayText[counter].setValue(forecast.getDescription());
                    String value = forecast.getHigh();
                    if (value.equals("N/A"))
                        value = mWeatherData.getCurrentConditions().getTemperature();
                    hiText[counter].setValue(value);
                    loText[counter].setValue(forecast.getLow());
                    value = dayPart.getDescription();
                    if (value.equals("N/A"))
                        value = nightPart.getDescription();
                    descriptionText[counter].setValue(value);
                }
                catch (Exception ex)
                {
                    log.error("Could not update weather image", ex);    
                }

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
            case KEY_LEFT: // TODO Why never gets this code?
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

        private WeatherData mWeatherData;
    }

    public class LocalRadarScreen extends DefaultScreen {
        private WeatherList list;

        public LocalRadarScreen(Weather app, WeatherData data) {
            super(app);

            below.setResource(mInfoBackground);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H * 2), height
                    - (SAFE_TITLE_V * 2));

            list = new WeatherList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            list.add("Press SELECT to go back");
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

            image.setResource(mNA);
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
            case KEY_LEFT: // TODO Why never gets this code?
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
        private WeatherList list;

        public NationalRadarScreen(Weather app, WeatherData data) {
            super(app);

            below.setResource(mInfoBackground);

            mWeatherData = data;

            setTitle(" ");

            image = new BView(below, SAFE_TITLE_H, SAFE_TITLE_V, width - (SAFE_TITLE_H * 2), height
                    - (SAFE_TITLE_V * 2));

            list = new WeatherList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            list.add("Press SELECT to go back");
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
            image.setResource(mNA);
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
            case KEY_LEFT: // TODO Why never gets this code?
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
        private WeatherList list;

        public AlertsScreen(Weather app, WeatherData data) {
            super(app);

            below.setResource(mInfoBackground);

            mWeatherData = data;

            mDateFormat = new SimpleDateFormat();
            mDateFormat.applyPattern("EEE M/d hh:mm a");

            setTitle("Alerts");

            int start = TOP - 30;

            eventText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 30);
            eventText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
            eventText.setFont("default-24-bold.font");
            eventText.setColor(new Color(150, 100, 100));
            eventText.setShadow(Color.black, 3);

            start += 30;

            datesText = new BText(normal, BORDER_LEFT, start, BODY_WIDTH, 20);
            datesText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
            datesText.setFont("default-18-bold.font");
            eventText.setColor(new Color(150, 100, 100));
            datesText.setShadow(true);

            start += 25;

            scrollText = new ScrollText(normal, SAFE_TITLE_H, start, BODY_WIDTH - 10, height - 2 * SAFE_TITLE_V - 193,
                    "");

            setFocusDefault(scrollText);

            setFooter("weather.gov");

            list = new WeatherList(this.normal, SAFE_TITLE_H + 10, (height - SAFE_TITLE_V) - 55, (int) Math
                    .round((width - (SAFE_TITLE_H * 2)) / 2), 90, 35);
            list.add("Press SELECT to go back");
            setFocusDefault(list);

            updateText();
        }

        private void updateText() {
            Iterator iterator = mWeatherData.getAlerts();
            if (iterator.hasNext()) {
                WeatherData.Alert alert = (WeatherData.Alert) iterator.next();

                eventText.setValue(alert.getEvent() != null ? alert.getEvent() : alert.getHeadline());
                if (alert.getEffective() != null)
                    datesText.setValue(mDateFormat.format(alert.getEffective()) + " to "
                            + mDateFormat.format(alert.getExpires()));
                scrollText.setText(alert.getDescription());
                scrollText.flush();
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
            case KEY_LEFT: // TODO Why never gets this code?
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_UP:
            case KEY_DOWN:
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                return scrollText.handleKeyPress(code, rawcode);
            }
            return super.handleKeyPress(code, rawcode);
        }

        public String toString() {
            return "Alerts";
        }

        private BText eventText;

        private BText datesText;

        private ScrollText scrollText;

        private SimpleDateFormat mDateFormat;

        WeatherData mWeatherData;
    }

    class WeatherList extends DefaultOptionList {
        public WeatherList(BView parent, int x, int y, int width, int height, int rowHeight) {
            super(parent, x, y, width, height, rowHeight);

            setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                getBApp().play("select.snd");
                getBApp().flush();
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public static class WeatherFactory extends AppFactory {
        WeatherData weatherData = null;

        public WeatherFactory(AppContext appContext) {
            super(appContext);
        }

        protected void init(ArgumentList args) {
            super.init(args);
            WeatherConfiguration weatherConfiguration = (WeatherConfiguration) getAppContext().getConfiguration();
            weatherData = new WeatherData(weatherConfiguration.getId(), weatherConfiguration.getCity(),
                    weatherConfiguration.getState(), weatherConfiguration.getZip(), 512, 384); // TODO get real
            // dimensions
        }

        public void handleHTTP(HttpRequest http, String uri) throws IOException {
            if (uri.equals("icon.png")) {
                if (weatherData.hasAlerts()) {
                    super.handleHTTP(http, "alerticon.png");
                    return;
                }
            }
            super.handleHTTP(http, uri);
        }

        public InputStream getStream(String uri) throws IOException {
            if (uri.toLowerCase().equals("alerticon.png")) {
                return getImage("alert");
            }

            return super.getStream(uri);
        }

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