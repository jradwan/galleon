package org.lnicholls.galleon.apps.rss;

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
import java.util.Iterator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.gui.OptionsTable;
import org.lnicholls.galleon.util.NameValue;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class RSSOptionsPanel extends AppConfigurationPanel {
    private static Logger log = Logger.getLogger(RSSOptionsPanel.class.getName());

    class ComboWrapper extends NameValue {
        public ComboWrapper(String name, String value) {
            super(name, value);
        }

        public String toString() {
            return getName();
        }
    }

    public RSSOptionsPanel(AppConfiguration appConfiguration) {
        super(appConfiguration);
        setLayout(new GridLayout(0, 1));

        RSSConfiguration rssConfiguration = (RSSConfiguration) appConfiguration;

        mTitleField = new JTextField(rssConfiguration.getName());
        mNameField = new JTextField("");
        mFeedField = new JTextField("");

        FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, " + "9dlu, " + "pref, "
                + // title
                "9dlu, " + "pref, " + // directories
                "9dlu, " + "pref, " + // name
                "3dlu, " + "pref, " + // feed
                "3dlu, " + "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addSeparator("General", cc.xyw(1, 1, 4));
        builder.addLabel("Title", cc.xy(1, 3));
        builder.add(mTitleField, cc.xyw(3, 3, 1));
        builder.addSeparator("Feeds", cc.xyw(1, 5, 4));

        builder.addLabel("Name", cc.xy(1, 7));
        builder.add(mNameField, cc.xyw(3, 7, 1));
        builder.addLabel("URL", cc.xy(1, 9));
        builder.add(mFeedField, cc.xyw(3, 9, 1));

        mColumnValues = new ArrayList();
        int counter = 0;
        for (Iterator i = rssConfiguration.getFeeds().iterator(); i.hasNext();) {
            NameValue value = (NameValue) i.next();
            ArrayList values = new ArrayList();
            values.add(0, value.getName());
            values.add(1, value.getValue());
            mColumnValues.add(counter++, values);
        }

        ArrayList columnNames = new ArrayList();
        columnNames.add(0, "Name");
        columnNames.add(1, "URL");
        ArrayList fields = new ArrayList();
        fields.add(mNameField);
        fields.add(mFeedField);
        mOptionsTable = new OptionsTable(this, columnNames, mColumnValues, fields);
        builder.add(mOptionsTable, cc.xyw(1, 11, 4));

        JPanel panel = builder.getPanel();
        //FormDebugUtils.dumpAll(panel);
        add(panel);

    }

    public boolean valid() {
        if (mTitleField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Invalid title.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (mColumnValues.size() == 0) {
            JOptionPane.showMessageDialog(this, "No URLs configured.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }    

    public void load() {
    }

    public void save() {
        RSSConfiguration rssConfiguration = (RSSConfiguration) mAppConfiguration;
        rssConfiguration.setName(mTitleField.getText());
        ArrayList newItems = new ArrayList();
        Iterator iterator = mColumnValues.iterator();
        while (iterator.hasNext()) {
            ArrayList rows = (ArrayList) iterator.next();
            newItems.add(new NameValue((String) rows.get(0), (String) rows.get(1)));
        }
        rssConfiguration.setFeeds(newItems);
    }

    private JTextComponent mTitleField;

    private JTextComponent mNameField;

    private JTextComponent mFeedField;

    private OptionsTable mOptionsTable;

    private ArrayList mColumnValues;

}