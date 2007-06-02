//////////////////////////////////////////////////////////////////////
//
// File: BananasSampleScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;

/**
 * This is the super screen class for all of our sample screens.  It handles
 * setting the title and background image
 * 
 * Each Screen needs to have a toString() method that is used to set the title
 *
 * @author Carl Haynes
 */
public class BananasSampleScreen extends BScreen
{       
    /**
     * Constructor
     */
    public BananasSampleScreen(BApplication app)
    {
        super(app);

        //
        // set the title of the screen
        //
        
        BText title = new BText(getNormal(), SAFE_TITLE_H+100, SAFE_TITLE_V, (getWidth()-(SAFE_TITLE_H*2))-100, 54);
        title.setValue(this.toString());
        title.setColor(Color.yellow);
        title.setShadow(Color.black, 3);
        title.setFlags(RSRC_VALIGN_TOP);
        title.setFont("default-48.font");
    }
}
