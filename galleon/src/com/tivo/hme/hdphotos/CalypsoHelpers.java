package com.tivo.hme.hdphotos;

import com.tivo.hme.bananas.BApplication;
import java.io.File;
import java.util.Vector;

public class CalypsoHelpers {
    private static HDPhotos app;
    private static RootDirectory root;

    public static boolean setCalypsoPhotoDirectories(BApplication app, RootDirectory root, String tsn) {
        CalypsoHelpers.app = (HDPhotos)app;
        CalypsoHelpers.root = root;
        setPhotoDirectories();
        return true;
    }
    
    public static void setPhotoDirectories() {
        String paths = System.getProperty("hdphotos.path");
        if (paths != null) {
            String[] split = paths.split(";");
            boolean expandRoot = (split.length == 1);
            for (String path : split) {
                int index = path.indexOf('|');
                String label = null;
                if (index != -1) {
                    label = path.substring(0, index);
                    path = path.substring(index+1);
                }
                File file = new File(path.trim());
                FileDirectory dir = new FileDirectory(app, file, label);
                if (expandRoot) {
                    Vector dirs = dir.getDirectories(true);
                    Vector images = dir.getImages(true);
                    if (images.isEmpty() && !dirs.isEmpty()) {
                        for (FileDirectory child : dir.getDirectories(true)) {
                            root.addDirectory(child);
                        }
                    } else {
                        //there are images in the directory so don't expand it
                        root.addDirectory(dir);
                    }
                } else {
                    root.addDirectory(dir);
                }
            }
        }
    }

    public static RootDirectory getRoot() {
        return root;
    }
}
