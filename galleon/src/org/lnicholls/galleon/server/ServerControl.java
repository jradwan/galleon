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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppDescriptor;
import org.lnicholls.galleon.database.Video;

public interface ServerControl extends Remote {

    public void reset() throws RemoteException;

    public ServerConfiguration getServerConfiguration() throws RemoteException;
    
    public void updateServerConfiguration(ServerConfiguration serverConfiguration) throws RemoteException;

    public List getRecordings() throws RemoteException;

    public List getAppDescriptors() throws RemoteException;

    public List getApps() throws RemoteException;

    public List getTiVos() throws RemoteException;
    
    public void updateTiVos(List tivos) throws RemoteException;

    public void updateApp(AppContext app) throws RemoteException;

    public void removeApp(AppContext app) throws RemoteException;

    public void updateVideo(Video video) throws RemoteException;
    
    public void removeVideo(Video video) throws RemoteException;
    
    public AppContext createAppContext(AppDescriptor appDescriptor) throws RemoteException;
    
    public List getRules() throws RemoteException;
    
    public void updateRules(List rules) throws RemoteException;
    
    public List getSkins() throws RemoteException;
}