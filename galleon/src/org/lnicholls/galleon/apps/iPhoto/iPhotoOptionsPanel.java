package org.lnicholls.galleon.apps.iPhoto;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang.SystemUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.util.Effects;
import org.lnicholls.galleon.util.NameValue;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

public class iPhotoOptionsPanel extends AppConfigurationPanel implements ActionListener {
    private static Logger log = Logger.getLogger(iPhotoOptionsPanel.class.getName());

	class ImagesWrapper extends NameValue {
		public ImagesWrapper(String name, String value) {
			super(name, value);
		}

		public String toString() {
			return getName();
		}
	}

    public iPhotoOptionsPanel(AppConfiguration appConfiguration) {
        super(appConfiguration);
        setLayout(new GridLayout(0, 1));

        iPhotoConfiguration iPhotoConfiguration = (iPhotoConfiguration) appConfiguration;

        mTitleField = new JTextField(iPhotoConfiguration.getName());
        mSharedField = new JCheckBox("Share");
        mSharedField.setSelected(iPhotoConfiguration.isShared());
        mSharedField.setToolTipText("Share this app");
        mUseSafeField = new JCheckBox("Use safe viewing area");
        mUseSafeField.setToolTipText("Check to specify that photos should fit within the safe viewing area of the TV");
        mUseSafeField.setSelected(iPhotoConfiguration.isUseSafe());
        mEffectsField = new JComboBox();
        mEffectsField.setToolTipText("Select a slideshow transition effect");
        mEffectsField.addItem(new ImagesWrapper(Effects.RANDOM, Effects.RANDOM));
        mEffectsField.addItem(new ImagesWrapper(Effects.SEQUENTIAL, Effects.SEQUENTIAL));
        String names[] = new String[0];
        names = (String[]) Effects.getEffectNames().toArray(names);
        Arrays.sort(names);
        for (int i = 0; i < names.length; i++) {
            String name = names[i];
            mEffectsField.addItem(new ImagesWrapper(name, name));
        }
        defaultCombo(mEffectsField, String.valueOf(iPhotoConfiguration.getEffect()));

        mDisplayTimeField = new JComboBox();
        mDisplayTimeField.setToolTipText("Select a slideshow display time");
        mDisplayTimeField.addItem(new ImagesWrapper("2 seconds", "2"));
        mDisplayTimeField.addItem(new ImagesWrapper("3 seconds", "3"));
        mDisplayTimeField.addItem(new ImagesWrapper("4 seconds", "4"));
        mDisplayTimeField.addItem(new ImagesWrapper("5 seconds", "5"));
        mDisplayTimeField.addItem(new ImagesWrapper("6 seconds", "6"));
        mDisplayTimeField.addItem(new ImagesWrapper("7 seconds", "7"));
        mDisplayTimeField.addItem(new ImagesWrapper("8 seconds", "8"));
        mDisplayTimeField.addItem(new ImagesWrapper("9 seconds", "9"));
        mDisplayTimeField.addItem(new ImagesWrapper("10 seconds", "10"));
        mDisplayTimeField.addItem(new ImagesWrapper("11 seconds", "11"));
        mDisplayTimeField.addItem(new ImagesWrapper("12 seconds", "12"));
        mDisplayTimeField.addItem(new ImagesWrapper("13 seconds", "13"));
        mDisplayTimeField.addItem(new ImagesWrapper("14 seconds", "14"));
        mDisplayTimeField.addItem(new ImagesWrapper("15 seconds", "15"));
        mDisplayTimeField.addItem(new ImagesWrapper("16 seconds", "16"));
        mDisplayTimeField.addItem(new ImagesWrapper("17 seconds", "17"));
        mDisplayTimeField.addItem(new ImagesWrapper("18 seconds", "18"));
        mDisplayTimeField.addItem(new ImagesWrapper("19 seconds", "19"));
        mDisplayTimeField.addItem(new ImagesWrapper("20 seconds", "20"));
        defaultCombo(mDisplayTimeField, String.valueOf(iPhotoConfiguration.getDisplayTime()));

        mTransitionTimeField = new JComboBox();
        mTransitionTimeField.setToolTipText("Select a slideshow transition time");
        mTransitionTimeField.addItem(new ImagesWrapper("2 seconds", "2"));
        mTransitionTimeField.addItem(new ImagesWrapper("3 seconds", "3"));
        mTransitionTimeField.addItem(new ImagesWrapper("4 seconds", "4"));
        mTransitionTimeField.addItem(new ImagesWrapper("5 seconds", "5"));
        mTransitionTimeField.addItem(new ImagesWrapper("6 seconds", "6"));
        mTransitionTimeField.addItem(new ImagesWrapper("7 seconds", "7"));
        mTransitionTimeField.addItem(new ImagesWrapper("8 seconds", "8"));
        mTransitionTimeField.addItem(new ImagesWrapper("9 seconds", "9"));
        mTransitionTimeField.addItem(new ImagesWrapper("10 seconds", "10"));
        mTransitionTimeField.addItem(new ImagesWrapper("11 seconds", "11"));
        mTransitionTimeField.addItem(new ImagesWrapper("12 seconds", "12"));
        mTransitionTimeField.addItem(new ImagesWrapper("13 seconds", "13"));
        mTransitionTimeField.addItem(new ImagesWrapper("14 seconds", "14"));
        mTransitionTimeField.addItem(new ImagesWrapper("15 seconds", "15"));
        mTransitionTimeField.addItem(new ImagesWrapper("16 seconds", "16"));
        mTransitionTimeField.addItem(new ImagesWrapper("17 seconds", "17"));
        mTransitionTimeField.addItem(new ImagesWrapper("18 seconds", "18"));
        mTransitionTimeField.addItem(new ImagesWrapper("19 seconds", "19"));
        mTransitionTimeField.addItem(new ImagesWrapper("20 seconds", "20"));
        defaultCombo(mTransitionTimeField, String.valueOf(iPhotoConfiguration.getTransitionTime()));

        mRandomPlayFoldersField = new JCheckBox("Random play folders          ");
        mRandomPlayFoldersField.setToolTipText("Check to specify that photos in folders should be played randomly");
        mRandomPlayFoldersField.setSelected(iPhotoConfiguration.isRandomPlayFolders());


        String imagelistPath = iPhotoConfiguration.getImagelistPath();
        if (imagelistPath == null) {
            if (SystemUtils.IS_OS_MAC_OSX)
                imagelistPath = "/Users/" + System.getProperty("user.name") + "/Pictures/iPhoto Library/AlbumData.xml";
            else
                // Is there even an iPhoto for Windows or Linux?
                imagelistPath = "C:\\Documents and Settings\\" + System.getProperty("user.name")
                    + "\\My Documents\\My Pictures\\iPhoto Library\\AlbumData.xml";
        }
        mImagelistPathField = new JTextField(imagelistPath);

        FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, 3dlu, pref, right:pref:grow", "pref, 9dlu, " + // general
//		FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, 9dlu, " + // general
				"pref, 3dlu, " + // title
				"pref, 9dlu, " + // share
				"pref, 9dlu, " + // options
				"pref, 9dlu, " + // safe
				"pref, 9dlu, " + // random
				"pref, 9dlu, " + // slideshow effects
				"pref, 9dlu, " + // effect
				"pref, 9dlu, " + // display time
				"pref, 9dlu, " + // transition time
				"pref, 9dlu, " + // username
				"pref"); // table

