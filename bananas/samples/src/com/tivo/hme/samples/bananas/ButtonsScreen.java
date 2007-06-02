//////////////////////////////////////////////////////////////////////
//
// File: ButtonsScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * A Screen to demonstrate the different look and feels of the standard Bananas
 * button
 *
 * @author Carl Haynes
 */
public class ButtonsScreen extends BananasSampleScreen
{   
    /**
     * Constructor
     */
    public ButtonsScreen(BApplication app)
    {
        super(app);
        
        int top = 175;
        
        //
        // a standard button with the highlights on the outside
        //
        
        boolean highlights_inside = false;
        BButton stdrdButton = new BButton(getNormal(), (getWidth()/2)-150, top, 300, 30);
        stdrdButton.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, "pop", null, null, H_DOWN, highlights_inside);
        stdrdButton.setResource(createText("default-24.font", Color.white, "A Standard Button"));
        setFocusDefault(stdrdButton);
        
        top += 60;
        
        //
        // A sandard button with the highlights on the inside
        //
        
        highlights_inside = true;
        BButton outsideButton = new BButton(getNormal(), (getWidth()/2)-150, top, 300, 30);
        outsideButton.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, "pop", null, H_UP, H_DOWN, highlights_inside);
        outsideButton.setResource(createText("default-24.font", Color.white, "Highlights on the Inside"));
        
        top += 60;
        
        //
        // a left hanging button
        //
        
        BButton leftButton = new BButton(getNormal(), SAFE_TITLE_H, top, 300, 30);
        leftButton.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP, H_DOWN, true);
        leftButton.setResource(createText("default-24.font", Color.white, "A Left Hanging Button"));
                
        top += 60;
        
        // 
        // a right hanging button
        //
        
        BButton rightButton = new BButton(getNormal(), (getWidth()-SAFE_TITLE_H)-300, top, 300, 30);
        rightButton.setBarAndArrows(BAR_DEFAULT, BAR_HANG, "pop", null, H_UP, null, true);
        rightButton.setResource(createText("default-24.font", Color.white, "A Right Hanging Button"));
    }
    
    /**
     * Title of the screen
     */
    public String toString() 
    {
        return "BButton";
    }
}
