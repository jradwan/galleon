package org.lnicholls.galleon.skin;

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


import java.awt.*;
import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.zip.*;import javax.imageio.*;

public class WidgetLoader {    // TODO Cant use this!!
    final static Button mediaTrackerComp = new Button();

    public WidgetLoader(String filename) {        mResources = new Hashtable();        loadResource(filename);
    }

    private void loadResource(String filename) {
        ZipInputStream wsz = null;
        try {
            wsz = new ZipInputStream(new FileInputStream(filename));
            ZipEntry resource = wsz.getNextEntry();
            while (resource != null)
            {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int success = wsz.read(data);
                while (success != -1) {
                    baos.write(data, 0, success);                    success = wsz.read(data);
                }                baos.close();
            
                String name = resource.getName().toLowerCase();
                if (!resource.isDirectory())                {
                    int pos = name.indexOf("/");
                    if (pos!=-1)
                    {                        name = name.substring(pos+1);
                        
                        pos = name.indexOf("/");
                        if (pos!=-1)
                        {                            name = name.substring(pos+1).toLowerCase();
                            //System.out.println(name);                            if (name.endsWith("kon"))
                            {                                mCode = new String(baos.toByteArray());                            }
                            else                            if (name.endsWith("png") || name.endsWith("gif"))
                            {                                /*
                                Image image = Toolkit.getDefaultToolkit().createImage(baos.toByteArray());                                try {
                                    MediaTracker mt = new MediaTracker(mediaTrackerComp);
                                    mt.addImage(image, 0);
                                    mt.waitForAll();
                                } catch(InterruptedException e) {
                                }                                */                                                                BufferedImage image = ImageIO.read(new ByteArrayInputStream(baos.toByteArray()));
                                
                                mResources.put(name, image);                            }
                            else
                            if (name.endsWith("kon") || name.endsWith("js"))
                            {                                mResources.put(name, new String(baos.toByteArray()));
                            }
                            else                
                            if (resource.getName().toLowerCase().endsWith("ttf"))
                            {                                mResources.put(resource.getName(), Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(baos.toByteArray())));
                            }                        }                                            }                                    }                    
                resource = wsz.getNextEntry();            }
        } catch (Exception ex) {
            ex.printStackTrace();        } finally {
            try {
                if (wsz != null)
                    wsz.close();
            } catch (IOException ex) {
            }
        }
    }       public Object getResource(String name)   {
        return mResources.get(name.toLowerCase());   }       public String getCode() { return mCode; }
 
   private String mCode;   private Hashtable mResources;
}