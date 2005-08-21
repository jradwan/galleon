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

package org.lnicholls.galleon.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.*;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.*;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.server.*;

public class DataUpdateThread extends Thread {

    private static Logger log = Logger.getLogger(DataUpdateThread.class.getName());

    public DataUpdateThread() throws IOException {
        super("DataUpdateThread");
        setPriority(Thread.MIN_PRIORITY);
    }

    public void run() {
        while (true)
        {
	    	ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
        	DataConfiguration dataConfiguration = serverConfiguration.getDataConfiguration();
        	/*
        	AppManager appManager = Server.getServer().getAppManager();
        	if (dataConfiguration!=null && dataConfiguration.isAgree())
        	{
	        	try {
		            if (log.isDebugEnabled())
		                log.debug("DataUpdateThread start");
		            
		            Users.login(dataConfiguration);
		            
		            // TODO Limit how much per hour can be uploaded
		            List apps = appManager.getApps();
		            Iterator iterator = apps.iterator();
		            while (iterator.hasNext())
		            {
		            	AppContext appContext = (AppContext)iterator.next();
		            	AppDescriptor appDescriptor = appContext.getDescriptor();
		            	
		            	System.out.println(appDescriptor);
		            }
		            
		            Users.logout(dataConfiguration);
		            
		            sleep(1000*60*60);
		        } 
		        catch (Exception ex) {
		            Tools.logException(DataUpdateThread.class, ex);
		        }
        	}
        	*/
        }
    }
}