//////////////////////////////////////////////////////////////////////
//
// File: BResSkin.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;
import java.util.*;
import java.util.zip.*;
import java.io.*;

/**
 * This class allows you to override the resources loaded by BSkin by providing
 * a base * path for your own resources. To replace an image, provide an image
 * with the same name in your application jar file.
 *
 * For example, if your application code is in the pakage:
 *
 * com.acme.dynamite.*
 *
 * Then you could provide a new skin for Bananas in your application's jar file
 * by putting the .png images in your package and creating a BResSkin from your
 * BApplication's init method like this:
 *
 * BSkin skin = new BResSkin(this, "com/acme/dynamite/");
 *
 * Any images, like bar.png, will be loaded from that location as a resource.
 * NOTE: you must include the trailing slash in your path.
 * 
 * @author      Brigham Stevens
 */
public class BResSkin extends BSkin 
{
    String resPath;
    Map resMap;

    /**
     * @param app the app being skinned
     * @param resPath the path to the resources.
     */
    public BResSkin(BApplication app, String resPath)
    {
        super(app);
        this.resPath = resPath;
        resMap = new HashMap();
    }

    /**
     * Get an element. This throws an exception if the element is not found.
     * This will use the element loaded from a resource in the classpath if found.
     *
     * @param name the name of the element
     * @return the element
     */
    public BSkin.Element get(String name)
    {
        Element e = (Element)map.get(name);
        if (e == null) {
            if (name.startsWith("background")) {
                e = new Element(this, name, 640, 480, null);
            } else {
                throw new RuntimeException("unknown element : " + name);
            }
        }

        /**
         * If there is an image for this element use it, otherwise take default.
         */
        Resource img = (Resource)resMap.get(name);
        if (img == null) {
            byte buf[] =  loadResource(resPath + name + ".png");
            if (buf != null) {
                img = app.createImage(buf);
                resMap.put(name, img);
            } else {
                img = app.getResource("com/tivo/hme/bananas/" + name + ".png");
            }
        } 
        e.setResource(img);
        return e;
    }

    /**
     * Load a file as an array of bytes from the Skin Directory.
     *
     * @return array of bytes that is the .png image loaded
     */
    private byte[] loadResource(String name)
    {
        InputStream in = null;
        try {
            try {
                in = getClass().getResourceAsStream("/"+name);
                if (in == null) {
                    return null;
                }
                int size = (int)in.available();
                byte buf[] = new byte[size];
                int pos = 0;
                while(pos < size) {
                    int len = in.read(buf, pos, size-pos);
                    pos += len;
                }
                return buf;
            } finally {
                if (in != null) {
                    in.close();
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
