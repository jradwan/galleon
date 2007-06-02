//////////////////////////////////////////////////////////////////////
//
// File: IBananas.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

/**
 * Constants for bananas.
 *
 * @author      Adam Doppelt
 */
public interface IBananas
{
    //
    // TRANSITON_XXX constants - see BApplication.push()
    //
    int TRANSITION_NONE = 0;
    int TRANSITION_LEFT = 1;
    int TRANSITION_FADE = 2;

    //
    // Events used by bananas
    //
    int BEVT_ACTION       = 7200;
    int BEVT_FOCUS        = 7201;
    int BEVT_SCREEN_ENTER = 7202;    
    int BEVT_SCREEN_EXIT  = 7203;

    //
    // well-known highlight names
    //
    String H_UP         = "up";
    String H_DOWN       = "down";
    String H_LEFT       = "left";
    String H_RIGHT      = "right";
    String H_BAR        = "bar";
    String H_PAGEUP     = "pageup";
    String H_PAGEDOWN   = "pagedown";
    String H_KEYBOARD	= "keyboard-focus";

    //
    // Arrow names.
    //
    // NOTE: These must match the order of the corresponding key codes.
    //
    String ARROW_NAMES[] = {H_UP, H_DOWN, H_LEFT, H_RIGHT};

    //
    // highlight visibility rules - see BHighlight.setVisible
    //
    int H_VIS_FOCUS              = 0;
    int H_VIS_TRUE               = 1;
    int H_VIS_FALSE              = 2;

    //
    // anchor flags - see BSkin.getAnchor
    //
    int A_CENTER  = 0x01000000;
    int A_LEFT    = 0x03000000;
    int A_RIGHT   = 0x05000000;
    int A_TOP     = 0x03000000;
    int A_BOTTOM  = 0x05000000;

    int A_ANCHOR_MASK   = 0xff000000;
    int A_NEGATIVE_MASK = 0x00f00000;
    int A_DELTA_MASK    = 0x000fffff;

    //
    // bar flags - see BHighlight.setBarAndArrows
    //
    int BAR_HANG    = Integer.MAX_VALUE;
    int BAR_DEFAULT = 0;
}
