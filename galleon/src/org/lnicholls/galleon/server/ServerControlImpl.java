package org.lnicholls.galleon.server;

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

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;

import net.sf.hibernate.HibernateException;

import org.lnicholls.galleon.database.VideoManager;

import org.apache.log4j.Logger;

public class ServerControlImpl extends UnicastRemoteObject implements ServerControl {
    
    private static Logger log = Logger.getLogger(ServerControlImpl.class.getName());
    
    public ServerControlImpl() throws RemoteException {
        super();
    }

    public void reset() throws RemoteException {
        Server.getServer().reconfigure();
    }
    
    public List getRecordings() throws RemoteException
    {
        try
        {
            return VideoManager.listAll();
        } catch (HibernateException ex) {
            log.error("Video listAll failed", ex);
        }
        return new ArrayList();
    }
}