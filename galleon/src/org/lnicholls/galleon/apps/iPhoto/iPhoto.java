package org.lnicholls.galleon.apps.iPhoto;

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


import java.awt.Color;
import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.lnicholls.galleon.database.Image;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.photos.PhotosConfiguration;
import org.lnicholls.galleon.apps.photos.Photos.PhotosFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.database.Imagelists;
import org.lnicholls.galleon.database.ImagelistsManager;
import org.lnicholls.galleon.database.ImagelistsTracks;
import org.lnicholls.galleon.database.ImagelistsTracksManager;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;

public class iPhoto extends DefaultApplication {

    private static Logger log = Logger.getLogger(iPhoto.class.getName());

    public final static String TITLE = "iPhoto";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mPlayerBackground;

    private Resource mLyricsBackground;

    private Resource mImagesBackground;

    private Resource mFolderIcon;

    private Resource mCameraIcon;

    private Resource mImagelistIcon;

    private Resource mLargeFolderIcon;

    public void init(IContext context) throws Exception {
        super.init(context);
    }
    public void initService() {
        super.initService();

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mLargeFolderIcon = getSkinImage("menu", "gridFolder");
        mCameraIcon = getSkinImage("menu", "item");
        mImagelistIcon = getSkinImage("menu", "playlist"); // XXX

        iPhotoConfiguration imagesConfiguration = (iPhotoConfiguration) ((iPhotoFactory) getFactory())
            .getAppContext().getConfiguration();

        PersistentValue persistentValue = PersistentValueManager
            .loadPersistentValue(DefaultApplication.TRACKER);
        if (persistentValue != null) {
            List files = new ArrayList();
            StringTokenizer tokenizer = new StringTokenizer(persistentValue
                                                            .getValue(), DefaultApplication.SEPARATOR);
            while (tokenizer.hasMoreTokens())
                {
                    String id = tokenizer.nextToken();
                    Audio audio = AudioManager.retrieveAudio(new Integer(id));
                    if (audio != null)
                        {
                            files.add(new FileItem(audio.getTitle(), new File(audio
                                                                              .getPath())));
                        }
                }
            Tracker tracker = new Tracker(files, 0);
            tracker.setRandom(imagesConfiguration.isRandomPlayFolders());
            setTracker(tracker);
        }

        List titles = null;
        try {
            titles = ImagelistsManager.listTitles();
        } catch (Exception ex) {
            Tools.logException(iPhoto.class, ex);
        }
        if (titles.size() == 1) {
            try {
                String title = (String) titles.get(0);

                List list = ImagelistsManager.findByTitle(title);
                if (list!=null && list.size()>0)
                    {
                        Imagelists imagelist = (Imagelists) list.get(0);

                        ArrayList tracks = new ArrayList();
                        List imagelists = ImagelistsTracksManager.findByImagelists(imagelist.getId());
                        if (imagelists!=null && imagelists.size()>0) {
                            Iterator iterator = imagelists.iterator();
                            while (iterator.hasNext()) {
                                ImagelistsTracks track = (ImagelistsTracks) iterator.next();
                                if (track.getTrack()!=0)
                                    {
                                        Image image = ImageManager.retrieveImage(track.getTrack());
                                        tracks.add(new FileItem(image.getTitle(), new File(image.getPath())));
                                    }
                            }
                            imagelists.clear();
                        }
                        Tracker tracker = new Tracker(tracks, 0);
                        PathScreen pathScreen = new PathScreen((iPhoto) this, tracker, true);
                        push(pathScreen, TRANSITION_LEFT);
                        list.clear();
                    }
            } catch (Exception ex) {
                Tools.logException(iPhoto.class, ex);
            }
        } else
            push(new iPhotoMenuScreen(this), TRANSITION_NONE);

        initialize();
    }
    public Resource getCameraIcon() {
        return mCameraIcon;
    }
    public void setCameraIcon(Resource cameraIcon) {
        this.mCameraIcon = cameraIcon;
    }
    public Resource getFolderIcon() {
        return mFolderIcon;
    }
    public void setFolderIcon(Resource folderIcon) {
        this.mFolderIcon = folderIcon;
    }
    public Resource getMenuBackground() {
        return mMenuBackground;
    }
    public Resource getInfoBackground() {
        return mInfoBackground;
    }
    public Resource getLargeFolderIcon() {
        return mLargeFolderIcon;
    }
    public void setLargeFolderIcon(Resource largeFolderIcon) {
        this.mLargeFolderIcon = largeFolderIcon;
    }
    // XXX crib more accessors from Photos.java?

