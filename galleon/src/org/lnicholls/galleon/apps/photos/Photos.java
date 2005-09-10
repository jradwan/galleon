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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.HibernateUtil;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.database.ImageManager;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.media.JpgFile;
import org.lnicholls.galleon.media.MediaManager;
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

public class Photos extends DefaultApplication {

	private static Logger log = Logger.getLogger(Photos.class.getName());

	private final static Runtime runtime = Runtime.getRuntime();

	public final static String TITLE = "Photos";

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
		
		PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory)getFactory()).getAppContext().getConfiguration();

		PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(DefaultApplication.TRACKER);
		if (persistentValue != null) {
			String trackerContext = persistentValue.getValue();
			File file = new File(trackerContext);
			if (file.exists()) {
				FileSystemContainer fileSystemContainer = new FileSystemContainer(trackerContext, true);
				Tracker tracker = new Tracker(fileSystemContainer.getItemsSorted(FileFilters.audioDirectoryFilter), 0);
				tracker.setRandom(imagesConfiguration.isRandomPlayFolders());
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
					Tools.logException(Photos.class, ex);
				}

				Tracker tracker = new Tracker(files, 0);
				tracker.setRandom(imagesConfiguration.isRandomPlayFolders());
				setTracker(tracker);
			}
		}

		if (imagesConfiguration.getPaths().size() == 1) {
			try {
				NameValue nameValue = (NameValue) imagesConfiguration.getPaths().get(0);
				File file = new File(nameValue.getValue());
				FileItem nameFile = new FileItem(nameValue.getName(), file);
				FileSystemContainer fileSystemContainer = new FileSystemContainer(file.getCanonicalPath());
				setCurrentTrackerContext(file.getCanonicalPath());
				Tracker tracker = new Tracker(fileSystemContainer.getItemsSorted(FileFilters.imageDirectoryFilter), 0);
				PathScreen pathScreen = new PathScreen(this, tracker, true);
				push(pathScreen, TRANSITION_LEFT);
				flush();
			} catch (Exception ex) {
				Tools.logException(Photos.class, ex);
			}
		} else
			push(new PhotosMenuScreen(this), TRANSITION_NONE);
	}

	public class PhotosMenuScreen extends DefaultMenuScreen {
		public PhotosMenuScreen(Photos app) {
			super(app, "Photos");

			getBelow().setResource(mMenuBackground);

			PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory())
					.getAppContext().getConfiguration();

			for (Iterator i = imagesConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
				NameValue nameValue = (NameValue) i.next();
				mMenuList.add(new FolderItem(nameValue.getName(), new File(nameValue.getValue())));
			}
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("push")) {
				if (mMenuList.size() > 0) {
					load();

					new Thread() {
						public void run() {
							try {
								FileItem nameFile = (FileItem) (mMenuList.get(mMenuList.getFocus()));
								File file = (File) nameFile.getValue();
								FileSystemContainer fileSystemContainer = new FileSystemContainer(file
										.getCanonicalPath());
								Tracker tracker = new Tracker(fileSystemContainer
										.getItemsSorted(FileFilters.imageDirectoryFilter), 0);
								PathScreen pathScreen = new PathScreen((Photos) getBApp(), tracker);
								getBApp().push(pathScreen, TRANSITION_LEFT);
								getBApp().flush();
							} catch (Exception ex) {
								Tools.logException(Photos.class, ex);
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
							File file = (File) nameFile.getValue();
							FileSystemContainer fileSystemContainer = new FileSystemContainer(file.getCanonicalPath(),
									true);
							Tracker tracker = new Tracker(fileSystemContainer
									.getItemsSorted(FileFilters.imageDirectoryFilter), 0);

							PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory()).getAppContext().getConfiguration();
							tracker.setRandom(imagesConfiguration.isRandomPlayFolders());
							getBApp().push(new SlideshowScreen((Photos) getBApp(), tracker), TRANSITION_LEFT);
							getBApp().flush();
						} catch (Exception ex) {
							Tools.logException(Photos.class, ex);
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
			if (nameFile.isFolder()) {
				icon.setResource(mFolderIcon);
			} else {
				icon.setResource(mCameraIcon);
			}

			BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
			name.setShadow(true);
			name.setFlags(RSRC_HALIGN_LEFT);
			name.setValue(Tools.trim(nameFile.getName(), 40));
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

	public class PGrid extends Grid {
		public PGrid(BView parent, int x, int y, int width, int height, int rowHeight) {
			super(parent, x, y, width, height, rowHeight);
			mThreads = new Vector();
		}

		public void createCell(final BView parent, int row, int column, boolean selected) {
			ArrayList photos = (ArrayList) get(row);
			if (column < photos.size()) {
				final Item nameFile = (Item) photos.get(column);
				if (nameFile.isFolder()) {
					BView folderImage = new BView(parent, 0, 0, parent.getWidth(), parent.getHeight());
					folderImage.setResource(mLargeFolderIcon);

					BText nameText = new BText(parent, 0, parent.getHeight() - 25, parent.getWidth(), 25);
					nameText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_BOTTOM);
					nameText.setFont("default-18-bold.font");
					nameText.setShadow(true);
					nameText.setValue(nameFile.getName());
					parent.flush();
				} else {
					// TODO Handle: Photos[#1,uri=null]
					// handleApplicationError(4,view 1402 not found)
					Thread thread = new Thread() {
						public void run() {
							try {
								synchronized (this) {
									parent.setResource(Color.GRAY);
									parent.setTransparency(0.5f);
									parent.flush();
								}

								Image image = null;
								synchronized (this) {
									image = getImage(((File) nameFile.getValue()).getCanonicalPath());
								}

								BufferedImage thumbnail = null;
								synchronized (this) {
									thumbnail = JpgFile.getThumbnail(image);
								}
								if (thumbnail != null) {
									synchronized (this) {
										if (image.getRotation() != null && image.getRotation().intValue() != 0) {
											thumbnail = ImageManipulator.rotate(thumbnail, parent.getWidth(), parent
													.getHeight(), image.getRotation().intValue());
										}

										parent.setResource(createImage(thumbnail), RSRC_IMAGE_BESTFIT);
										parent.setTransparency(0.0f);
										parent.flush();
									}
								}
							} catch (Throwable ex) {
								log.error(ex);
							} finally {
								mThreads.remove(this);
							}
						}

						public void interrupt() {
							synchronized (this) {
								super.interrupt();
							}
						}
					};
					mThreads.add(thread);
					thread.start();
				}
			}
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_SELECT:
				postEvent(new BEvent.Action(this, "push"));
				return true;
			case KEY_CHANNELUP:
			case KEY_CHANNELDOWN:
				boolean result = super.handleKeyPress(code, rawcode);
				if (!result) {
					getBApp().play("bonk.snd");
					getBApp().flush();
				}
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}

		public void shutdown() {
			try {
				setPainting(false);
				Iterator iterator = mThreads.iterator();
				while (iterator.hasNext()) {
					Thread thread = (Thread) iterator.next();
					if (thread.isAlive()) {
						thread.interrupt();
					}
				}
				mThreads.clear();
			} finally {
				setPainting(true);
			}
		}

		private Vector mThreads;
	}

	public class PathScreen extends DefaultScreen {
		private PGrid grid;

		public PathScreen(Photos app, Tracker tracker) {
			this(app, tracker, false);
		}

		public PathScreen(Photos app, Tracker tracker, boolean first) {
			super(app);

			getBelow().setResource(mMenuBackground);

			setTitle("Photos");

			mTracker = tracker;
			mFirst = first;

			int w = getWidth() - 2 * BORDER_LEFT;
			int h = getHeight() - TOP - SAFE_TITLE_V - 2 * PAD;
			grid = new PGrid(this.getNormal(), BORDER_LEFT, BORDER_TOP, w, h, h / 3);

			BHighlights highlights = grid.getHighlights();
			highlights.setPageHint(H_PAGEUP, A_RIGHT + 13, A_TOP - 25);
			highlights.setPageHint(H_PAGEDOWN, A_RIGHT + 13, A_BOTTOM + 30);

			setFocusDefault(grid);

			mBusy = new BView(getNormal(), SAFE_TITLE_H, SAFE_TITLE_V, 32, 32);
			mBusy.setResource(mBusyIcon);
			mBusy.setVisible(false);
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("push")) {
				if (grid.getFocus() != -1) {
					Object object = grid.get(grid.getFocus());
					getBApp().play("select.snd");
					getBApp().flush();

					mTop = grid.getTop();

					ArrayList photos = (ArrayList) object;
					final Item nameFile = (Item) photos.get(grid.getPos() % 3);
					if (nameFile.isFolder()) {
						new Thread() {
							public void run() {
								try {
									mTracker.setPos(grid.getPos());

									File file = (File) nameFile.getValue();
									FileSystemContainer fileSystemContainer = new FileSystemContainer(file
											.getCanonicalPath());
									Tracker tracker = new Tracker(fileSystemContainer
											.getItemsSorted(FileFilters.imageDirectoryFilter), 0);
									PathScreen pathScreen = new PathScreen((Photos) getBApp(), tracker);
									getBApp().push(pathScreen, TRANSITION_LEFT);
									getBApp().flush();
								} catch (Exception ex) {
									Tools.logException(Photos.class, ex);
								}
							}
						}.start();
					} else {
						new Thread() {
							public void run() {
								try {
									PhotosScreen photosScreen = new PhotosScreen((Photos) getBApp());
									mTracker.setPos(grid.getPos());
									photosScreen.setTracker(mTracker);

									getBApp().push(photosScreen, TRANSITION_LEFT);
									getBApp().flush();
								} catch (Exception ex) {
									Tools.logException(Photos.class, ex);
								}
							}
						}.start();
					}

					return true;
				}
			} else if (action.equals("play")) {
				Object object = grid.get(grid.getFocus());
				getBApp().play("select.snd");
				getBApp().flush();

				mTop = grid.getTop();

				ArrayList photos = (ArrayList) object;
				final Item nameFile = (Item) photos.get(grid.getPos() % 3);
				if (nameFile.isFolder()) {
					new Thread() {
						public void run() {
							try {
								mTracker.setPos(grid.getPos());

								File file = (File) nameFile.getValue();
								FileSystemContainer fileSystemContainer = new FileSystemContainer(file
										.getCanonicalPath(), true);
								Tracker tracker = new Tracker(fileSystemContainer
										.getItems(FileFilters.imageDirectoryFilter), 0);
								PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory()).getAppContext().getConfiguration();
								tracker.setRandom(imagesConfiguration.isRandomPlayFolders());
								getBApp().push(new SlideshowScreen((Photos) getBApp(), tracker), TRANSITION_LEFT);
								getBApp().flush();
							} catch (Exception ex) {
								Tools.logException(Photos.class, ex);
							}
						}
					}.start();
				} else {
					new Thread() {
						public void run() {
							try {
								PhotosScreen photosScreen = new PhotosScreen((Photos) getBApp());
								mTracker.setPos(grid.getPos());
								getBApp().push(new SlideshowScreen((Photos) getBApp(), mTracker), TRANSITION_LEFT);
								getBApp().flush();
							} catch (Exception ex) {
								Tools.logException(Photos.class, ex);
							}
						}
					}.start();
				}

				return true;
			}
			return super.handleAction(view, action);
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {

			if (grid.size() == 0) {
				try {
					setPainting(false);
					// mBusy.setVisible(true);

					ArrayList photos = new ArrayList();
					Iterator iterator = mTracker.getList().iterator();
					while (iterator.hasNext()) {
						Item nameFile = (Item) iterator.next();
						photos.add(nameFile);
						if (photos.size() == 3) {
							grid.add(photos);
							photos = new ArrayList();
						}
					}
					if (photos.size() > 0)
						grid.add(photos);
					// mBusy.setVisible(false);
					// mBusy.flush();

					grid.setTop(mTop);
					grid.setPos(mTracker.getPos());
				} catch (Exception ex) {
					Tools.logException(Photos.class, ex);
				} finally {
					setPainting(true);
				}
			}
			return super.handleEnter(arg, isReturn);
		}

		public boolean handleExit() {
			grid.shutdown();
			mTop = grid.getTop();
			grid.clear();
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_LEFT:
				if (!mFirst) {
					postEvent(new BEvent.Action(this, "pop"));
					return true;
				}
				break;
			case KEY_PLAY:
				postEvent(new BEvent.Action(this, "play"));
				return true;
			}

			return super.handleKeyPress(code, rawcode);
		}

		private Tracker mTracker;

		private int mTop;

		private boolean mFirst;
	}

	public class PhotosScreen extends DefaultScreen {

		private BList list;

		public PhotosScreen(Photos app) {
			super(app, true);

			getBelow().setResource(mInfoBackground);
			getBelow().flush();

			setTitle("Photo");

			mDateFormat = new SimpleDateFormat();
			mDateFormat.applyPattern("EEE M/d hh:mm a");

			int start = BORDER_TOP;

			mThumbnail = new BView(getBelow(), getWidth() - SAFE_TITLE_H - 210, getHeight() - SAFE_TITLE_V - 200, 200,
					200, false);

			mTitleText = new LabelText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30, true);
			mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
			mTitleText.setFont("system-24-bold.font");
			mTitleText.setColor(Color.CYAN);
			mTitleText.setShadow(true);

			start += 50;

			mTakenText = new LabelText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 20, true);
			mTakenText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
			mTakenText.setFont("default-18-bold.font");
			mTakenText.setShadow(true);

			mImportedText = new LabelText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 20, true);
			mImportedText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
			mImportedText.setFont("default-18-bold.font");
			mImportedText.setShadow(true);

			start += 20;

			mModifiedText = new LabelText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 20, true);
			mModifiedText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
			mModifiedText.setFont("default-18-bold.font");
			mModifiedText.setShadow(true);

			mStars = new BView[5];
			for (int i = 0; i < 5; i++) {
				mStars[i] = new BView(getNormal(), BORDER_LEFT + (i * 40), getHeight() - SAFE_TITLE_V - 200, 34, 34,
						true);
				mStars[i].setResource(mStarIcon, RSRC_IMAGE_BESTFIT);
				mStars[i].setTransparency(0.6f);
			}

			list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 150,
					(int) Math.round((getWidth() - (SAFE_TITLE_H * 2)) / 2.5), 150, 35);
			list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
			list.add("View photo");
			list.add("View slideshow");
			list.add("Rotate photo");
			list.add("Don't do anything");

			setFocusDefault(list);
		}

		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			updateView();

			return super.handleEnter(arg, isReturn);
		}

		private void updateView() {
			final Image image = currentImage();
			if (image != null) {
				try {
					setPainting(false);

					clearThumbnail();

					File file = new File(image.getPath());
					String name = Tools.extractName(file.getName());
					mTitleText.setValue(name);
					mTakenText.setValue("Taken: " + mDateFormat.format(image.getDateCreated()));
					mImportedText.setValue("Imported: " + mDateFormat.format(image.getDateCaptured()));
					mModifiedText.setValue("Modified: " + mDateFormat.format(image.getDateModified()));

					setRating();

					updateHints();

					final PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory()).getAppContext().getConfiguration();
					if (mThumbnailThread != null && mThumbnailThread.isAlive())
						mThumbnailThread.interrupt();
					mThumbnailThread = new Thread() {
						public void run() {
							try {
								BufferedImage thumbnail = JpgFile.getThumbnail(image);
								if (thumbnail != null) {
									synchronized (mThumbnail) {
										if (mThumbnail.getID() != -1) {
											synchronized (this) {
												if (image.getRotation() != null && image.getRotation().intValue() != 0) {
													thumbnail = ImageManipulator.rotate(thumbnail, mThumbnail
															.getWidth(), mThumbnail.getHeight(), image.getRotation()
															.intValue());
												}

												mThumbnail.setResource(createImage(thumbnail), RSRC_IMAGE_BESTFIT);
												mThumbnail.setVisible(true);
												mThumbnail.setTransparency(1.0f);
												mThumbnail.setTransparency(0.0f, mAnim);
												mThumbnail.flush();
											}
										}
									}
								}
							} catch (Exception ex) {
								Tools.logException(Photos.class, ex, "Could retrieve thumbnail");
							}
						}

						public void interrupt() {
							synchronized (this) {
								super.interrupt();
							}
						}
					};
					mThumbnailThread.start();

				} finally {
					setPainting(true);
				}
			}
		}

		private void clearThumbnail() {
			Image image = currentImage();
			if (image != null) {
				mThumbnail.setVisible(false);
				if (mThumbnail.getResource() != null)
					mThumbnail.getResource().remove();
			}
		}

		public boolean handleExit() {
			try {
				setPainting(false);
				if (mThumbnailThread != null && mThumbnailThread.isAlive())
					mThumbnailThread.interrupt();
				clearThumbnail();
			} finally {
				setPainting(true);
			}
			return super.handleExit();
		}

		public boolean handleKeyPress(int code, long rawcode) {
			Image image = currentImage();
			switch (code) {
			case KEY_THUMBSDOWN:
				if (image != null && image.getRating() > 0) {
					getBApp().play("thumbsdown.snd");
					getBApp().flush();
					try {
						image.setRating(Math.max(image.getRating() - 1, 0));
						ImageManager.updateImage(image);
					} catch (Exception ex) {
						Tools.logException(Photos.class, ex);
					}
					setRating();
				} else {
					getBApp().play("bonk.snd");
					getBApp().flush();
				}
				return true;
			case KEY_THUMBSUP:
				if (image != null && image.getRating() < 5) {
					getBApp().play("thumbsup.snd");
					getBApp().flush();
					try {
						image.setRating(Math.min(image.getRating() + 1, 5));
						ImageManager.updateImage(image);
					} catch (Exception ex) {
						Tools.logException(Photos.class, ex);
					}
					setRating();
				} else {
					getBApp().play("bonk.snd");
					getBApp().flush();
				}
				return true;
			case KEY_SELECT:
			case KEY_RIGHT:
				if (list.getFocus() == 2) {
					postEvent(new BEvent.Action(this, "rotate"));
					return true;
				} else if (list.getFocus() == 0) {
					postEvent(new BEvent.Action(this, "play"));
					return true;
				} else if (list.getFocus() == 1) {
					postEvent(new BEvent.Action(this, "slideshow"));
					return true;
				} else {
					postEvent(new BEvent.Action(this, "pop"));
					return true;
				}
			case KEY_LEFT:
				postEvent(new BEvent.Action(this, "pop"));
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
			}
			return super.handleKeyPress(code, rawcode);
		}

		public void getNextPos() {
			if (mTracker != null) {
				int pos = mTracker.getNextPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder()) {
					pos = mTracker.getNextPos();
					nameFile = (FileItem) mTracker.getList().get(pos);
				}
			}
		}

		public void getPrevPos() {
			if (mTracker != null) {
				int pos = mTracker.getPrevPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder()) {
					pos = mTracker.getPrevPos();
					nameFile = (FileItem) mTracker.getList().get(pos);
				}
			}
		}

		private void setRating() {
			Image image = currentImage();
			if (image != null) {
				for (int i = 0; i < 5; i++) {
					if (i < image.getRating())
						mStars[i].setTransparency(0.0f);
					else
						mStars[i].setTransparency(0.6f);
				}
			}
		}

		public boolean handleAction(BView view, Object action) {
			if (action.equals("rotate")) {

				getBApp().play("select.snd");
				getBApp().flush();

				Image image = currentImage();
				try {
					int rotation = 0;
					if (image.getRotation() != null)
						rotation = image.getRotation().intValue();
					rotation = rotation + 90;
					image.setRotation(new Integer(rotation % 360));
					ImageManager.updateImage(image);
				} catch (Exception ex) {
					Tools.logException(Photos.class, ex);
				}

				updateView();
				return true;
			} else if (action.equals("play")) {

				getBApp().play("select.snd");
				getBApp().flush();

				new Thread() {
					public void run() {
						getBApp().push(new SlideshowScreen((Photos) getBApp(), mTracker, false), TRANSITION_LEFT);
						getBApp().flush();
					}
				}.start();
				return true;
			} else if (action.equals("slideshow")) {

				getBApp().play("select.snd");
				getBApp().flush();

				new Thread() {
					public void run() {
						getBApp().push(new SlideshowScreen((Photos) getBApp(), mTracker, true), TRANSITION_LEFT);
						getBApp().flush();
					}
				}.start();
				return true;
			}

			return super.handleAction(view, action);
		}

		public void setTracker(Tracker value) {
			mTracker = value;
		}

		private Image currentImage() {
			if (mTracker != null) {
				try {
					FileItem nameFile = (FileItem) mTracker.getList().get(mTracker.getPos());
					if (nameFile != null) {
						return getImage(((File) nameFile.getValue()).getCanonicalPath());
					}
				} catch (Exception ex) {
					Tools.logException(Photos.class, ex);
				}
			}
			return null;
		}

		private SimpleDateFormat mDateFormat;

		private Resource mAnim = getResource("*1000");

		private BView mThumbnail;

		private LabelText mTitleText;

		private LabelText mTakenText;

		private LabelText mImportedText;

		private LabelText mModifiedText;

		private Tracker mTracker;

		private BView[] mStars;

		private Thread mThumbnailThread;
	}

	public class SlideshowScreen extends DefaultScreen {

		public SlideshowScreen(Photos app, Tracker tracker) {
			this(app, tracker, true);
		}

		public SlideshowScreen(Photos app, Tracker tracker, boolean showSlideshow) {
			super(app, null, null, false);

			mTracker = tracker;
			mShowSlideshow = showSlideshow;

			setTitle(" ");

			PhotosConfiguration imagesConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory())
					.getAppContext().getConfiguration();
			if (imagesConfiguration.isUseSafe())
				mPhoto = new View(getBelow(), SAFE_ACTION_H, SAFE_ACTION_V, getWidth() - 2 * SAFE_ACTION_H, getHeight()
						- 2 * SAFE_ACTION_V);
			else
				mPhoto = new View(getBelow(), 0, 0, getWidth(), getHeight());
		}

		private void updateView() {
			final Image image = currentImage();
			if (image != null) {
				try {
					setPainting(false);

					// clearImage();

					final File file = new File(image.getPath());

					updateHints();

					new Thread() {
						public void run() {
							try {
								BufferedImage photo = Tools.ImageIORead(file);

								if (photo != null) {
									photo = (BufferedImage) Tools.getImage(photo);

									BufferedImage scaled = ImageManipulator.getScaledImage(photo,
											mPhoto.getWidth(), mPhoto.getHeight());

									if (image.getRotation() != null && image.getRotation().intValue() != 0) {
										scaled = ImageManipulator.rotate(scaled, mPhoto.getWidth(), mPhoto
												.getHeight(), image.getRotation().intValue());
									}

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
								}
							} catch (Throwable ex) {
								Tools.logException(Photos.class, ex, "Could not retrieve image: "
										+ file.getAbsolutePath());
							}
						}
					}.start();

				} finally {
					setPainting(true);
				}
			}
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
			}
			return super.handleKeyPress(code, rawcode);
		}

		public void getNextPos() {
			if (mTracker != null && mTracker.getList().size()>0) {
				int pos = mTracker.getNextPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder()) {
					pos = mTracker.getNextPos();
					nameFile = (FileItem) mTracker.getList().get(pos);
				}
			}
		}

		public void getPrevPos() {
			if (mTracker != null && mTracker.getList().size()>0) {
				int pos = mTracker.getPrevPos();
				Item nameFile = (Item) mTracker.getList().get(pos);
				while (nameFile.isFolder()) {
					pos = mTracker.getPrevPos();
					nameFile = (FileItem) mTracker.getList().get(pos);
				}
			}
		}

		private Image currentImage() {
			if (mTracker != null && mTracker.getList().size()>0) {
				try {
					FileItem nameFile = (FileItem) mTracker.getList().get(mTracker.getPos());
					if (nameFile != null) {
						return getImage(((File) nameFile.getValue()).getCanonicalPath());
					}
				} catch (Exception ex) {
					Tools.logException(Photos.class, ex);
				}
			}
			return null;
		}

		private Resource mAnim = getResource("*100");

		Image mImage;

		View mPhoto;

		Slideshow mSlideshow;

		// when did the last key press occur
		long lastKeyPress;

		private Tracker mTracker;

		private boolean mShowSlideshow;
	}

	private static Image getImage(String path) {
		Image image = null;
		try {
			List list = ImageManager.findByPath(path);
			if (list != null && list.size() > 0) {
				image = (Image) list.get(0);
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

	private class Slideshow extends Thread {
		public Slideshow(SlideshowScreen slideshowScreen) {
			mSlideshowScreen = slideshowScreen;
			
			((DefaultApplication)getApp()).setHandleTimeout(true);
		}

		public void run() {
			final PhotosConfiguration photosConfiguration = (PhotosConfiguration) ((PhotosFactory) getFactory()).getAppContext().getConfiguration();

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
					image = mSlideshowScreen.currentImage();
					if (image != null) {
						File file = new File(image.getPath());
						try {
							photo = Tools.ImageIORead(file);

							if (photo != null) {
								long startTime = System.currentTimeMillis();
								photo = (BufferedImage) Tools.getImage(photo);
								long estimatedTime = System.currentTimeMillis() - startTime;
								BufferedImage scaled = ImageManipulator.getScaledImage(photo,
										mSlideshowScreen.mPhoto.getWidth(), mSlideshowScreen.mPhoto.getHeight());
								if (image.getRotation() != null && image.getRotation().intValue() != 0) {
									scaled = ImageManipulator.rotate(scaled, mSlideshowScreen.getWidth(),
											mSlideshowScreen.getHeight(), image.getRotation().intValue());
								}
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
							Tools.logException(Photos.class, ex, "Could retrieve image");
							if (getApp().getContext() == null)
								return;
						}
					}
					image = null;
				} catch (InterruptedException ex) {
					return;
				} catch (OutOfMemoryError ex) {
					System.gc();
				} catch (Exception ex2) {
					Tools.logException(Photos.class, ex2);
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

	public static class PhotosFactory extends AppFactory {

		public void initialize() {
			PhotosConfiguration imagesConfiguration = (PhotosConfiguration) getAppContext().getConfiguration();
		}
	}
}