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

package org.lnicholls.galleon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import de.nava.informa.core.ChannelBuilderIF;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;

public class VersionCheck {

	private static final Logger log = Logger.getLogger(VersionCheck.class.getName());

	public boolean isCurrentSourceforgeVersion() {
		try {
			String page = Tools.getPage(new URL(
					"http://sourceforge.net/export/rss2_projnews.php?group_id=126291&rss_fulltext=1")); // Sourceforge
			// RSS
			// feed
			ChannelBuilderIF builder = new ChannelBuilder();
			ChannelIF channel = FeedParser.parse(builder, new ByteArrayInputStream((page.getBytes("UTF-8"))));
			//ChannelIF channel = FeedParser.parse(builder, new FileInputStream(new File("d:/galleon/rss2_projnews.php.xml")));

			if (channel != null) {
				if (channel.getItems() != null && channel.getItems().size() > 0) {
					Collection items = channel.getItems();
					Iterator i = items.iterator();
					ItemIF item = (ItemIF) i.next();

					String REGEX = "Galleon (.*) for TiVo";
					Pattern p = Pattern.compile(REGEX);
					Matcher m = p.matcher(item.getTitle());
					if (m.find()) {
						if (log.isDebugEnabled())
							log.debug("Current version: " + m.group(1).trim());
						if (!Tools.getVersion().equals(m.group(1).trim())) {
							return false;
						}
					}
				}
			}
			builder.close();
			builder = null;
		} catch (Exception ex) {
			Tools.logException(Tools.class, ex);
		}
		return true;
	}

}
