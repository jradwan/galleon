package org.lnicholls.galleon.util;

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
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

public class Amazon {

    private static final Logger log = Logger.getLogger(Amazon.class.getName());

    /*
     * The following is required by the Amazon.com web services. DO NOT USE THESE WITH ANY OTHER PROJECT SINCE THEY HAVE
     * BEEN REGISTERED WITH AMAZON.COM FOR THE GALLEON PROJECT. Obtain your own key by registering at:
     * http://www.amazon.com/gp/browse.html/ref=smm_sn_aws/102-9770766-0077768?%5Fencoding=UTF8&node=3435361
     */

    private final static String SUBSCRIPTION_ID = "1S15VY5XR4PV42R2YRG2";

    public static BufferedImage getAlbumImage(String key, String artist, String title) {
        // http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&SubscriptionId=1S15VY5XR4PV42R2YRG2&Operation=ItemSearch&SearchIndex=Music&ResponseGroup=Images&Artist="
        // + artist + "&Title=" + title

        try {
            URL url = new URL("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&SubscriptionId="
                    + SUBSCRIPTION_ID + "&Operation=ItemSearch&SearchIndex=Music&ResponseGroup=Images&Artist="
                    + URLEncoder.encode(artist) + "&Title=" + URLEncoder.encode(title));
            return getImage(key, url);
        } catch (MalformedURLException ex) {
            log.error("Could not create AWS url", ex);
        }
        return null;
    }

    public static BufferedImage getMusicImage(String key, String keywords) {
        try {
            URL url = new URL("http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&SubscriptionId="
                    + SUBSCRIPTION_ID + "&Operation=ItemSearch&SearchIndex=Music&ResponseGroup=Images&Keywords="
                    + URLEncoder.encode(keywords));
            return getImage(key, url);
        } catch (MalformedURLException ex) {
            log.error("Could not create AWS url", ex);
        }
        return null;
    }

    public static synchronized BufferedImage getImage(String key, URL url) {
        BufferedImage image = null;

        if (System.currentTimeMillis() - mTime < 1000) {
            // Not allowed to call AWS more than once a second
            try {
                Thread.currentThread().sleep(1000);
            } catch (Exception ex) {
            }
        }

        try {
            SAXReader saxReader = new SAXReader();
            // http://webservices.amazon.com/onca/xml?Service=AWSECommerceService&SubscriptionId=1S15VY5XR4PV42R2YRG2&Operation=ItemSearch&SearchIndex=Music&ResponseGroup=Images&Artist="
            // + artist + "&Title=" + title
            String page = Tools.getPage(url);
            log.debug("Amazon images: " + page);

            StringReader stringReader = new StringReader(page);
            Document document = saxReader.read(stringReader);

            //Document document = saxReader.read(new File("d:/galleon/amazon.xml"));
            Element root = document.getRootElement();

            Element items = root.element("Items");
            if (items != null) {
                for (Iterator i = items.elementIterator("Item"); i.hasNext();) {
                    Element item = (Element) i.next();

                    Element someImage = item.element("LargeImage");
                    if (someImage==null)
                        someImage = item.element("MediumImage");
                    if (someImage==null)
                        someImage = item.element("SmallImage");
                    if (someImage != null) {
                        Element address = someImage.element("URL");
                        if (address != null) {
                            log.debug(address.getTextTrim());

                            Element height = someImage.element("Height");
                            Element width = someImage.element("Width");
                            try {
                                Tools.cacheImage(new URL(address.getTextTrim()),
                                        Integer.parseInt(height.getTextTrim()), Integer.parseInt(width.getTextTrim()),
                                        key);
                                image = Tools.retrieveCachedImage(key);
                                break;
                            } catch (Exception ex) {
                                log.error("Could not download Amazon image: " + address.getTextTrim(), ex);
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Could not determine weather conditions", ex);
        }

        mTime = System.currentTimeMillis();
        return image;
    }

    private static long mTime = System.currentTimeMillis();

}