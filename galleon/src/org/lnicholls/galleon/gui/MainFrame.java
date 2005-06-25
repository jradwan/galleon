package org.lnicholls.galleon.gui;

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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;
import javax.jmdns.ServiceTypeListener;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.MaskFormatter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.app.AppConfigurationPanel;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppDescriptor;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.plaf.BorderStyle;
import com.jgoodies.plaf.HeaderStyle;

import edu.stanford.ejalbert.BrowserLauncher;

public class MainFrame extends JFrame {
    private static Logger log = Logger.getLogger(MainFrame.class.getName());

    public MainFrame(String version) {
        super("Galleon " + version);
        setDefaultCloseOperation(0);

        JMenuBar menuBar = new JMenuBar();
        menuBar.putClientProperty("jgoodies.headerStyle", HeaderStyle.BOTH);
        menuBar.putClientProperty("jgoodies.windows.borderStyle", BorderStyle.SEPARATOR);
        menuBar.putClientProperty("Plastic.borderStyle", BorderStyle.SEPARATOR);

        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic('F');
        fileMenu.add(new MenuAction("New App...", null, "", new Integer(KeyEvent.VK_N)) {

            public void actionPerformed(ActionEvent event) {
                new AddAppDialog(Galleon.getMainFrame()).setVisible(true);
            }

        });
        fileMenu.addSeparator();
        fileMenu.add(new MenuAction("Properties...", null, "", new Integer(KeyEvent.VK_P)) {

            public void actionPerformed(ActionEvent event) {
                new ServerDialog(Galleon.getMainFrame(), Galleon.getServerConfiguration()).setVisible(true);
            }

        });
        fileMenu.add(new MenuAction("Music Player...", null, "", new Integer(KeyEvent.VK_M)) {

            public void actionPerformed(ActionEvent event) {
                new MusicPlayerDialog(Galleon.getMainFrame(), Galleon.getServerConfiguration()).setVisible(true);
            }

        });
        fileMenu.add(new MenuAction("ToGo...", null, "", new Integer(KeyEvent.VK_T)) {

            public void actionPerformed(ActionEvent event) {
                new ToGoDialog(Galleon.getMainFrame(), Galleon.getServerConfiguration()).setVisible(true);
            }

        });
        fileMenu.addSeparator();
        fileMenu.add(new MenuAction("Exit", null, "", new Integer(KeyEvent.VK_X)) {

            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }

        });

        menuBar.add(fileMenu);

        JMenu helpMenu = new JMenu("Help");
        helpMenu.setMnemonic('H');
        helpMenu.putClientProperty("jgoodies.noIcons", Boolean.TRUE);
        helpMenu.add(new MenuAction("About...", null, "", new Integer(KeyEvent.VK_A)) {

            public void actionPerformed(ActionEvent event) {
                JOptionPane
                        .showMessageDialog(
                                Galleon.getMainFrame(),
                                "Galleon Version "
                                        + Tools.getVersion()
                                        + "\nJava Version "+System.getProperty("java.vm.version")
                                        + "\nhttp://galleon.sourceforge.net\njavahmo@users.sourceforge.net\n\251 2005 Leon Nicholls. All Rights Reserved.",
                                "About", JOptionPane.INFORMATION_MESSAGE);
            }

        });
        menuBar.add(helpMenu);

        setJMenuBar(menuBar);
        JComponent content = createContentPane();
        setContentPane(content);

        pack();
        Dimension paneSize = getSize();
        Dimension screenSize = getToolkit().getScreenSize();
        setLocation((screenSize.width - paneSize.width) / 2, (screenSize.height - paneSize.height) / 2);

        URL url = getClass().getClassLoader().getResource("guiicon.gif");

