package org.lnicholls.galleon.widget;

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

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BSkin;
import com.tivo.hme.bananas.BSkin.Element;
import com.tivo.hme.sdk.HmeEvent.ResourceInfo;

import java.util.Hashtable;

public class Skin extends BSkin {
        
    public Skin(BApplication app)
    {
        super(app);
    }

    public Element get(String name)
    {
        Element element = (Element)map.get(name);
        if(element == null)
            throw new RuntimeException("unknown element: " + name);
        
        DefaultApplication defaultApplication = (DefaultApplication)app;
        DefaultScreen currentScreen = (DefaultScreen)defaultApplication.getCurrentScreen();
        if (element.name.equals("bar"))
        {
            element.rsrc = app.getResource("bar2.png");
        }
        else
            element = super.get(name);
        return element;
    }
}