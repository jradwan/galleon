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
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;
import java.util.Properties;
import java.util.*;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JWindow;
import javax.swing.UIManager;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.app.*;
import org.lnicholls.galleon.togo.*;

import com.jgoodies.plaf.FontSizeHints;
import com.jgoodies.plaf.LookUtils;
import com.jgoodies.plaf.Options;

import java.rmi.server.*;
import java.rmi.*;
import java.rmi.registry.*;

public final class Galleon implements Constants {

    static class SplashWindow extends JWindow {
        public SplashWindow() {
            super((Frame) null);

            URL url = getClass().getClassLoader().getResource("javahmo.gif");
            ImageIcon logo = new ImageIcon(url);

            JLabel l = new JLabel(logo);
            getContentPane().add(l, BorderLayout.CENTER);

            pack();
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension labelSize = l.getPreferredSize();
            setLocation(screenSize.width / 2 - (labelSize.width / 2), screenSize.height / 2 - (labelSize.height / 2));
        }
    }

    //private static SplashWindow splashWindow = new SplashWindow();
    
    public Galleon() {
        //splashWindow.setVisible(true);

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

            //mAppManager = new AppManager();  // starts server??
            mServerConfiguration = new ServerConfiguration();
            if (mConfigureDir != null) {
                log.info("Configuration Dir=" + mConfigureDir.getAbsolutePath());
                new Configurator(mServerConfiguration).load(mAppManager, mConfigureDir);
            } else
                new Configurator(mServerConfiguration).load(mAppManager);
            mAddress = mServerConfiguration.getIPAddress();
            if (mAddress == null || mAddress.length() == 0)
                mAddress = "127.0.0.1";
            mPort = mServerConfiguration.getConfiguredPort();
            mRegistry = LocateRegistry.getRegistry("localhost", 1099);
            
            mRulesList = new RulesList();
            
            mMainFrame = new MainFrame(mServerConfiguration.getVersion());
            
            //splashWindow.setVisible(false);
            mMainFrame.setVisible(true);
            
            /*
            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    mToGo.getRecordings();
                }
            });
            */            
            
        } catch (Exception ex) {
            Tools.logException(Galleon.class, ex);
            System.exit(0);
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            File configureDir = new File(args[0]);
            if (configureDir.exists() && configureDir.isDirectory())
                mConfigureDir = configureDir;
        }

        //splashWindow.setVisible(true);

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

    public static AppManager getAppManager() {
        return mAppManager;
    }

    public static ServerConfiguration getServerConfiguration() {
        return mServerConfiguration;
    }
    
    public static RulesList getRulesList() {
        return mRulesList;
    }    

    public static MainFrame getMainFrame() {
        return mMainFrame;
    }

    public static void save(boolean reload) {
        if (log.isDebugEnabled())
            log.debug("save: " + reload);
        if (mConfigureDir != null)
            new Configurator(mServerConfiguration).save(mAppManager, mConfigureDir);
        else
            new Configurator(mServerConfiguration).save(mAppManager);
        if (reload) {
            // Connect to server and save config
            try {
                ServerControl serverControl = (ServerControl) mRegistry.lookup("serverControl");
                serverControl.reset();
            } catch (Exception ex) {
                Tools.logException(Galleon.class, ex, "Could not reconfigure Galleon server at port " + mPort);

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
            Tools.logException(Galleon.class, ex, "Could not get recordings at port " + mPort);

            JOptionPane.showMessageDialog(mMainFrame, "Could not get recordings.", "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
        return null;
    }

    private static Logger log;

    private static AppManager mAppManager;

    private static ServerConfiguration mServerConfiguration;

    private static MainFrame mMainFrame;

    private static String mAddress;

    private static int mPort;

    private static File mConfigureDir;
    
    private static RulesList mRulesList;
    
    private static Registry mRegistry;
}