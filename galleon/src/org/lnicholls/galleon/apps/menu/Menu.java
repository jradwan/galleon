package org.lnicholls.galleon.apps.menu;

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
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.tivo.core.ds.TeDict;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.internet.Internet;
import org.lnicholls.galleon.apps.internet.InternetConfiguration;
import org.lnicholls.galleon.apps.internet.Internet.InternetFactory;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultScreen;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;

public class Menu extends DefaultApplication {

	private static Logger log = Logger.getLogger(Menu.class.getName());

	private final static Runtime runtime = Runtime.getRuntime();

	public final static String TITLE = "Menu";

	private Resource mMenuBackground;

	private Resource mFolderIcon;

	public void init(IContext context) throws Exception {
		super.init(context);
		
		mMenuBackground = getSkinImage("menu", "background");
		mFolderIcon = getSkinImage("menu", "folder");

		MenuConfiguration menuConfiguration = (MenuConfiguration) ((MenuFactory) getFactory())
				.getAppContext().getConfiguration();
		
		mLocationMenuScreen = new LocationMenuScreen(this);

		push(mLocationMenuScreen, TRANSITION_NONE);
		
		initialize();
	}

	public class LocationMenuScreen extends DefaultMenuScreen {
		public LocationMenuScreen(Menu app) {
			super(app, "Menu");
			
			getBelow().setResource(mMenuBackground);

			MenuConfiguration menuConfiguration = (MenuConfiguration) ((MenuFactory) getFactory())
					.getAppContext().getConfiguration();
			
			List list = Server.getServer().getAppUrls(false);
			Iterator iterator = Server.getServer().getAppUrls(false).iterator();
			while (iterator.hasNext())
			{
				NameValue nameValue = (NameValue)iterator.next();
				mMenuList.add(nameValue);
			}
/*
			mMenuList.add(new NameValue("Music", "http://192.168.0.3:7288/Allmymusic/"));
			mMenuList.add(new NameValue("Photos", "http://192.168.0.3:7288/Photos/"));
			mMenuList.add(new NameValue("Playlists", "http://192.168.0.3:7288/Playlists/"));
			mMenuList.add(new NameValue("Email", "http://192.168.0.3:7288/Email/"));
			mMenuList.add(new NameValue("Movies", "http://192.168.0.3:7288/Movies/"));
			mMenuList.add(new NameValue("ToGo", "http://192.168.0.3:7288/ToGo/"));
			mMenuList.add(new NameValue("Organizer", "http://192.168.0.3:7288/Organzier/"));
			mMenuList.add(new NameValue("Internet", "http://192.168.0.3:7288/Internet/"));
			mMenuList.add(new NameValue("Video", "http://192.168.0.3:7288/Video/"));
			mMenuList.add(new NameValue("Slideshow", "http://192.168.0.3:7288/slside/"));
			mMenuList.add(new NameValue("Shoutcast", "http://192.168.0.3:7288/Shoutcast/"));
			mMenuList.add(new NameValue("Podcasting", "http://192.168.0.3:7288/Podcasting/"));
			mMenuList.add(new NameValue("RSS", "http://192.168.0.3:7288/RSS/"));
			mMenuList.add(new NameValue("Jukebox", "http://192.168.0.3:7288/Jukebox/"));
			mMenuList.add(new NameValue("Upcoming", "http://192.168.0.3:7288/Upcoming/"));
			mMenuList.add(new NameValue("iTunes", "http://192.168.0.3:7288/iTunes/"));
			mMenuList.add(new NameValue("Traffic", "http://192.168.0.3:7288/Traffic/"));
			mMenuList.add(new NameValue("Weather", "http://192.168.0.3:7288/Weather/"));
*/			
		}
		
		public boolean handleAction(BView view, Object action) {
			if (action.equals("push") || action.equals("play")) {
				load();
				new Thread() {
					public void run() {
						try {
							NameValue value = (NameValue) (mMenuList
									.get(mMenuList.getFocus()));
							
							byte mem[] = new byte[2];
							mem[0] = (byte)mMenuList.getFocus();
							mem[1] = (byte)mMenuList.getTop();
				    		TeDict params = new TeDict();
				    		transitionForward(value.getValue(), params, mem);
						} catch (Exception ex) {
							Tools.logException(Menu.class, ex);
						}
					}
				}.start();
				return true;
			}
			return super.handleAction(view, action);
		}

		protected void createRow(BView parent, int index) {
			BView icon = new BView(parent, 9, 2, 32, 32);
			NameValue value = (NameValue) mMenuList.get(index);
			try
			{
				URL url = new URL(value.getValue()+"icon.png");
				URLConnection urlConnection = url.openConnection();
				InputStream inputStream = urlConnection.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int success = inputStream.read(data);
                while (success != -1) {
                    baos.write(data, 0, success);
                    success = inputStream.read(data);
                }
                baos.close();
                inputStream.close();
				icon.setResource(createImage(baos.toByteArray()));
			}
			catch (Exception ex)
			{
				icon.setResource(mFolderIcon);				
			}

			BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
			name.setShadow(true);
			name.setFlags(RSRC_HALIGN_LEFT);
			name.setValue(Tools.trim(value.getName(), 40));
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

	public static class MenuFactory extends AppFactory {

		public void initialize() {
			MenuConfiguration menuConfiguration = (MenuConfiguration) getAppContext().getConfiguration();
		}
	}
	
	public boolean handleInitInfo(HmeEvent.InitInfo info)
    {
    	if (mLocationMenuScreen!=null)
    	{
    		if (getMemento()!=null && getMemento().length>0)
			{
				int pos = getMemento()[0];
				int top = getMemento()[1];
				mLocationMenuScreen.getMenuList().setTop(top);
				mLocationMenuScreen.getMenuList().setFocus(pos, false);
				mLocationMenuScreen.getMenuList().flush();
			}
    	}
		return true;
    }

	private LocationMenuScreen mLocationMenuScreen;
}