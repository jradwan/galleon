package com.tivo.hme.hdphotos;

import java.io.File;
import java.util.Vector;

public class FileDirectory implements TVDirectory {
    private HDPhotos app;
    private File file;
    private String label;
    
    public FileDirectory(HDPhotos app, File file) {
        this(app, file, null);
    }
    
    public FileDirectory(HDPhotos app, File file, String label) {
        this.app = app;
        this.file = file;
        this.label = label;
    }

    public Vector<FileDirectory> getDirectories(boolean refresh) {
        Vector<FileDirectory> dirs = new Vector<FileDirectory>();
        
        File[] files = file.listFiles();
        if (files != null) {
            for (File dir : files) {
                if (dir.isDirectory()) {
                    dirs.add(new FileDirectory(app, dir));
                }
            }
        }
        return dirs;
    }

    public String getErrorMessage() {
        if (!file.exists()) {
            return "Directory doesn't exist";
        } else {
            return "The directory does not contain any images";
        }
    }

    public Vector<FileImage> getImages(boolean refresh) {
        Vector<FileImage> images = new Vector<FileImage>();

        File[] files = file.listFiles(new ImageFilter());
        if (files != null) {
            for (File image : files) {
                images.add(new FileImage(app, image));
            }
        }
        return images;
    }

    public String getName() {
        if (label != null) {
            return label;
        } else {
            return file.getName();
        }
    }

    public String getThumbnailLocation() {
        return "images/pc_thumbnail.jpg";
    }

    public boolean isSortHandled() {
        return false;
    }

}
