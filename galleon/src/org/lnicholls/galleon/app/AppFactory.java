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

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Configurator;

import com.tivo.hme.sdk.*;
import com.tivo.hme.io.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.share.*;
import com.tivo.hme.http.server.*;

public class AppFactory extends Factory {

    private static Logger log = Logger.getLogger(AppFactory.class.getName());

    public AppFactory(AppManager appManager) {
        if (System.getProperty("apps") != null) {
            File file = new File(System.getProperty("apps") + "/launcher.txt");
            if (file.exists()) {
                FastInputStream in = null;
                try {
                    in = new FastInputStream(new FileInputStream(file), 1024);

                    Listener listener = new Listener(new ArgumentList(""));
                    String ln = in.readLine();
                    while (ln != null) {
                        ln = ln.trim();
                        if (!ln.startsWith("#") && ln.length() > 0) {
                            try
                            {
                                System.out.println("Found: "+ln);
                                appManager.addHMEApp(ln);
                                startFactory(listener, new ArgumentList(ln));
                            }
                            catch (Throwable th)
                            {
                                System.out.println("error: " + th.getMessage()+" for "+ln);
                                th.printStackTrace();
                            }
                        }
                        ln = in.readLine();
                    }
                } catch (IOException e) {
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
}