package org.lnicholls.galleon.app;
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
import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.interfaces.IApplication;
import com.tivo.hme.interfaces.IHmeConstants;
import com.tivo.hme.interfaces.IHttpRequest;
import com.tivo.hme.sdk.Application;
import com.tivo.hme.sdk.Factory;
import java.io.IOException;
import java.io.InputStream;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.media.Mp3Url;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.ScreenSaverManager;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;

public class AppFactory extends Factory {
	private static final Logger log = Logger.getLogger(AppFactory.class.getName());
    private static final String SKIN = "skin/";
    
	public AppFactory() {
		super();
	}
	public void initialize() {
	}
	public void remove() {
	}
    
	@Override
    public void addApplication(IApplication app) {
        super.addApplication(app);
        
        if (app instanceof BApplication) {
            ScreenSaverManager.getManager().addApplication((BApplication)app);
        }
    }
    
    @Override
    public void removeApplication(IApplication app) {
        super.removeApplication(app);
        
        if (app instanceof BApplication) {
            ScreenSaverManager.getManager().removeApplication((BApplication)app);
        }
    }
    
    public void setConfiguration(AppConfiguration appConfiguration) {
		getAppContext().setConfiguration(appConfiguration);
	}
    
	protected InputStream getImageStream(String uri) throws IOException {
		try
		{
            String path = uri.substring(SKIN.length());
			return Server.getServer().getSkin().getResourceAsStream(path);
		}
		catch (Throwable ex)
		{
			Tools.logException(AppFactory.class, ex);
			try
			{
				return super.getStream(uri);
			}
			catch (Throwable ex2){}
		}
		return null;
	}
    
    protected InputStream getIconImage() throws IOException {
        try
        {
            String appId = this.getClass().getName().substring(0,
                    this.getClass().getName().indexOf("$"));
            
            return Server.getServer().getSkin().getImageInputStream(appId, null, 0, "icon");
        }
        catch (Throwable ex)
        {
            Tools.logException(AppFactory.class, ex);
            try
            {
                return super.getStream(uri);
            }
            catch (Throwable ex2){}
        }
        return this.getClass().getResourceAsStream("/guiicon.png");
    }
    
	public InputStream getStream(String uri) throws IOException {
		try {
            if (log.isDebugEnabled()) {
                log.debug("Loading stream: " + uri);
            }
            if (uri.startsWith(SKIN)) {
                return getImageStream(uri);
                
            } else if (uri.toLowerCase().equals("icon.png")) {
				return getIconImage();
                
			} else if (uri.toLowerCase().endsWith(".mp3")) {
				String[] parts = uri.split("/");
				DefaultApplication application = null;
				int id = -1;
				if (parts.length == 2) {
					try {
						id = Integer.parseInt(parts[0]);
					} catch (Exception ex) {
					}
					// Find the app that asked for the stream
					for (int i = 0; i < active.size(); i++) {
						Application app = (Application) active.elementAt(i);
						if (app.hashCode() == id) {
							if (app instanceof DefaultApplication) {
								application = (DefaultApplication) app;
								break;
							}
						}
					}
				}
				if (uri.toLowerCase().endsWith(".http.mp3")) {
					return Mp3Url.getStream(uri, application);
				}
				return Mp3File.getStream(uri);
			}
		} catch (Throwable ex) {
			Tools.logException(AppFactory.class, ex);
		}
		return super.getStream(uri);
	}
	protected void addHeaders(IHttpRequest http, String uri) throws IOException {
		if (uri.toLowerCase().endsWith(".mp3")) {
			long duration = -1;
			try {
				String id = Tools.extractName(uri);
				if (uri.toLowerCase().endsWith(".http.mp3"))
					id = Tools.extractName(id);
				Audio audio = AudioManager.retrieveAudio(Integer.valueOf(id));
				duration = audio.getDuration();
			} catch (Exception ex) {
				Tools.logException(AppFactory.class, ex, uri);
			}
			if (duration != -1)
				http.addHeader(IHmeConstants.TIVO_DURATION, String
						.valueOf(duration));
		}
		super.addHeaders(http, uri);
	}
	public void setAppContext(AppContext appContext) {
		mAppContext = appContext;
	}
	public void updateAppContext(AppContext appContext) {
		mAppContext = appContext;
	}
	public AppContext getAppContext() {
		return mAppContext;
	}
	public Class getClassName() {
		return clazz;
	}
	private AppContext mAppContext;
}
