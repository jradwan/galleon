package org.lnicholls.galleon.media;

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

import java.awt.image.BufferedImage;
import java.io.*;
import java.io.IOException;
import java.util.*;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Session;

import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.AudioManager.Callback;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileGatherer;
import org.lnicholls.galleon.util.*;

import org.apache.log4j.Logger;

public class MediaRefreshThread extends Thread {
    private static Logger log = Logger.getLogger(MediaRefreshThread.class.getName());

    public MediaRefreshThread() throws IOException {
        super("MediaRefreshThread");
        setPriority(Thread.MIN_PRIORITY);
        mPaths = new ArrayList();
    }

    public void run() {
        try {
            if (log.isDebugEnabled())
                log.debug("MediaRefreshThread start");

            for (int i = 0; i < mPaths.size(); i++)
                refresh((PathInfo) mPaths.get(i));

            if (log.isDebugEnabled())
                log.debug("MediaRefreshThread end");
        } // handle silently for waking up
        catch (Exception ex) {
            Tools.logException(MediaRefreshThread.class, ex);
        }
    }

    private void refresh(PathInfo pathInfo) {
        log.info("Refreshing media for: " + pathInfo.mPath);
        if (log.isDebugEnabled())
            Tools.logMemory("refresh1");
        long startTime = System.currentTimeMillis();
        // Update existing records and add new records
        FileGatherer.gatherDirectory(new File(pathInfo.mPath), pathInfo.mFilter, true,
                new FileGatherer.GathererCallback() {
                    public void visit(File file) {
                        try {
                            List list = AudioManager.findByPath(file.getAbsolutePath());
                            if (list.size() > 0) {
                                Audio audio = (Audio) list.get(0);
                                Date date = new Date(file.lastModified());
                                if (date.getTime() > audio.getDateModified().getTime()) {
                                    if (log.isDebugEnabled())
                                        log.debug("Changed: "+file.getAbsolutePath());
                                    audio = (Audio) MediaManager.getMedia(audio, file.getAbsolutePath());
                                    AudioManager.updateAudio(audio);
                                }
                            } else {
                                if (log.isDebugEnabled())
                                    log.debug("New: "+file.getAbsolutePath());
                                Audio audio = (Audio) MediaManager.getMedia(file.getAbsolutePath());
                                AudioManager.createAudio(audio);
                            }
                            Thread.sleep(10); // give the CPU some breathing time
                        } catch (Exception ex) {
                            Tools.logException(MediaRefreshThread.class, ex, file.getAbsolutePath());
                        }
                    }
                });
        // Determine any records that need to be removed
        try {
            AudioManager.scroll(new AudioManager.Callback() {
                public void visit(Session session, Audio audio) {
                    File file = new File(audio.getPath());
                    if (!file.exists())
                    {
                        if (log.isDebugEnabled())
                            log.debug("Removed: "+file.getAbsolutePath());
                        
                        try
                        {
                            session.delete(audio);
                            Thread.sleep(10);  // give the CPU some breathing time
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        
        System.gc();

        long estimatedTime = System.currentTimeMillis() - startTime;
        log.info("Refreshing media took " + (estimatedTime/1000) + " seconds");
        if (log.isDebugEnabled())
            Tools.logMemory("refresh2");
    }
    
    public static class PathInfo
    {
        public PathInfo(String path, FileFilter filter)
        {
            mPath = path;
            mFilter = filter;
        }
        
        private String mPath;
        private FileFilter mFilter;
    }
    
    
    public void addPath(PathInfo pathInfo)
    {
        mPaths.add(pathInfo);
    }
    
    public void removePath(PathInfo pathInfo)
    {
        mPaths.remove(pathInfo);
    }

    private ArrayList mPaths;
}