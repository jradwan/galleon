package org.lnicholls.galleon.app;

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

import java.io.File;
import java.io.FileFilter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

/**
 * Manage all app jar files. The directory in which app jars are deployed are scanned and queried for their app
 * descriptors. The manager will instantiate a app based on settings in the configuration file.
 */
public final class AppManager {
    private static Logger log = Logger.getLogger(AppManager.class.getName());

    public AppManager(URLClassLoader appClassLoader) throws Exception {
        mAppClassLoader = appClassLoader;
        
        mJars = new ArrayList();
        mApps = new ArrayList();
        mAppDescriptors = new ArrayList();
        getJars();
        mHMEApps = new ArrayList();
        mAppFactory = new AppFactory(this);
        //loadAppDescriptors();
    }
    
    public void loadApps() {
        mAppFactory.loadApps();
    }

    private void getJars() {
        // TODO Handle reloading; what if list changes?
        File directory = new File(System.getProperty("apps"));
        File[] files = directory.listFiles(new FileFilter() {
            public final boolean accept(File file) {
                return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
            }
        });
        for (int i = 0; i < files.length; ++i) {
            try {
                log.debug("Found app: " + files[i].getAbsolutePath());
                File file = new File(files[i].getCanonicalPath());
                mJars.add(file);
                
                AppDescriptor appDescriptor = new AppDescriptor(file);
                log.debug("appDescriptor=" + appDescriptor);
                if (appDescriptor.getClassName()!=null)
                    mAppDescriptors.add(appDescriptor);
            } catch (Exception ex) {
                log.error("Could not create app descriptor", ex);
            }
        }
        
        /*
        directory = new File(System.getProperty("hme"));
        files = directory.listFiles(new FileFilter() {
            public final boolean accept(File file) {
                return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
            }
        });
        for (int i = 0; i < files.length; ++i) {
            try {
                log.debug("Found HME app: " + files[i].getAbsolutePath());
                File file = new File(files[i].getCanonicalPath());
                mJars.add(file);
                
                AppDescriptor appDescriptor = new AppDescriptor(file);
                log.debug("appDescriptor=" + appDescriptor);
                if (appDescriptor.getClassName()!=null)
                    mAppDescriptors.add(appDescriptor);
            } catch (Exception ex) {
                log.error("Could not create app descriptor", ex);
            }
        } 
        */       
    }
    
    public void addHMEApp(String launcher) {
        mHMEApps.add(launcher);
    }

    public ClassLoader getClassLoader() {
        return mAppClassLoader;
    }

    public void addApp(AppFactory app) {
        mApps.add(app);
        Iterator iterator = mAppDescriptors.iterator();
        while (iterator.hasNext()) {
            AppDescriptor appDescriptor = (AppDescriptor) iterator.next();
            if (app.getClass().getName().startsWith(appDescriptor.getClassName()))
            {
                app.getAppContext().setDescriptor(appDescriptor);
                break;
            }
        }
    }

    public AppConfiguration getAppConfiguration(String className) {
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            AppContext app = (AppContext) iterator.next();
            // TODO Handle multiple instances
            if (app.getClass().getName().equals(className + "Factory"))
                return app.getConfiguration();
        }
        return null;
    }

    public void setAppConfiguration(String className, AppConfiguration appConfiguration) {
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            AppFactory app = (AppFactory) iterator.next();
            // TODO Handle multiple instances
            if (app.getClass().getName().equals(className + "Factory")) {
                app.getAppContext().setConfiguration(appConfiguration);
            }
        }
    }

    public List getAppDescriptors() {
        return mAppDescriptors;
    }
    
    public List getApps() {
        List appContexts = new ArrayList();
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            AppFactory app = (AppFactory) iterator.next();
            appContexts.add(app.getAppContext());
        }
        return appContexts; 
    }
    
    public void createApp(AppContext appContext)
    {
        log.debug("createApp: "+appContext);
        try {
            AppFactory appFactory = mAppFactory.addApp(appContext);
        } catch (Exception ex) {
            log.error("Could not create app", ex);
        }
    }    
    
    public void removeApp(AppContext appContext)
    {
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            AppFactory app = (AppFactory) iterator.next();
            if (appContext.getId()==app.getAppContext().getId())
            {
                mApps.remove(app);
                return;
            }
        }
    }
    
    public void updateApp(AppContext appContext)
    {
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            AppFactory app = (AppFactory) iterator.next();
            if (appContext.getId()==app.getAppContext().getId())
            {
                app.setAppContext(appContext);
                return;
            }
        }
        
        try {
            AppFactory appFactory = mAppFactory.addApp(appContext);
        } catch (Exception ex) {
            log.error("Could not update app", ex);
        }
    }    

    private LinkedList mPluginListeners = new LinkedList();

    private LinkedList mPluginDescriptors = new LinkedList();

    private LinkedList mPlugins = new LinkedList();

    private ArrayList mJars;

    private ArrayList mApps;

    private ArrayList mAppDescriptors;

    private AppFactory mAppFactory;

    private ArrayList mHMEApps;

    private URLClassLoader mAppClassLoader;
}