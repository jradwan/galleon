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
                    {
                        if (file.isDirectory())
                            items.add(new FolderItem(file.getName(), file));
                        else
                        {
                            if (FileFilters.playlistFilter.accept(file))
                                items.add(new PlaylistItem(Tools.extractName(file.getName()), file));
                            else
                                items.add(new FileItem(Tools.extractName(file.getName()), file));
                        }
                    }
                    else
                    {
                        if (file.isDirectory())
                            items.add(new FolderItem(originalFile.getName(), file));
                        else
                        {
                            if (FileFilters.playlistFilter.accept(file))
                                items.add(new PlaylistItem(Tools.extractName(originalFile.getName()), file));
                            else
                                items.add(new FileItem(Tools.extractName(originalFile.getName()), file));
                        }
                    }
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

    public static class Item
    {
        public Item(String name, Object value)
        {
            mName = name;
            mValue = value;
        }
        
        public String getName()
        {
            return mName;
        }
        public Object getValue()
        {
            return mValue;
        }
        
        public boolean isFile()
        {
            return false;
        }
        public boolean isFolder()
        {
            return false;
        }
        public boolean isPlaylist()
        {
            return false;
        }
        
        public String toString()
        {
            return mName;
        }
        
        private String mName;
        private Object mValue;
    }
    
    public static class FileItem extends Item
    {
        public FileItem(String name, Object value)
        {
            super(name,value);
        }
        
        public boolean isFile()
        {
            return true;
        }
    }
    
    public static class FolderItem extends FileItem
    {
        public FolderItem(String name, Object value)
        {
            super(name,value);
        }
        
        public boolean isFolder()
        {
            return true;
        }
    }
    
    public static class PlaylistItem extends Item
    {
        public PlaylistItem(String name, Object value)
        {
            super(name,value);
        }
        
        public boolean isPlaylist()
        {
            return true;
        }
    }

    private String mPath;
    
    private boolean mRecursive;
}