package org.lnicholls.galleon.media;

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

import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.HashMap;

import javax.imageio.ImageIO;

public class ImageManipulator {

    private static HashMap mRenderingHintsMap;

    private static RenderingHints mRenderingHints;

    static {
        mRenderingHintsMap = new HashMap(5);
        mRenderingHintsMap.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mRenderingHintsMap.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        mRenderingHintsMap.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        mRenderingHintsMap.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
        mRenderingHintsMap.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        mRenderingHints = new RenderingHints(mRenderingHintsMap);
        
        ImageIO.setUseCache(false);
    }

    public static BufferedImage getScaledImage(BufferedImage photo, int width, int height) {
        double heightScale = (double) height / (double) photo.getHeight();
        double widthScale = (double) width / (double) photo.getWidth();
        double scaleFactor = 1.0;
        if (heightScale < widthScale)
            scaleFactor = heightScale;
        else
            scaleFactor = widthScale;

        AffineTransformOp op = new AffineTransformOp(AffineTransform.getScaleInstance(scaleFactor, scaleFactor),
                mRenderingHints);
        BufferedImage scaledImage = op.filter(photo, null);
        photo.flush();
        photo = null;
        op = null;

        return scaledImage;
    }
}