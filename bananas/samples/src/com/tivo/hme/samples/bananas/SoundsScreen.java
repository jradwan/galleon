//////////////////////////////////////////////////////////////////////
//
// File: SoundsScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * A screen demonstrating how to over-ride the default sounds when specific
 * actions are selected, in this case right, left, up and down. We over-ride
 * handleAction and play our sound there.
 * 
 * When BApplication.play() is called, the application will not play the default
 * sound.
 *
 * @author chaynes
 */
public class SoundsScreen extends BananasSampleScreen
{
    /**
     * Constructor
     */
    public SoundsScreen(BApplication app)
    {
        super(app);
        
        //
        // Add a description of the screen.
        //
        
        BText desc = new BText(getNormal(), SAFE_TITLE_H+32, SAFE_TITLE_V+90, getWidth()-((SAFE_TITLE_H*2)+32), 100);
        desc.setVisible(true);
        desc.setValue("The standard navigation sounds have been changed to play custom sounds when the left, right, up and down actions are triggered.");
        desc.setFont("default-24.font");
        desc.setFlags(RSRC_TEXT_WRAP | RSRC_HALIGN_LEFT);
        
        //
        // Create 4 buttons with highlights so that the user can change focus
        // between the buttons.
        //
        
        BButton button1 = new BButton(getNormal(), SAFE_TITLE_H+20, 275, 200, 30);
        button1.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, "pop", H_RIGHT, null, H_DOWN, false);
        button1.setResource(createText("default-18.font", Color.white, "Button 1"));
 
        setFocusDefault(button1);
        
        BButton button2 = new BButton(getNormal(), SAFE_TITLE_H+250, 275, 200, 30);
        button2.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, null, null, H_DOWN, false);
        button2.setResource(createText("default-18.font", Color.white, "Button 2"));

        BButton button3 = new BButton(getNormal(), SAFE_TITLE_H+20, 350, 200, 30);
        button3.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, "pop", H_RIGHT, H_UP, null, false);
        button3.setResource(createText("default-18.font", Color.white, "Button 3"));
 
        BButton button4 = new BButton(getNormal(), SAFE_TITLE_H+250, 350, 200, 30);
        button4.setBarAndArrows(BAR_DEFAULT, BAR_DEFAULT, H_LEFT, null, H_UP, null, false);
        button4.setResource(createText("default-18.font", Color.white, "Button 4"));
    }

    /**
     * Play a custom sound when an action is selcted.
     */
    public boolean handleAction(BView view, Object action) 
    {
        //
        // Override the default sound for a given action.
        //
        
        if (action.equals(H_RIGHT)) {
            getBApp().play("speedup3.snd");
        }  else if (action.equals(H_LEFT)) {
            getBApp().play("slowdown1.snd");
        }  else if (action.equals(H_UP)) {
            getBApp().play("thumbsup.snd");
        }  else if (action.equals(H_DOWN)) {
            getBApp().play("thumbsdown.snd");
        }
        
        return super.handleAction(view, action);
    }   
    
    /**
     * The title of this screen.
     */
    public String toString()
    {
        return "Sounds";
    }
}
