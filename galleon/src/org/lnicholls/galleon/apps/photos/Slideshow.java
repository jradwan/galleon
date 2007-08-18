package org.lnicholls.galleon.apps.photos;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.util.Effect;
import org.lnicholls.galleon.util.Effects;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;


public class Slideshow extends Thread {
    public Slideshow(SlideshowScreen slideshowScreen) {
        mSlideshowScreen = slideshowScreen;
        ((DefaultApplication) getApp()).setHandleTimeout(true);
    }
    
    public Photos getApp() {
        return (Photos)mSlideshowScreen.getApp();
    }
    
    public PhotosConfiguration getConfiguration() {
        return getApp().getConfiguration();
    }
    
    public void run() {
        PhotosConfiguration photosConfiguration = getConfiguration();
        Effect[] effects = new Effect[0];
        if (photosConfiguration.getEffect().equals(Effects.RANDOM)
        || photosConfiguration.getEffect().equals(Effects.SEQUENTIAL)) {
            String names[] = new String[0];
            names = (String[]) Effects.getEffectNames().toArray(names);
            Arrays.sort(names);
            effects = new Effect[names.length];
            for (int i = 0; i < names.length; i++) {
                String name = names[i];
                effects[i] = Effects.getEffect(name);
            }
        } else {
            ArrayList list = new ArrayList();
            list.add(Effects.getEffect(photosConfiguration.getEffect()));
            effects = (Effect[]) list.toArray(effects);
        }
        BufferedImage photo = null;
        FileInputStream is = null;
        Image image = null;
        Random random = new Random();
        int currentEffect = 0;
        if (photosConfiguration.getEffect().equals(Effects.SEQUENTIAL))
            currentEffect = 0;
        else if (photosConfiguration.getEffect().equals(Effects.RANDOM)) {
            currentEffect = random.nextInt(effects.length);
        }
        while (getApp().getContext() != null) {
            try {
                sleep(1000 * photosConfiguration.getDisplayTime());
                mSlideshowScreen.getNextPos();
                image = mSlideshowScreen.currentImage();
                if (image != null) {
                    File file = new File(image.getPath());
                    try {
                        photo = Tools.ImageIORead(file);
                        if (photo != null) {
                            long startTime = System.currentTimeMillis();
                            photo = (BufferedImage) Tools.getImage(photo);
                            long estimatedTime = System.currentTimeMillis()
                                    - startTime;
                            BufferedImage scaled = ImageManipulator
                                    .getScaledImage(photo,
                                            mSlideshowScreen.mPhoto
                                            .getWidth(),
                                            mSlideshowScreen.mPhoto
                                                    .getHeight());
                            photo.flush();
                            photo = null;
                            if (image.getRotation() != 0
                                    && image.getRotation() != 0) {
                                scaled = ImageManipulator.rotate(scaled,
                                        mSlideshowScreen.getWidth(),
                                        mSlideshowScreen.getHeight(), image
                                                .getRotation());
                            }
                            estimatedTime = System.currentTimeMillis()
                                    - startTime;
                            if (photosConfiguration.getEffect().equals(
                                    Effects.SEQUENTIAL))
                                currentEffect = (currentEffect + 1)
                                        % effects.length;
                            else if (photosConfiguration.getEffect()
                                    .equals(Effects.RANDOM)) {
                                currentEffect = random
                                        .nextInt(effects.length);
                            }
                            Effect effect = (Effect) effects[currentEffect];
                            effect.setDelay(photosConfiguration
                                    .getTransitionTime() * 1000);
                            effect.apply(mSlideshowScreen.mPhoto, scaled);
                        }
                    } catch (Exception ex) {
                        Tools.logException(Photos.class, ex,
                                "Could retrieve image");
                        if (getApp().getContext() == null)
                            return;
                    }
                }
                image = null;
            } catch (InterruptedException ex) {
                return;
            } catch (OutOfMemoryError ex) {
                System.gc();
            } catch (Exception ex2) {
                Tools.logException(Photos.class, ex2);
            }
        }
    }
    public void interrupt() {
        synchronized (this) {
            super.interrupt();
        }
        ((DefaultApplication) getApp()).setHandleTimeout(false);
    }
    private SlideshowScreen mSlideshowScreen;
}