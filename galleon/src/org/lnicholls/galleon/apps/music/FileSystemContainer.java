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

import java.io.File;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Date;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.media.*;
import org.lnicholls.galleon.database.*;

public class FileSystemContainer {
    private static Logger log = Logger.getLogger(FileSystemContainer.class.getName());

    public FileSystemContainer(String path) {
        mPath = path;
    }

    public List getItems() {
        if (log.isDebugEnabled())
            log.debug("getItems:");
        
        final ArrayList items = new ArrayList();

        File directory = FileGatherer.resolveLink(new File(getPath())); // Handle shortcuts
        if (!directory.isHidden() && directory.isDirectory()) {
            FileGatherer.gatherDirectory(directory, FileFilters.audioDirectoryFilter, false,
                    new FileGatherer.GathererCallback() {
                        public void visit(File file) {
                            items.add(new NameFile(Tools.extractName(file.getAbsolutePath()),file));
                        }
                    });
        }
        return items;
    }

    public Date getLastModified() {
        File file = new File(getPath());
        if (file.exists())
            return new Date(file.lastModified());
        return new Date();
    }

    public String getPath() {
        return mPath;
    }

    public void setPath(String path) {
        mPath = path;
    }
    
    public static class NameFile
    {
        public NameFile(String name, File file)
        {
            this.mName = name;
            this.mFile = file;
        }
        
        public String getName() {
            return mName;
        }

        public void setName(String value) {
            mName = value;
        }        
        
        public File getFile() {
            return mFile;
        }

        public void setFile(File value) {
            mFile = value;
        }
        
        public String toString()
        {
            return mName;
        }
        
        private String mName;
        private File mFile;
    }    
    

    private String mPath;
}