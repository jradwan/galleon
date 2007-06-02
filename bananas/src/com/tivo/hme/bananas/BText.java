//////////////////////////////////////////////////////////////////////
//
// File: BText.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * A text widget. Calling the various setters (setFont, setValue, etc.) will
 * update the text resource.
 * 
 * @author      Adam Doppelt
 * @author      Jonathan Payne
 */
public class BText extends BView
{
    /**
     * Font for the text.
     */
    private Object font;

    /**
     * Color for the text.
     */
    private Object color;

    /**
     * Color for the text shadow.
     */
    private Object shadowColor;

    /**
     * The current text string for the view.
     */
    private Object value;

    /**
     * Resource flags.
     */
    private int flags;

    /**
     * Shadow x/y offset.
     */
    private int shadowOffset;

    /**
     * View for the text
     */
    private BView fg;
    
    /**
     * View for the shadow
     */
    private BView shadow;

    /**
     * Constructor.
     */
    public BText(BView parent, int x, int y, int width, int height)
    {
        this(parent, x, y, width, height, true);
    }

    /**
     * Constructor.
     */
    public BText(BView parent, int x, int y, int width, int height, boolean visible)
    {
        super(parent, x, y, width, height, visible);
        font = "default-24.font";
        color = "0xD9D9D9";
        shadowOffset = 2;
        fg = this;
    }

    //
    // accessors
    //

    /**
     * Set the font.
     */
    public void setFont(Object font)
    {
        if (!this.font.equals(font)) {
            this.font = font;
            refresh();
        }
    }

    /**
     * Set the color.
     */
    public void setColor(Object color)
    {
        if (!equal(this.color, color)) {
            this.color = color;
            refresh();
        }
    }

    /**
     * Enable the shadow with default values.
     */
    public void setShadow(boolean on)
    {
        setShadow(on ? "0x000000" : null, 2);
    }

    /**
     * Set the shadow's color and offset. A null color turns off the shadow.
     */
    public void setShadow(Object shadowColor, int shadowOffset)
    {
        if (!equal(this.shadowColor, shadowColor) || this.shadowOffset != shadowOffset) {
            this.shadowColor = shadowColor;
            this.shadowOffset = shadowOffset;
            refresh();
        }
    }

	/**
	 * @return the shadow offset
	 */
	public int getShadowOffset()
	{
		return shadowOffset;
	}

	/**
	 * @return the shadow color
	 */
	public Object getShadowColor()
	{
		return shadowColor;
	}
	
	/**
	 * @return the text value
	 */
	public Object getValue()
	{
		return value;
	}
	
    /**
     * Set the value.
     */
    public void setValue(Object value)
    {
        if (!equal(value, this.value)) {
            this.value = value;
            refresh();
        }
    }

    /**
     * Set the rsrc flags.
     */
    public void setFlags(int flags)
    {
        if (this.flags != flags) {
            this.flags = flags;
            refresh();
        }
    }

	/**
     * Something changed - refresh our resource.
     */
    protected void refresh()
    {
        boolean wasShadow = shadow != null;
        boolean isShadow = shadowColor != null;
        String text = (value != null) ? value.toString() : null;

        // create or destroy shadow accordingly
        if (wasShadow != isShadow) {
            flushRsrc(fg, true);
            if (isShadow) {
                // add children
                shadow = new BView(this, shadowOffset, shadowOffset, getWidth(), getHeight());
                fg = new BView(this, 0, 0, getWidth(), getHeight());
            } else {
                // delete children
                flushRsrc(shadow, true);
                fg.remove(); shadow.remove();
                fg = this;
                shadow = null;
            }
        } else {
            // no change in shadow - flush resources
            flushRsrc(fg, text == null);
            flushRsrc(shadow, text == null);
        }
        if (text != null) {
            fg.setResource(createText(font, color, text), flags);
            if (shadow != null) {
                shadow.setResource(createText(font, shadowColor, text), flags);
                shadow.setTransparency(.4f);
            }
        }
    }

    protected void toString(StringBuffer buf)
    {
        super.toString(buf);
        buf.append(",\"").append(value).append("\"");
    }

    /**
     * Remove a view's resource if it has one. Possibly clear the resource as
     * well.
     */
    private void flushRsrc(BView view, boolean clear)
    {
        if (view != null && view.getResource() != null) {
            view.getResource().remove();
            if (clear) {
                view.clearResource();
            }
        }
    }

    /**
     * Compare two possibly null object.
     */
    private boolean equal(Object a, Object b)
    {
        if (a == b) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a.equals(b);
    }
}
