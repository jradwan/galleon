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

import java.io.IOException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.*;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;

public class TiVoListener implements ServiceListener, ServiceTypeListener {
    private static Logger log = Logger.getLogger(TiVoListener.class.getName());

    // TiVo with 7.1 software supports rendevouz and has a web server
    private final static String HTTP_SERVICE = "_http._tcp.local.";

    private final static String TIVO_PLATFORM = "platform";

    private final static String TIVO_PLATFORM_PREFIX = "tcd"; // platform=tcd/Series2

    private final static String TIVO_TSN = "TSN";

    private final static String TIVO_SW_VERSION = "swversion";

    private final static String TIVO_PATH = "path";

    public TiVoListener() {
        try {
            ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
            String address = serverConfiguration.getIPAddress();
            InetAddress inetAddress = InetAddress.getLocalHost();
            if (address != null && address.length() > 0)
                inetAddress = InetAddress.getByName(address);
            
            final JmDNS jmdns = new JmDNS(inetAddress);
            jmdns.addServiceListener(HTTP_SERVICE, this);
            log.debug("Interface: " + jmdns.getInterface());
            
            Server.getServer().scheduleShortTerm(new ReloadTask(new ReloadCallback() {
                public void reload() {
                    try {
                        Set keys = mServices.keySet();
                        Iterator iterator = keys.iterator();
                        while (iterator.hasNext())
                        {
                            String service = (String)iterator.next();
                            log.debug("refresh service request: "+service);
                            ServiceEvent event = (ServiceEvent)mServices.get(service);
                            jmdns.requestServiceInfo(event.getType(), event.getName());
                        }
                    } catch (Exception ex) {
                        log.error("Could not refresh services", ex);
                    }
                }
            }), 15);
            
        } catch (IOException ex) {
            Tools.logException(TiVoListener.class, ex);
        }
    }

    public void serviceAdded(ServiceEvent event) {
        JmDNS jmdns = event.getDNS();
        String type = event.getType();
        String name = event.getName();
        log.debug("addService: " + name);

        jmdns.requestServiceInfo(type, name);
        mServices.put(name,event);
    }

    public void serviceRemoved(ServiceEvent event) {
        JmDNS jmdns = event.getDNS();
        String type = event.getType();
        String name = event.getName();
        log.debug("removeService: " + name);
        mServices.remove(name);
    }

    public void serviceTypeAdded(ServiceEvent event) {
        JmDNS jmdns = event.getDNS();
        String type = event.getType();
        String name = event.getName();
        log.debug("addServiceType: " + type);
    }

    public void serviceResolved(ServiceEvent event) {
        JmDNS jmdns = event.getDNS();
        String type = event.getType();
        String name = event.getName();
        ServiceInfo info = event.getInfo();
        log.debug("resolveService: " + type + " (" + name + ")");

        /*
         * DVR AAB0._http._tcp.local. // name DVR-AAB0.local.:80 // server:port 192.168.0.5:80 // address:port
         * platform=tcd/Series2 TSN=24020348251AAB0 swversion=7.1.R1-01-2-240 path=/index.html
         */

        if (type.equals(HTTP_SERVICE)) {
            boolean found = false;
            TiVo tivo = new TiVo();
            tivo.setName(name);
            tivo.setServer(info.getServer());
            tivo.setPort(info.getPort());
            tivo.setAddress(info.getAddress().getHostAddress());

            for (Enumeration names = info.getPropertyNames(); names.hasMoreElements();) {
                String prop = (String) names.nextElement();
                if (prop.equals(TIVO_PLATFORM)) {
                    tivo.setPlatform(info.getPropertyString(prop));
                    if (tivo.getPlatform().startsWith(TIVO_PLATFORM_PREFIX))
                        found = true;
                } else if (prop.equals(TIVO_TSN)) {
                    tivo.setServiceNumber(info.getPropertyString(prop));
                } else if (prop.equals(TIVO_SW_VERSION)) {
                    tivo.setSoftwareVersion(info.getPropertyString(prop));
                } else if (prop.equals(TIVO_PATH)) {
                    tivo.setPath(info.getPropertyString(prop));
                }
            }

            if (found) {
                List tivos = Server.getServer().getTiVos();
                boolean matched = false;
                Iterator iterator = tivos.iterator();
                while (iterator.hasNext()) {
                    TiVo knownTiVo = (TiVo) iterator.next();
                    if (knownTiVo.getAddress().equals(tivo.getAddress())) {
                        matched = true;
                        boolean modified = false;
                        if (!tivo.getPlatform().equals(knownTiVo.getPlatform())) {
                            knownTiVo.setPlatform(tivo.getPlatform());
                            modified = true;
                        }
                        if (!tivo.getServiceNumber().equals(knownTiVo.getServiceNumber())) {
                            knownTiVo.setServiceNumber(tivo.getServiceNumber());
                            modified = true;
                        }
                        if (!tivo.getSoftwareVersion().equals(knownTiVo.getSoftwareVersion())) {
                            knownTiVo.setSoftwareVersion(tivo.getSoftwareVersion());
                            modified = true;
                        }
                        if (!tivo.getPath().equals(knownTiVo.getPath())) {
                            knownTiVo.setPath(tivo.getPath());
                            modified = true;
                        }
                        if (!tivo.getServer().equals(knownTiVo.getServer())) {
                            knownTiVo.setServer(tivo.getServer());
                            modified = true;
                        }
                        if (tivo.getPort() != knownTiVo.getPort()) {
                            knownTiVo.setPort(tivo.getPort());
                            modified = true;
                        }
                        if (modified)
                            Server.getServer().updateTiVos(tivos);
                    }
                }
                if (!matched) {
                    tivos.add(tivo);
                    Server.getServer().updateTiVos(tivos);
                    log.info("Found TiVo: " + tivo.toString());
                }
            }
        }
    }
    
    private HashMap mServices = new HashMap();
}