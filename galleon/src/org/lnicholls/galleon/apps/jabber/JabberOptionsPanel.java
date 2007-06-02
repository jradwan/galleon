package org.lnicholls.galleon.apps.jabber;
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
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.util.Tools;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import edu.stanford.ejalbert.BrowserLauncher;
public class JabberOptionsPanel extends AppConfigurationPanel {
	private static Logger log = Logger.getLogger(JabberOptionsPanel.class
			.getName());
	public JabberOptionsPanel(AppConfiguration appConfiguration) {
		super(appConfiguration);
		setLayout(new GridLayout(0, 1));
		JabberConfiguration jabberConfiguration = (JabberConfiguration) appConfiguration;
		mTitleField = new JTextField(jabberConfiguration.getName());
		mSharedField = new JCheckBox("Share");
		mSharedField.setSelected(jabberConfiguration.isShared());
		mSharedField.setToolTipText("Share this app");
		mUsernameField = new JTextField(jabberConfiguration.getUsername());
		mPasswordField = new JPasswordField(Tools.decrypt(jabberConfiguration
				.getPassword()));
		mServerField = new JTextField(jabberConfiguration.getServer());
		FormLayout layout = new FormLayout(
				"right:pref, 3dlu, 50dlu:g, right:pref:grow", "pref, "
						+ "9dlu, " + "pref, "
						+ // title
						"3dlu, " + "pref, " + // share
						"9dlu, " + "pref, " + // login
						"9dlu, " + "pref, " + // username
						"3dlu, " + "pref, " + // password
						"3dlu, " + "pref"); // server
		PanelBuilder builder = new PanelBuilder(layout);
		// DefaultFormBuilder builder = new DefaultFormBuilder(new
		// FormDebugPanel(), layout);
		builder.setDefaultDialogBorder();
		CellConstraints cc = new CellConstraints();
		builder.addSeparator("General", cc.xyw(1, 1, 4));
		builder.addLabel("Title", cc.xy(1, 3));
		builder.add(mTitleField, cc.xyw(3, 3, 1));
		builder.add(mSharedField, cc.xyw(3, 5, 1));
		builder.addSeparator("Login", cc.xyw(1, 7, 4));
		builder.addLabel("Username", cc.xy(1, 9));
		builder.add(mUsernameField, cc.xyw(3, 9, 1));
		builder.addLabel("Password", cc.xy(1, 11));
		builder.add(mPasswordField, cc.xyw(3, 11, 1));
		JLabel label = new JLabel("Server");
		label.setForeground(Color.blue);
		label.setCursor(new Cursor(Cursor.HAND_CURSOR));
		label.setToolTipText("Open list of Jabber servers in web browser");
		label.addMouseListener(new MouseAdapter() {
			public void mousePressed(MouseEvent e) {
				try {
					BrowserLauncher.openURL("http://www.jabber.org/network");
				} catch (Exception ex) {
				}
			}
		});
		builder.add(label, cc.xyw(1, 13, 1));
		builder.add(mServerField, cc.xyw(3, 13, 1));
		JPanel panel = builder.getPanel();
		// FormDebugUtils.dumpAll(panel);
		add(panel);
	}
	public boolean valid() {
		if (mTitleField.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Invalid title.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (mUsernameField.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Invalid username.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (mPasswordField.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Invalid password.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		if (mServerField.getText().trim().length() == 0) {
			JOptionPane.showMessageDialog(this, "Invalid server.", "Error",
					JOptionPane.ERROR_MESSAGE);
			return false;
		}
		return true;
	}
	public void load() {
	}
	public void save() {
		JabberConfiguration jabberConfiguration = (JabberConfiguration) mAppConfiguration;
		jabberConfiguration.setName(mTitleField.getText());
		jabberConfiguration.setUsername(mUsernameField.getText());
		jabberConfiguration
				.setPassword(Tools.encrypt(mPasswordField.getText()));
		jabberConfiguration.setServer(mServerField.getText());
		jabberConfiguration.setShared(mSharedField.isSelected());
	}
	private JTextField mTitleField;
	private JTextField mUsernameField;
	private JPasswordField mPasswordField;
	private JTextField mServerField;
	private JCheckBox mSharedField;
}
