//////////////////////////////////////////////////////////////////////
//
// File: BSkin.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;
import java.util.*;

/**
 * A factory for skinnable views. BSkin contains a set of BSkin.Elements. Each
 * element contains the name and size of the asset, and may contain additional
 * properties used when creating the view.<p>
 *
 * BSkin is a container for whispering arrows, highlight bars, and other assets
 * that can be customized.
 * 
 * @author      Adam Doppelt
 */
public class BSkin implements IHmeProtocol, IBananas
{
    /**
     * The application being skinned.
     * Changed to default to allow subclasses in Bananas package to access.
     */
    BApplication app;

    /**
     * The map of skinnable elements.
     * Changed to default to allow subclasses in Bananas package to access.
     */
    Hashtable map;

    /**
     * Create the default skin.
     *
     * @param app the app being skinned
     */
    public BSkin(BApplication app)
    {
        this.app = app;
        map = new Hashtable();

        // whispering arrows
        new Element(this, H_UP,    20, 7, null);
        new Element(this, H_DOWN,  20, 7, null);
        new Element(this, H_LEFT,  8, 20, null);
        new Element(this, H_RIGHT, 8, 20, null);

        // highlight bar
        Element e = new Element(this, H_BAR, 640, 48, null);
        e.set("cap", "30");
        e.set("padh", "10");
        e.set("padv", "4");
        
        // keyboard highlight
        Element eH = new Element(this, H_KEYBOARD, 72, 50, null);
        e.set("cap", "30");
        e.set("padh", "10");
        e.set("padv", "4");

        
        // page up/down
        new Element(this, H_PAGEUP,   14, 26, null);
        new Element(this, H_PAGEDOWN, 14, 26, null);
    }

    //
    // accessors
    //

    /**
     * Get an element. This throws an exception if the element is not found.
     *
     * @param name the name of the element
     * @return the element
     */
    public Element get(String name)
    {
        Element e = (Element)map.get(name);
        if (e == null) {
            throw new RuntimeException("unknown element : " + name);
        }
        
        // stuff in a default asset if necessary
        if (e.rsrc == null) {
            e.rsrc = app.getResource("com/tivo/hme/bananas/" + e.name + ".png");
        }
        
        return e;
    }

    //
    // layout helpers
    //

    /**
     * Get an anchor location. This is used to position a highlight relative to
     * the highlighted view.
     *
     * @param name the element to which the anchors are being applied
     * @param rect the size of the containing view
     * @param anchorX one of the A_XXXX constants, plus an optional delta
     * @param anchorY one of the A_XXXX constants, plus an optional delta
     * @return the x,y of the anchor position
     */
    public BPoint getAnchor(String name, BRect rect, int anchorX, int anchorY)
    {
        //
        // use bit twiddling to extract a delta from the anchor value
        //

	int deltaX;
	if (anchorX > A_DELTA_MASK) {
	    if ((anchorX & A_NEGATIVE_MASK) != 0) {
		deltaX = anchorX | ~A_DELTA_MASK;
		anchorX = (anchorX | A_CENTER) & A_ANCHOR_MASK;
	    } else {
		deltaX  = anchorX & A_DELTA_MASK;
		anchorX = anchorX & A_ANCHOR_MASK;
	    }
	} else {
	    deltaX = anchorX;
	    anchorX = 0;
	}

	int deltaY;
	if (anchorY > A_DELTA_MASK) {
	    if ((anchorY & A_NEGATIVE_MASK) != 0) {
		deltaY = anchorY | ~A_DELTA_MASK;
		anchorY = (anchorY | A_CENTER) & A_ANCHOR_MASK;
	    } else {
		deltaY  = anchorY & A_DELTA_MASK;
		anchorY = anchorY & A_ANCHOR_MASK;
	    }
	} else {
	    deltaY = anchorY;
	    anchorY = 0;
	}
        
        Element e = get(name);

        int x;
        switch (anchorX) {
          case A_LEFT:
            x = 0;
            break;
          default:
          case A_CENTER:
            x = (rect.width - e.width) / 2;
            break;
          case A_RIGHT:
            x = rect.width - e.width;
            break;
        }

        int y;
        switch (anchorY) {
          case A_TOP:
            y = 0;
            break;
          default:
          case A_CENTER:
            y = (rect.height - e.height) / 2;
            break;
          case A_BOTTOM:
            y = rect.height - e.height;
            break;
        }

        return new BPoint(x + deltaX, y + deltaY);
    }

    //
    // skin builders
    //

