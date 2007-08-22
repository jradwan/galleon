package com.tivo.hme.hdphotos;

import java.io.File;
import java.io.FileFilter;
import javax.imageio.ImageIO;

public class ImageFilter implements FileFilter {
    private String[] types;
    public ImageFilter() {
        types = ImageIO.getReaderFormatNames();
    }

    public boolean accept(File pathname) {
        String name = pathname.getName().toLowerCase();
        if (pathname.isFile()) {
            for (String type : types) {
                if (name.endsWith("." + type)) { 
                    return true;
                }
            }
        }
        return false;
    }

}
