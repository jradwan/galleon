/*
 * Copyright (C) 2005 Leon Nicholls
 * Copyright (C) 2007 John Kohl
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
/* Derived from iTunes.java */
package org.lnicholls.galleon.apps.photos;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.IHmeProtocol;

import java.awt.Color;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.database.ImageAlbums;
import org.lnicholls.galleon.database.ImageAlbumsManager;
import org.lnicholls.galleon.database.ImageAlbumsPictures;
import org.lnicholls.galleon.database.ImageAlbumsPicturesManager;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class iPhotoMenuScreen extends DefaultMenuScreen {
    public iPhotoMenuScreen(Photos app) {
        this(app, false, false);
    }
    public iPhotoMenuScreen(Photos app, boolean isAlbumScreen, boolean isRollScreen) {
        super(app, "iPhoto" + (isAlbumScreen ? " Albums" : "") + (isRollScreen ? " Rolls" : ""));
        misAlbumScreen = isAlbumScreen;
        misRollScreen = isRollScreen;
        setFooter("Press ENTER for options");
        getBelow().setResource(app.getMenuBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
        getBelow().flush();
        List albums = null;
        String title;
        if (isAlbumScreen) {
            try {
                albums = ImageAlbumsManager.listAlbums();
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            }
            for (Iterator i = albums.iterator(); i.hasNext(); /* Nothing */) {
                title = (String) i.next();
                mMenuList.add(new FolderItem(title, title));
            }
        } else if (isRollScreen) {
            try {
                albums = ImageAlbumsManager.listRolls();
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            }
            for (Iterator i = albums.iterator(); i.hasNext(); /* Nothing */) {
                title = (String) i.next();
                mMenuList.add(new FolderItem(title, title));
            }
        } else {
            mMenuList.add(new FolderItem("Albums", "Albums"));
            mMenuList.add(new FolderItem("Rolls", "Rolls"));
        }
        mCountText = new BText(getNormal(), BORDER_LEFT, TOP - 30, BODY_WIDTH, 20);
        mCountText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER);
        mCountText.setFont("default-18.font");
        mCountText.setColor(Color.GREEN);
        mCountText.setShadow(true);
    }
    
    public Photos getApp() {
        return (Photos)super.getApp();
    }
    
    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        long count = 0;
        try {
            count = ImageManager.countImagesByOrigin("iPhoto");
        } catch (Exception ex) {
            Tools.logException(Photos.class, ex);
        }

        String status = "Total images: " + String.valueOf(count);
        PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Photos.class.getName()
                                                                                     + ".refresh3");
        if (persistentValue != null) {
            String refreshed = persistentValue.getValue();
            if (refreshed != null) {
                try
					{
						SimpleDateFormat dateFormat = new SimpleDateFormat();
						dateFormat.applyPattern("M/d/yy hh:mm a");
						status = status + "   Refreshed: " + dateFormat.format(new Date(Long.valueOf(refreshed))); 
					}
                catch (Exception ex)
					{
						Logger log = Logger.getLogger(Photos.class.getName());
						log.error("Invalid date: " + persistentValue.getValue(), ex);
					}
            }
        }

        mCountText.setValue(status);

        return super.handleEnter(arg, isReturn);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("push")) {
            if (mMenuList.size() > 0) {
                load();
                if (!misAlbumScreen && !misRollScreen) {
                    switch (mMenuList.getFocus()) {
                    case 0:
                        getBApp().push(new iPhotoMenuScreen(getApp(), true, false), TRANSITION_NONE);
                        break;
                    case 1:
                        getBApp().push(new iPhotoMenuScreen(getApp(), false, true), TRANSITION_NONE);
                        break;
                    default:
                        break;
                    }
                    return true;
                }

                new Thread() {
                    public void run() {
                        try {
                            FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
                            List list = ImageAlbumsManager.findByTitle((String) nameFile.getValue());
                            // XXX common code with size == 1!
                            if (list!=null && list.size()>0)
								{
									ImageAlbums imageAlbum = (ImageAlbums) list.get(0);
									Logger log = Logger.getLogger(Photos.class.getName());
									
									ArrayList pictures = new ArrayList();
									List imageAlbums = ImageAlbumsPicturesManager.findByImageAlbums(imageAlbum.getId());
									if (imageAlbums!=null && imageAlbums.size()>0)
                                        {
											log.debug("Album " + (String) nameFile.getValue() + " has " + imageAlbums.size() + " pictures");
                                            Iterator iterator = imageAlbums.iterator();
                                            while (iterator.hasNext()) {
                                                ImageAlbumsPictures picture = (ImageAlbumsPictures) iterator.next();
                                                if (picture.getPicture()!=0) {
                                                    Image image = ImageManager.retrieveImage(picture.getPicture());
                									log.debug("Album " + (String) nameFile.getValue() + "includes " + image.getPath());
                                                    pictures.add(new FileItem(image.getTitle(), new File(image.getPath())));
                                                }
                                            }
                                            imageAlbums.clear();
                                        }
									Tracker tracker = new Tracker(pictures, 0);
									PathScreen pathScreen = new PathScreen((Photos) getBApp(), tracker);
									getBApp().push(pathScreen, TRANSITION_LEFT);
									list.clear();
								}
                        } catch (Exception ex) {
                            Tools.logException(Photos.class, ex);
                        }
                    }
                }.start();
                return true;
            }
        } else if (action.equals("play")) {
            load();
            if (!misAlbumScreen && !misRollScreen) {
                switch (mMenuList.getFocus()) {
                case 0:
                    getBApp().push(new iPhotoMenuScreen(getApp(), true, false), TRANSITION_NONE);
                    break;
                case 1:
                    getBApp().push(new iPhotoMenuScreen(getApp(), false, true), TRANSITION_NONE);
                    break;
                default:
                    break;
                }
                return true;
            }

            new Thread() {
                public void run() {
                    try {
                        FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
                        List imageAlbums = ImageAlbumsManager.findByTitle((String) nameFile.getValue());
						Logger log = Logger.getLogger(Photos.class.getName());

						if (imageAlbums != null && imageAlbums.size() > 0) {
                            ImageAlbums imageAlbum = (ImageAlbums) imageAlbums.get(0);

                            List pList = ImageAlbumsPicturesManager.findByImageAlbums(imageAlbum.getId());
                            ArrayList pictures = new ArrayList();
                            Iterator iterator = pList.iterator();
                            while (iterator.hasNext()) {
                                ImageAlbumsPictures picture = (ImageAlbumsPictures) iterator.next();
                                if (picture.getPicture()!=0) {
                                    Image image = ImageManager.retrieveImage(picture.getPicture());
									log.debug("Album " + (String) nameFile.getValue() + "includes " + image.getPath());
                                    pictures.add(new FileItem(image.getTitle(), new File(image.getPath())));
                                }
                            }
                            Tracker tracker = new Tracker(pictures, 0);
                            PhotosConfiguration imagesConfiguration = getConfiguration();
                            tracker.setRandom(imagesConfiguration
                                              .isRandomPlayFolders());
                            getBApp().push(
                                new SlideshowScreen((Photos) getBApp(),
                                                    tracker), TRANSITION_LEFT);
                            getBApp().flush();
                        }
                    } catch (Exception ex) {
                        Tools.logException(Photos.class, ex);
                    }
                }
            }.start();
            return true;
        }

        return super.handleAction(view, action);
    }
    protected PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
	}

	protected void createRow(BView parent, int index) {
        BView icon = new BView(parent, 9, 2, 32, 32);
        Item nameFile = (Item) mMenuList.get(index);
        if (!misAlbumScreen && !misRollScreen) {
            icon.setResource(getApp().getFolderIcon());
            icon.flush();
        } else {
            icon.setResource(getApp().getCameraIcon());
            icon.flush();
        }
        BText name = new BText(parent, 50, 4, parent.getWidth() - 40,
                               parent.getHeight() - 4);
        name.setShadow(true);
        name.setFlags(RSRC_HALIGN_LEFT);
        name.setValue(Tools.trim(nameFile.getName(), 40));
    }
    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_LEFT:
        	postEvent(new BEvent.Action(this, "pop"));
        	return true;
        case KEY_PLAY:
            postEvent(new BEvent.Action(this, "play"));
            return true;
        case KEY_ENTER:
            getBApp().push(new OptionsScreen((Photos) getBApp()),
                           TRANSITION_LEFT);
        }
        return super.handleKeyPress(code, rawcode);
    }
    BText mCountText;
    private boolean misAlbumScreen;
    private boolean misRollScreen;
}
