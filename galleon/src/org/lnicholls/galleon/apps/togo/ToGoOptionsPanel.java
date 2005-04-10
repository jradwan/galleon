package org.lnicholls.galleon.apps.togo;

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

import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class ToGoOptionsPanel extends AppConfigurationPanel {
    private static Logger log = Logger.getLogger(ToGoOptionsPanel.class.getName());

    public ToGoOptionsPanel(AppConfiguration appConfiguration) {
        super(appConfiguration);
        setLayout(new GridLayout(0, 1));

        ToGoConfiguration togoConfiguration = (ToGoConfiguration) appConfiguration;

        mTitleField = new JTextField(togoConfiguration.getName());
        mShowStatsField = new JCheckBox("Show statistics");
        mShowStatsField.setToolTipText("Check to specify that ToGo statistics should be shown");
        mShowStatsField.setSelected(togoConfiguration.isShowStats());

        FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, " + // general
                "9dlu, " + "pref, " + // title
                "9dlu, " + "pref, " + // Options
                "9dlu, " + "pref"); // show stats

        PanelBuilder builder = new PanelBuilder(layout);
        //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addSeparator("General", cc.xyw(1, 1, 4));
        builder.addLabel("Title", cc.xy(1, 3));
        builder.add(mTitleField, cc.xyw(3, 3, 1));
        builder.addSeparator("Options", cc.xyw(1, 5, 4));
        builder.addLabel("", cc.xy(1, 7));
        builder.add(mShowStatsField, cc.xyw(3, 7, 1));

        JPanel panel = builder.getPanel();
        //FormDebugUtils.dumpAll(panel);
        add(panel);
    }

    public boolean valid() {
        if (mTitleField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Invalid title.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    public void load() {
    }

    public void save() {
        ToGoConfiguration togoConfiguration = (ToGoConfiguration) mAppConfiguration;
        togoConfiguration.setName(mTitleField.getText());
        togoConfiguration.setShowStats(mShowStatsField.isSelected());
    }

    private JTextComponent mTitleField;

    private JCheckBox mShowStatsField;
}