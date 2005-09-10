package org.lnicholls.galleon.apps.internetSlideshows;

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
import java.awt.image.BufferedImage;
import java.awt.image.*;
import java.awt.*;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.net.URL;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import javax.imageio.ImageIO;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.podcasting.Podcasting;
import org.lnicholls.galleon.apps.rss.RSS;
import org.lnicholls.galleon.apps.rss.RSS.RSSFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.HibernateUtil;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.Podcast;
import org.lnicholls.galleon.database.PodcastTrack;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.media.JpgFile;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.FolderItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionList;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.Grid;
import org.lnicholls.galleon.widget.LabelText;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BHighlights;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.IArgumentList;

import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

public class InternetSlideshows extends DefaultApplication {

	private static Logger log = Logger.getLogger(InternetSlideshows.class.getName());

	private final static Runtime runtime = Runtime.getRuntime();

	public final static String TITLE = "Internet Slideshows";

	private Resource mMenuBackground;

	private Resource mInfoBackground;

	private Resource mFolderIcon;

	private Resource mLargeFolderIcon;

	private Resource mCameraIcon;

	public void init(IContext context) throws Exception {
		super.init(context);

		mMenuBackground = getSkinImage("menu", "background");
		mInfoBackground = getSkinImage("info", "background");
		mFolderIcon = getSkinImage("menu", "folder");
		mLargeFolderIcon = getSkinImage("menu", "gridFolder");
		mCameraIcon = getSkinImage("menu", "item");
		
		InternetSlideshowsConfiguration imagesConfiguration = (InternetSlideshowsConfiguration) ((InternetSlideshowsFactory)getFactory()).getAppContext().getConfiguration();

		PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(DefaultApplication.TRACKER);
		if (persistentValue != null) {
			String trackerContext = persistentValue.getValue();
			File file = new File(trackerContext);
			if (file.exists()) {
				FileSystemContainer fileSystemContainer = new FileSystemContainer(trackerContext, true);
				Tracker tracker = new Tracker(fileSystemContainer.getItemsSorted(FileFilters.audioDirectoryFilter), 0);
				setTracker(tracker);
			} else {
				// Organizer
				List files = new ArrayList();
				try {
					Transaction tx = null;
					Session session = HibernateUtil.openSession();
					try {
						tx = session.beginTransaction();
						Query query = session.createQuery(trackerContext).setCacheable(true);
						Audio audio = null;
						ScrollableResults items = query.scroll();
						if (items.first()) {
							items.beforeFirst();
							while (items.next()) {
								audio = (Audio) items.get(0);
								files.add(new FileItem(audio.getTitle(), new File(audio.getPath())));
							}
						}

						tx.commit();
					} catch (HibernateException he) {
						if (tx != null)
							tx.rollback();
						throw he;
					} finally {
						HibernateUtil.closeSession();
					}
				} catch (Exception ex) {
					Tools.logException(InternetSlideshows.class, ex);
				}

				Tracker tracker = new Tracker(files, 0);
				setTracker(tracker);
			}
		}

		push(new PhotosMenuScreen(this), TRANSITION_NONE);
	}

	public class PhotosMenuScreen extends DefaultMenuScreen {
		public PhotosMenuScreen(InternetSlideshows app) {
			super(app, "Internet Slideshows");
			
			getBelow().setResource(mMenuBackground);
			
			InternetSlideshowsConfiguration imagesConfiguration = (InternetSlideshowsConfiguration) ((InternetSlideshowsFactory) getFactory())
					.getAppContext().getConfiguration();
			
			List list = imagesConfiguration.getPaths();
			Iterator iterator = list.iterator();
			while (iterator.hasNext())
			{
				NameValue nameValue = (NameValue)iterator.next();
				mMenuList.add(nameValue);
			}
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("push") || action.equals("play")) {
				load();
				new Thread() {
					public void run() {
						try {
							NameValue nameValue = (NameValue) (mMenuList.get(mMenuList.getFocus()));
							
							List stories = getPhotoDescriptions(nameValue.getValue());
							Tracker tracker = new Tracker(stories, 0);

							InternetSlideshowsConfiguration imagesConfiguration = (InternetSlideshowsConfiguration) ((InternetSlideshowsFactory) getFactory()).getAppContext().getConfiguration();
							getBApp().push(new SlideshowScreen((InternetSlideshows) getBApp(), tracker), TRANSITION_LEFT);
							getBApp().flush();
						} catch (Exception ex) {
							Tools.logException(InternetSlideshows.class, ex);
						}
					}
				}.start();
				return true;
			}
			return super.handleAction(view, action);
		}

