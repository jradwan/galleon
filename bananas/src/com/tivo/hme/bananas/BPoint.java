//////////////////////////////////////////////////////////////////////
//
// File: BPoint.java
//
// Copyright (c) 2004 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * A point.
 * 
 * @author      Adam Doppelt
 */
public class BPoint
{
    /**
     * x coordinate of the point
     */
    public int x;
    
    /**
     * y coordinate of the point
     */
    public int y;

    /**
     * Init point at 0,0.
     */
    public BPoint()
    {
        this(0, 0);
    }

    /**
     * Init point at x,y.
     */
    public BPoint(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Init point at p.x, p.y.
     */
    public BPoint(BPoint p)
    {
        this.x = p.x;
        this.y = p.y;
    }
    
    // accessors
    
    /**
     * @return x coordinate of the point
     */
    public int getX()
    {
    	return x;
    }
    
    /**
     * @return y coordinate of the point
     */
    public int getY()
    {
    	return y;
    }
}
