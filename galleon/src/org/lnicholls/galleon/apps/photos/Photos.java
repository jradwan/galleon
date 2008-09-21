package org.lnicholls.galleon.apps.photos;
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
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;

public class Photos extends DefaultApplication {
	private static Logger log = Logger.getLogger(Photos.class.getName());
	public final static String TITLE = "Photos";
	private Resource menuBackground;
	private Resource infoBackground;
	private Resource folderIcon;
	private Resource largeFolderIcon;
	private Resource cameraIcon;
    private static Thread mThread;
	public void init(IContext context) throws Exception {
		super.init(context);
	}
	public void initService() {
        super.initService();
		menuBackground = getSkinImage("menu", "background");
		infoBackground = getSkinImage("info", "background");
		folderIcon = getSkinImage("menu", "folder");
		largeFolderIcon = getSkinImage("menu", "gridFolder");
		cameraIcon = getSkinImage("menu", "item");
		PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory())
				.getAppContext()
				.getConfiguration();
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
		if (imagesConfiguration.getPaths().size() == 1 && !imagesConfiguration.isiPhotoEnabled()) {
			try {
				NameValue nameValue = (NameValue) imagesConfiguration
						.getPaths().get(0);
				File file = new File(nameValue.getValue());
				FileItem nameFile = new FileItem(nameValue.getName(), file);
				FileSystemContainer fileSystemContainer = new FileSystemContainer(
						file.getCanonicalPath());
				setCurrentTrackerContext(file.getCanonicalPath());
				Tracker tracker = new Tracker(fileSystemContainer
						.getItemsSorted(FileFilters.imageDirectoryFilter), 0);
				PathScreen pathScreen = new PathScreen(this, tracker, true);
				push(pathScreen, TRANSITION_LEFT);
				flush();
			} catch (Exception ex) {
				Tools.logException(Photos.class, ex);
			}
		} else if (imagesConfiguration.getPaths().size() == 0 && imagesConfiguration.isiPhotoEnabled()) {
            push(new iPhotoMenuScreen(this), TRANSITION_NONE);
        } else
			push(new PhotosMenuScreen(this), TRANSITION_NONE);
		initialize();
	}
    
    public PhotosConfiguration getConfiguration() {
        return (PhotosConfiguration)getFactory().getAppContext().getConfiguration();
    }
    
    protected static Image getImage(String path) {
        Image image = null;
        try {
            List<Image> list = ImageManager.findByPath(path);
            if (list != null && list.size() > 0) {
                image = list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(Photos.class, ex);
        }
        if (image == null) {
            try {
                image = (Image) MediaManager.getMedia(path);
                ImageManager.createImage(image);
            } catch (Exception ex) {
                Tools.logException(Photos.class, ex);
            }
        }
        return image;
    }
    
    public PhotosFactory getFactory() {
        return (PhotosFactory)super.getFactory();
    }
	public static class PhotosFactory extends AppFactory {
		public void initialize() {
			PhotosConfiguration imagesConfiguration = (PhotosConfiguration) getAppContext()
					.getConfiguration();
            Server.getServer().scheduleLongTerm(new ReloadTask(new ReloadCallback() {
                    public void reload() {
                        try {
                            log.debug("iPhoto album reload");
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
            final PhotosConfiguration photosConfiguration = (PhotosConfiguration) getAppContext().getConfiguration();

            if (!photosConfiguration.isiPhotoEnabled())
                return;

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
                            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Photos.class.getName()
                                                                                                         + "." + "refresh3");
                            if (persistentValue != null) {
                                try
                                    {
                                        Date date = new Date(Long.parseLong(persistentValue.getValue()));
                                        File file = new File(photosConfiguration.getAlbumDataPath());
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
                            //AlbumParser AlbumParser = new AlbumParser("D:/galleon/Photos Music Library.xml");

                            if (reload) {
                                log.info("Reloading Photos Library");
                                String location = photosConfiguration.getAlbumDataPath();
                                File file = new File(photosConfiguration.getAlbumDataPath());
                                if (file.exists()) {
                                    String path = System.getProperty("data") + File.separator + "iphoto";
                                    File dir = new File(path);
                                    if (!dir.exists()) {
                                        dir.mkdirs();
                                    }
                                    File copy = new File(dir.getCanonicalPath()  + File.separator + "imageAlbum.xml");
                                    Tools.copy(file,copy);
                                    if (copy.exists())
                                        location = copy.getCanonicalPath();
                                }

                                AlbumParser AlbumParser = new AlbumParser(location);

                                synchronized (this) {
                                    try {
                                        AudioManager.clean();
                                    } catch (Exception ex) {
                                        Tools.logException(Photos.class, ex);
                                    }
                                }

                                PersistentValueManager.savePersistentValue(Photos.class.getName() + ".refresh3", String.valueOf(fileDate.getTime()));
                                log.info("Reloaded Photos Library");
                            }
                        } catch (Throwable ex) {
                            Tools.logException(Photos.class, ex);
                        }
                    }
                };
            mThread.setPriority(Thread.MIN_PRIORITY);
            mThread.start();
        }
	}
    public Resource getCameraIcon() {
        return cameraIcon;
    }
    public void setCameraIcon(Resource cameraIcon) {
        this.cameraIcon = cameraIcon;
    }
    public Resource getFolderIcon() {
        return folderIcon;
    }
    public void setFolderIcon(Resource folderIcon) {
        this.folderIcon = folderIcon;
    }
    public Resource getInfoBackground() {
        return infoBackground;
    }
    public void setInfoBackground(Resource infoBackground) {
        this.infoBackground = infoBackground;
    }
    public Resource getLargeFolderIcon() {
        return largeFolderIcon;
    }
    public void setLargeFolderIcon(Resource largeFolderIcon) {
        this.largeFolderIcon = largeFolderIcon;
    }
    public Resource getMenuBackground() {
        return menuBackground;
    }
    public void setMenuBackground(Resource menuBackground) {
        this.menuBackground = menuBackground;
    }
}
