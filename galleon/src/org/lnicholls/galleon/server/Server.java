package org.lnicholls.galleon.server;

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
import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.rmi.registry.Registry;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.*;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;

import org.apache.log4j.AsyncAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.RollingFileAppender;
import org.apache.log4j.xml.DOMConfigurator;
import org.lnicholls.galleon.util.Configurator;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.app.*;
import org.lnicholls.galleon.database.HibernateUtil;
import org.lnicholls.galleon.database.NetworkServerManager;
import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.togo.*;

import java.rmi.server.*;
import java.rmi.*;
import java.rmi.registry.*;


/*
 * Main class. Called by service wrapper to initialise and start Galleon.
 */

public class Server {
    public Server() {
        mServer = this;

        try {
            ArrayList errors = new ArrayList();
            setup(errors);
            
            log = setupLog(Server.class.getName(), "TraceFile");
            
            for (int i=0;i<errors.size();i++)
                log.error(errors.get(i));
            
            mRegistry = LocateRegistry.createRegistry(1099);
            mRegistry.bind("serverControl", new ServerControlImpl());             
            
            mServerConfiguration = new ServerConfiguration();
            
            // Log the system properties
            printSystemProperties();
            
            log.info("Galleon version="+Tools.getVersion());

            // Redirect standard out; some third-party libraries use this for error logging
            // TODO
            //Tools.redirectStandardStreams();
            
        } catch (Exception ex) {
            Tools.logException(Server.class, ex);
        }
    }
    
