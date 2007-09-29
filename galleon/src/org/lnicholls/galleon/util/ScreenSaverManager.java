package org.lnicholls.galleon.util;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeEventHandler;
import com.tivo.hme.sdk.util.Ticker;
import java.awt.Color;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConvertUtilsBean;
import org.apache.commons.beanutils.Converter;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.widget.ShadeScreenSaver;

public class ScreenSaverManager {
    private static final Log log = LogFactory.getLog(ScreenSaverManager.class);
    private static ScreenSaverManager manager = new ScreenSaverManager();
    
    private Map<BApplication, ScreenSaverListener> listenerMap = 
        new WeakHashMap<BApplication, ScreenSaverListener>();
    
    public void addApplication(BApplication app) {
        if (!listenerMap.containsKey(app)) {
            ScreenSaverListener listener = new ScreenSaverListener(app);
            app.addHandler(listener);
            listenerMap.put(app, listener);
        }
    }
    
    public void removeApplication(BApplication app) {
        ScreenSaverListener listener = listenerMap.remove(app);
        if (listener != null) {
            listener.stop();
            app.removeHandler(listener);
        }
    }

    public static ScreenSaverManager getManager() {
        return manager;
    }
    
    private static class ScreenSaverListener implements IHmeEventHandler, Ticker.Client {

        private BeanUtilsBean beanUtils;
        private BApplication app;
        private long idleStart;
        private boolean timing;
        private boolean sleeping;
        private boolean disposed;

        private ScreenSaver overriddenScreenSaver;
        private ScreenSaver screenSaver;

        public ScreenSaverListener(BApplication app) {
            this.app = app;
            idleStart = System.currentTimeMillis();

            ConvertUtilsBean convertUtils = new ConvertUtilsBean();
            convertUtils.register(new ColorConverter(), Color.class);
            beanUtils = new BeanUtilsBean(convertUtils, new PropertyUtilsBean());
            resetTimer();
        }
        
        public void stop() {
            disposed = true;
            Ticker.master.remove(this, null);
            if (sleeping) {
                wakeUp();
            }
        }
        
        public void postEvent(HmeEvent event) {
            
            if (event instanceof HmeEvent.Key) {
                ScreenSaver current = currentScreenSaver();
                if (sleeping && current != null && current.isWakeEvent(event)) {
                    wakeUp();
                }
                
                resetTimer();
            }
        }
        
        private ScreenSaver currentScreenSaver() {
            if (overriddenScreenSaver != null) {
                return overriddenScreenSaver;
            }
            return screenSaver;
        }
        
        public long tick(long tm, Object arg) {
            long millis = getRemainingIdle();
            if (millis == -1) {
                //abort
                return 0;
            }
            if (millis == 0) {
                timing = false;
                if (!sleeping) {
                    sleep();
                }
            }
            return System.currentTimeMillis() + millis;
        }

        protected void resetTimer() {
            if (!timing) {
                log.debug("Start idle timer");
                idleStart = System.currentTimeMillis();
                long remaining = getRemainingIdle();
                if (remaining > 0) {
                    timing = true;
                    if (!disposed) {
                        Ticker.master.add(this, remaining, null);
                    }
                } else if (!sleeping && !disposed) {
                    sleep();
                }
                
            } else {
                log.debug("Update idle timer");
                idleStart = System.currentTimeMillis();
            }
        }
        
        private long getRemainingIdle() {
            int interval = 120000;
            ScreenSaverConfiguration config = Server.getServer().
                getServerConfiguration().getScreenSaverConfiguration();
            if (config != null) {
                interval = config.getDuration() * 1000;
            }
            return Math.max(idleStart + interval - System.currentTimeMillis(), 0);
        }
        
        protected void wakeUp() {
            log.info("Disabling screen saver");
            sleeping = false;
            ScreenSaver current = currentScreenSaver();
            if (current != null) {
                current.deactivate();
            }
            //get rid of the temporary override
            overriddenScreenSaver = null;
        }
        
        protected void sleep() {
            if (app.isApplicationClosing()) {
                log.debug("ignoring sleep exiting application");
                return;
            }
            if (app.getCurrentScreen() == null) {
                return;
            }
            log.info("Enabling screen saver");
            sleeping = true;
            
            ScreenSaver screenSaver = this.screenSaver;
            BScreen screen = app.getCurrentScreen();
            if (screen instanceof ScreenSaverFactory) {
                //override the normal screen saver for this screen
                screenSaver = ((ScreenSaverFactory)screen).getScreenSaver();
                if (screenSaver != null) {
                    screenSaver.init(app);
                }
                overriddenScreenSaver = screenSaver;
            }
            if (screenSaver == null && screen instanceof ScreenSaver) {
                //override the normal screen saver for this screen
                screenSaver = (ScreenSaver)screen;
                screenSaver.init(app);
                overriddenScreenSaver = screenSaver;
            }
            
            if (screenSaver == null) {
                screenSaver = createScreenSaver();
                this.screenSaver = screenSaver;
            }
            if (screenSaver != null) {
                screenSaver.activate();
            }
        }
        
        @SuppressWarnings("unchecked")
        private ScreenSaver createScreenSaver() {
            ScreenSaverConfiguration config = Server.getServer().
                getServerConfiguration().getScreenSaverConfiguration();

            Map<String, String> settings = Collections.EMPTY_MAP;
            ScreenSaver screenSaver = null;
            try {
                if (config != null && config.getScreenSaverClass() != null) {
                    settings = config.getAttributeMap();
                    String cls = config.getScreenSaverClass();
                    screenSaver = (ScreenSaver)Class.forName(cls).newInstance();
                }
            } catch (Exception e) {
                if (log.isErrorEnabled()) {
                    log.error("Cannot create screensaver: " + config.getScreenSaverClass(), e);
                }
            }

            //use a default screen saver if we couldn't create the configured one
            if (screenSaver == null) {
                screenSaver = new ShadeScreenSaver();
                settings = Collections.EMPTY_MAP;
            }
            
            //set all the properties on the screen saver
            for (Map.Entry<String, String> entry : settings.entrySet()) {
                try {
                    beanUtils.setProperty(screenSaver, entry.getKey(), entry.getValue());
                } catch (Throwable e) {
                    log.warn("Unable to set bean property: " + entry.getKey() + "=" + entry.getValue(), e);
                }
            }
            screenSaver.init(app);
            return screenSaver;
        }
    }
    
    private static class ColorConverter implements Converter {

        public Object convert(Class cls, Object value) {
            if (value instanceof String) {
                String str = (String)value;
                if (str.charAt(0) =='#') {
                    int rgb = Integer.parseInt(str.substring(1), 16);
                    return new Color(rgb);
                } else {
                    return Color.getColor(str);
                }
            }
            return null;
        }
    }
}
