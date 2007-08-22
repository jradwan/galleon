package org.lnicholls.galleon.apps.hdphotos;

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

import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.gui.FileOptionsTable;
import org.lnicholls.galleon.util.Effects;
import org.lnicholls.galleon.util.NameValue;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class HDPhotosOptionsPanel extends AppConfigurationPanel {
	private static Logger log = Logger.getLogger(HDPhotosOptionsPanel.class);

    private JTextComponent titleField;
    
    private FileOptionsTable fileOptionsTable;

    private ArrayList columnValues;
    
    private JTextComponent flickrFavoriteUsersField;
    
    private JTextComponent flickrUsernameField;

	public HDPhotosOptionsPanel(AppConfiguration appConfiguration) {
		super(appConfiguration);
		setLayout(new GridLayout(0, 1));

		HDPhotosConfiguration imagesConfiguration = (HDPhotosConfiguration) appConfiguration;

		titleField = new JTextField(imagesConfiguration.getName());
        flickrFavoriteUsersField = new JTextField(imagesConfiguration.getFlickrFavoriteUsers());
        flickrUsernameField = new JTextField(imagesConfiguration.getFlickrUsername());

		FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, 9dlu, " + // general
				"pref, 3dlu, " + // title
                "pref, 3dlu, " + // flickr username
                "pref, 3dlu, " + // flickr favorite users
				"pref, 9dlu, " + // directories
				"pref"); // table

		PanelBuilder builder = new PanelBuilder(layout);
		// DefaultFormBuilder builder = new DefaultFormBuilder(new
		// FormDebugPanel(), layout);
		builder.setDefaultDialogBorder();

		CellConstraints cc = new CellConstraints();

		builder.addSeparator("General", cc.xyw(1, 1, 4));
		builder.addLabel("Title", cc.xy(1, 3));
		builder.add(titleField, cc.xyw(3, 3, 1));
        builder.addLabel("Flickr Username", cc.xy(1, 5));
        builder.add(flickrUsernameField, cc.xyw(3, 5, 1));
        builder.addLabel("Flickr Favorite Users", cc.xy(1, 7));
        builder.add(flickrFavoriteUsersField, cc.xyw(3, 7, 1));
		builder.addSeparator("Directories", cc.xyw(1, 9, 4));

		columnValues = new ArrayList();
		int counter = 0;
		for (Iterator i = imagesConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
			NameValue value = (NameValue) i.next();
			ArrayList values = new ArrayList();
			values.add(0, value.getName());
			values.add(1, value.getValue());
			columnValues.add(counter++, values);
		}

		fileOptionsTable = new FileOptionsTable(true, this, columnValues);
		ArrayList columnNames = new ArrayList();
		columnNames.add(0, "Name");
		columnNames.add(1, "Path");
		// OptionsTable optionsTable = new OptionsTable(this, columnNames, new
		// ArrayList(), new JTextField(), new
		// JTextField());
		builder.add(fileOptionsTable, cc.xyw(1, 11, 4));

		JPanel panel = builder.getPanel();
		// FormDebugUtils.dumpAll(panel);
		add(panel);
	}

	public void load() {
	}

	public boolean valid() {
		if (titleField.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Invalid title.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (columnValues.size() == 0) {
			JOptionPane.showMessageDialog(this, "No directories configured.", "Error", JOptionPane.ERROR_MESSAGE);
			return false;
		}

		return true;
	}

	public void save() {
		log.debug("save()");
		HDPhotosConfiguration imagesConfiguration = (HDPhotosConfiguration) mAppConfiguration;
		imagesConfiguration.setName(titleField.getText());
        imagesConfiguration.setFlickrFavoriteUsers(flickrFavoriteUsersField.getText());
        imagesConfiguration.setFlickrUsername(flickrUsernameField.getText());
		ArrayList newItems = new ArrayList();
		Iterator iterator = columnValues.iterator();
		while (iterator.hasNext()) {
			ArrayList rows = (ArrayList) iterator.next();
			log.debug("Path=" + rows.get(0));
			newItems.add(new NameValue((String) rows.get(0), (String) rows.get(1)));
		}
		imagesConfiguration.setPaths(newItems);
	}

    class ImagesWrapper extends NameValue {
        public ImagesWrapper(String name, String value) {
            super(name, value);
        }

        public String toString() {
            return getName();
        }
    }
}