    /**
     * Create a skinnable view.
     * 
     * @param parent the parent for the new view
     * @param name the name of the skin element
     * @param x the x coord at which to create the view
     * @param y the y coord at which to create the view
     */
    public BView createSkin(BView parent, String name, int x, int y)
    {
        Element e = get(name);
        BView view = new BView(parent, x, y, e.width, e.height);
        view.setResource(e.rsrc);
        return view;
    }

    /**
     * Create a skinnable view and stretch it to the given width. This is used
     * to create stretchable bars.
     * 
     * @param parent the parent for the new view
     * @param name the name of the skin element
     * @param x the x coord at which to create the view
     * @param y the y coord at which to create the view
     * @param width the width of the new view
     */
    public BView createSkinStretch(BView parent, String name, int x, int y, int width)
    {
        Element e = get(name);
        HStretch stretch = new HStretch(parent, e);
        stretch.setBounds(x, y, width, e.height);
        return stretch;
    }

    /**
     * A horizontally stretchable view.
     */
    static class HStretch extends BView
    {
        Element e;
        BView left, right;
        
        HStretch(BView parent, Element e)
        {
            super(parent, 0, 0, 0, 0);

            this.e = e;
            left = new BView(this, 0, 0, 0, 0);
            right = new BView(this, 0, 0, 0, 0);
            left.setResource(e.rsrc, RSRC_HALIGN_LEFT);
            right.setResource(e.rsrc, RSRC_HALIGN_RIGHT);
        }

        public void setBounds(int x, int y, int width, int height, Resource animation)
        {
            super.setBounds(x, y, width, height, animation);

            int split = (Math.min(width, e.width) - e.getInt("cap", 30));

            //
            // REMIND: This makes things look good on brcm hardware that's in
            // rectangular pixel mode. We need a better approach to this kind of
            // thing in the long run. NOTE: This doesn't hurt anything on other
            // hardware that has no restrictions (or the Simulator, for
            // example).
            //
            if (true) {
                BPoint p = toScreen();
                // Scale to rect pixel space to make sure split is still on even
                // boundery (for brcm hardware). NOTE: Clearly this does NOT belong
                // in the SDK so what the hell are we going to do about this?
                int x0 = (p.x * 11) / 10;
                x0 &= ~1;
                while (((x0 + (split * 11) / 10) & 1) != 0) {
                    split -= 1;
                }
            }
            left.setBounds(0, 0, split, e.height, animation);
            right.setBounds(split, 0, width - split, e.height, animation);
        }
    }

    /**
     * A single skinnable element.
     */
    public static class Element
    {
        /**
         * The name of the element - used to find the underlying asset.
         */
        private String name;

        /**
         * Width of the asset.
         */
        private int width;
        
        /**
         * Height of the asset.
         */
        private int height;

        /**
         * Image resource associated with the element.
         */
        private Resource rsrc;

        /**
         * An arbitrary hashtable of properties for use by the element.
         */
        private Hashtable properties;

        /**
         * Create a new element.
         */
        public Element(BSkin skin, String name, int width, int height, Resource rsrc)
        {
            this.name = name.intern();
            this.width = width;
            this.height = height;
            this.rsrc = rsrc;
            skin.map.put(name, this);
        }

        /**
         * @return gives all the properties of this element.
         */
        public Hashtable getProperties()
        {
            return properties;
        }

        /**
         * Set the properties of this element to the given properties.
         */
        public void setProperties(Hashtable properties)
        {
            this.properties = properties;
        }
        
        /**
         * Change the resource of this element.
         */
        public void setResource(Resource rsrc)
        {
            this.rsrc = rsrc;
        }

        /**
         * Change the height of this element.
         */
        public void setHeight(int height)
        {
            this.height = height;
        }
        
        /**
         * Change the width of this element.
         */
        public void setWidth(int width)
        {
            this.width = width;
        }

        /**
         * @return The name of the element - used to find the underlying asset.
         */
        public String getName()
        {
        	return name;
        }
        
        /**
         * @return The width of the asset
         */
        public int getWidth()
        {
        	return width;
        }
        
        /**
         * @return The height of the asset
         */
        public int getHeight()
        {
        	return height;
        }
        
        /**
         * @return The resource associated with the element
         */
        public Resource getResource()
        {
        	return rsrc;
        }
        
        /**
         * Set a property for the element.
         */
        public void set(String key, String value)
        {
            if (properties == null) {
                properties = new Hashtable();
            }
            properties.put(key, value);
        }

        /**
         * Get a property from the element. Returns null if not found.
         */
        public String get(String key)
        {
            if (properties == null) {
                return null;
            }
            return (String)properties.get(key);
        }

        /**
         * Get a property from the element as an integer.
         */
        public int getInt(String key, int defaultValue)
        {
            String value = get(key);
            return (value != null) ? Integer.parseInt(value) : defaultValue;
        }
    }
}
