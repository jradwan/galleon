//////////////////////////////////////////////////////////////////////
//
// File: IHighlightsLayout.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * An interface describing how to lay out a set of highlights.
 *
 * @author      Adam Doppelt
 */
public interface IHighlightsLayout
{
    /**
     * Return the screen associated with the highlights.
     */
    BScreen getScreen();

    /**
     * Return the rectangle to which the highlights should be anchored in screen
     * coordinates.
     */
    BRect getHighlightBounds();

    /**
     * Return true if a highlight should be visible.
     * @param visible one of the H_VIS_XXX constants
     */
    boolean getHighlightIsVisible(int visible);
}
