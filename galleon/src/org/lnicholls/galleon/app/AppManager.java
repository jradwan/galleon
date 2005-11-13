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
import java.lang.reflect.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import com.tivo.hme.sdk.Factory;
import com.tivo.hme.host.sample.JarClassLoader;
import com.tivo.hme.host.sample.Main;
import com.tivo.hme.host.util.*;
import com.tivo.hme.interfaces.IFactory;

import org.lnicholls.galleon.database.Application;
import org.lnicholls.galleon.database.ApplicationManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.util.*;

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
        mAppContexts = new ArrayList();
        mAppDescriptors = new ArrayList();
        getJars();
        mHMEApps = new ArrayList();
        //mAppFactory = new AppFactory(this);
        //loadAppDescriptors();
    }
    
    public void loadApps() {
        //mAppFactory.loadApps();
    	File file = new File(System.getProperty("hme") + "/launcher.txt");
        if (file.exists()) {
        	try
        	{
        		getAppHost().loadLaunchFile(file.getAbsolutePath(), mAppClassLoader);
        	}
        	catch (Exception ex)
        	{
        		log.error("Could not load HME launch file", ex);
        	}
        }
        
        try
    	{
        	getAppHost().listen();
    	}
    	catch (Exception ex)
    	{
    		log.error("Could not listen", ex);
    	}
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
            	File file = new File(files[i].getCanonicalPath());
            	AppDescriptor appDescriptor = new AppDescriptor(file);
            	if (appDescriptor.getClassName()!=null)
            	{
            		log.debug("Found HME app: " + file.getAbsolutePath());
                    mJars.add(file);
                    appDescriptor.setHME(true);
	                log.debug("appDescriptor=" + appDescriptor);
                    mAppDescriptors.add(appDescriptor);
            	}
            } catch (Exception ex) {
                log.error("Could not create HME app descriptor", ex);
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

    public void addApp(Factory app) {
    	mApps.add(app);
        if (app instanceof AppFactory)
        {
	        Iterator iterator = mAppDescriptors.iterator();
	        while (iterator.hasNext()) {
	            AppDescriptor appDescriptor = (AppDescriptor) iterator.next();
	            if (app.getClass().getName().startsWith(appDescriptor.getClassName()))
	            {
	                ((AppFactory)app).getAppContext().setDescriptor(appDescriptor);
	                break;
	            }
	        }
        }
    }

    /*
    public Object getAppConfiguration(String className) {
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
                app.setConfiguration(appConfiguration);
            }
        }
    }
    */

    public List getAppDescriptors() {
    	return mAppDescriptors;
    }
    
    public List getApps() {
    	List appContexts = new ArrayList();
        Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            Factory app = (Factory) iterator.next();
            if (app instanceof AppFactory)
            {
            	appContexts.add(((AppFactory)app).getAppContext());
            }
        }
        iterator = mAppContexts.iterator();
        while (iterator.hasNext()) {
        	appContexts.add(iterator.next());
        }
        return appContexts; 
    }
    
    public IFactory createApp(AppContext appContext)
    {
    	IFactory appFactory = null;
    	try {
        	StringBuffer stringbuffer = new StringBuffer(64);
        	AppHost.addArg(stringbuffer, "--class", appContext.getDescriptor().getClassName());
        	//AppHost.addArg(stringbuffer, null, attributes.getValue("HME-Arguments"));
            appFactory = getAppHost().createFactory(new ArgumentList(stringbuffer.toString()), mAppClassLoader);
            if (!(appFactory instanceof AppFactory))
            {
            	try
        		{
        			Class[] parameters = new Class[1];
    				parameters[0] = appContext.getConfiguration().getClass();
        			Method method = appFactory.getClass().getMethod("setConfiguration",parameters);
        			if (method!=null)
        			{
        				Object[] values = new Object[1];
        				values[0] = appContext.getConfiguration();
        				method.invoke(appFactory, values);
        			}
        			else
        				log.error("App does not allow update of configuration");
        		} catch (Exception ex) {
                    log.error("Could not configure app", ex);
                }
            	mAppContexts.add(appContext);
            }
            else
            {
            	((AppFactory)appFactory).setAppContext(appContext);
            	appFactory.setAppTitle(appContext.getTitle());
            	appFactory.setAppName(clean(appContext.getTitle()));
            	((AppFactory)appFactory).initialize();
            	
            	addApp(((AppFactory)appFactory));
            }
        } catch (Exception ex) {
            log.error("Could not create app", ex);
        }
        return appFactory;
    }    
    
    private static String clean(String value) {
        StringBuffer buffer = new StringBuffer(value.length());
        synchronized (buffer) {
            for (int i = 0; i < value.length(); i++) {
                if (Character.isLetter(value.charAt(i)) && value.charAt(i)!=' ')
                    buffer.append(value.charAt(i));
            }
        }
        return buffer.toString();
    }
    
    
    public void removeApp(AppContext appContext)
    {
    	try {
	    	Iterator iterator = mApps.iterator();
	        while (iterator.hasNext()) {
	            Factory app = (Factory) iterator.next();
	            if (app instanceof AppFactory)
	            {
	            	AppFactory appFactory = (AppFactory)app;
		            if (appContext.getId()==appFactory.getAppContext().getId())
		            {
		            	appFactory.remove();
		            	getAppHost().remove(app);
		                mApps.remove(app);
		                
		                try
		                {
		                	List list = ApplicationManager.findByClazz(appContext.getDescriptor().getClassName());
		                	if (list!=null && list.size()>0)
		                	{
		                		Application application = (Application)list.get(0);
		                		application.setDateRemoved(new Date());
		                		ApplicationManager.updateApplication(application);
		                	}
		                } catch (Exception ex) {
		                    Tools.logException(AppManager.class, ex);
		                }
		                
		                return;
		            }
	            }
	            else
	            {
	            	/*
	            	if (app.getClassName().equals(appContext.getDescriptor().getClassName()))
	            	{
	            		mAppFactory.getListener().remove(app);
		                mApps.remove(app);
		                
		                Iterator contextsIterator = mAppContexts.iterator();
	                    while (contextsIterator.hasNext()) {
	                    	AppContext currentContext = (AppContext)contextsIterator.next();
	                    	if (currentContext.getDescriptor().getClassName().equals(appContext.getDescriptor().getClassName()))
	                    	{
	                    		mAppContexts.remove(currentContext);
	                    		break;
	                    	}
	                    }
		                return;
	            	}
	            	*/
	            }
	        }
    	} catch (Exception ex) {
            Tools.logException(AppManager.class, ex, "Could not remove app");
        }
    }
    
    public void updateApp(AppContext appContext)
    {
    	Iterator iterator = mApps.iterator();
        while (iterator.hasNext()) {
            Factory app = (Factory) iterator.next();
            if (app instanceof AppFactory)
            {
            	AppFactory appFactory = (AppFactory)app;
	            if (appContext.getId()==appFactory.getAppContext().getId())
	            {
	            	appFactory.setAppContext(appContext);
	                return;
	            }
            }
            else
            {
            	/*
            	if (app.getClassName().equals(appContext.getDescriptor().getClassName()))
            	{
            		try
            		{
            			Class[] parameters = new Class[1];
        				parameters[0] = appContext.getConfiguration().getClass();
            			Method method = app.getClass().getMethod("setConfiguration",parameters);
            			if (method!=null)
            			{
            				Object[] values = new Object[1];
            				values[0] = appContext.getConfiguration();
            				method.invoke(app, values);
            			}
            			else
            				log.error("App does not allow update of configuration");
            		} catch (Exception ex) {
                        log.error("Could not configure app", ex);
                    }
            		
            		Iterator contextsIterator = mAppContexts.iterator();
                    while (contextsIterator.hasNext()) {
                    	AppContext currentContext = (AppContext)contextsIterator.next();
                    	if (currentContext.getDescriptor().getClassName().equals(appContext.getDescriptor().getClassName()))
                    	{
                    		currentContext.setConfiguration(appContext.getConfiguration());
                    		break;
                    	}
                    }
	                return;
            	}
            	*/
            }
        }
        try {
            IFactory appFactory = createApp(appContext);
            if (!(appFactory instanceof AppFactory))
            {
            	try
        		{
        			Class[] parameters = new Class[1];
    				parameters[0] = appContext.getConfiguration().getClass();
        			Method method = appFactory.getClass().getMethod("setConfiguration",parameters);
        			if (method!=null)
        			{
        				Object[] values = new Object[1];
        				values[0] = appContext.getConfiguration();
        				method.invoke(appFactory, values);
        			}
        			else
        				log.error("App does not allow update of configuration");
        		} catch (Exception ex) {
                    log.error("Could not configure app", ex);
                }            	
            	
            	mAppContexts.add(appContext);
            }
            getAppHost().listen(appFactory);
        } catch (Exception ex) {
            log.error("Could not update app", ex);
        }
    }
    
    private AppHost getAppHost() throws Exception
    {
    	if (mAppHost==null)
    	{
	    	String arguments = "";
	        ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
	        if (serverConfiguration.getIPAddress() != null
	                && serverConfiguration.getIPAddress().trim().length() > 0) {
	            arguments = "--intf " + serverConfiguration.getIPAddress();
	            //arguments = "--nomdns " + serverConfiguration.getIPAddress();
	        }
	        if (serverConfiguration.getPort() != 0) {
	            arguments = arguments + (arguments.length() == 0 ? "" : " ") + "--port "
	                    + Server.getServer().getPort();
	        }
	        log.debug("Arguments: "+arguments);
	        ArgumentList argumentList = new ArgumentList(arguments);
	        
	        mAppHost = new AppHost(argumentList);
    	}
        
        return mAppHost;
    }
    
    private LinkedList mPluginListeners = new LinkedList();

    private LinkedList mPluginDescriptors = new LinkedList();

    private LinkedList mPlugins = new LinkedList();

    private ArrayList mJars;

    private ArrayList mApps;
    
    private ArrayList mAppContexts;

    private ArrayList mAppDescriptors;

    //private AppFactory mAppFactory;

    private ArrayList mHMEApps;

    private URLClassLoader mAppClassLoader;
    
    private AppHost mAppHost;
}