    public class iPhotoMenuScreen extends DefaultMenuScreen {
        // XXX can't we share code with PhotosMenuScreen.java ?
        public iPhotoMenuScreen(iPhoto app) {
            super(app, TITLE);
            setFooter("Press ENTER for options");
            getBelow().setResource(app.getMenuBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);
            getBelow().flush();
            List titles = null;
            try {
                titles = ImagelistsManager.listTitles();
            } catch (Exception ex) {
                Tools.logException(iPhoto.class, ex);
            }
            for (Iterator i = titles.iterator(); i.hasNext(); /* Nothing */) {
				String title = (String) i.next();
				mMenuList.add(new FolderItem(title, title));
			}

			mCountText = new BText(getNormal(), BORDER_LEFT, TOP - 30, BODY_WIDTH, 20);
			mCountText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER);
			mCountText.setFont("default-18.font");
			mCountText.setColor(Color.GREEN);
			mCountText.setShadow(true);
        }
    
        public iPhoto getApp() {
            return (iPhoto)super.getApp();
        }
    
		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			long count = 0;
			try {
				count = ImageManager.countImagesByOrigin("iPhoto");
			} catch (Exception ex) {
				Tools.logException(iPhoto.class, ex);
			}

			String status = "Total images: " + String.valueOf(count);
			PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(iPhoto.class.getName()
					+ ".refresh3");
			if (persistentValue != null) {
				String refreshed = persistentValue.getValue();
				if (refreshed != null) {
					try
					{
						SimpleDateFormat dateFormat = new SimpleDateFormat();
						dateFormat.applyPattern("M/d/yy hh:mm a");
						status = status + "   Refreshed: " + dateFormat.format(new Date(refreshed)); // doesn't work ...
					}
					catch (Exception ex)
					{
						log.error("Invalid date: " + persistentValue.getValue(), ex);
					}
				}
			}

			mCountText.setValue(status);

			return super.handleEnter(arg, isReturn);
		}

 //       public iPhotoConfiguration getConfiguration() {
 //           return getApp().getConfiguration();
 //       }
        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
				if (mMenuList.size() > 0) {
					load();

					new Thread() {
						public void run() {
							try {
								FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
								List list = ImagelistsManager.findByTitle((String) nameFile.getValue());
                                // XXX common code with size == 1!
								if (list!=null && list.size()>0)
								{
									Imagelists imagelist = (Imagelists) list.get(0);

									ArrayList tracks = new ArrayList();
									List imagelists = ImagelistsTracksManager.findByImagelists(imagelist.getId());
									if (imagelists!=null && imagelists.size()>0)
									{
										Iterator iterator = imagelists.iterator();
										while (iterator.hasNext()) {
											ImagelistsTracks track = (ImagelistsTracks) iterator.next();
											if (track.getTrack()!=0) {
                                                Image image = ImageManager.retrieveImage(track.getTrack());
                                                tracks.add(new FileItem(image.getTitle(), new File(image.getPath())));
											}
										}
										imagelists.clear();
									}
									Tracker tracker = new Tracker(tracks, 0);
									PathScreen pathScreen = new PathScreen((iPhoto) getBApp(), tracker);
									push(pathScreen, TRANSITION_LEFT);
									list.clear();
								}
							} catch (Exception ex) {
								Tools.logException(iPhoto.class, ex);
							}
						}
					}.start();
					return true;
				}
			} else if (action.equals("play")) {
				load();
				new Thread() {
					public void run() {
						try {
							FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
							List imagelists = ImagelistsManager.findByTitle((String) nameFile.getValue());
							if (imagelists != null && imagelists.size() > 0) {
								Imagelists imagelist = (Imagelists) imagelists.get(0);

								List pList = ImagelistsTracksManager.findByImagelists(imagelist.getId());
								ArrayList tracks = new ArrayList();
								Iterator iterator = pList.iterator();
								while (iterator.hasNext()) {
									ImagelistsTracks track = (ImagelistsTracks) iterator.next();
									if (track.getTrack()!=0) {
                                        Image image = ImageManager.retrieveImage(track.getTrack());
                                        tracks.add(new FileItem(image.getTitle(), new File(image.getPath())));
                                    }
								}
								Tracker tracker = new Tracker(tracks, 0);
                                iPhotoConfiguration imagesConfiguration = getConfiguration();
                                tracker.setRandom(imagesConfiguration
                                                  .isRandomPlayFolders());
                                getBApp().push(
                                    new SlideshowScreen((iPhoto) getBApp(),
                                                        tracker), TRANSITION_LEFT);
                                getBApp().flush();
							}
						} catch (Exception ex) {
							Tools.logException(iPhoto.class, ex);
						}
					}
				}.start();
				return true;
			}

            return super.handleAction(view, action);
        }
        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            Item nameFile = (Item) mMenuList.get(index);
