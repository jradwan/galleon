//////////////////////////////////////////////////////////////////////
//
// File: BDirSkin.java
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
 * A Class that implements a skin stored in a directory.
 * The default extended properties of BSkin are used for highlight placement.
 * In the future, these properties could also be stored in the zip file.
 * 
 * @author      Brigham Stevens
 */
public class BDirSkin extends BSkin 
{
    File skinDir;
    Map imgMap;
    /**
     * Create a skin that loads images from a directory using the default skin
     * properties. If any images are not present in the directory the default image
     * will be used.
     *
     * @param app the app being skinned
     */
    public BDirSkin(BApplication app, String skinDir)
    {
        super(app);
        this.skinDir = new File(skinDir);
        imgMap = new HashMap();
    }



    /**
     * Get an element. This throws an exception if the element is not found.
     * This will use the element from the zip file if it is present.
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
        Resource img = (Resource)imgMap.get(name);
        if (img != null) {
            e.setResource(img);
        } else {
            byte buf[] = loadSkinFile(name + ".png");
            if (buf != null) {
                img = app.createImage(buf);
                imgMap.put(name, img);
                e.setResource(img);
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
    private byte[] loadSkinFile(String name)
    {
        // make sure file exists
        File f = new File(skinDir, name);
        if (!f.exists()) {
            return null;
        }

        byte buf[] = new byte[(int)f.length()];
        FileInputStream in = null;
        try {
            try {
                in = new FileInputStream(f);
                int len = in.read(buf, 0, (int)f.length());
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
