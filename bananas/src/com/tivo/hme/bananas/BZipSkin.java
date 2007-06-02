//////////////////////////////////////////////////////////////////////
//
// File: BZipSkin.java
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
 * A Class that implements a skin stored in zip files.
 * The default extended properties of BSkin are used for highlight placement.
 * In the future, these properties could also be stored in the zip file.
 * 
 * @author      Brigham Stevens
 */
public class BZipSkin extends BSkin 
{
    Map imgMap;
    /**
     * Create a skin that loads images from a zip file using the default skin
     * properties. If any images are not present in the zip the default image
     * will be used.
     *
     * Skin Zip Files contain .png images. The entry name must match the
     * elemenet name. Directory names in the zip file are ignored.
     *
     * @param app the app being skinned
     */
    public BZipSkin(BApplication app, String zipFile) throws IOException 
    {
        super(app);
        ZipFile zip = null;
        try {
            imgMap = new HashMap();
            zip = new ZipFile(new File(zipFile), ZipFile.OPEN_READ);
            Vector v = new Vector();
            Enumeration e = zip.entries();
            while (e.hasMoreElements()) {
                ZipEntry ze = (ZipEntry)e.nextElement();
                String name = ze.getName();
                if (name.toLowerCase().endsWith(".png")) {
                    String key = name.substring(0,name.length()-4);
                    if (key.indexOf("/") != -1) {
                        int pos = key.lastIndexOf("/")+1;
                        key = key.substring(pos, key.length());
                    }

                    InputStream in = zip.getInputStream(ze);
                    int size = (int)ze.getSize();
                    byte buf[] = new byte[size];
                    int pos = 0;
                    while(pos < size) {
                        int len= in.read(buf, pos, size-pos);
                        if(len == -1) {
                            break;
                        }
                        pos += len;
                    }
                    Resource imageResource = app.createImage(buf);
                    imgMap.put(key, imageResource);
                }
            }
        } finally {
            if (zip != null) {
                zip.close();
            }
        }
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
        BSkin.Element e = (Element)map.get(name);
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
            e.setResource(app.getResource("com/tivo/hme/bananas/" + name + ".png"));
        }
        
        return e;
    }
}
