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

import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.gui.FileOptionsTable;
import org.lnicholls.galleon.gui.Galleon;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class MusicOptionsPanel extends AppConfigurationPanel {
    private static Logger log = Logger.getLogger(MusicOptionsPanel.class.getName());

    class SkinWrapper extends NameValue {
        public SkinWrapper(String name, String value) {
            super(name, value);
        }

        public String toString() {
            return getName();
        }
    }

    public MusicOptionsPanel(AppConfiguration appConfiguration) {
        super(appConfiguration);
        setLayout(new GridLayout(0, 1));

        MusicConfiguration musicConfiguration = (MusicConfiguration) appConfiguration;

        List skins = Galleon.getSkins();

        mTitleField = new JTextField(musicConfiguration.getName());

        mSkinsField = new JComboBox();
        mSkinsField.setToolTipText("Select a Winamp classic skin for music player");
        Iterator iterator = skins.iterator();
        while (iterator.hasNext()) {
            File file = (File) iterator.next();
            try {
                String name = Tools.extractName(file.getCanonicalPath());
                mSkinsField.addItem(new SkinWrapper(name, file.getCanonicalPath()));
            } catch (Exception ex) {
            }
        }
        defaultCombo(mSkinsField, musicConfiguration.getSkin());
        mUseAmazonField = new JCheckBox("Use Amazon");
        mUseAmazonField.setToolTipText("Check to specify that Amazon.com should be used for album art");
        mUseAmazonField.setSelected(musicConfiguration.isUseAmazon());
        mUseFileField = new JCheckBox("Use Folder.jpg");
        mUseFileField.setToolTipText("Check to specify that the Folder.jpg file should be used for album art");
        mUseFileField.setSelected(musicConfiguration.isUseFile());

        FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow",
                "pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref");

        PanelBuilder builder = new PanelBuilder(layout);
        //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addSeparator("General", cc.xyw(1, 1, 4));
        builder.addLabel("Title", cc.xy(1, 3));
        builder.add(mTitleField, cc.xyw(3, 3, 1));
        builder.addLabel("Winamp Classic Skin", cc.xy(1, 5));
        builder.add(mSkinsField, cc.xyw(3, 5, 1));
        builder.addSeparator("Album Art", cc.xyw(1, 7, 4));
        builder.addLabel("", cc.xy(1, 9));
        builder.add(mUseAmazonField, cc.xy(3, 9));
        builder.addLabel("", cc.xy(1, 11));
        builder.add(mUseFileField, cc.xy(3, 11));
        builder.addSeparator("Directories", cc.xyw(1, 13, 4));

        mColumnValues = new ArrayList();
        int counter = 0;
        for (Iterator i = musicConfiguration.getPaths().iterator(); i.hasNext(); /* Nothing */) {
            NameValue value = (NameValue) i.next();
            ArrayList values = new ArrayList();
            values.add(0, value.getName());
            values.add(1, value.getValue());
            mColumnValues.add(counter++, values);
        }

        mFileOptionsTable = new FileOptionsTable(true, this, mColumnValues);
        ArrayList columnNames = new ArrayList();
        columnNames.add(0, "Name");
        columnNames.add(1, "Path");
        //OptionsTable optionsTable = new OptionsTable(this, columnNames, new ArrayList(), new JTextField(), new
        // JTextField());
        builder.add(mFileOptionsTable, cc.xyw(1, 15, 4));

        JPanel panel = builder.getPanel();
        //FormDebugUtils.dumpAll(panel);
        add(panel);
    }

    public void load() {
    }

    public void save() {
        log.debug("save()");
        MusicConfiguration musicConfiguration = (MusicConfiguration) mAppConfiguration;
        musicConfiguration.setName(mTitleField.getText());
        musicConfiguration.setSkin(((NameValue) mSkinsField.getSelectedItem()).getValue());
        ArrayList newItems = new ArrayList();
        Iterator iterator = mColumnValues.iterator();
        while (iterator.hasNext()) {
            ArrayList rows = (ArrayList) iterator.next();
            log.debug("Path=" + rows.get(0));
            newItems.add(new NameValue((String) rows.get(0), (String) rows.get(1)));
        }
        musicConfiguration.setPaths(newItems);
    }

    private JTextComponent mTitleField;

    private JComboBox mSkinsField;
    
    private JCheckBox mUseFileField; 
    
    private JCheckBox mUseAmazonField;

    private FileOptionsTable mFileOptionsTable;

    private ArrayList mColumnValues;
}