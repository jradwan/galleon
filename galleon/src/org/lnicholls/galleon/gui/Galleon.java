package org.lnicholls.galleon.gui;

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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.net.*;
import java.util.Properties;
import java.net.URL;
import java.net.URLClassLoader;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppDescriptor;
import org.lnicholls.galleon.app.AppManager;
import org.lnicholls.galleon.server.Constants;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.server.ServerControl;
import org.lnicholls.galleon.togo.RulesList;
import org.lnicholls.galleon.util.Configurator;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.database.*;

import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.Options;

public final class Galleon implements Constants {

    static class SplashWindow extends JWindow {
        public SplashWindow() {
            super((Frame) null);

            //Image image = Tools.getResourceAsImage(Galleon.class, "galleon.png");
            //image = Tools.getImage(image);

            URL url = getClass().getClassLoader().getResource("galleon.png");
            ImageIcon logo = new ImageIcon(url);
            //ImageIcon logo = new ImageIcon(image);

            JLabel l = new JLabel(logo);
            l.setBackground(java.awt.Color.black);
            getContentPane().add(l, BorderLayout.CENTER);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
            pack();
        }
    }

    private static SplashWindow splashWindow = new SplashWindow();

    public Galleon() {
        createAndShowGUI();
    }

    private static void createAndShowGUI() {
        try {
            System.setProperty("os.user.home", System.getProperty("user.home"));

            ArrayList errors = new ArrayList();
            Server.setup(errors);
            log = Server.setupLog(Galleon.class.getName(), "GuiFile");
            //log = Logger.getLogger(Galleon.class.getName());
            printSystemProperties();

            UIManager.put("ClassLoader", (com.jgoodies.plaf.LookUtils.class).getClassLoader());
            UIManager.put("Application.useSystemFontSettings", Boolean.TRUE);
            Options.setGlobalFontSizeHints(FontSizeHints.MIXED2);
            Options.setDefaultIconSize(new Dimension(18, 18));
            try {
                UIManager.setLookAndFeel(LookUtils.IS_OS_WINDOWS_XP ? "com.jgoodies.plaf.plastic.PlasticXPLookAndFeel"
                        : Options.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                Tools.logException(Galleon.class, e);
            }

            /*
            mServerConfiguration = new ServerConfiguration();
            if (mConfigureDir != null) {
                log.info("Configuration Dir=" + mConfigureDir.getAbsolutePath());
                new Configurator(mServerConfiguration).load(mAppManager, mConfigureDir);
            } else
                new Configurator(mServerConfiguration).load(mAppManager);
            mAddress = mServerConfiguration.getIPAddress();
            if (mAddress == null || mAddress.length() == 0)
                mAddress = "127.0.0.1";
*/                
            mRegistry = LocateRegistry.getRegistry(mServerAddress, 1099);
            
            File directory = new File(System.getProperty("apps"));
            if (!directory.exists() || !directory.isDirectory()) {
                String message = "App Class Loader directory not found: " + System.getProperty("apps");
                InstantiationException exception = new InstantiationException(message);
                log.error(message, exception);
                throw exception;
            }
            
            File[] files = directory.listFiles(new FileFilter() {
                public final boolean accept(File file) {
                    return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
                }
            });
            URL urlList[] = new URL[files.length];
            for (int i = 0; i < files.length; ++i) {
                try {
                    urlList[i] = files[i].toURL();
                } catch (Exception ex) {
                    // should never happen
                }
            }

            URLClassLoader classLoader = new URLClassLoader(urlList);
            Thread.currentThread().setContextClassLoader(classLoader);
            
            mRulesList = new RulesList();

            //mMainFrame = new MainFrame(mServerConfiguration.getVersion());
            mMainFrame = new MainFrame("0.0.0 beta 2");

            splashWindow.setVisible(false);
            mMainFrame.setVisible(true);

            /*
             * javax.swing.SwingUtilities.invokeLater(new Runnable() { public void run() { mToGo.getRecordings(); } });
             */

        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        // TODO Get everything throug RMI
        if (args.length > 0) {
            File configureDir = new File(args[0]);
            if (configureDir.exists() && configureDir.isDirectory())
                mConfigureDir = configureDir;
            if (args.length > 1)
                mServerAddress = args[1];
        }

        splashWindow.setVisible(true);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }

    private static void printSystemProperties() {
        Properties properties = System.getProperties();
        Enumeration Enumeration = properties.propertyNames();
        for (Enumeration e = properties.propertyNames(); e.hasMoreElements();) {
            String propertyName = (String) e.nextElement();
            log.info(propertyName + "=" + System.getProperty(propertyName));
        }
        Runtime runtime = Runtime.getRuntime();
        log.info("Max Memory: " + runtime.maxMemory());
        log.info("Total Memory: " + runtime.totalMemory());
        log.info("Free Memory: " + runtime.freeMemory());
    }
/*
    public static ServerConfiguration getServerConfiguration() {
        return mServerConfiguration;
    }
*/    

    public static RulesList getRulesList() {
        return mRulesList;
    }

    public static MainFrame getMainFrame() {
        return mMainFrame;
    }

    public static void save(boolean reload) {
        if (log.isDebugEnabled())
            log.debug("save: " + reload);
        if (reload) {
            // Connect to server and save config
            try {
                ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
                serverControl.reset();
            } catch (Exception ex) {
                Tools.logException(Galleon.class, ex, "Could not update server: " + mServerAddress);

                JOptionPane.showMessageDialog(mMainFrame, "Could not update Galleon server.", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
        mMainFrame.refresh();
    }

    public static List getRecordings() {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            return serverControl.getRecordings();
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get recordings from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    public static List getTiVos() {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            return serverControl.getTiVos();
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get TiVo's from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    public static void updateTiVos(List tivos) {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            serverControl.updateTiVos(tivos);
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not update TiVo's on server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    
    
    public static List getApps() {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            return serverControl.getApps();
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get apps from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    public static List getAppDescriptors() {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            return serverControl.getAppDescriptors();
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get app descriptors from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }    
    
    public static void removeApp(AppContext app)
    {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            serverControl.removeApp(app);
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not remove app from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    
    
    public static void updateApp(AppContext app)
    {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            serverControl.updateApp(app);
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not update app from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
    
    public static ServerConfiguration getServerConfiguration()
    {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            return serverControl.getServerConfiguration();
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get app server configuration from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }
    
    public static void updateServerConfiguration(ServerConfiguration serverConfiguration)
    {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            serverControl.updateServerConfiguration(serverConfiguration);
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not get app server configuration from server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    
    
    public static void updateVideo(Video video)
    {
        try {
            ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
            serverControl.updateVideo(video);
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex, "Could not update video at server: " + mServerAddress);

            JOptionPane.showMessageDialog(mMainFrame, "Could not connect to server.", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }    

    private static Logger log;

    private static AppManager mAppManager;

    private static MainFrame mMainFrame;

    private static String mAddress;

    private static File mConfigureDir;

    private static RulesList mRulesList;

    private static Registry mRegistry;

    private static String mServerAddress = "localhost";
}