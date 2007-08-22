package com.tivo.hme.hdphotos;

import java.io.File;
import java.io.InputStream;
import java.util.Date;
import java.util.Vector;

public class FileImage implements TVImage {
    private HDPhotos app;

    private File file;
    private int rotation;

    public FileImage(HDPhotos app, File file) {
        this.file = file;
        this.app = app;
    }

    public String getArtist() {
        return "";
    }

    public Date getDateModified() {
        return new Date(file.lastModified());
    }

    public Date getDateTaken() {
        return new Date(file.lastModified());
    }

    public String getDescription() {
        return "";
    }

    public String getFileLocation(int width, int height) {
        return file.getAbsolutePath();
    }

    public InputStream getImageData(int width, int height) {
        return null;
    }

    public String getMimeType() {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".png")) {
            return "image/png";
            
        } else if (name.endsWith(".gif")) {
            return "image/gif";
            
        } else { 
            return "image/jpeg";
        }
    }

    public String getMetaData(String keys) {
        return "";
    }

    public Vector getMetaDataKeys() {
        return new Vector();
    }

    public Vector getTags() {
        return new Vector();
    }

    public String getTitle() {
        String name = file.getName();
        int index = name.lastIndexOf('.');
        if (index != -1) {
            name = name.substring(0, index);
        }
        return name;
    }

    public int getRotationDegrees() {
        return rotation;
    }

    public void setRotationDegrees(int rotation) {
        this.rotation = rotation;
    }

}