        PanelBuilder builder = new PanelBuilder(layout);
        //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addSeparator("General", cc.xyw(1, 1, 4));
        builder.addLabel("Title", cc.xy(1, 3));
        builder.add(mTitleField, cc.xyw(3, 3, 1));
        builder.add(mSharedField, cc.xyw(3, 5, 1));
        builder.addSeparator("Options", cc.xyw(1, 7, 4));
        builder.add(mUseSafeField, cc.xyw(3, 9, 1));
        builder.add(mRandomPlayFoldersField, cc.xyw(3, 11, 1));
        builder.addSeparator("Slideshow Effects", cc.xyw(1, 13, 4));
        builder.addLabel("Effect", cc.xy(1, 15));
        builder.add(mEffectsField, cc.xyw(3, 15, 1));
        builder.addLabel("Display Time", cc.xy(1, 17));
        builder.add(mDisplayTimeField, cc.xy(3, 17));
        builder.addLabel("Transition Time", cc.xy(1, 19));
        builder.add(mTransitionTimeField, cc.xy(3, 19));

        builder.addLabel("Album data Path", cc.xy(1, 7));
        builder.add(mImagelistPathField, cc.xyw(3, 7, 1));
        JButton button = new JButton("...");
        button.setActionCommand("pick");
        button.addActionListener(this);
        builder.add(button, cc.xyw(5, 7, 1));

        JPanel panel = builder.getPanel();
        //FormDebugUtils.dumpAll(panel);
        add(panel);
    }

    public void actionPerformed(ActionEvent e) {
        if ("pick".equals(e.getActionCommand())) {
            final JFileChooser fc = new JFileChooser();
            fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            fc.addChoosableFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    } else if (f.isFile() && f.getName().toLowerCase().endsWith("xml")) {
                        return true;
                    }

                    return false;
                }

                //The description of this filter
                public String getDescription() {
                    return "Album List";
                }
            });

            int returnVal = fc.showOpenDialog(this);

            if (returnVal == JFileChooser.APPROVE_OPTION) {
                File file = fc.getSelectedFile();
                mImagelistPathField.setText(file.getAbsolutePath());
            }
        }
    }
    
    public boolean valid() {
        if (mTitleField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Invalid title.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        File file = new File(mImagelistPathField.getText());
        if (file.exists())
            return true;
        else
            JOptionPane.showMessageDialog(this, "Invalid Album Data path", "Error", JOptionPane.ERROR_MESSAGE);
        return false;
    }

    public void load() {
    }

    public void save() {
        log.debug("save()");
        iPhotoConfiguration iPhotoConfiguration = (iPhotoConfiguration) mAppConfiguration;
        iPhotoConfiguration.setName(mTitleField.getText());
        iPhotoConfiguration.setImagelistPath(mImagelistPathField.getText());
		iPhotoConfiguration.setUseSafe(mUseSafeField.isSelected());
		iPhotoConfiguration.setEffect(((NameValue) mEffectsField.getSelectedItem()).getValue());
		iPhotoConfiguration.setDisplayTime(Integer.parseInt(((NameValue) mDisplayTimeField.getSelectedItem())
				.getValue()));
		iPhotoConfiguration.setTransitionTime(Integer.parseInt(((NameValue) mTransitionTimeField.getSelectedItem())
				.getValue()));
		iPhotoConfiguration.setRandomPlayFolders(mRandomPlayFoldersField.isSelected());
        iPhotoConfiguration.setShared(mSharedField.isSelected());

        JOptionPane
                .showMessageDialog(
                        this,
                        "Depending on the size of your iPhoto collection, it will take some time to import your collection.\nYou will be able to use the application immediately and the Albums will grow over time.",
                        "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private JTextComponent mTitleField;

    private JTextComponent mImagelistPathField;
    
    private JCheckBox mSharedField;

    private JCheckBox mUseSafeField;
    
    private JCheckBox mRandomPlayFoldersField;

    private JComboBox mEffectsField;

    private JComboBox mDisplayTimeField;

    private JComboBox mTransitionTimeField;

}
