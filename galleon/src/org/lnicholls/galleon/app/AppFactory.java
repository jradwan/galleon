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

import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.reflect.*;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.apps.weather.WeatherConfiguration;
import org.lnicholls.galleon.apps.weather.WeatherData;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.util.Configurator;

import com.tivo.hme.sdk.*;
import com.tivo.hme.io.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.share.*;
import com.tivo.hme.http.server.*;

public class AppFactory extends Factory {

    private static Logger log = Logger.getLogger(AppFactory.class.getName());
    
    public AppFactory(AppContext appContext) {
        super();
        setAppContext(appContext);
    }

    public AppFactory(AppManager appManager) {
        mAppManager = appManager;
        if (System.getProperty("apps") != null) {
            File file = new File(System.getProperty("apps") + "/launcher.txt");
            if (file.exists()) {
                setClassLoader(Thread.currentThread().getContextClassLoader());

                FastInputStream in = null;
                try {
                    in = new FastInputStream(new FileInputStream(file), 1024);

                    mListener = new Listener(new ArgumentList(""));
                    String ln = in.readLine();
                    while (ln != null) {
                        ln = ln.trim();
                        if (!ln.startsWith("#") && ln.length() > 0) {
                            try {
                                System.out.println("Found: " + ln);
                                appManager.addHMEApp(ln);
                                Factory factory = startFactory(mListener, new ArgumentList(ln));
                                if (factory instanceof AppFactory)
                                    appManager.addApp((AppFactory) factory);
                            } catch (Throwable th) {
                                System.out.println("error: " + th.getMessage() + " for " + ln);
                                th.printStackTrace();
                            }
                        }
                        ln = in.readLine();
                    }
                } catch (IOException e) {
                    // TODO
                    System.out.println("error: " + e.getMessage());
                    e.printStackTrace();
                } finally {
                    if (in != null) {
                        try {
                            in.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }
    }

    // Modified from SDK code
    public static Factory createFactory(Listener listener, ArgumentList args, AppContext appContext) throws InstantiationException,
            IllegalAccessException, ClassNotFoundException, NoSuchMethodException, IOException, InvocationTargetException {
        AppFactory factory;
        
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        //
        // create the factory
        //

        String clazz = args.getValue("-class", null);
        if (clazz == null) {
            clazz = args.shift();
        }
        String clazzNoPackage = clazz.substring(clazz.lastIndexOf('.') + 1);

        try {
            // try to load clazz$Factory
            Class factoryClass = classLoader.loadClass(clazz + "$" + clazzNoPackage + "Factory");
            
            Class[] parameters = new Class[1];
            parameters[0] = AppContext.class;
            Constructor constructor = factoryClass.getConstructor(parameters);
            AppContext[] values = new AppContext[1];
            values[0] = appContext; 

            factory = (AppFactory)constructor.newInstance(values);
            
        } catch (ClassNotFoundException ex) {
            log.error(AppFactory.class, ex);
            throw ex;
        } catch (InvocationTargetException ex) {
            log.error(AppFactory.class, ex);
            throw ex;
        }
        factory.setListener(listener);
        factory.setClassName(clazz);

        // name
        String uri = args.getValue("-uri", getField(clazz, "URI"));
        if (uri == null) {
            uri = clazz;

            int d2 = clazz.lastIndexOf('.');
            if (d2 >= 0) {
                int d1 = clazz.lastIndexOf('.', d2 - 1);
                if (d1 >= 0) {
                    uri = clazz.substring(d1 + 1, d2);
                } else {
                    uri = clazz.substring(0, d2);
                }
            }
        }
        factory.setURI(uri);

        // title
        String title = factory.getAppContext().getConfiguration().getName();
        if (title==null)
        {
            title = args.getValue("-title", getField(clazz, "TITLE"));
            if (title == null) {
                title = clazzNoPackage;
            }
            String user = System.getProperty("hme.user");
            if (user != null) {
                title += " " + user;
            }
        }
        factory.setTitle(title);

        // init the app last
        factory.init(args);

        return factory;
    }

    // Modified from SDK code
    public static Factory startFactory(Listener listener, ArgumentList args, AppContext appContext) {
        try {
            Factory factory = createFactory(listener, args, appContext);
            args.checkForIllegalFlags();
            factory.start();
            return factory;
        } catch (IOException e) {
            System.out.println("error: I/O exception: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.out.println("error: class not found: " + e.getMessage());
            System.out.println("error: check the classpath and access permissions");
        } catch (IllegalAccessException e) {
            System.out.println("error: illegal access: " + e.getMessage());
            System.out.println("error: make sure the class is public and has a public default constructor");
        } catch (NoSuchMethodException e) {
            System.out.println("error: no constructor: " + e.getMessage());
            System.out.println("error: make sure the class is public and has a public default constructor");
        } catch (InstantiationException e) {
            System.out.println("error: instantiation exception: " + e.getMessage());
        } catch (InvocationTargetException e) {
            System.out.println("error: InvocationTargetException: " + e.getMessage());
        }
        
        System.exit(1);
        return null;
    }

    public AppFactory addApp(String className) {
        AppFactory appFactory = null;

        if (mAppManager != null) {
            try {
                appFactory = (AppFactory) startFactory(mListener, new ArgumentList(className));
                mAppManager.addApp(appFactory);
            } catch (Throwable th) {
                // TODO
                System.out.println("error: " + th.getMessage());
                th.printStackTrace();
            }
        }
        return appFactory;
    }
    
    public AppFactory addApp(AppContext appContext) {
        log.debug("addApp: "+appContext);
        AppFactory appFactory = null;

        if (mAppManager != null) {
            try {
                appFactory = (AppFactory) startFactory(mListener, new ArgumentList(appContext.getDescriptor().getClassName()),appContext);
                mAppManager.addApp(appFactory);
            } catch (Throwable th) {
                // TODO
                System.out.println("error: " + th.getMessage());
                th.printStackTrace();
            }
        }
        return appFactory;
    }

    public void setAppContext(AppContext appContext) {
        mAppContext = appContext;
    }

    public AppContext getAppContext() {
        return mAppContext;
    }
    
    private AppManager mAppManager;

    private AppContext mAppContext;

    private Listener mListener;
}