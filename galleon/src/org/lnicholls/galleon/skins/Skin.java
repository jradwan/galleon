package org.lnicholls.galleon.skins;

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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Iterator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.util.Tools;

public class Skin {

    private static Logger log = Logger.getLogger(Skin.class.getName());

    private String mPath;
    
    private ZipFile skinFile;

    private SkinDescriptor mSkinDescriptor;

    public Skin(String path) throws IOException {
        mPath = path;
        skinFile = new ZipFile(path);
        InputStream input = null;
        try {
            input = getResourceAsStream("skin.xml");
            mSkinDescriptor = parse(input);
        } finally {
            if (input != null) {
                input.close();
            }
        }
    }
    
    public void close() throws IOException {
        if (skinFile != null) {
            skinFile.close();
            skinFile = null;
        }
    }

    public InputStream getResourceAsStream(String key) {
        try {
            ZipEntry entry = skinFile.getEntry(key);
            return skinFile.getInputStream(entry);
            
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private SkinDescriptor parse(InputStream input) {
        try {
            SAXReader saxReader = new SAXReader();
            Reader reader = new InputStreamReader(input);
            Document document = saxReader.read(reader);
            //Document document = saxReader.read(new File("d:/galleon/skin.xml"));

            SkinDescriptor skinDescriptor = new SkinDescriptor();

            Element root = document.getRootElement(); // check for errors
            skinDescriptor.setVersion(Tools.getAttribute(root, "version"));
            skinDescriptor.setTitle(Tools.getAttribute(root, "title"));
            skinDescriptor.setReleaseDate(Tools.getAttribute(root, "releaseDate"));
            skinDescriptor.setDescription(Tools.getAttribute(root, "description"));
            skinDescriptor.setAuthorName(Tools.getAttribute(root, "authorName"));
            skinDescriptor.setAuthorEmail(Tools.getAttribute(root, "authorEmail"));
            skinDescriptor.setAuthorHomepage(Tools.getAttribute(root, "authorHomepage"));

            for (Iterator imageIterator = root.elementIterator("image"); imageIterator.hasNext();) {
                Element element = (Element) imageIterator.next();
                Descriptor.Image image = new Descriptor.Image();
                image.setSource(Tools.getAttribute(element, "source"));
                image.setId(Tools.getAttribute(element, "id"));
                skinDescriptor.addImage(image);
            }

            for (Iterator i = root.elementIterator("app"); i.hasNext();) {
                Element appElement = (Element) i.next();
                SkinDescriptor.App app = new SkinDescriptor.App();
                app.setId(Tools.getAttribute(appElement, "id"));

                for (Iterator imageIterator = appElement.elementIterator("image"); imageIterator.hasNext();) {
                    Element element = (Element) imageIterator.next();
                    Descriptor.Image image = new Descriptor.Image();
                    image.setSource(Tools.getAttribute(element, "source"));
                    image.setId(Tools.getAttribute(element, "id"));
                    String resStr = Tools.getAttribute(element, "resolution");
                    if (resStr != null) {
                        image.setResolution(new Integer(resStr));
                    }
                    app.addImage(image);
                }

                for (Iterator screenIterator = appElement.elementIterator("screen"); screenIterator.hasNext();) {
                    Element screenElement = (Element) screenIterator.next();
                    SkinDescriptor.App.Screen screen = new SkinDescriptor.App.Screen();
                    screen.setId(Tools.getAttribute(screenElement, "id"));

                    for (Iterator imageIterator = screenElement.elementIterator("image"); imageIterator.hasNext();) {
                        Element element = (Element) imageIterator.next();
                        Descriptor.Image image = new Descriptor.Image();
                        image.setSource(Tools.getAttribute(element, "source"));
                        image.setId(Tools.getAttribute(element, "id"));
                        String resStr = Tools.getAttribute(element, "resolution");
                        if (resStr != null) {
                            image.setResolution(new Integer(resStr));
                        }
                        screen.addImage(image);
                    }

                    app.addScreen(screen);
                }
                skinDescriptor.addApp(app);
            }

            return skinDescriptor;
        } catch (Exception ex) {
            log.error("Could not parse skin definition", ex);
        }
        return null;
    }

    public InputStream getImageInputStream(String appId, String screenId, int resolution, String id) {
        String path = getImagePath(appId, screenId, resolution, id);
        if (path != null) {
            return getResourceAsStream(path);
        }
        return null;
    }
    
    public String getImagePath(String appId, String screenId, int resolution, String id) {
        return mSkinDescriptor.getImage(appId, screenId, resolution, id);
    }

    public String getPath() {
        return mPath;
    }
}