//            if (nameFile.isFolder()) {  // XXX never happens ...
//                icon.setResource(getApp().getFolderIcon());
//                icon.flush();
//            } else {
                icon.setResource(getApp().getCameraIcon());
                icon.flush();
//            }
            BText name = new BText(parent, 50, 4, parent.getWidth() - 40,
                                   parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(nameFile.getName(), 40));
        }
        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_PLAY:
                postEvent(new BEvent.Action(this, "play"));
                return true;
            case KEY_ENTER:
                getBApp().push(new OptionsScreen((iPhoto) getBApp()),
                               TRANSITION_LEFT);
            }
            return super.handleKeyPress(code, rawcode);
        }
		BText mCountText;
    }

    public static class iPhotoFactory extends AppFactory {

        public void initialize() {
            iPhotoConfiguration iPhotoConfiguration = (iPhotoConfiguration) getAppContext().getConfiguration();

            Server.getServer().scheduleLongTerm(new ReloadTask(new ReloadCallback() {
                    public void reload() {
                        try {
                            log.debug("iPhoto");
                            reloadiPhotoLibrary(false);
                        } catch (Exception ex) {
                            log.error("Could not reload iPhoto albums", ex);
                        }
                    }
                }), 5);
        }

        public void updateAppContext(AppContext appContext) {
            super.updateAppContext(appContext);

            reloadiPhotoLibrary(true);
        }

        private void reloadiPhotoLibrary(boolean interrupt) {
            final iPhotoConfiguration iPhotoConfiguration = (iPhotoConfiguration) getAppContext().getConfiguration();

            if (mThread != null && mThread.isAlive()) {
                if (interrupt)
                    mThread.interrupt();
                else
                    return;
            }

            mThread = new Thread() {
                    public void run() {
                        try {
                            boolean reload = false;
                            Date fileDate = new Date();
                            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(iPhoto.class.getName()
                                                                                                         + "." + "refresh3");
                            if (persistentValue != null) {
                                try
                                    {
                                        Date date = new Date(Long.parseLong(persistentValue.getValue()));
                                        File file = new File(iPhotoConfiguration.getImagelistPath());
                                        if (file.exists()) {
                                            fileDate = new Date(file.lastModified());
                                            if (fileDate.getTime() - date.getTime() > 1000)
                                                reload = true;
                                        }
                                    }
                                catch (Exception ex)
                                    {
                                        log.error("Invalid date: " + persistentValue.getValue(), ex);
                                        reload = true;
                                    }
                            } else
                                reload = true;
                            //AlbumParser AlbumParser = new AlbumParser("D:/galleon/iPhoto Music Library.xml");

                            if (reload) {
                                log.info("Reloading iPhoto Library");
                                String location = iPhotoConfiguration.getImagelistPath();
                                File file = new File(iPhotoConfiguration.getImagelistPath());
                                if (file.exists()) {
                                    String path = System.getProperty("data") + File.separator + "iphoto";
                                    File dir = new File(path);
                                    if (!dir.exists()) {
                                        dir.mkdirs();
                                    }
                                    File copy = new File(dir.getCanonicalPath()  + File.separator + "imagelist.xml");
                                    Tools.copy(file,copy);
                                    if (copy.exists())
                                        location = copy.getCanonicalPath();
                                }

                                AlbumParser AlbumParser = new AlbumParser(location);

                                synchronized (this) {
                                    try {
                                        AudioManager.clean();
                                    } catch (Exception ex) {
                                        Tools.logException(iPhoto.class, ex);
                                    }
                                }

                                PersistentValueManager.savePersistentValue(iPhoto.class.getName() + ".refresh3", String.valueOf(fileDate.getTime()));
                                log.info("Reloaded iPhoto Library");
                            }
                        } catch (Throwable ex) {
                            Tools.logException(iPhoto.class, ex);
                        }
                    }
                };
            mThread.setPriority(Thread.MIN_PRIORITY);
            mThread.start();
        }

        private String decode(String value) {
            try {
                return Tools.unEscapeXMLChars(URLDecoder.decode(value, "UTF-8"));
            } catch (Exception ex) {
            }
            return value;
        }
    }

    // TODO Handle multiple iPhoto imagelists
    private static Thread mThread;

    public iPhotoConfiguration getConfiguration() {
        return (iPhotoConfiguration)getFactory().getAppContext().getConfiguration();
    }
    public iPhotoFactory getFactory() {
        return (iPhotoFactory)super.getFactory();
    }
}
