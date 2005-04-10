package org.lnicholls.galleon.util;

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
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

public class FileSystemContainer {
    private static Logger log = Logger.getLogger(FileSystemContainer.class.getName());

    public FileSystemContainer(String path) {
         this(path, false);
    }
    
    public FileSystemContainer(String path, boolean recursive) {
        mPath = path;
        mRecursive = recursive;
    }

    public List getItems(FileFilter fileFilter) {
        if (log.isDebugEnabled())
            log.debug("getItems:");

        final ArrayList items = new ArrayList();

        File directory = FileGatherer.resolveLink(new File(getPath())); // Handle shortcuts
        if (!directory.isHidden() && directory.isDirectory()) {
            FileGatherer.gatherDirectory(directory, fileFilter, mRecursive, new FileGatherer.GathererCallback() {
                public void visit(File file, File originalFile) {
                    if (originalFile.equals(file))
                        items.add(new NameFile(file.isDirectory()?file.getName():Tools.extractName(file.getName()), file));
                    else
                        items.add(new NameFile(originalFile.isDirectory()?originalFile.getName():Tools.extractName(originalFile.getName()), file));
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

    public static class NameFile {
        public NameFile(String name, File file) {
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

        public String toString() {
            return mName;
        }

        private String mName;

        private File mFile;
    }

    private String mPath;
    
    private boolean mRecursive;
}