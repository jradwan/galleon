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

    private void getJars() {
        // TODO Handle reloading; what if list changes?
        File directory = new File(System.getProperty("apps"));
        File[] files = directory.listFiles(new FileFilter() {
            public final boolean accept(File file) {
                return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
            }
        });
        for (int i = 0; i < files.length; ++i) {
            log.debug("Found app: " + files[i].getAbsolutePath());
            mJars.add(files[i]);
            try {
                AppDescriptor appDescriptor = new AppDescriptor(files[i]);
                log.debug("appDescriptor=" + appDescriptor);
                if (appDescriptor.getClassName()!=null)
                    mAppDescriptors.add(appDescriptor);
            } catch (Exception ex) {
                log.error("Could not create app descriptor", ex);
            }
        }
    }
    
    /*
     * public void addPlugin(Plugin plugin) { if (log.isDebugEnabled()) log.debug("addPlugin(): plugin=" + plugin); if
     * (mPlugins.contains(plugin)) return;
     * 
     * mPlugins.add(plugin); try { if (log.isDebugEnabled()) log.debug("Initializing plugin: " + plugin);
     * plugin.init(new PluginContext(getPluginDescriptor(plugin),mReset));
     * 
     * for (int i = 0; i < mPluginListeners.size(); i++) ((PluginListener) mPluginListeners.get(i)).pluginAdded(plugin); }
     * catch (Exception ex) { Tools.logException(AppManager.class, ex); } }
     * 
     * public void removePlugin(Plugin plugin) { if (log.isDebugEnabled()) log.debug("removePlugin(): plugin=" +
     * plugin); if (!mPlugins.contains(plugin)) return;
     * 
     * try { if (log.isDebugEnabled()) log.debug("Stopping plugin: " + plugin); plugin.stop(); if (log.isDebugEnabled())
     * log.debug("Destroying plugin: " + plugin); plugin.destroy(); mPlugins.remove(plugin);
     * 
     * for (int i = 0; i < mPluginListeners.size(); i++) ((PluginListener)
     * mPluginListeners.get(i)).pluginRemoved(plugin); } catch (Exception ex) { Tools.logException(AppManager.class,
     * ex); } }
     * 
     * public void startPlugins() { Iterator it = mPlugins.iterator();
     * 
     * while (it.hasNext()) { try { Plugin plugin = (Plugin) it.next(); if (log.isDebugEnabled()) log.debug("Starting
     * plugin: " + plugin); plugin.start(); } catch (Exception ex) { Tools.logException(AppManager.class, ex); } } }
     * 
     * public Iterator getPlugins() { return mPlugins.iterator(); }
     * 
     * public int getPluginSize() { return mPlugins.size(); }
     * 
     * public void addPluginListener(PluginListener listener) { if (log.isDebugEnabled())
     * log.debug("addPluginListener()"); if (!mPluginListeners.contains(listener)) { mPluginListeners.add(listener);
     * updateListener(listener); } }
     * 
     * private void updateListener(PluginListener listener) { if (log.isDebugEnabled()) log.debug("updateListener()");
     * Iterator it = mPlugins.iterator();
     * 
     * while (it.hasNext()) { try { listener.pluginAdded((Plugin) it.next()); } catch (Exception ex) {
     * Tools.logException(AppManager.class, ex); } } }
     * 
     * public void removePluginListener(PluginListener listener) { mPluginListeners.remove(listener); }
     * 
     * public void destroyAllPlugins() { if (log.isDebugEnabled()) log.debug("destroyAllPlugins()"); Iterator it =
     * mPlugins.iterator(); Plugin plugin;
     * 
     * while (it.hasNext()) { try { plugin = (Plugin) it.next(); if (log.isDebugEnabled()) log.debug("Stopping plugin: " +
     * plugin); plugin.stop(); if (log.isDebugEnabled()) log.debug("Destroying plugin: " + plugin); plugin.destroy(); }
     * catch (Exception ex) { Tools.logException(AppManager.class, ex); } } }
     * 
     * public Class loadPlugin(PluginDescriptor pluginDescriptor) { if (log.isDebugEnabled()) log.debug("loadPlugin():
     * pluginDescriptor=" + pluginDescriptor); Plugin thePlugin = null; try { if (log.isDebugEnabled())
     * log.debug("Scanning JAR file: " + pluginDescriptor.getJar()); PluginClassLoader pluginClassLoader = new
     * PluginClassLoader(pluginDescriptor); Class theClass = Class.forName(pluginDescriptor.getClassName(), true,
     * pluginClassLoader); return theClass; } catch (IOException ex) { log.error("Cannot load plugin: " +
     * pluginDescriptor); Tools.logException(AppManager.class, ex); } catch (ClassNotFoundException ex) {
     * log.error("Cannot load plugin: " + pluginDescriptor); Tools.logException(AppManager.class, ex); } return null; }
     * 
     * private void loadPluginDescriptors(String directory) { if (log.isDebugEnabled())
     * log.debug("loadPluginDescriptors(): directory=" + directory); File file = new File(directory); if
     * (!(file.exists() && file.isDirectory())) { log.error("Could not load plugins from : " + directory); return; }
     * String[] plugins = file.list(); if (plugins == null) { log.info("There are no plugins to load"); return; }
     * 
     * for (int i = 0; i < plugins.length; i++) { String plugin = plugins[i]; if
     * (!plugin.toLowerCase().endsWith(".jar")) continue;
     * 
     * String path = directory + "/" + plugin;
     * 
     * try { if (log.isDebugEnabled()) log.debug("Scanning JAR file: " + path); PluginDescriptor pluginDescriptor = new
     * PluginDescriptor(path); if (log.isDebugEnabled()) log.debug("Plugin Descriptor: " + pluginDescriptor);
     * mPluginDescriptors.add(pluginDescriptor); } catch (IOException ex) { log.error("Cannot load plugin descriptor: " +
     * plugin); Tools.logException(AppManager.class, ex); } catch (AppException ex) { log.error("Cannot load plugin
     * descriptor: " + plugin); Tools.logException(AppManager.class, ex); } } }
     * 
     * public PluginDescriptor getPluginDescriptor(Plugin plugin) { Iterator iterator = getPluginDescriptors(); while
     * (iterator.hasNext()) { PluginDescriptor pluginDescriptor = (PluginDescriptor) iterator.next(); if
     * (pluginDescriptor.getClassName().equals(plugin.getClass().getName())) return pluginDescriptor; } return null; }
     * 
     * public Iterator getPluginDescriptors() { return mPluginDescriptors.iterator(); }
     */

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
            if (appContext.getDescriptor().getClassName().equals(app.getAppContext().getDescriptor().getClassName()))
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
            if (appContext.getDescriptor().getClassName().equals(app.getAppContext().getDescriptor().getClassName()))
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