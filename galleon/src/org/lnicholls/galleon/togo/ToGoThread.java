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
import java.util.ArrayList;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.server.*;

public class ToGoThread extends Thread implements Constants, ProgressListener  {
    private static Logger log = Logger.getLogger(ToGoThread.class.getName());

    public ToGoThread(Server server) throws IOException {
        super("ToGoThread");
        mServer = server;
        setPriority(Thread.MIN_PRIORITY);
        
        mToGo = new ToGo(server.getServerConfiguration());
    }

    public void run() {
        ToGoList togoList = new ToGoList();
        ArrayList recordings = new ArrayList();
        while (true) {
            try {
                ArrayList tivos = (ArrayList)mToGo.getServerConfiguration().getTiVos().clone(); 
                log.debug("tivos="+tivos.size());

                mToGo.getRecordings(tivos,this,recordings);
                
                ArrayList downloaded = (ArrayList)togoList.load();
                
                ArrayList cleaned = new ArrayList();
                
                // Remove recordings that dont exist on TiVo anymore
                Iterator downloadedIterator = downloaded.listIterator();
                while (downloadedIterator.hasNext()) {
                    Show next = (Show) downloadedIterator.next();
                
                    boolean found = false;
                    Iterator recordedIterator = recordings.iterator();
                    while (recordedIterator.hasNext()) {
                        Show show = (Show) recordedIterator.next();
                        if (show.equals(next)) {
                            found = true;
                            break;
                        }
                    }
                    if (found || (next.getStatus()==Show.STATUS_DOWNLOADED))
                    {
                        log.debug("Adding: "+next);
                        cleaned.add(next);
                    }
                    else
                    {
                        log.debug("Removing: "+next);
                    }
                }
                
                // Update status of recordings
                Iterator recordedIterator = recordings.iterator();
                while (recordedIterator.hasNext()) {
                    Show next = (Show) recordedIterator.next();
                
                    boolean found = false;
                    Iterator iterator = cleaned.iterator();
                    while (iterator.hasNext()) {
                        Show show = (Show) iterator.next();
                        if (show.equals(next)) {
                            if (show.getStatus()==Show.STATUS_RECORDED || show.getStatus()==Show.STATUS_RECORDING)
                                show.setStatus(next.getStatus());
                            show.setDuration(next.getDuration());
                            show.setSize(next.getSize());
                            show.setIcon(next.getIcon());
                            found = true;
                            break;
                        }
                    }
                    if (!found)
                    {
                        cleaned.add(next);
                    }
                }
                togoList.save(cleaned);
                
                mToGo.applyRules();
                
                tivos.clear();
                //recordings.clear();
                downloaded.clear();
                cleaned.clear();

                sleep(1000 * 60 * 1);
            } catch (InterruptedException ex) {
            } // handle silently for waking up
            catch (Exception ex2) {
                Tools.logException(ToGoThread.class, ex2);
            }
        }
    }
    
    public void progress(String value)
    {
        if (log.isDebugEnabled())
            log.debug(value);
    }

    private Server mServer;

    private ToGo mToGo;
    
    public void setServerConfiguration(ServerConfiguration value)
    {
        mToGo.setServerConfiguration(value);
    }
}