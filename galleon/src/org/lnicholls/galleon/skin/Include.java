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
import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;

import org.mozilla.javascript.*;public class Include {
     public Include(WidgetLoader widgetLoader, Scriptable scope) 
     { 
        mWidgetLoader = widgetLoader;
        mScope = scope;
     }
     
     private String getSource(String file) 
     { 
        return (String)mWidgetLoader.getResource(file);
     }
     
     public void load(String file)
     {
        Context context = Context.enter();
        try
        {
            String source = getSource(file);            if (mScope!=null && source!=null)
            {                Object result = context.evaluateString(mScope, source, "<include>", 1, null);
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
        }     
    }        
     
    private WidgetLoader mWidgetLoader;
    private Scriptable mScope;
}