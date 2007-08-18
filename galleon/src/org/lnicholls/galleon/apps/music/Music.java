package org.lnicholls.galleon.apps.music;

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

import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.Resource;
import java.io.File;
import java.util.List;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;

public class Music extends DefaultApplication {

	public final static String TITLE = "Music";

	private Resource menuBackground;

	private Resource infoBackground;

	private Resource playerBackground;

	private Resource lyricsBackground;

	private Resource imagesBackground;

	private Resource folderIcon;

	private Resource cdIcon;

	private Resource playlistIcon;

	public void init(IContext context) throws Exception {
		super.init(context);
	}
	
	public void initService() {
        super.initService();
		menuBackground = getSkinImage("menu", "background");
		infoBackground = getSkinImage("info", "background");
		playerBackground = getSkinImage("player", "background");
		lyricsBackground = getSkinImage("lyrics", "background");
		imagesBackground = getSkinImage("images", "background");
		folderIcon = getSkinImage("menu", "folder");
		cdIcon = getSkinImage("menu", "item");
		playlistIcon = getSkinImage("menu", "playlist");

		MusicConfiguration musicConfiguration = (MusicConfiguration) ((MusicFactory) getFactory()).getAppContext()
				.getConfiguration();

		if (musicConfiguration.getPaths().size() == 1) {
			try {
				NameValue nameValue = (NameValue) musicConfiguration.getPaths().get(0);
				File file = new File(nameValue.getValue());
				FileSystemContainer fileSystemContainer = new FileSystemContainer(file.getCanonicalPath());
				//setCurrentTrackerContext(file.getCanonicalPath());
				Tracker tracker = new Tracker(fileSystemContainer.getItemsSorted(FileFilters.audioDirectoryFilter), 0);
				PathScreen pathScreen = new PathScreen(this, tracker, true);
				push(pathScreen, TRANSITION_LEFT);
			} catch (Throwable ex) {
				Tools.logException(Music.class, ex);
			}
		} else
			push(new MusicMenuScreen(this), TRANSITION_NONE);

		initialize();
	}

	protected static Audio getAudio(String path) {
		Audio audio = null;
		try {
			List list = AudioManager.findByPath(path);
			if (list != null && list.size() > 0) {
				audio = (Audio) list.get(0);
			}
		} catch (Exception ex) {
			Tools.logException(Music.class, ex);
		}

		if (audio == null) {
			try {
				audio = (Audio) MediaManager.getMedia(path);
				AudioManager.createAudio(audio);
			} catch (Exception ex) {
				Tools.logException(Music.class, ex);
			}
		}
		return audio;
	}

    public Resource getCdIcon() {
        return cdIcon;
    }

    public Resource getFolderIcon() {
        return folderIcon;
    }

    public Resource getImagesBackground() {
        return imagesBackground;
    }

    public Resource getInfoBackground() {
        return infoBackground;
    }

    public Resource getLyricsBackground() {
        return lyricsBackground;
    }

    public Resource getMenuBackground() {
        return menuBackground;
    }

    public Resource getPlayerBackground() {
        return playerBackground;
    }

    public Resource getPlaylistIcon() {
        return playlistIcon;
    }

    public static class MusicFactory extends AppFactory {

        public void initialize() {
            MusicConfiguration musicConfiguration = (MusicConfiguration) getAppContext().getConfiguration();

            MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer().getMusicPlayerConfiguration();
        }
    }
}
