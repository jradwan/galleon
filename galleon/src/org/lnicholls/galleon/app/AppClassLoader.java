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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.SecureClassLoader;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.*;

import org.apache.log4j.Logger;

/*
 * Utility class loader for apps deployed in jars
 * 
 * @see java.security.SecureClassLoader
 */

public class AppClassLoader extends SecureClassLoader {
    private static Logger log = Logger.getLogger(AppClassLoader.class.getName());

    public AppClassLoader() throws Exception {
        mDirectory = new File(System.getProperty("apps"));
        if (!mDirectory.exists() || !mDirectory.isDirectory()) {
            String message = "App Class Loader directory not found: " + System.getProperty("apps");
            InstantiationException exception = new InstantiationException(message);
            log.error(message, exception);
            throw exception;
        }
        mApps = new ArrayList();
        getApps();
    }

    private void getApps() {
        // TODO Handle reloading; what if list changes?
        File[] files = mDirectory.listFiles(new FileFilter() {
            public final boolean accept(File file) {
                return !file.isDirectory() && !file.isHidden() && file.getName().toLowerCase().endsWith(".jar");
            }
        });
        for (int i = 0; i < files.length; ++i) {
            log.debug("Found app: " + files[i].getAbsolutePath());
            mApps.add(files[i]);
        }
    }

    public Class loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (log.isDebugEnabled())
            log.debug("loadClass: " + name);

        Class clas = null;

        // If the class has already been loaded, just return it.
        clas = findLoadedClass(name);
        if (clas != null) {
            if (resolve) {
                resolveClass(clas);
            }
            return clas;
        }

        String fileName = classToFile(name);

        byte[] data = loadResource(fileName);
        if (data != null && data.length > 0)
            clas = defineClass(name, loadResource(fileName), 0, data.length);

        // We weren't able to get the class, so
        // use the default Classloader.
        if (clas == null) {
            clas = Class.forName(name);
        }

        // If we still can't find it, then it's a real
        // exception.
        if (clas == null) {
            throw new ClassNotFoundException(name);
        }

        // Resolve the class -- load all the classes
        // that this needs, and do any necessary linking.
        if (resolve) {
            resolveClass(clas);
        }

        // Return the class to the runtime system.
        return clas;
    }

    public static String fileToClass(String name) {
        char[] clsName = name.toCharArray();
        for (int i = clsName.length - 6; i >= 0; i--)
            if (clsName[i] == '/')
                clsName[i] = '.';
        return new String(clsName, 0, clsName.length - 6);
    }

    public static String classToFile(String name) {
        return name.replace('.', '/').concat(".class");
    }

    public InputStream getResourceAsStream(String name) {
        if (log.isDebugEnabled())
            log.debug("getResourceAsStream: " + name);
        byte[] resourceBytes = loadResource(name);
        if (resourceBytes != null) {
            if (log.isDebugEnabled())
                log.debug("getResourceAsStream: resourceBytes=" + resourceBytes.length);
            return new ByteArrayInputStream(resourceBytes);
        }
        return super.getSystemResourceAsStream(name);
    }

    public URL getResource(String name) {
        if (log.isDebugEnabled())
            log.debug("getResource: " + name);
        try {
            for (int i = 0; i < mApps.size(); i++) {
                JarFile jar = new JarFile((File) mApps.get(i));
                ZipEntry entry = jar.getEntry(name);
                jar.close();
                if (entry != null) {
                    return makeURL(name);
                }
            }
        } catch (IOException ex) {
            //log.debug("Failed to load resource " + name + " from " + mAppDescriptor.getJar());
        }
        return super.getSystemResource(name);
    }

    private byte[] loadResource(String name) {
        if (log.isDebugEnabled())
            log.debug("loadResource: " + name);
        BufferedInputStream bis = null;
        try {
            for (int i = 0; i < mApps.size(); i++) {
                JarFile jar = new JarFile((File) mApps.get(i));
                ZipEntry entry = jar.getEntry(name);
                if (entry != null) {
                    bis = new BufferedInputStream(jar.getInputStream(entry));
                    int len = (int) entry.getSize();
                    byte[] data = new byte[len];
                    int success = bis.read(data, 0, len);
                    bis.close();
                    jar.close();
                    if (success == -1) {
                        throw new ClassNotFoundException(name);
                    }
                    return data;
                }
                jar.close();
            }
            throw new ClassNotFoundException(name);
        } catch (Exception e) {
            //log.debug("Failed to load resource " + name + " from " + mAppDescriptor.getJar());
        }
        return null;
    }

    private URL makeURL(String name) throws MalformedURLException {
        // jar:file:///c:/galleon/apps/app.jar!/icon.gif'
        StringBuffer path = new StringBuffer();
        try {
            for (int i = 0; i < mApps.size(); i++) {
                JarFile jar = new JarFile((File) mApps.get(i));
                ZipEntry entry = jar.getEntry(name);
                if (entry != null) {
                    path.append(((File) mApps.get(i)).getAbsoluteFile()).append("!/").append(name);
                }
                jar.close();
            }
        } catch (Exception e) {
            //log.debug("Failed to load resource " + name + " from " + mAppDescriptor.getJar());
        }
        return new URL("jar", "", path.toString().replace('\\', '/'));
    }

    private File mDirectory;

    private ArrayList mApps;
}