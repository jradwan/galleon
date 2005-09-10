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

import java.awt.Image;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.ArrayList;

import net.sf.hibernate.HibernateException;

import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppDescriptor;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.downloads.Download;

import org.apache.log4j.Logger;

public class ServerControlImpl extends UnicastRemoteObject implements ServerControl {
    
    private static Logger log = Logger.getLogger(ServerControlImpl.class.getName());
    
    public ServerControlImpl() throws RemoteException {
        super();
    }
    
    public ServerConfiguration getServerConfiguration()  throws RemoteException
    {
        return Server.getServer().getServerConfiguration();
    }
    
    public void updateServerConfiguration(ServerConfiguration serverConfiguration) throws RemoteException
    {
        try
        {
        	Server.getServer().updateServerConfiguration(serverConfiguration);
        }
        catch (Exception ex)
        {
        	throw new RemoteException(ex.getMessage(), ex);
        }
    }
    
    public DataConfiguration getDataConfiguration()  throws RemoteException
    {
        return Server.getServer().getDataConfiguration();
    }
    
    public void updateDataConfiguration(DataConfiguration dataConfiguration) throws RemoteException
    {
        try
        {
        	Server.getServer().updateDataConfiguration(dataConfiguration);
        }
        catch (Exception ex)
        {
        	throw new RemoteException(ex.getMessage(), ex);
        }
    }    
    
    public GoBackConfiguration getGoBackConfiguration()  throws RemoteException
    {
        return Server.getServer().getGoBackConfiguration();
    }
    
    public void updateGoBackConfiguration(GoBackConfiguration goBackConfiguration) throws RemoteException
    {
        try
        {
        	Server.getServer().updateGoBackConfiguration(goBackConfiguration);
        }
        catch (Exception ex)
        {
        	throw new RemoteException(ex.getMessage(), ex);
        }
    }    
    
    public DownloadConfiguration getDownloadConfiguration()  throws RemoteException
    {
        return Server.getServer().getDownloadConfiguration();
    }
    
    public void updateDownloadConfiguration(DownloadConfiguration downloadConfiguration) throws RemoteException
    {
        try
        {
        	Server.getServer().updateDownloadConfiguration(downloadConfiguration);
        }
        catch (Exception ex)
        {
        	throw new RemoteException(ex.getMessage(), ex);
        }
    }    

    public void reset() throws RemoteException {
        Server.getServer().reconfigure();
    }
    
    public List getRecordings() throws RemoteException
    {
        return Server.getServer().getToGoThread().getRecordings();
    }
    
    public List getAppDescriptors() throws RemoteException
    {
        return Server.getServer().getAppDescriptors();
    }
    
    public List getApps() throws RemoteException
    {
        return Server.getServer().getApps();
    }    
    
    public List getTiVos() throws RemoteException
    {
        return Server.getServer().getTiVos();
    }
    
    public void updateTiVos(List tivos) throws RemoteException
    {
        Server.getServer().updateTiVos(tivos);
    }
    
    public void removeApp(AppContext app)  throws RemoteException
    {
        Server.getServer().removeApp(app);
    }
    
    public void updateApp(AppContext app)  throws RemoteException
    {
        Server.getServer().updateApp(app);
    }    
    
    public void updateVideo(Video video) throws RemoteException
    {
        Server.getServer().updateVideo(video);
    }
    
    public void removeVideo(Video video) throws RemoteException
    {
        Server.getServer().removeVideo(video);
    }
    
    public AppContext createAppContext(AppDescriptor appDescriptor) throws RemoteException
    {
        return Server.getServer().createAppContext(appDescriptor);
    }
    
    public List getRules() throws RemoteException
    {
        return Server.getServer().getRules();
    }
    
    public void updateRules(List rules) throws RemoteException
    {
        Server.getServer().updateRules(rules);
    }
    
    public List getSkins() throws RemoteException
    {
        return Server.getServer().getSkins();
    }    
    
    public List getWinampSkins() throws RemoteException
    {
        return Server.getServer().getWinampSkins();
    }
    
    public int getPort() throws RemoteException
    {
        return Server.getServer().getPort();
    }
    
    public List getPodcasts() throws RemoteException
    {
        return Server.getServer().getPodcasts();
    }    
    
    public void setPodcasts(List list) throws RemoteException
    {
        Server.getServer().setPodcasts(list);
    }
    
    public List getVideocasts() throws RemoteException
    {
        return Server.getServer().getVideocasts();
    }    
    
    public void setVideocasts(List list) throws RemoteException
    {
        Server.getServer().setVideocasts(list);
    }
    
    public boolean isCurrentVersion() throws RemoteException
    {
    	return Server.getServer().isCurrentVersion();
    }
    
    public Object getCodeImage() throws RemoteException
    {
    	return Server.getServer().getCodeImage();
    }
    
    public List getDownloads()  throws RemoteException
    {
        return Server.getServer().getDownloads();
    }
    
    public void pauseDownload(Download download) throws RemoteException
    {
    	Server.getServer().pauseDownload(download);
    }

	public void resumeDownload(Download download) throws RemoteException
	{
		Server.getServer().resumeDownload(download);
	}
}