        ImageIcon logo = new ImageIcon(url);
        if (logo != null)
            setIconImage(logo.getImage());

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }

        });
    }

    protected JComponent createContentPane() {
        JPanel panel = new JPanel(new BorderLayout());

        mOptionsPanelManager = new OptionsPanelManager(this);
        mOptionsPanelManager.setMinimumSize(new Dimension(200, 100));
        mOptionsPanelManager.setPreferredSize(new Dimension(400, 200));

        InternalFrame navigator = new InternalFrame("Apps");
        mAppTree = new AppTree(this, getAppsModel());
        navigator.setContent(createScrollPane(mAppTree));
        navigator.setSelected(true);
        navigator.setMinimumSize(new Dimension(100, 100));
        navigator.setPreferredSize(new Dimension(150, 400));

        JSplitPane mainSplitPane = createSplitPane(1, navigator, mOptionsPanelManager, 0.25D);
        mainSplitPane.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        panel.add(mainSplitPane, "Center");

        JLabel statusField = new JLabel("\251 2005 Leon Nicholls");
        statusField.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.add(statusField, "West");
        panel.add(statusPanel, "South");

        panel.setPreferredSize(new Dimension(700, 420));
        return panel;
    }

    public static JScrollPane createScrollPane(Component component) {
        JScrollPane scrollPane = new JScrollPane(component);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        return scrollPane;
    }

    public static JSplitPane createSplitPane(int orientation, Component comp1, Component comp2, double resizeWeight) {
        JSplitPane split = new JSplitPane(1, false, comp1, comp2);
        split.setBorder(new EmptyBorder(0, 0, 0, 0));
        split.setOneTouchExpandable(false);
        split.setResizeWeight(resizeWeight);
        return split;
    }

    public void handleAppSelection(AppNode appNode) {
        mOptionsPanelManager.setSelectedOptionsPanel(appNode);
    }

    public DefaultTreeModel getAppsModel() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("ROOT");
        if (Galleon.getApps()!=null)
        {
            Iterator iterator = Galleon.getApps().iterator();
            while (iterator.hasNext()) {
                AppContext app = (AppContext) iterator.next();
                root.add(new DefaultMutableTreeNode(getAppNode(app)));
            }
        }
        return new DefaultTreeModel(root);
    }

    private AppNode getAppNode(AppContext app) {
        AppDescriptor appDescriptor = app.getDescriptor();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        AppConfigurationPanel appConfigurationPanel = null;
        try {
            Class configurationPanel = classLoader.loadClass(appDescriptor.getConfigurationPanel());

            Class[] parameters = new Class[1];
            parameters[0] = AppConfiguration.class;
            Constructor constructor = configurationPanel.getConstructor(parameters);
            AppConfiguration[] values = new AppConfiguration[1];
            values[0] = app.getConfiguration();

            appConfigurationPanel = (AppConfigurationPanel) constructor.newInstance((Object[]) values);
        } catch (Exception ex) {
            ex.printStackTrace();
            Tools.logException(OptionsPanelManager.class, ex, "Could not load configuration panel "
                    + appDescriptor.getConfigurationPanel() + " for app " + appDescriptor.getClassName());
        }

        ImageIcon icon = null;
        try {
            String pkg = Tools.getPackage(appDescriptor.getClassName());
            URL url = classLoader.getResource(pkg + "/icon.png");
            if (url == null)
                url = classLoader.getResource("icon.png");
            icon = new ImageIcon(new ImageIcon(url).getImage().getScaledInstance(16, 16, Image.SCALE_SMOOTH));
        } catch (Exception ex) {
            Tools.logException(OptionsPanelManager.class, ex, "Could not load icon " + " for app "
                    + appDescriptor.getClassName());
        }

        AppNode appNode = new AppNode(app, icon, appConfigurationPanel);

        return appNode;
    }

    public void addApp(AppContext app) {
        if (log.isDebugEnabled())
            log.debug("addApp: " + app);
        mAppTree.addApp(getAppNode(app));
    }

    public void removeApp(AppContext app) {
        if (log.isDebugEnabled())
            log.debug("removeApp: " + app);
        AppNode appNode = getAppNode(app);
        Galleon.removeApp(app);
        mAppTree.removeApp(appNode);
    }

    public void refresh() {
        mAppTree.refresh();
    }

    class MenuAction extends AbstractAction {
        public MenuAction(String text, ImageIcon icon, String desc, Integer mnemonic) {
            super(text, icon);
            putValue(SHORT_DESCRIPTION, desc);
            putValue(MNEMONIC_KEY, mnemonic);
        }

        public void actionPerformed(ActionEvent e) {
        }
    }

    public class AddAppDialog extends JDialog implements ActionListener, ItemListener, KeyListener {

        class AppDescriptorWrapper {
            public AppDescriptorWrapper(AppDescriptor appDescriptor) {
                mAppDescriptor = appDescriptor;
            }

            public String toString() {
                return mAppDescriptor.getTitle();
            }

            AppDescriptor mAppDescriptor;
        }

        private AddAppDialog(JFrame frame) {
            super(frame, "New App", true);

            mNameField = new JTextField();
            mNameField.addKeyListener(this);
            mVersionField = new JTextField();
            mVersionField.setEditable(false);
            mReleaseDateField = new JTextField();
            mReleaseDateField.setEditable(false);
            mAuthorNameField = new JTextField();
            mAuthorNameField.setEditable(false);
            mAuthorEmailField = new JTextField();
            mAuthorEmailField.setEditable(false);
            mAuthorHomeField = new JTextField();
            mAuthorHomeField.setEditable(false);
            mDocumentationField = new JTextPane();
            mDocumentationField.setEditable(false);
            mAppsCombo = new JComboBox();
            mAppsCombo.addItemListener(this);

            JScrollPane paneScrollPane = new JScrollPane(mDocumentationField);
            paneScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            paneScrollPane.setPreferredSize(new Dimension(250, 150));
            paneScrollPane.setMinimumSize(new Dimension(10, 10));

            Iterator iterator = Galleon.getAppDescriptors().iterator();
            String items[] = new String[0];
            while (iterator.hasNext()) {
                AppDescriptor appDescriptor = (AppDescriptor) iterator.next();
                mAppsCombo.addItem(new AppDescriptorWrapper(appDescriptor));
            }

            getContentPane().setLayout(new BorderLayout());

            FormLayout layout = new FormLayout("right:pref, 3dlu, 150dlu:g, 3dlu, right:pref:grow", "pref, " + //name
                    "9dlu, " + "pref, " + //apps
                    "3dlu, " + "pref, " + //type
                    "9dlu, " + "pref, " + //description
                    "3dlu, " + "pref, " + //version
                    "3dlu, " + "pref, " + //release date
                    "9dlu, " + "pref, " + //author
                    "3dlu, " + "pref, " + //name
                    "3dlu, " + "pref, " + //email
                    "3dlu, " + "pref, " //homepage
            );

            PanelBuilder builder = new PanelBuilder(layout);
            //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
            builder.setDefaultDialogBorder();

            CellConstraints cc = new CellConstraints();

            builder.addLabel("Title", cc.xy(1, 1));
            builder.add(mNameField, cc.xy(3, 1));
            builder.addSeparator("Apps", cc.xyw(1, 3, 5));
            builder.addLabel("Type", cc.xy(1, 5));
            builder.add(mAppsCombo, cc.xy(3, 5));
            builder.addSeparator("Description", cc.xyw(1, 7, 5));
            builder.add(paneScrollPane, cc.xywh(5, 9, 1, 11, CellConstraints.RIGHT, CellConstraints.TOP));
            builder.addLabel("Version", cc.xy(1, 9));
            builder.add(mVersionField, cc.xy(3, 9));
            builder.addLabel("Release Date", cc.xy(1, 11));
            builder.add(mReleaseDateField, cc.xy(3, 11));
            builder.addSeparator("Author", cc.xyw(1, 13, 3));
            builder.addLabel("Name", cc.xy(1, 15));
            builder.add(mAuthorNameField, cc.xy(3, 15));
            builder.addLabel("Email", cc.xy(1, 17));
            builder.add(mAuthorEmailField, cc.xy(3, 17));
            builder.addLabel("Homepage", cc.xy(1, 19));
            builder.add(mAuthorHomeField, cc.xy(3, 19));

            getContentPane().add(builder.getPanel(), "Center");

            JButton[] array = new JButton[3];
            mOKButton = new JButton("OK");
            array[0] = mOKButton;
            array[0].setActionCommand("ok");
            array[0].addActionListener(this);
            array[0].setEnabled(false);
            array[1] = new JButton("Cancel");
            array[1].setActionCommand("cancel");
            array[1].addActionListener(this);
            array[2] = new JButton("Help");
            array[2].setActionCommand("help");
            array[2].addActionListener(this);
            JPanel buttons = ButtonBarFactory.buildCenteredBar(array);

            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            getContentPane().add(buttons, "South");
            pack();
            setLocationRelativeTo(frame);
        }

        public void actionPerformed(ActionEvent e) {
            if ("ok".equals(e.getActionCommand())) {
                AppDescriptor appDescriptor = ((AppDescriptorWrapper) mAppsCombo.getSelectedItem()).mAppDescriptor;
                try {
                    //AppContext app = new AppContext(appDescriptor);
                    AppContext app = Galleon.createAppContext(appDescriptor);
                    app.getConfiguration().setName(mNameField.getText());
                    addApp(app);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    Tools.logException(AddAppDialog.class, ex, "Could not add app : " + appDescriptor);
                }
            } else if ("help".equals(e.getActionCommand())) {
                try {
                    URL url = getClass().getClassLoader().getResource("newapp.html");
                    displayHelp(url);
                } catch (Exception ex) {
                    Tools.logException(AddAppDialog.class, ex, "Could not find new app help ");
                }
                return;
            }
            this.setVisible(false);
        }

        public void itemStateChanged(ItemEvent e) {
            int state = e.getStateChange();
            if (state == ItemEvent.SELECTED) {
                AppDescriptor appDescriptor = ((AppDescriptorWrapper) mAppsCombo.getSelectedItem()).mAppDescriptor;
                mVersionField.setText(appDescriptor.getVersion());
                mReleaseDateField.setText(appDescriptor.getReleaseDate());
                mAuthorNameField.setText(appDescriptor.getAuthorName());
                mAuthorEmailField.setText(appDescriptor.getAuthorEmail());
                mAuthorHomeField.setText(appDescriptor.getAuthorHomepage());
                mDocumentationField.setText(appDescriptor.getDescription());
            } else {
                mVersionField.setText("");
                mReleaseDateField.setText("");
                mAuthorNameField.setText("");
                mAuthorEmailField.setText("");
                mAuthorHomeField.setText("");
                mDocumentationField.setText("");
            }
        }

        public void keyTyped(KeyEvent e) {
        }

        public void keyPressed(KeyEvent e) {
        }

        public void keyReleased(KeyEvent e) {
            String name = mNameField.getText();
            if (name.length() > 0) {
                if (Galleon.getApps()!=null)
                {
                    Iterator iterator = Galleon.getApps().iterator();
                    while (iterator.hasNext()) {
                        AppContext app = (AppContext) iterator.next();
                        if (app.getConfiguration().getName().equals(name)) {
                            mOKButton.setEnabled(false);
                            return;
                        }
                    }
                }
                mOKButton.setEnabled(true);
                return;
            }
            mOKButton.setEnabled(false);
        }

        private JTextField mNameField;

        private JComboBox mAppsCombo;

        private JTextField mVersionField;

        private JTextField mReleaseDateField;

        private JTextField mAuthorNameField;

        private JTextField mAuthorEmailField;

        private JTextField mAuthorHomeField;

        private JTextPane mDocumentationField;

        private JButton mOKButton;

    }

    public class ServerDialog extends JDialog implements ActionListener {

        class NameValueWrapper extends NameValue {
            public NameValueWrapper(String name, String value) {
                super(name, value);
            }

            public String toString() {
                return getName();
            }
        }

        private ServerDialog(JFrame frame, ServerConfiguration serverConfiguration) {
            super(frame, "Server Properties", true);
            mServerConfiguration = serverConfiguration;

            //enable debug logging

            mNameField = new JTextField();
            mNameField.setText(serverConfiguration.getName());
            mVersionField = new JTextField();
            mVersionField.setEditable(false);
            mVersionField.setText(serverConfiguration.getVersion());
            mReloadCombo = new JComboBox();
            mReloadCombo.addItem(new NameValueWrapper("5 minutes", "5"));
            mReloadCombo.addItem(new NameValueWrapper("10 minutes", "10"));
            mReloadCombo.addItem(new NameValueWrapper("20 minutes", "20"));
            mReloadCombo.addItem(new NameValueWrapper("30 minutes", "30"));
            mReloadCombo.addItem(new NameValueWrapper("1 hour", "60"));
            mReloadCombo.addItem(new NameValueWrapper("2 hours", "120"));
            mReloadCombo.addItem(new NameValueWrapper("4 hours", "240"));
            mReloadCombo.addItem(new NameValueWrapper("6 hours", "720"));
            mReloadCombo.addItem(new NameValueWrapper("24 hours", "1440"));
            defaultCombo(mReloadCombo, Integer.toString(serverConfiguration.getReload()));
            mSkinCombo = new JComboBox();
            mSkinCombo.setToolTipText("Select a skin for the Galleon apps");
            List skins = Galleon.getSkins();
            Iterator iterator = skins.iterator();
            String defaultSkin = "";
            while (iterator.hasNext()) {
                File file = (File) iterator.next();
                try {
                    String name = Tools.extractName(file.getCanonicalPath());
                    mSkinCombo.addItem(new NameValueWrapper(name, file.getCanonicalPath()));
                    if (defaultSkin.length() == 0)
                        defaultSkin = file.getCanonicalPath();
                } catch (Exception ex) {
                }
            }
            defaultCombo(mSkinCombo, serverConfiguration.getSkin().length() == 0 ? defaultSkin : serverConfiguration
                    .getSkin());
            mGenerateThumbnails = new JCheckBox("Generate Thumbnails");
            mGenerateThumbnails.setSelected(serverConfiguration.getGenerateThumbnails());
            mShuffleItems = new JCheckBox("Shuffle Items");
            mShuffleItems.setSelected(serverConfiguration.getShuffleItems());
            mDebug = new JCheckBox("Debug logging");
            mDebug.setSelected(serverConfiguration.isDebug());
            mPort = new JFormattedTextField();
            try {
                MaskFormatter formatter = new MaskFormatter("####");
                mPort = new JFormattedTextField(formatter);
                mPort.setValue(new Integer(serverConfiguration.getPort()));
            } catch (Exception ex) {
            }
            mIPAddress = new JComboBox();
            mIPAddress.addItem(new NameValueWrapper("Default", ""));
            try {
                Enumeration enumeration = NetworkInterface.getNetworkInterfaces();
                while (enumeration.hasMoreElements()) {
                    NetworkInterface networkInterface = (NetworkInterface) enumeration.nextElement();
                    Enumeration inetAddressEnumeration = networkInterface.getInetAddresses();
                    while (inetAddressEnumeration.hasMoreElements()) {
                        InetAddress inetAddress = (InetAddress) inetAddressEnumeration.nextElement();
                        mIPAddress.addItem(new NameValueWrapper(inetAddress.getHostAddress(), inetAddress
                                .getHostAddress()));
                    }
                }
            } catch (Exception ex) {
                Tools.logException(MainFrame.class, ex, "Could not get network interfaces");
            }
            defaultCombo(mIPAddress, serverConfiguration.getIPAddress());
            mRecordingsPath = new JTextField();
            mRecordingsPath.setText(serverConfiguration.getRecordingsPath());
            mMediaAccessKey = new JTextField();
            mMediaAccessKey.setText(Tools.decrypt(serverConfiguration.getMediaAccessKey()));

            getContentPane().setLayout(new BorderLayout());

            FormLayout layout = new FormLayout("right:pref, 3dlu, pref, left:pref, 3dlu, right:pref:grow", "pref, " + //settings
                    "6dlu, " + "pref, " + //name
                    "3dlu, " + "pref, " + //version
                    "3dlu, " + "pref, " + //reload
                    "3dlu, " + "pref, " + //reload
                    "3dlu, " + "pref, " + //generatethumbnails, streamingproxy
                    "3dlu, " + "pref, " + //debug
                    "3dlu, " + "pref, " + //recordings path
                    "3dlu, " + "pref, " + //media access key
                    "9dlu, " + "pref, " + //network
                    "6dlu, " + "pref, " + //port
                    "3dlu, " + "pref, " //address
            );

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();

            CellConstraints cc = new CellConstraints();

            builder.addSeparator("Settings", cc.xyw(1, 1, 6));
            builder.addLabel("Name", cc.xy(1, 3));
            builder.add(mNameField, cc.xyw(3, 3, 2));
            builder.addLabel("Version", cc.xy(1, 5));
            builder.add(mVersionField, cc.xyw(3, 5, 2));
            builder.addLabel("Reload", cc.xy(1, 7));
            builder.add(mReloadCombo, cc.xyw(3, 7, 2));
            builder.addLabel("Skin", cc.xy(1, 9));
            builder.add(mSkinCombo, cc.xyw(3, 9, 2));
            // TODO Only show for Windows
            builder.add(mGenerateThumbnails, cc.xy(3, 11));
            builder.add(mDebug, cc.xy(3, 13));
            JButton button = new JButton("...");
            button.setActionCommand("pick");
            button.addActionListener(this);
            builder.addLabel("Recordings Path", cc.xy(1, 15));
            builder.add(mRecordingsPath, cc.xyw(3, 15, 2));
            builder.add(button, cc.xyw(6, 15, 1));
            builder.addLabel("Media Access Key", cc.xy(1, 17));
            builder.add(mMediaAccessKey, cc.xyw(3, 17, 2));

            builder.addSeparator("Network", cc.xyw(1, 19, 6));
            builder.addLabel("Port", cc.xy(1, 21));
            builder.add(mPort, cc.xy(3, 21));
            builder.addLabel("IP Address", cc.xy(1, 23));
            builder.add(mIPAddress, cc.xy(3, 23));
            button = new JButton("<< Test...");
            button.setActionCommand("network");
            button.addActionListener(this);
            builder.add(button, cc.xyw(5, 23, 2));

            getContentPane().add(builder.getPanel(), "Center");

            JButton[] array = new JButton[3];
            array[0] = new JButton("OK");
            array[0].setActionCommand("ok");
            array[0].addActionListener(this);
            array[1] = new JButton("Cancel");
            array[1].setActionCommand("cancel");
            array[1].addActionListener(this);
            array[2] = new JButton("Help");
            array[2].setActionCommand("help");
            array[2].addActionListener(this);
            JPanel buttons = ButtonBarFactory.buildCenteredBar(array);

            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            getContentPane().add(buttons, "South");
            pack();
            setLocationRelativeTo(frame);
        }

        public void defaultCombo(JComboBox combo, String value) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (((NameValue) combo.getItemAt(i)).getValue().equals(value)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if ("ok".equals(e.getActionCommand())) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    mServerConfiguration.setName(mNameField.getText());
                    mServerConfiguration.setReload(Integer.parseInt(((NameValue) mReloadCombo.getSelectedItem())
                            .getValue()));
                    mServerConfiguration.setSkin(((NameValue) mSkinCombo.getSelectedItem()).getValue());
                    try {
                        mServerConfiguration.setPort(Integer.parseInt(mPort.getText()));
                    } catch (NumberFormatException ex) {
                        Tools.logException(MainFrame.class, ex, "Invalid port: " + mPort.getText());
                    }
                    mServerConfiguration.setIPAddress(((NameValue) mIPAddress.getSelectedItem()).getValue());
                    mServerConfiguration.setShuffleItems(mShuffleItems.isSelected());
                    mServerConfiguration.setGenerateThumbnails(mGenerateThumbnails.isSelected());
                    mServerConfiguration.setRecordingsPath(mRecordingsPath.getText());
                    mServerConfiguration.setMediaAccessKey(Tools.encrypt(mMediaAccessKey.getText()));
                    mServerConfiguration.setDebug(mDebug.isSelected());

                    Galleon.updateServerConfiguration(mServerConfiguration);
                } catch (Exception ex) {
                    Tools.logException(MainFrame.class, ex, "Could not configure server");
                }
                //JOptionPane.showMessageDialog(this,
                //        "You need to restart Galleon for any changes in the server properties to take effect.",
                //        "Warning", JOptionPane.WARNING_MESSAGE);

                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else if ("help".equals(e.getActionCommand())) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    URL url = getClass().getClassLoader().getResource("server.html");
                    displayHelp(url);
                } catch (Exception ex) {
                    Tools.logException(MainFrame.class, ex, "Could not find server help ");
                }
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            } else if ("pick".equals(e.getActionCommand())) {
                final JFileChooser fc = new JFileChooser();
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.addChoosableFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        if (f.isDirectory()) {
                            return true;
                        }

                        return false;
                    }

                    public String getDescription() {
                        return "Directories";
                    }
                });

                int returnVal = fc.showOpenDialog(this);

                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    mRecordingsPath.setText(file.getAbsolutePath());
                }
                this.toFront();
                return;
            } else if ("network".equals(e.getActionCommand())) {
                new NetworkDialog(this).setVisible(true);
                this.toFront();
                return;
            }

            this.setVisible(false);
        }

        private JTextField mNameField;

        private JTextField mVersionField;

        private JComboBox mReloadCombo;

        private JComboBox mSkinCombo;

        private JCheckBox mGenerateThumbnails;

        private JCheckBox mShuffleItems;

        private JCheckBox mDebug;

        private JFormattedTextField mPort;

        private JComboBox mIPAddress;

        private JTextField mRecordingsPath;

        private JTextField mMediaAccessKey;

        private ServerConfiguration mServerConfiguration;
    }

    public class MusicPlayerDialog extends JDialog implements ActionListener {

        class MusicWrapper extends NameValue {
            public MusicWrapper(String name, String value) {
                super(name, value);
            }

            public String toString() {
                return getName();
            }
        }

        private MusicPlayerDialog(JFrame frame, ServerConfiguration serverConfiguration) {
            super(frame, "Music Player", true);
            mServerConfiguration = serverConfiguration;

            MusicPlayerConfiguration musicPlayerConfiguration = mServerConfiguration.getMusicPlayerConfiguration();

            mPlayersField = new JComboBox();
            mPlayersField.setToolTipText("Select a music player");
            mPlayersField.addItem(new MusicWrapper(StringUtils.capitalize(MusicPlayerConfiguration.CLASSIC),
                    MusicPlayerConfiguration.CLASSIC));
            mPlayersField.addItem(new MusicWrapper(StringUtils.capitalize(MusicPlayerConfiguration.WINAMP),
                    MusicPlayerConfiguration.WINAMP));
            defaultCombo(mPlayersField, musicPlayerConfiguration.getPlayer());

            List skins = Galleon.getWinampSkins();

            mSkinsField = new JComboBox();
            mSkinsField.setPreferredSize(new Dimension(400, 20));
            mSkinsField.setToolTipText("Select a Winamp classic skin for music player");
            Iterator iterator = skins.iterator();
            while (iterator.hasNext()) {
                File file = (File) iterator.next();
                try {
                    String name = Tools.extractName(file.getCanonicalPath());
                    mSkinsField.addItem(new MusicWrapper(name, file.getCanonicalPath()));
                } catch (Exception ex) {
                }
            }
            defaultCombo(mSkinsField, musicPlayerConfiguration.getSkin());
            mUseAmazonField = new JCheckBox("Use Amazon.com ");
            mUseAmazonField.setToolTipText("Check to specify that Amazon.com should be used for album art");
            mUseAmazonField.setSelected(musicPlayerConfiguration.isUseAmazon());
            mUseAmazonField.setForeground(Color.blue);
            mUseAmazonField.setCursor(new Cursor(Cursor.HAND_CURSOR));
            mUseAmazonField.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    try {
                        BrowserLauncher
                                .openURL("http://www.amazon.com/exec/obidos/tg/browse/-/5174/ref%3Dtab%5Fm%5Fm%5F9/104-1230741-3818310");
                    } catch (Exception ex) {
                    }
                }
            });

            mUseFileField = new JCheckBox("Use Folder.jpg          ");
            mUseFileField.setToolTipText("Check to specify that the Folder.jpg file should be used for album art");
            mUseFileField.setSelected(musicPlayerConfiguration.isUseFile());

            mShowImagesField = new JCheckBox("Show web images        ");
            mShowImagesField.setToolTipText("Check to specify that web images of the artist should be shown");
            mShowImagesField.setSelected(musicPlayerConfiguration.isShowImages());
            mShowImagesField.addItemListener(new ItemListener() {
                public void itemStateChanged(ItemEvent e) {
                    if (mShowImagesField.isSelected()) {
                        JOptionPane
                                .showMessageDialog(
                                        MainFrame.this,
                                        "All search engine queries for images are configured to filter out adult content by default.\nHowever, it is still possible that undesirable content might be returned in these search results.",
                                        "Warning", JOptionPane.WARNING_MESSAGE);
                    }
                }
            });

            mRandomPlayFoldersField = new JCheckBox("Random play folders          ");
            mRandomPlayFoldersField.setToolTipText("Check to specify that music in folders should be played randomly");
            mRandomPlayFoldersField.setSelected(musicPlayerConfiguration.isRandomPlayFolders());

            FormLayout layout = new FormLayout("right:pref, 3dlu, 100dlu:g, right:pref:grow",
                    "pref, 3dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref, 9dlu, pref");

            PanelBuilder builder = new PanelBuilder(layout);
            //DefaultFormBuilder builder = new DefaultFormBuilder(new FormDebugPanel(), layout);
            builder.setDefaultDialogBorder();

            CellConstraints cc = new CellConstraints();

            builder.addSeparator("General", cc.xyw(1, 1, 4));
            builder.addLabel("Player", cc.xy(1, 5));
            builder.add(mPlayersField, cc.xyw(3, 5, 1));
            JLabel label = new JLabel("Winamp Classic Skin");
            label.setToolTipText("Open Winamp site in web browser");
            label.setForeground(Color.blue);
            label.setCursor(new Cursor(Cursor.HAND_CURSOR));
            label.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    try {
                        BrowserLauncher.openURL("http://www.winamp.com/skins");
                    } catch (Exception ex) {
                    }
                }
            });
            builder.add(label, cc.xy(1, 7));
            builder.add(mSkinsField, cc.xyw(3, 7, 1));
            builder.add(mRandomPlayFoldersField, cc.xyw(1, 9, 3));
            builder.addSeparator("Album Art", cc.xyw(1, 11, 4));
            builder.add(mUseAmazonField, cc.xyw(1, 13, 3));
            builder.add(mUseFileField, cc.xyw(1, 15, 3));
            builder.add(mShowImagesField, cc.xyw(1, 17, 3));

            getContentPane().add(builder.getPanel(), "Center");

            JButton[] array = new JButton[3];
            array[0] = new JButton("OK");
            array[0].setActionCommand("ok");
            array[0].addActionListener(this);
            array[1] = new JButton("Cancel");
            array[1].setActionCommand("cancel");
            array[1].addActionListener(this);
            array[2] = new JButton("Help");
            array[2].setActionCommand("help");
            array[2].addActionListener(this);
            JPanel buttons = ButtonBarFactory.buildCenteredBar(array);

            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            getContentPane().add(buttons, "South");
            pack();
            setLocationRelativeTo(frame);
        }

        public void defaultCombo(JComboBox combo, String value) {
            for (int i = 0; i < combo.getItemCount(); i++) {
                if (((NameValue) combo.getItemAt(i)).getValue().equals(value)) {
                    combo.setSelectedIndex(i);
                    return;
                }
            }
        }

        public void actionPerformed(ActionEvent e) {
            if ("ok".equals(e.getActionCommand())) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    MusicPlayerConfiguration musicPlayerConfiguration = mServerConfiguration
                            .getMusicPlayerConfiguration();
                    musicPlayerConfiguration.setPlayer(((NameValue) mPlayersField.getSelectedItem()).getValue());
                    musicPlayerConfiguration.setSkin(((NameValue) mSkinsField.getSelectedItem()).getValue());
                    musicPlayerConfiguration.setUseAmazon(mUseAmazonField.isSelected());
                    musicPlayerConfiguration.setUseFile(mUseFileField.isSelected());
                    musicPlayerConfiguration.setShowImages(mShowImagesField.isSelected());
                    musicPlayerConfiguration.setRandomPlayFolders(mRandomPlayFoldersField.isSelected());

                    Galleon.updateServerConfiguration(mServerConfiguration);
                } catch (Exception ex) {
                    Tools.logException(MainFrame.class, ex, "Could not configure server");
                }
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            } else if ("help".equals(e.getActionCommand())) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    URL url = getClass().getClassLoader().getResource("musicplayer.html");
                    displayHelp(url);
                } catch (Exception ex) {
                    Tools.logException(MainFrame.class, ex, "Could not find server help ");
                }
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            }

            this.setVisible(false);
        }

        private JComboBox mPlayersField;

        private JComboBox mSkinsField;

        private JCheckBox mUseFileField;

        private JCheckBox mUseAmazonField;

        private JCheckBox mShowImagesField;

        private JCheckBox mRandomPlayFoldersField;

        private ServerConfiguration mServerConfiguration;
    }

    public void displayHelp(URL url) {
        if (mHelpDialog != null) {
            mHelpDialog.setVisible(false);
            mHelpDialog.dispose();
        }

        mHelpDialog = new HelpDialog(MainFrame.this, url);
        mHelpDialog.setVisible(true);
    }

    public class NetworkDialog extends JDialog implements ActionListener {

        private NetworkDialog(final ServerDialog serverDialog) {
            super(serverDialog, "Network Wizard", true);

            //enable debug logging

            mProgressBar = new JProgressBar(0, 30);
            mProgressBar.setValue(0);
            //mProgressBar.setStringPainted(true);
            mResultsField = new JTextArea(3, 60);
            mResultsField.setEditable(false);

            getContentPane().setLayout(new BorderLayout());

            FormLayout layout = new FormLayout("right:pref, 3dlu, pref, right:pref:grow", "pref, " + //progress
                    "3dlu, " + "pref" //results
            );

            PanelBuilder builder = new PanelBuilder(layout);
            builder.setDefaultDialogBorder();

            CellConstraints cc = new CellConstraints();

            builder.add(mProgressBar, cc.xyw(1, 1, 4));
            builder.add(mResultsField, cc.xyw(1, 3, 4));

            getContentPane().add(builder.getPanel(), "Center");

            JButton[] array = new JButton[2];
            array[0] = new JButton("Close");
            array[0].setActionCommand("cancel");
            array[0].addActionListener(this);
            array[1] = new JButton("Help");
            array[1].setActionCommand("help");
            array[1].addActionListener(this);
            JPanel buttons = ButtonBarFactory.buildCenteredBar(array);

            buttons.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
            getContentPane().add(buttons, "South");
            pack();
            setLocationRelativeTo(serverDialog);

            mTimer = new Timer();
            mTimer.schedule(new TimerTask() {
                int counter = 0;

                public void run() {
                    if (counter == 0) {
                        mResultsField.setText("Searching...");
                        mTiVoListener = new TiVoListener(((NameValue) serverDialog.mIPAddress.getSelectedItem())
                                .getValue());
                    }
                    mProgressBar.setValue(counter);
                    if (counter++ > 30) {
                        Toolkit.getDefaultToolkit().beep();
                        mProgressBar.setValue(mProgressBar.getMinimum());
                        mProgressBar.setString("");
                        mTiVoListener.stop();
                        if (!mTiVoListener.found())
                            mResultsField.setText("No TiVos found on this network interface");
                        this.cancel();
                    }
                }
            }, 0, 1000);
        }

        public void actionPerformed(ActionEvent e) {
            if ("cancel".equals(e.getActionCommand())) {
                mTiVoListener.stop();
                mTimer.cancel();
            } else if ("help".equals(e.getActionCommand())) {
                this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                try {
                    URL url = getClass().getClassLoader().getResource("network.html");
                    displayHelp(url);
                } catch (Exception ex) {
                    Tools.logException(MainFrame.class, ex, "Could not find network help ");
                }
                this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
                return;
            }
            this.setVisible(false);
        }

        private final class TiVoListener implements ServiceListener, ServiceTypeListener {
            // TiVo with 7.1 software supports rendevouz and has a web server
            private final static String HTTP_SERVICE = "_http._tcp.local.";

            private final static String TIVO_PLATFORM = "platform";

            private final static String TIVO_PLATFORM_PREFIX = "tcd"; // platform=tcd/Series2

            private final static String TIVO_TSN = "TSN";

            private final static String TIVO_SW_VERSION = "swversion";

            private final static String TIVO_PATH = "path";

            public TiVoListener(String address) {
                try {
                    InetAddress inetAddress = null;
                    if (address.equals("Default"))
                        inetAddress = InetAddress.getLocalHost();
                    else
                        inetAddress = InetAddress.getByName(address);

                    mJmDNS = new JmDNS(inetAddress);
                    mJmDNS.addServiceListener(HTTP_SERVICE, this);
                    log.debug("Interface: " + mJmDNS.getInterface());
                } catch (IOException ex) {
                    Tools.logException(TiVoListener.class, ex);
                }
            }

            public void serviceAdded(ServiceEvent event) {
                JmDNS jmdns = event.getDNS();
                String type = event.getType();
                String name = event.getName();
                log.debug("addService: " + name);
                jmdns.requestServiceInfo(type, name);
            }

            public void serviceRemoved(ServiceEvent event) {
                JmDNS jmdns = event.getDNS();
                String type = event.getType();
                String name = event.getName();
                log.debug("removeService: " + name);
            }

            public void serviceTypeAdded(ServiceEvent event) {
                JmDNS jmdns = event.getDNS();
                String type = event.getType();
                String name = event.getName();
                log.debug("addServiceType: " + type);
            }

            public void serviceResolved(ServiceEvent event) {
                JmDNS jmdns = event.getDNS();
                String type = event.getType();
                String name = event.getName();
                ServiceInfo info = event.getInfo();
                log.debug("resolveService: " + type + " (" + name + ")");

                if (type.equals(HTTP_SERVICE)) {
                    for (Enumeration names = info.getPropertyNames(); names.hasMoreElements();) {
                        String prop = (String) names.nextElement();
                        if (prop.equals(TIVO_PLATFORM)) {
                            if (info.getPropertyString(prop).startsWith(TIVO_PLATFORM_PREFIX)) {
                                mFound = true;
                            }
                        }
                    }

                    if (mFound) {
                        mResultsField.setText((mResultsField.getText().equals("Searching...") ? "" : mResultsField
                                .getText()
                                + ", ")
                                + name);
                    }
                }
            }

            public void stop() {
                mJmDNS.removeServiceListener(HTTP_SERVICE, this);
            }

            public boolean found() {
                return mFound;
            }

            private JmDNS mJmDNS;
        }

        private JProgressBar mProgressBar;

        private JTextArea mResultsField;

        private ServerConfiguration mServerConfiguration;

        private Timer mTimer;

        private TiVoListener mTiVoListener;

        private boolean mFound;
    }

    private AppTree mAppTree;

    private OptionsPanelManager mOptionsPanelManager;

    private HelpDialog mHelpDialog;
}