		protected void createRow(BView parent, int index) {
			BView icon = new BView(parent, 9, 2, 32, 32);
			NameValue nameValue = (NameValue) mMenuList.get(index);
			icon.setResource(mFolderIcon);

			BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
			name.setShadow(true);
			name.setFlags(RSRC_HALIGN_LEFT);
			name.setValue(Tools.trim(nameValue.getName(), 40));
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_PLAY:
				postEvent(new BEvent.Action(this, "play"));
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}
	}

	public class SlideshowScreen extends DefaultScreen {

		public SlideshowScreen(InternetSlideshows app, Tracker tracker) {
			this(app, tracker, true);
			
			if (!mShowDescription)
				setFooter("Press INFO for details", mAnim);
		}

		public SlideshowScreen(InternetSlideshows app, Tracker tracker, boolean showSlideshow) {
			super(app, null, null, false);

			mTracker = tracker;
			mShowSlideshow = showSlideshow;
			
			setTitle(" ");

			InternetSlideshowsConfiguration imagesConfiguration = (InternetSlideshowsConfiguration) ((InternetSlideshowsFactory) getFactory())
					.getAppContext().getConfiguration();
			if (imagesConfiguration.isUseSafe())
				mPhoto = new View(getBelow(), SAFE_ACTION_H, SAFE_ACTION_V, getWidth() - 2 * SAFE_ACTION_H, getHeight()
						- 2 * SAFE_ACTION_V);
			else
				mPhoto = new View(getBelow(), 0, 0, getWidth(), getHeight());
		}

		private void updateView() {
				try {
					setPainting(false);

					// clearImage();

					updateHints();

					new Thread() {
						public void run() {
							try {
								BufferedImage photo = currentImage();

								BufferedImage scaled = ImageManipulator.getScaledImage(photo, mPhoto.getWidth(), mPhoto.getHeight());

								if (scaled!=null)
								{
									mPhoto.setResource(createImage(scaled), RSRC_IMAGE_BESTFIT);
									mPhoto.setVisible(true);
									// mPhoto.setTransparency(1);
									// mPhoto.setTransparency(0, mAnim);
									getBApp().flush();
									scaled.flush();
									scaled = null;
								}
							} catch (Throwable ex) {
								Tools.logException(InternetSlideshows.class, ex, "Could not retrieve image");
							}
						}
					}.start();

				} finally {
					setPainting(true);
				}
				showDescription();
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			updateView();

			if (mShowSlideshow) {
				mSlideshow = new Slideshow(this);
				mSlideshow.start();
			}
			return super.handleEnter(arg, isReturn);
		}

		private void clearImage() {
			Image image = currentImage();
			if (image != null) {
				mPhoto.setVisible(false);
				if (mPhoto.getResource() != null)
					mPhoto.getResource().remove();
				getBApp().flush();
			}
		}

		public boolean handleExit() {
			try {
				setPainting(false);
				clearImage();
				if (mSlideshow != null && mSlideshow.isAlive()) {
					mSlideshow.interrupt();
					mSlideshow = null;
				}
			} finally {
				setPainting(true);
			}
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_UP:
				code = KEY_CHANNELUP;
				getApp().handleKeyPress(code, rawcode);
				return true;
			case KEY_DOWN:
				code = KEY_CHANNELDOWN;
				getApp().handleKeyPress(code, rawcode);
				return true;
			case KEY_CHANNELUP:
				getBApp().play("pageup.snd");
				getBApp().flush();
				getPrevPos();
				updateView();
				return true;
			case KEY_CHANNELDOWN:
				getBApp().play("pagedown.snd");
				getBApp().flush();
				getNextPos();
				updateView();
				return true;
			case KEY_SELECT:
			case KEY_RIGHT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_LEFT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_INFO:
				mShowDescription = !mShowDescription;
				showDescription();
				return true;				
			}
			return super.handleKeyPress(code, rawcode);
		}

		public void getNextPos() {
			if (mTracker != null && mTracker.getList().size()>0) {
				int pos = mTracker.getNextPos();
			}
		}

		public void getPrevPos() {
			if (mTracker != null && mTracker.getList().size()>0) {
				int pos = mTracker.getPrevPos();
			}
		}

		private BufferedImage currentImage() {
			if (mTracker != null && mTracker.getList().size()>0) {
				try {
					PhotoDescription photoDescription = (PhotoDescription) mTracker.getList().get(mTracker.getPos());
					if (photoDescription != null) {
						return (BufferedImage)Tools.getImage(new URL(photoDescription.getUrl()),-1,-1);
					}
				} catch (Exception ex) {
					Tools.logException(InternetSlideshows.class, ex);
				}
			}
			return null;
		}
		
		private String currentDescription() {
			if (mTracker != null && mTracker.getList().size()>0) {
				try {
					PhotoDescription photoDescription = (PhotoDescription) mTracker.getList().get(mTracker.getPos());
					if (photoDescription != null) {
						return photoDescription.getDescription();
					}
				} catch (Exception ex) {
					Tools.logException(InternetSlideshows.class, ex);
				}
			}
			return null;
		}
		
		public void showDescription() {
			if (mShowDescription)
			{
				String value = currentDescription();
				if (value != null) {
		        	try {
		                setPainting(false);
			        	if (mDescription == null) {
			            	mDescriptionBackground = new View(getNormal(), SAFE_TITLE_H, getHeight() - SAFE_TITLE_V - 60, (getWidth() - (SAFE_TITLE_H * 2)), 75);
			                mDescriptionBackground.setResource(Color.BLACK);
			                mDescriptionBackground.setTransparency(0.2f);
			            	
			            	mDescription = new BText(getNormal(), SAFE_TITLE_H, getHeight() - SAFE_TITLE_V - 60, (getWidth() - (SAFE_TITLE_H * 2)), 75);
			                mDescription.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_TOP | RSRC_TEXT_WRAP);
			                mDescription.setFont("default-18.font");
			                mDescription.setShadow(true);
			            }
		
			            if (value.length()==0)
			            {
			            	mDescription.setVisible(false);
			            	mDescriptionBackground.setVisible(false);
			            }
			            else
			            {
			            	mDescription.setValue(value);
			            	mDescription.setVisible(true);
			            	mDescriptionBackground.setVisible(true);
			            }
		        	} finally {
		                setPainting(true);
		            }
		        }
			}
			else
			{
				if (mDescription!=null)
				{
					mDescription.setVisible(false);
					mDescriptionBackground.setVisible(false);
				}
			}
	    }

		private Resource mAnim = getResource("*5000");
		
		private BText mDescription;
		
		private View mDescriptionBackground;

		Image mImage;

		View mPhoto;

		Slideshow mSlideshow;

		// when did the last key press occur
		long lastKeyPress;

		private Tracker mTracker;

		private boolean mShowSlideshow;
	}

	private class Slideshow extends Thread {
		public Slideshow(SlideshowScreen slideshowScreen) {
			mSlideshowScreen = slideshowScreen;
			
			((DefaultApplication)getApp()).setHandleTimeout(true);
		}

		public void run() {
			final InternetSlideshowsConfiguration photosConfiguration = (InternetSlideshowsConfiguration) ((InternetSlideshowsFactory) getFactory()).getAppContext().getConfiguration();

			Effect[] effects = new Effect[0];

			if (photosConfiguration.getEffect().equals(Effects.RANDOM)
					|| photosConfiguration.getEffect().equals(Effects.SEQUENTIAL)) {
				String names[] = new String[0];
				names = (String[]) Effects.getEffectNames().toArray(names);
				Arrays.sort(names);
				effects = new Effect[names.length];
				for (int i = 0; i < names.length; i++) {
					String name = names[i];
					effects[i] = Effects.getEffect(name);
				}
			} else {
				ArrayList list = new ArrayList();
				list.add(Effects.getEffect(photosConfiguration.getEffect()));
				effects = (Effect[]) list.toArray(effects);
			}

			BufferedImage photo = null;
			FileInputStream is = null;
			Image image = null;
			Random random = new Random();

			int currentEffect = 0;

			if (photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
				currentEffect = 0;
			else if (photosConfiguration.getEffect().equals(Effects.RANDOM)) {
				currentEffect = random.nextInt(effects.length);
			}
			
			while (getApp().getContext()!=null) {
				try {
					sleep(1000 * photosConfiguration.getDisplayTime());
					mSlideshowScreen.getNextPos();
						try {
							photo = mSlideshowScreen.currentImage();
							String description = mSlideshowScreen.currentDescription();

							if (photo != null) {
								long startTime = System.currentTimeMillis();
								photo = (BufferedImage) Tools.getImage(photo);
								long estimatedTime = System.currentTimeMillis() - startTime;
								
								mSlideshowScreen.showDescription();
								
								BufferedImage scaled = ImageManipulator.getScaledImage(photo,
										mSlideshowScreen.mPhoto.getWidth(), mSlideshowScreen.mPhoto.getHeight());
								photo.flush();
								photo = null;
								estimatedTime = System.currentTimeMillis() - startTime;
								if (photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
									currentEffect = (currentEffect + 1) % effects.length;
								else if (photosConfiguration.getEffect().equals(Effects.RANDOM)) {
									currentEffect = random.nextInt(effects.length);
								}
								Effect effect = (Effect) effects[currentEffect];
								effect.setDelay(photosConfiguration.getTransitionTime() * 1000);
								effect.apply(mSlideshowScreen.mPhoto, scaled);
							}
						} catch (Exception ex) {
							Tools.logException(InternetSlideshows.class, ex, "Could not retrieve image");
							if (getApp().getContext() == null)
								return;
						}
				} catch (InterruptedException ex) {
					return;
				} catch (OutOfMemoryError ex) {
					System.gc();
				} catch (Exception ex2) {
					Tools.logException(InternetSlideshows.class, ex2);
				}
			}
		}

		public void interrupt() {
			synchronized (this) {
				super.interrupt();
			}
			
			((DefaultApplication)getApp()).setHandleTimeout(false);
		}

		private SlideshowScreen mSlideshowScreen;
	}
	
	static class PhotoDescription
	{
		public PhotoDescription(String url, String description)
		{
			mUrl = url;
			mDescription = description;
		}
		
		public String getUrl()
		{
			return mUrl;
		}
		
		public String getDescription()
		{
			return mDescription;
		}
		
		private String mUrl;
		
		private String mDescription;
	}
	
	public static List getPhotoDescriptions(String url) {
		List photoDescriptions = new ArrayList();
		
		PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(InternetSlideshows.class.getName() + "."
				+ url);
		String content = persistentValue == null ? null : persistentValue.getValue();
		if (PersistentValueManager.isAged(persistentValue)) {
			try {
				String page = Tools.getPage(new URL(url));
				if (page != null && page.length() > 0)
					content = page;
			} catch (Exception ex) {
				Tools.logException(Podcasting.class, ex, "Could not cache listing: " + url);
			}
		}

		if (content != null) {
			try {
				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(content);
				// Document document = saxReader.read(new
				// File("d:/galleon/itunes2.rss.xml"));
				Document document = saxReader.read(stringReader);
				stringReader.close();
				stringReader = null;

				Element root = document.getRootElement(); // check for errors
				if (root != null && root.getName().equals("rss")) {
					Element channel = root.element("channel");
					if (channel != null) {
						for (Iterator i = channel.elementIterator("item"); i.hasNext();) {
							Element item = (Element) i.next();

							String value = null;
							String title = null;
							String link = null;
		
							if ((value = Tools.getAttribute(item, "title")) != null) {
								title = value;
							}
							
							if ((value = Tools.getAttribute(item, "description")) != null) {
								title = Tools.cleanHTML(value);
							}

							Element contentElement = item.element("content");
							if (contentElement != null) {
								if ((value = contentElement.attributeValue("url")) != null) {
									link = value;
									if (url.startsWith("http://rss.news.yahoo.com"))
									{
										URL location = new URL(link);
										photoDescriptions.add(new PhotoDescription(location.getProtocol()+"://"+location.getHost()+location.getPath(), title));
									}
									else
										photoDescriptions.add(new PhotoDescription(link, title));
								}
							}
							
							Element enclosureElement = item.element("enclosure");
							if (enclosureElement != null) {
								if ((value = enclosureElement.attributeValue("url")) != null) {
									link = value;
									if (url.startsWith("http://rss.news.yahoo.com"))
									{
										URL location = new URL(link);
										photoDescriptions.add(new PhotoDescription(location.getProtocol()+"://"+location.getHost()+location.getPath(), title));
									}
									else
										photoDescriptions.add(new PhotoDescription(link, title));
								}
							}
						}
					}
				}
				document.clearContent();
				document = null;

				if (PersistentValueManager.isAged(persistentValue)) {
					PersistentValueManager.savePersistentValue(InternetSlideshows.class.getName() + "." + url,
							content, 60);
				}
			} catch (Exception ex) {
				Tools.logException(InternetSlideshows.class, ex, "Could not download listing: " + url);
				return null;
			}
		}
		return photoDescriptions;
	}	

	public static class InternetSlideshowsFactory extends AppFactory {

		public void initialize() {
			InternetSlideshowsConfiguration imagesConfiguration = (InternetSlideshowsConfiguration) getAppContext().getConfiguration();
		}
	}
	
	private boolean mShowDescription;
}