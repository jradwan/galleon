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

import javax.swing.Icon;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppConfigurationPanel;

public class AppNode {
    private static Logger log = Logger.getLogger(AppNode.class.getName());

    public AppNode(AppContext appContext, Icon icon, AppConfigurationPanel appConfigurationPanel) {
        mAppContext = appContext;
        mIcon = icon;
        mAppConfigurationPanel = appConfigurationPanel;
    }

    public String getTitle() {
        return mAppContext.getConfiguration().getName();
    }

    public Icon getIcon() {
        return mIcon;
    }

    public String toString() {
        return getTitle();
    }

    public AppContext getAppContext() {
        return mAppContext;
    }
    
    public AppConfigurationPanel getConfigurationPanel() {
        return mAppConfigurationPanel;
    }

    private AppContext mAppContext;
    
    private AppConfigurationPanel mAppConfigurationPanel;

    private Icon mIcon;
}