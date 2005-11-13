package org.lnicholls.galleon.togo;

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

import java.io.IOException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;

import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.server.Constants;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.util.ProgressListener;
import org.lnicholls.galleon.util.Tools;

public class ToGoThread extends Thread implements Constants, ProgressListener {
    private static Logger log = Logger.getLogger(ToGoThread.class.getName());

    public ToGoThread(Server server) throws IOException {
        super("ToGoThread");
        mServer = server;
        setPriority(Thread.MIN_PRIORITY);

        mToGo = new ToGo();
    }

    public void run() {
        while (true) {
            try {
                ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
                List tivos = (List) serverConfiguration.getTiVos();
                log.debug("tivos=" + tivos.size());

                ArrayList downloaded = null;
                synchronized(mToGo)
                {
                    downloaded = mToGo.getRecordings(tivos, this);
                }
                
                if (downloaded!=null && downloaded.size()>0)
                {
                    log.debug("downloaded.size()=" + downloaded.size());
                    List recordings = null; 
                    synchronized(mToGo)
                    {
                        recordings = VideoManager.listAll();
                    }
    
                    // Remove recordings that dont exist on TiVo anymore
                    Iterator iterator = recordings.listIterator();
                    while (iterator.hasNext()) {
                        Video next = (Video) iterator.next();
    
                        if (next.getStatus() != Video.STATUS_DOWNLOADED && next.getStatus() != Video.STATUS_DELETED) {
                            boolean found = false;
                            Iterator downloadedIterator = downloaded.iterator();
                            while (downloadedIterator.hasNext()) {
                                Video video = (Video) downloadedIterator.next();
                                if (video.equals(next)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found && next.getOrigen()==null) {
                                synchronized(mToGo)
                                {
                                    try {
                                        VideoManager.deleteVideo(next);
                                    } catch (HibernateException ex) {
                                        log.error("Video delete failed", ex);
                                    }
                                }
                            }
                        }
                    }
                    recordings.clear();
                    synchronized(mToGo)
                    {
                        recordings = VideoManager.listAll();
                    }
    
                    // Update status of recordings
                    iterator = downloaded.iterator();
                    while (iterator.hasNext()) {
                        Video next = (Video) iterator.next();
    
                        boolean found = false;
                        Iterator recordingsIterator = recordings.iterator();
                        while (recordingsIterator.hasNext()) {
                            Video video = (Video) recordingsIterator.next();
                            if (video.equals(next)) {
                                synchronized(mToGo)
                                {
                                    try {
                                        if (video.getStatus() == Video.STATUS_DOWNLOADED
                                                || video.getStatus() == Video.STATUS_DOWNLOADING
                                                || video.getStatus() == Video.STATUS_USER_SELECTED
                                                || video.getStatus() == Video.STATUS_USER_CANCELLED
                                                || video.getStatus() == Video.STATUS_DELETED)
                                            next.setStatus(video.getStatus());
                                        if (video.getStatus() == Video.STATUS_DOWNLOADED)
                                        {
                                            next.setPath(video.getPath());
                                            next.setDownloadSize(video.getDownloadSize());
                                            next.setDownloadTime(video.getDownloadTime());
                                            next.setDateDownloaded(video.getDateDownloaded());
                                            next.setDateUploaded(video.getDateUploaded());
                                        }
                                        PropertyUtils.copyProperties(video, next);
                                        VideoManager.updateVideo(video);
                                    } catch (HibernateException ex) {
                                        log.error("Video update failed", ex);
                                    } catch (Exception ex) {
                                        log.error("Video properties update failed", ex);
                                    }
                                }
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            synchronized(mToGo)
                            {
                                try {
                                    VideoManager.createVideo(next);
                                } catch (HibernateException ex) {
                                    log.error("Video create failed", ex);
                                }
                            }
                        }
                    }
                    synchronized(mToGo)
                    {
                        mToGo.applyRules();
                    }
    
                    recordings.clear();
                    downloaded.clear();
                    
                    sleep(1000 * 60 * 10);
                }
                else
                    sleep(1000 * 60 * 5);
                
            } catch (InterruptedException ex) {
                Tools.logException(ToGoThread.class, ex);
            } // handle silently for waking up
            catch (Exception ex2) {
                Tools.logException(ToGoThread.class, ex2);
            }
        }
    }

    public void progress(String value) {
        if (log.isDebugEnabled())
            log.debug(value);
    }
    
    public void interrupt()
    {
        synchronized(mToGo)
        {
            super.interrupt();
        }
    }
    
    public List getRecordings() throws RemoteException
    {
        try
        {
            return VideoManager.listAll();
        } catch (HibernateException ex) {
            log.error("Video listAll failed", ex);
        }
        return null;
    }
    
    private Server mServer;

    private ToGo mToGo;
}