    public static void setup(ArrayList errors) {
        System.setProperty("os.user.home", System.getProperty("user.home"));
        
        System.setProperty("http.agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1)");

        try {
            File file = new File(".");
            
            if (System.getProperty("root") == null)
                System.setProperty("root", file.getAbsolutePath() + "/..");
            
            File check = new File(System.getProperty("root"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: root="+System.getProperty("root"));

            System.setProperty("user.home", System.getProperty("root"));

            if (System.getProperty("conf") == null)
                System.setProperty("conf", file.getAbsolutePath() + "/../conf");
            
            check = new File(System.getProperty("conf"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: conf="+System.getProperty("conf"));            

            if (System.getProperty("cache") == null)
                System.setProperty("cache", file.getAbsolutePath() + "/../conf");
            
            check = new File(System.getProperty("cache"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: cache="+System.getProperty("cache"));            
            
            if (System.getProperty("data") == null)
                System.setProperty("data", file.getAbsolutePath() + "/../data");
            
            check = new File(System.getProperty("data"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: data="+System.getProperty("data"));            
            
            if (System.getProperty("apps") == null)
                System.setProperty("apps", file.getAbsolutePath() + "/../apps");
            
            check = new File(System.getProperty("apps"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: apps="+System.getProperty("apps"));            

            if (System.getProperty("logs") == null)
                System.setProperty("logs", file.getAbsolutePath() + "/../logs");
            
            check = new File(System.getProperty("logs"));
            if (!check.exists() || !check.isDirectory())
                errors.add("Invalid system propery: logs="+System.getProperty("logs"));            
            
        } catch (Exception ex) {
            Tools.logException(Server.class, ex);
        }        
    }
    
    public static Logger setupLog(String name, String appenderName) {
        /* Make a logs directory if one doesn't exist */
        File dir = new File(System.getProperty("logs"));
        if (!dir.exists()) {
            dir.mkdirs();
        }
        try
        {
            DOMConfigurator.configureAndWatch(System.getProperty("conf") + "/log4j.xml", 60000);
        }
        catch (Exception ex) {}
        Logger log = Logger.getLogger(name);

        // Start with a new log file with each restart
        Logger root = Logger.getRootLogger();
        AsyncAppender asyncAppender = (AsyncAppender) root.getAppender("AsyncTrace");
        if (asyncAppender != null) {
            RollingFileAppender rollingFileAppender = (RollingFileAppender) asyncAppender.getAppender(appenderName);
            if (rollingFileAppender != null) {
                rollingFileAppender.setFile(System.getProperty("logs")+"/"+Constants.LOG_FILE);
                rollingFileAppender.rollOver();
            }
        }
        
        return log;
    }    

    // Service wrapper required method
    public Integer start() {
        if (log.isDebugEnabled())
            log.debug("start()");
        try {
            // Start the database
            NetworkServerManager.initialize();
            
            HibernateUtil.initialize();
            if (NetworkServerManager.findSchema())
                HibernateUtil.updateSchema();
            else
                HibernateUtil.createSchema();
            
            // Start time task for period operations such as internet downloads
            mLongTermTimer = new Timer();
            mShortTermTimer = new Timer();

            // Load apps
            mAppManager = new AppManager();
            
            // Read the conf/configure.xml file
            mConfigurator = new Configurator(mServerConfiguration);
            mConfigurator.load(mAppManager);
            
            mPort = getConfiguredPort();

            // Create a port listener
            //findAvailablePort();

            preLoadFonts();
            
            //mAppManager.startPlugins();
            
            // Start the Media Manager refresh thread
            //scheduleLongTerm(new MediaManager.RefreshTask(), 3); //60*24);
            //MediaManager.addPath(new MediaRefreshThread.PathInfo("d:/download/mp3",FileFilters.audioFilter));
            
            try {
                mToGoThread = new ToGoThread(this);
                mToGoThread.start();
                mDownloadThread = new DownloadThread(this);
                mDownloadThread.start();
            } catch (Exception ex) {
                Tools.logException(Server.class, ex);

                mToGoThread = null;
                mDownloadThread = null;
            }            
            
        } catch (Exception ex) {
            Tools.logException(Server.class, ex);
            return new Integer(1);
        }
        return null;
    }

    // Service wrapper required method
    public void stop() {
        if (log.isDebugEnabled())
            log.debug("stop()");
        try {
            /*
            if (mPluginManager != null) {
                mPluginManager.destroyAllPlugins();
                mPluginManager = null;
            }
            */
            
            if (mToGoThread != null) {
                mToGoThread.interrupt();
                mToGoThread = null;
            }

            if (mDownloadThread != null) {
                mDownloadThread.interrupt();
                mDownloadThread = null;
            }
            
            NetworkServerManager.shutdown();
        } catch (Exception ex) {
            mToGoThread = null;
            mDownloadThread = null;
            
            System.runFinalization();
            Tools.logException(Server.class, ex);
        }
    }

    public void setReload(int reload) {
        mServerConfiguration.setReload(reload);
    }

    public int getReload() {
        return mServerConfiguration.getReload();
    }

    // Used by FileSystemTiVoContainer to determine if items or container should be shuffled
    public void setShuffleItems(boolean shuffleItems) {
        mServerConfiguration.setShuffleItems(shuffleItems);
    }

    public boolean getShuffleItems() {
        return mServerConfiguration.getShuffleItems();
    }

    // Used by ImageTiVoContainer to determine if thumbnails should be generated at startup
    public void setGenerateThumbnails(boolean generateThumbnails) {
        mServerConfiguration.setGenerateThumbnails(generateThumbnails);
    }

    public boolean getGenerateThumbnails() {
        return mServerConfiguration.getGenerateThumbnails();
    }

    //	Used by AudioPlaylistTiVoItem to determine if proxy should be used for streaming stations
    public void setUseStreamingProxy(boolean useStreamingProxy) {
        mServerConfiguration.setUseStreamingProxy(useStreamingProxy);
    }

    public boolean getUseStreamingProxy() {
        return mServerConfiguration.getUseStreamingProxy();
    }

    private void printSystemProperties() {
        Properties properties = System.getProperties();
        Enumeration Enumeration = properties.propertyNames();
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String propertyName = (String) e.nextElement();
            log.info(propertyName + "=" + System.getProperty(propertyName));
        }
    }

    private void printServerProperties() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();

            log.info("Galleon Version=" + getVersion());
            System.setProperty("version", getVersion());
            log.info("Local IP=" + inetAddress.getHostAddress());
            log.info("Host=" + inetAddress.getHostName());
            System.setProperty("host", inetAddress.getHostName());

            Tools.logMemory();
        } catch (UnknownHostException ex) {
            Tools.logException(Server.class, ex);
        }
    }

    public synchronized void reset() {
        if (log.isDebugEnabled())
            log.debug("reset()");
        log.info("Server update");
        try {
            if (mLongTermTimer != null) {
                mLongTermTimer.cancel();
                mLongTermTimer = null;
            }
        } catch (IllegalStateException ex) {
            Tools.logException(Server.class, ex);
        }
        mLongTermTimer = new Timer();
        try {
            if (mShortTermTimer != null) {
                mShortTermTimer.cancel();
                mShortTermTimer = null;
            }
        } catch (IllegalStateException ex) {
            Tools.logException(Server.class, ex);
        }
        mShortTermTimer = new Timer();        
    }

    public synchronized void reconfigure() {
        if (log.isDebugEnabled())
            log.debug("reconfigure()");        
        // Reset timer
        reset();

        // Load plugins
        //mPluginManager.destroyAllPlugins();
        //mPluginManager = null;
        //mPluginManager = new PluginManager(true);

        mServerConfiguration = null;
        mServerConfiguration = new ServerConfiguration();

        // Read the conf/configure.xml file
        mConfigurator = null;
        mConfigurator = new Configurator(mServerConfiguration);
        mConfigurator.load(mAppManager);
        
        //mAppManager.startPlugins();
    }

    public void setIPAddress(String ipaddress) {
        mServerConfiguration.setIPAddress(ipaddress);
    }

    public String getIPAddress() {
        return mServerConfiguration.getIPAddress();
    }

    public void setNetMask(String netMask) {
        mServerConfiguration.setNetMask(netMask);
    }

    public String getNetMask() {
        return mServerConfiguration.getNetMask();
    }

    public void setPort(int port) {
        mPort = port;
    }

    public int getPort() {
        return mPort;
    }

    public void setConfiguredPort(int port) {
        mServerConfiguration.setConfiguredPort(port);
    }

    public int getConfiguredPort() {
        return mServerConfiguration.getConfiguredPort();
    }

    public void setName(String name) {
        mServerConfiguration.setName(name);
    }

    public String getName() {
        return mServerConfiguration.getName();
    }

    public void setVersion(String version) {
        mServerConfiguration.setVersion(version);
    }

    public String getVersion() {
        return mServerConfiguration.getVersion();
    }

    // Singleton pattern
    public static synchronized Server getServer() {
        if (mServer == null) {
            mServer = new Server();
            mServer.start();
        }
        return mServer;
    }

    public void save() {
        if (log.isDebugEnabled())
            log.debug("save()");
        try {
            mConfigurator.save(mAppManager);
            mLastModified = System.currentTimeMillis();
        } catch (Exception e) {
            Tools.logException(Server.class, e);
        }
    }

    public synchronized void scheduleLongTerm(TimerTask task, long time) {
        if (log.isDebugEnabled())
            log.debug("Server schedule long term: " + task + " for " + time);
        if (time <= 0)
            time = getReload();
        try {
            mLongTermTimer.schedule(task, 1000 * 30, time * 1000 * 60);
        } catch (IllegalStateException ex) {
            Tools.logException(Server.class, ex);
            // Try again...
            reset();
            try {
                mLongTermTimer.schedule(task, 1000 * 30, time * 1000 * 60);
            } catch (IllegalStateException ex2) {
                Tools.logException(Server.class, ex2);
            }
        }
    }
    
    public synchronized void scheduleShortTerm(TimerTask task, long time) {
        if (log.isDebugEnabled())
            log.debug("Server schedule short term: " + task + " for " + time);
        if (time <= 0)
            time = getReload();
        try {
            mShortTermTimer.schedule(task, 1000 * 30, time * 1000 * 60);
        } catch (IllegalStateException ex) {
            Tools.logException(Server.class, ex);
            // Try again...
            reset();
            try {
                mShortTermTimer.schedule(task, 1000 * 30, time * 1000 * 60);
            } catch (IllegalStateException ex2) {
                Tools.logException(Server.class, ex2);
            }
        }
    }    

    private void findAvailablePort() {
        if (log.isDebugEnabled())
            log.debug("findAvailablePort()");
        boolean found = false;
        boolean configurationChanged = false;
        int port = getPort();
        while (!found) {
            try {
                if (log.isDebugEnabled())
                    log.debug("Trying port " + port);
                ServerSocket serverSocket = new ServerSocket(port);
                found = true;
                serverSocket.close();
                serverSocket = null;
            } catch (Exception ex) {
                configurationChanged = true;
                if (log.isDebugEnabled())
                    log.debug("Port " + port + " is already in use.");
                port = port + 1;
            }
        }
        if (configurationChanged) {
            if (log.isDebugEnabled())
                log.debug("Changed server port to " + port);
            setPort(port);
        }
        log.info("Using port " + port);
    }

    private void preLoadFonts() {
        if (log.isDebugEnabled())
            log.debug("preLoadFonts()");
        Font font = new Font("Arial", Font.BOLD, 50);
        font = new Font("Helvetica", Font.BOLD, 40);
        font = new Font("Courier", Font.BOLD, 40);
    }

    /**
     * @return last date configuration was modified
     */
    public long getLastModified() {
        return mLastModified;
    }

    public AppManager getAppManager()
    {
        return mAppManager;
    }
    
    public ToGoThread getToGoThread() {
        return mToGoThread;
    }

    public DownloadThread getDownloadThread() {
        return mDownloadThread;
    }
    
    public ServerConfiguration getServerConfiguration() {
        return mServerConfiguration;
    }    
    
    public static void main(String args[]) {
        Server server = getServer();
    }

    private Logger log;

    private static Server mServer;

    private int mPort = 8081;

    private Timer mLongTermTimer;
    private Timer mShortTermTimer;

    private Configurator mConfigurator;

    private long mLastModified = System.currentTimeMillis();

    private ServerConfiguration mServerConfiguration;
    
    private AppManager mAppManager;
    
    private ToGoThread mToGoThread;

    private DownloadThread mDownloadThread;
    
    private static Registry mRegistry;
}