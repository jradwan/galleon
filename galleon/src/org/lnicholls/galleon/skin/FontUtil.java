package org.lnicholls.galleon.skin;

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


import java.awt.*;
import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;

import org.lnicholls.galleon.util.Tools;public class FontUtil {

     public static int getLineHeight(Font font)
     {
        BufferedImage buffer = Tools.createBufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = (Graphics2D)buffer.getGraphics();
        try
        {
            FontMetrics fontMetrics = graphics2D.getFontMetrics(font);
            return fontMetrics.getAscent() + fontMetrics.getDescent();
        }
        finally
        {
            graphics2D.dispose();
            buffer.flush();
        }            
     }
     
     public static int getStringWidth(Font font, String value)
     {
        BufferedImage buffer = Tools.createBufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics2D = (Graphics2D)buffer.getGraphics();
        try
        {
            FontMetrics fontMetrics = graphics2D.getFontMetrics(font);
            return fontMetrics.stringWidth(value);
        }
        finally
        {
            graphics2D.dispose();
            buffer.flush();
        }            
     }     
}
