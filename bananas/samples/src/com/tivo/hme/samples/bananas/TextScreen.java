//////////////////////////////////////////////////////////////////////
//
// File: TextScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.*;

import com.tivo.hme.bananas.*;
import com.tivo.hme.sdk.*;

/**
 * An Example Screen to to show various attributes you can use when using the
 * BText widget. With the BText widget you can set colors, styles, alignment and
 * display an optional shadow.
 *
 * @author Carl Haynes
 */
public class TextScreen extends BananasSampleScreen
{
    /**
     * Constructor
     */
    public TextScreen(BApplication app)
    {
        super(app);
        
        int top = SAFE_TITLE_V + 100;
        final int border_left = SAFE_TITLE_H + 32;
        final int text_width = getWidth() - ((SAFE_TITLE_H*2)+32);
        
        //
        // A line of text left aligned.
        //
        
        BText plainSample = new BText(getNormal(), border_left, top, text_width, 30);
        plainSample.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
        plainSample.setValue("Left aligned text");
        plainSample.setFont("default-18.font");
 
        //
        // A line of text center aligned.
        //
        
        BText centerAligned = new BText(getNormal(), border_left, top, text_width, 30);
        centerAligned.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER);
        centerAligned.setValue("Centered text");
        centerAligned.setFont("default-18.font");
        
        //
        // A line of text right aligned.
        //
        
        BText rightAligned = new BText(getNormal(), border_left, top, text_width, 30);
        rightAligned.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
        rightAligned.setValue("Right aligned text");
        rightAligned.setFont("default-18.font");
 
        top += 35;
        
        //
        // A line of text with a bold style.
        //
        
        BText boldText = new BText(getNormal(), border_left, top, text_width, 30);
        boldText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
        boldText.setFont("default-18-bold.font");
        boldText.setValue("Bold text");
        
        //
        // A line of text with italic style.
        //
        
        BText italicText = new BText(getNormal(), border_left, top, text_width, 30);
        italicText.setFlags(IHmeProtocol.RSRC_HALIGN_CENTER);
        italicText.setFont("default-18-italic.font");
        italicText.setValue("Italic text");
        
        //
        // A line of text with bold/italic style.
        //
        
        BText boldItalicText = new BText(getNormal(), border_left, top, text_width, 30);
        boldItalicText.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
        boldItalicText.setFont("default-18-bolditalic.font");
        boldItalicText.setValue("Bold italic text");

        top += 35;
        
        //
        // A line of text width a custom color.
        //
        
        BText coloredText = new BText(getNormal(), border_left, top, text_width, 30);
        coloredText.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT);
        coloredText.setFont("default-18-bolditalic.font");
        coloredText.setColor(new Color(150, 100, 100));
        coloredText.setValue("Colored text");

        top += 35;

        //
        // A line of text with a custom shadow (custom color, 
        // offset by 5 pixels).
        //
        
        BText shadowSample2 = new BText(getNormal(), border_left, top, text_width, 50);
        shadowSample2.setFlags(IHmeProtocol.RSRC_HALIGN_LEFT | IHmeProtocol.RSRC_VALIGN_TOP);
        shadowSample2.setShadow(new Color(255, 200, 200), 5);
        shadowSample2.setValue("Text with a custom shadow");
        shadowSample2.setFont("default-18.font");
        
        //
        // a line of text with a default shadow (black, one pixel offset).
        //
        
        BText shadowSample = new BText(getNormal(), border_left, top, text_width, 30);
        shadowSample.setFlags(IHmeProtocol.RSRC_HALIGN_RIGHT);
        shadowSample.setShadow(true);
        shadowSample.setValue("Text with a default shadow");
        shadowSample.setFont("default-18.font");

        top += 35;
        
        //
        // A wrapped line of text over a darkened area.
        //

        BView bg = new BView (getNormal(), border_left, top, text_width, 60);
        bg.setResource(new Color(25, 25, 50));
        bg.setTransparency(.5f);
        
        BText wrapText = new BText(getNormal(), border_left, top, text_width, 60);
        wrapText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP);
        wrapText.setFont("default-18.font");
        wrapText.setValue("This is a very long piece of text that will wrap because the RSRC_TEXT_WRAP flag is set.");

        //
        // Return to main menu button, set a left arrow highlight to call action
        // "pop" when selected.
        //
        
        BButton button = new BButton(getNormal(), SAFE_TITLE_H, (getHeight()-SAFE_TITLE_V)-50, 300, 30);
        button.setResource(createText("default-24.font", Color.white, "Return to main menu"));
        button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
        setFocusDefault(button);
    }
    
    /**
     * Since there is only one button, we will treat select like a left 
     * action and pop the screen.
     */
    public boolean handleKeyPress(int code, long rawcode) 
    {
        switch (code) {
          case KEY_SELECT:
            postEvent(new BEvent.Action(this, "pop"));            
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }
    
    /**
     *  Title of the screen.
     */
    public String toString() 
    {
        return "BText";
    }
}
