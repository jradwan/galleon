//////////////////////////////////////////////////////////////////////
//
// File: BRect.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * A rectangle.
 * 
 * @author      Adam Doppelt
 */
public class BRect
{
    /**
     * x coordinate of the rectangle
     */
    public int x;
    
    /**
     * y coordinate fo the rectangle
     */
    public int y;
    
    /**
     * width of the rectangle
     */
    public int width;
    
    /**
     * height of the rectangle
     */
    public int height;

    /**
     * Init rect with 0,0,0,0
     */
    public BRect()
    {
        this(0, 0, 0, 0);
    }

    /**
     * Init rect with x,y,width,height.
     */
    public BRect(int x, int y, int width, int height)
    {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Init rect with point and width/height.
     */
    public BRect(BPoint p, int width, int height)
    {
        this.x = p.x;
        this.y = p.y;
        this.width = width;
        this.height = height;
    }

    /**
     * Init rect with two points.
     */
    public BRect(BPoint p1, BPoint p2)
    {
        this.x = p1.x;
        this.y = p1.y;
        this.width = p2.x - p1.x;
        this.height = p2.y - p2.y;
    }
    
    /**
     * Init rect with r.
     */
    public BRect(BRect r)
    {
        this.x = r.x;
        this.y = r.y;
        this.width = r.width;
        this.height = r.height;
    }
    
    // accessors
    
    public int getX()
    {
    	return x;
    }
    
    public int getY()
    {
    	return y;
    }
    
    public int getWidth()
    {
    	return width;
    }

    public int getHeight()
    {
    	return height;
    }
}
