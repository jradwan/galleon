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


import java.awt.Image;import java.awt.Color;
import java.awt.image.*;import java.io.*;import java.net.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;import org.mozilla.javascript.*;public class ScriptUrl extends ScriptableObject {
     // The zero-argument constructor used by Rhino runtime to create instances
     public ScriptUrl() 
     { 
     }
     
     // Method jsConstructor defines the JavaScript constructor
     public void jsConstructor() { }
 
     // The class name is defined by the getClassName method
     public String getClassName() { return "URL"; }
 
     public String jsFunction_fetch(String address) 
     { 
        System.out.println("fetch: "+address);
        try
        {
            URL url = new URL(address);
            InputStream inputStream = url.openStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] data = new byte[1024];
            int success = inputStream.read(data);
            while (success != -1) {
                baos.write(data, 0, success);                success = inputStream.read(data);
            }            baos.close();
            return new String(baos.toByteArray());
        }
        catch (Exception ex) 
        {
            ex.printStackTrace();
        }    
        
        return "";
     }
}