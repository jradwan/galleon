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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.net.*;
import java.io.*;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;

/**
 * Manage all plugin jar files. The directory in which plugin jars are deployed are scanned and queried for their plugin
 * descriptors. The manager will instantiate a plugin based on settings in the configuration file.
 *
 * @see org.lnicholls.javahmo.plugin.Plugin
 * @see org.lnicholls.javahmo.plugin.AppDescriptor
 * @see org.lnicholls.javahmo.util.Configurator
 */
public final class AppManager {
    private static Logger log = Logger.getLogger(AppManager.class.getName());
    
    public AppManager() throws Exception {
        mDirectory = new File(System.getProperty("apps"));
        if (!mDirectory.exists() || !mDirectory.isDirectory()) {
            String message = "App Class Loader directory not found: " + System.getProperty("apps");
            InstantiationException exception = new InstantiationException(message);
            log.error(message, exception);
            throw exception;
        }
        mApps = new ArrayList();
        getApps();
        Thread.currentThread().setContextClassLoader(mClassLoader);
        mHMEApps = new ArrayList();
        mAppFactory = new AppFactory(this);
        //loadAppDescriptors();
    }    
    
    private void getApps() {
        // TODO Handle reloading; what if list changes?
        File[] files = mDirectory.listFiles(new FileFilter() {
            public final boolean accept(File file) {
                return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
            }
        });
        URL urlList[] = new URL[files.length];
        for (int i = 0; i < files.length; ++i) {
            log.debug("Found app: " + files[i].getAbsolutePath());
            mApps.add(files[i]);
            
            try
            {
                urlList[i] = files[i].toURL();
            }
            catch (Exception ex)
            { 
                // should never happen 
            }
        }
        
        mClassLoader = new URLClassLoader(urlList);
    }    
/*
    public void addPlugin(Plugin plugin) {
        if (log.isDebugEnabled())
            log.debug("addPlugin(): plugin=" + plugin);
        if (mPlugins.contains(plugin))
            return;

        mPlugins.add(plugin);
        try {
            if (log.isDebugEnabled())
                log.debug("Initializing plugin: " + plugin);
            plugin.init(new PluginContext(getPluginDescriptor(plugin),mReset));

            for (int i = 0; i < mPluginListeners.size(); i++)
                ((PluginListener) mPluginListeners.get(i)).pluginAdded(plugin);
        } catch (Exception ex) {
            Tools.logException(AppManager.class, ex);
        }
    }

    public void removePlugin(Plugin plugin) {
        if (log.isDebugEnabled())
            log.debug("removePlugin(): plugin=" + plugin);
        if (!mPlugins.contains(plugin))
            return;

        try {
            if (log.isDebugEnabled())
                log.debug("Stopping plugin: " + plugin);
            plugin.stop();
            if (log.isDebugEnabled())
                log.debug("Destroying plugin: " + plugin);
            plugin.destroy();
            mPlugins.remove(plugin);

            for (int i = 0; i < mPluginListeners.size(); i++)
                ((PluginListener) mPluginListeners.get(i)).pluginRemoved(plugin);
        } catch (Exception ex) {
            Tools.logException(AppManager.class, ex);
        }
    }

    public void startPlugins() {
        Iterator it = mPlugins.iterator();

        while (it.hasNext()) {
            try {
                Plugin plugin = (Plugin) it.next();
                if (log.isDebugEnabled())
                    log.debug("Starting plugin: " + plugin);
                plugin.start();
            } catch (Exception ex) {
                Tools.logException(AppManager.class, ex);
            }
        }
    }

    public Iterator getPlugins() {
        return mPlugins.iterator();
    }

    public int getPluginSize() {
        return mPlugins.size();
    }

    public void addPluginListener(PluginListener listener) {
        if (log.isDebugEnabled())
            log.debug("addPluginListener()");
        if (!mPluginListeners.contains(listener)) {
            mPluginListeners.add(listener);
            updateListener(listener);
        }
    }

    private void updateListener(PluginListener listener) {
        if (log.isDebugEnabled())
            log.debug("updateListener()");
        Iterator it = mPlugins.iterator();

        while (it.hasNext()) {
            try {
                listener.pluginAdded((Plugin) it.next());
            } catch (Exception ex) {
                Tools.logException(AppManager.class, ex);
            }
        }
    }

    public void removePluginListener(PluginListener listener) {
        mPluginListeners.remove(listener);
    }

    public void destroyAllPlugins() {
        if (log.isDebugEnabled())
            log.debug("destroyAllPlugins()");
        Iterator it = mPlugins.iterator();
        Plugin plugin;

        while (it.hasNext()) {
            try {
                plugin = (Plugin) it.next();
                if (log.isDebugEnabled())
                    log.debug("Stopping plugin: " + plugin);
                plugin.stop();
                if (log.isDebugEnabled())
                    log.debug("Destroying plugin: " + plugin);
                plugin.destroy();
            } catch (Exception ex) {
                Tools.logException(AppManager.class, ex);
            }
        }
    }

    public Class loadPlugin(PluginDescriptor pluginDescriptor) {
        if (log.isDebugEnabled())
            log.debug("loadPlugin(): pluginDescriptor=" + pluginDescriptor);
        Plugin thePlugin = null;
        try {
            if (log.isDebugEnabled())
                log.debug("Scanning JAR file: " + pluginDescriptor.getJar());
            PluginClassLoader pluginClassLoader = new PluginClassLoader(pluginDescriptor);
            Class theClass = Class.forName(pluginDescriptor.getClassName(), true, pluginClassLoader);
            return theClass;
        } catch (IOException ex) {
            log.error("Cannot load plugin: " + pluginDescriptor);
            Tools.logException(AppManager.class, ex);
        } catch (ClassNotFoundException ex) {
            log.error("Cannot load plugin: " + pluginDescriptor);
            Tools.logException(AppManager.class, ex);
        }
        return null;
    }

    private void loadPluginDescriptors(String directory) {
        if (log.isDebugEnabled())
            log.debug("loadPluginDescriptors(): directory=" + directory);
        File file = new File(directory);
        if (!(file.exists() && file.isDirectory())) {
            log.error("Could not load plugins from : " + directory);
            return;
        }
        String[] plugins = file.list();
        if (plugins == null) {
            log.info("There are no plugins to load");
            return;
        }

        for (int i = 0; i < plugins.length; i++) {
            String plugin = plugins[i];
            if (!plugin.toLowerCase().endsWith(".jar"))
                continue;

            String path = directory + "/" + plugin;

            try {
                if (log.isDebugEnabled())
                    log.debug("Scanning JAR file: " + path);
                PluginDescriptor pluginDescriptor = new PluginDescriptor(path);
                if (log.isDebugEnabled())
                    log.debug("Plugin Descriptor: " + pluginDescriptor);
                mPluginDescriptors.add(pluginDescriptor);
            } catch (IOException ex) {
                log.error("Cannot load plugin descriptor: " + plugin);
                Tools.logException(AppManager.class, ex);
            } catch (AppException ex) {
                log.error("Cannot load plugin descriptor: " + plugin);
                Tools.logException(AppManager.class, ex);
            }
        }
    }

    public PluginDescriptor getPluginDescriptor(Plugin plugin) {
        Iterator iterator = getPluginDescriptors();
        while (iterator.hasNext()) {
            PluginDescriptor pluginDescriptor = (PluginDescriptor) iterator.next();
            if (pluginDescriptor.getClassName().equals(plugin.getClass().getName()))
                return pluginDescriptor;
        }
        return null;
    }

    public Iterator getPluginDescriptors() {
        return mPluginDescriptors.iterator();
    }
*/    
    
    public void addHMEApp(String launcher)
    {
        mHMEApps.add(launcher);
    }
    
    public ClassLoader getClassLoader()
    {
        return mClassLoader;
    }
    

    private LinkedList mPluginListeners = new LinkedList();

    private LinkedList mPluginDescriptors = new LinkedList();

    private LinkedList mPlugins = new LinkedList();
    
    private File mDirectory;

    private ArrayList mApps;
    
    private AppFactory mAppFactory;
    
    private ArrayList mHMEApps;
    
    private URLClassLoader mClassLoader;
}