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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.util.Tools;

import com.tivo.hme.http.server.HttpRequest;
import com.tivo.hme.io.FastInputStream;
import com.tivo.hme.sdk.Factory;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Listener;
import com.tivo.hme.util.ArgumentList;
import com.tivo.hme.util.Mp3Duration;

public class AppFactory extends Factory {

    private static Logger log = Logger.getLogger(AppFactory.class.getName());

    public AppFactory(AppContext appContext) {
        super();
        setAppContext(appContext);
    }

    public AppFactory(AppManager appManager) {
        mAppManager = appManager;
        setClassLoader(Thread.currentThread().getContextClassLoader());
    }

    public void loadApps() {
        if (System.getProperty("hme") != null) {
            File file = new File(System.getProperty("hme") + "/launcher.txt");
            if (file.exists()) {

                FastInputStream in = null;
                try {
                    in = new FastInputStream(new FileInputStream(file), 1024);

                    Listener listener = getSharedListener();
                    String ln = in.readLine();
                    while (ln != null) {
                        ln = ln.trim();
                        if (!ln.startsWith("#") && ln.length() > 0) {
                            try {
                                log.info("Found: " + ln);
                                mAppManager.addHMEApp(ln);
                                Factory factory = startFactory(listener, new ArgumentList(ln));
                                if (factory instanceof AppFactory)
                                    mAppManager.addApp((AppFactory) factory);
                            } catch (Throwable th) {
                                log.error("error: " + th.getMessage() + " for " + ln);
                                th.printStackTrace();
                            }
                        }
                        ln = in.readLine();
                    }
                } catch (IOException ex) {
                    Tools.logException(AppFactory.class, ex);
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
    public static Factory createFactory(Listener listener, ArgumentList args, AppContext appContext)
            throws InstantiationException, IllegalAccessException, ClassNotFoundException, NoSuchMethodException,
            IOException, InvocationTargetException {
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

            factory = (AppFactory) constructor.newInstance((Object[]) values);

        } catch (ClassNotFoundException ex) {
            Tools.logException(AppFactory.class, ex);
            throw ex;
        } catch (InvocationTargetException ex) {
            Tools.logException(AppFactory.class, ex);
            throw ex;
        }
        factory.setListener(listener);
        factory.setClassName(clazz);

        String title = factory.getAppContext().getConfiguration().getName();
        // name
        String uri = args.getValue("-uri", URLEncoder.encode(title.replaceAll(" ", ""), "UTF-8")); //getField(clazz,
        // "URI"));
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
        if (title == null) {
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
        } catch (Exception ex) {
            Tools.logException(AppFactory.class, ex);
        }

        System.exit(1);
        return null;
    }

    public AppFactory addApp(String className) {
        AppFactory appFactory = null;

        if (mAppManager != null) {
            try {
                appFactory = (AppFactory) startFactory(getSharedListener(), new ArgumentList(className));
                mAppManager.addApp(appFactory);
            } catch (Throwable ex) {
                Tools.logException(AppFactory.class, ex);
            }
        }
        return appFactory;
    }

    public AppFactory addApp(AppContext appContext) {
        log.debug("addApp: " + appContext);
        AppFactory appFactory = null;

        if (mAppManager != null) {
            try {
                appFactory = (AppFactory) startFactory(getSharedListener(), new ArgumentList(appContext.getDescriptor()
                        .getClassName()), appContext);
                mAppManager.addApp(appFactory);
            } catch (Throwable th) {
                Tools.logException(AppFactory.class, th);
            }
        }
        return appFactory;
    }

    public InputStream getStream(String uri) throws IOException {
        if (uri.toLowerCase().endsWith(".mp3")) {
            return Mp3File.getStream(uri);
        }

        return super.getStream(uri);
    }

    protected void addHeaders(HttpRequest http, String uri) throws IOException {
        if (uri.toLowerCase().endsWith(".mp3")) {
            long duration = -1;
            try {
                String id = Tools.extractName(uri);
                Audio audio = AudioManager.retrieveAudio(Integer.valueOf(id));
                duration = audio.getDuration();
            } catch (Exception ex) {
                Tools.logException(AppFactory.class, ex, uri);
            }

            if (duration==-1)
            {
                InputStream tmp = getStream(uri);
                if (tmp != null) {
                    try {
                        duration = Mp3Duration.getMp3Duration(tmp, tmp.available());
                    } finally {
                        tmp.close();
                    }
                }
            }
            
            if (duration!=-1)
                http.addHeader(IHmeProtocol.TIVO_DURATION, String.valueOf(duration));
        }
        super.addHeaders(http, uri);
    }

    public void setAppContext(AppContext appContext) {
        mAppContext = appContext;
    }

    public AppContext getAppContext() {
        return mAppContext;
    }

    private Listener getSharedListener() {
        if (mListener == null) {
            try {
                ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
                String arguments = "";
                if (serverConfiguration.getIPAddress() != null
                        && serverConfiguration.getIPAddress().trim().length() > 0) {
                    arguments = "-i " + serverConfiguration.getIPAddress();
                }
                if (serverConfiguration.getPort() != 0) {
                    arguments = arguments + (arguments.length() == 0 ? "" : " ") + "-port "
                            + serverConfiguration.getPort();
                }
                log.debug("arguments: " + arguments);
                ArgumentList argumentList = new ArgumentList(arguments);
                mListener = new Listener(argumentList);
            } catch (Exception ex) {
                Tools.logException(AppFactory.class, ex);
            }
        }
        return mListener;
    }

    private AppManager mAppManager;

    private AppContext mAppContext;

    private Listener mListener;
}