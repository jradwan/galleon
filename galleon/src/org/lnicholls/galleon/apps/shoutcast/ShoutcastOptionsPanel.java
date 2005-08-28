package org.lnicholls.galleon.apps.shoutcast;

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
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.JComboBox;
import javax.swing.JLabel;
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

import edu.stanford.ejalbert.BrowserLauncher;

public class ShoutcastOptionsPanel extends AppConfigurationPanel {
    private static Logger log = Logger.getLogger(ShoutcastOptionsPanel.class.getName());

    class SkinWrapper extends NameValue {
        public SkinWrapper(String name, String value) {
            super(name, value);
        }

        public String toString() {
            return getName();
        }
    }

    public ShoutcastOptionsPanel(AppConfiguration appConfiguration) {
        super(appConfiguration);
        setLayout(new GridLayout(0, 1));

        ShoutcastConfiguration shoutcastConfiguration = (ShoutcastConfiguration) appConfiguration;

        setLayout(new GridLayout(0, 1));

        mTitleField = new JTextField(shoutcastConfiguration.getName());
        mGenreField = new JComboBox();
        mGenreField.addItem(new ComboWrapper("80s", "80s"));
        mGenreField.addItem(new ComboWrapper("Acid Jazz", "Acid Jazz"));
        mGenreField.addItem(new ComboWrapper("African", "African"));
        mGenreField.addItem(new ComboWrapper("Alternative", "Alternative"));
        mGenreField.addItem(new ComboWrapper("Ambient", "Ambient"));
        mGenreField.addItem(new ComboWrapper("Americana", "Americana"));
        mGenreField.addItem(new ComboWrapper("Asian", "Asian"));
        mGenreField.addItem(new ComboWrapper("Big Band", "Big Band"));
        mGenreField.addItem(new ComboWrapper("Bluegrass", "Bluegrass"));
        mGenreField.addItem(new ComboWrapper("Blues", "Blues"));
        mGenreField.addItem(new ComboWrapper("Breakbeat", "Breakbeat"));
        mGenreField.addItem(new ComboWrapper("Cajun", "Cajun"));
        mGenreField.addItem(new ComboWrapper("Christmas", "Christmas"));
        mGenreField.addItem(new ComboWrapper("Classic", "Classic"));
        mGenreField.addItem(new ComboWrapper("Classical", "Classical"));
        mGenreField.addItem(new ComboWrapper("College", "College"));
        mGenreField.addItem(new ComboWrapper("Contemporary", "Contemporary"));
        mGenreField.addItem(new ComboWrapper("Comedy", "Comedy"));
        mGenreField.addItem(new ComboWrapper("Country", "Country"));
        mGenreField.addItem(new ComboWrapper("Downtempo", "Downtempo"));
        mGenreField.addItem(new ComboWrapper("Drum and Bass", "Drum and Bass"));
        mGenreField.addItem(new ComboWrapper("Eclectic", "Eclectic"));
        mGenreField.addItem(new ComboWrapper("Electronic", "Electronic"));
        mGenreField.addItem(new ComboWrapper("European", "European"));
        mGenreField.addItem(new ComboWrapper("Film/Show", "Film/Show"));
        mGenreField.addItem(new ComboWrapper("Folk", "Folk"));
        mGenreField.addItem(new ComboWrapper("Funk", "Funk"));
        mGenreField.addItem(new ComboWrapper("Hardcore", "Hardcore"));
        mGenreField.addItem(new ComboWrapper("Hip Hop", "Hip Hop"));
        mGenreField.addItem(new ComboWrapper("House", "House"));
        mGenreField.addItem(new ComboWrapper("Industrial", "Industrial"));
        mGenreField.addItem(new ComboWrapper("Instrumental", "Instrumental"));
        mGenreField.addItem(new ComboWrapper("Jazz", "Jazz"));
        mGenreField.addItem(new ComboWrapper("Latin", "Latin"));
        mGenreField.addItem(new ComboWrapper("Metal", "Metal"));
        mGenreField.addItem(new ComboWrapper("Middle Eastern", "Middle Eastern"));
        mGenreField.addItem(new ComboWrapper("New School", "New School"));
        mGenreField.addItem(new ComboWrapper("Oldies", "Oldies"));
        mGenreField.addItem(new ComboWrapper("Old School", "Old School"));
        mGenreField.addItem(new ComboWrapper("Opera", "Opera"));
        mGenreField.addItem(new ComboWrapper("Other/Mixed", "Other/Mixed"));
        mGenreField.addItem(new ComboWrapper("Pop", "Pop"));
        mGenreField.addItem(new ComboWrapper("Pop/Rock", "Pop/Rock"));
        mGenreField.addItem(new ComboWrapper("Punk", "Punk"));
        mGenreField.addItem(new ComboWrapper("R&B/Soul", "R&B/Soul"));
        mGenreField.addItem(new ComboWrapper("Reggae/Island", "Reggae/Island"));
        mGenreField.addItem(new ComboWrapper("Rock", "Rock"));
        mGenreField.addItem(new ComboWrapper("Ska", "Ska"));
        mGenreField.addItem(new ComboWrapper("Smooth", "Smooth"));
        mGenreField.addItem(new ComboWrapper("Spiritual", "Spiritual"));
        mGenreField.addItem(new ComboWrapper("Spoken", "Spoken"));
        mGenreField.addItem(new ComboWrapper("Spoken Word", "Spoken Word"));
        mGenreField.addItem(new ComboWrapper("Symphonic", "Symphonic"));
        mGenreField.addItem(new ComboWrapper("Swing", "Swing"));
        mGenreField.addItem(new ComboWrapper("Talk", "Talk"));
        mGenreField.addItem(new ComboWrapper("Techno", "Techno"));
        mGenreField.addItem(new ComboWrapper("Top 40", "Top 40"));
        mGenreField.addItem(new ComboWrapper("TopTen", "TopTen"));
        mGenreField.addItem(new ComboWrapper("Trance", "Trance"));
        mGenreField.addItem(new ComboWrapper("Turntablism", "Turntablism"));
        mGenreField.addItem(new ComboWrapper("Urban", "Urban"));
        mGenreField.addItem(new ComboWrapper("Western Swing", "Western Swing"));
        mGenreField.addItem(new ComboWrapper("World", "World"));

        FormLayout layout = new FormLayout("right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, " + 
                "9dlu, " + "pref, " + // title
                "9dlu, " + "pref, " + // directories
                "9dlu, " + "pref, " + // genre
                "3dlu, " + "pref");

        PanelBuilder builder = new PanelBuilder(layout);
        //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
        builder.setDefaultDialogBorder();

        CellConstraints cc = new CellConstraints();

        builder.addSeparator("General", cc.xyw(1, 1, 4));
        builder.addLabel("Title", cc.xy(1, 3));
        builder.add(mTitleField, cc.xyw(3, 3, 1));
        builder.addSeparator("Options", cc.xyw(1, 5, 4));
        JLabel label = new JLabel("Genre");
        label.setForeground(Color.blue);
        label.setCursor(new Cursor(Cursor.HAND_CURSOR));
        label.setToolTipText("Open site in web browser");
        label.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                try {
                    BrowserLauncher.openURL("http://www.shoutcast.com");
                } catch (Exception ex) {
                }
            }
        });
        builder.add(label, cc.xy(1, 7));
        builder.add(mGenreField, cc.xyw(3, 7, 1));

        mColumnValues = new ArrayList();
        int counter = 0;
        for (Iterator i = shoutcastConfiguration.getGenres().iterator(); i.hasNext();) {
            String value = (String) i.next();
            ArrayList values = new ArrayList();
            values.add(0, value);
            mColumnValues.add(counter++, values);
        }

        ArrayList columnNames = new ArrayList();
        columnNames.add(0, "Genre");
        ArrayList fields = new ArrayList();
        fields.add(mGenreField);
        mOptionsTable = new OptionsTable(this, columnNames, mColumnValues, fields);
        builder.add(mOptionsTable, cc.xyw(1, 9, 4));

        JPanel panel = builder.getPanel();
        //FormDebugUtils.dumpAll(panel);
        add(panel);
    }

    public void load() {
    }
    
    public boolean valid() {
        if (mTitleField.getText().trim().length() == 0) {
            JOptionPane.showMessageDialog(this, "Invalid title.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (mColumnValues.size() == 0) {
            JOptionPane.showMessageDialog(this, "No genres configured.", "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        
        return true;
    }        

    public void save() {
        log.debug("save()");

        ShoutcastConfiguration shoutcastConfiguration = (ShoutcastConfiguration) mAppConfiguration;
        boolean first = shoutcastConfiguration.getGenres().size() == 0;
        shoutcastConfiguration.setName(mTitleField.getText());
        ArrayList newItems = new ArrayList();
        Iterator iterator = mColumnValues.iterator();
        while (iterator.hasNext()) {
            ArrayList rows = (ArrayList) iterator.next();
            newItems.add((String) rows.get(0));
        }
        shoutcastConfiguration.setGenres(newItems);
        
        if (first)
            JOptionPane.showMessageDialog(this,
                            "Shoutcast.com limits the number of stations that can be retrieved per day. Therefore, as explained in the FAQ,\nit might take some time for all your selected genres to be filled with stations.",
                            "Info", JOptionPane.INFORMATION_MESSAGE);
    }

    private JTextComponent mTitleField;

    private JComboBox mGenreField;

    private OptionsTable mOptionsTable;

    private ArrayList mColumnValues;
}