//////////////////////////////////////////////////////////////////////
//
// File: BananasSample.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.HmeEvent;

/**
 * A sample application to show off the features of the Bananas toolkit
 *
 * @author Carl Haynes
 */
public class BananasSample extends BApplication
{
    //
    // This is the title of the application. The runtime environment will
    // discover the title here and use it as the name of the application.
    //
    
    public final static String TITLE = "Bananas Sample";
    
    /**
     * Show the main screen for our sample app, as this is the first screen
     * shown, we will not show a transition to this screen
     */
    public void init(IContext context) throws Exception 
    {
        super.init(context);
        
        getBelow().setResource("blue.mpg");

        push(new MainMenuScreen(this), TRANSITION_NONE);
    }
    
    /**
     * Every screen handle the "pop" action in the same way, so we will handle
     * this in the application.
     */
    public boolean handleAction(BView view, Object action) 
    {
        if (action.equals("pop")) {
            pop();
            return true;
        }        
        return super.handleAction(view, action);
    }
    
    /**
     * This event handler looks for device info in order to substitute a jpeg
     * image as a background instead of the mpeg. Without this the root view in
     * the simulator the root would be left with a black background.
     *
     * This code and extra jpeg image are meant to be removed from a shipping
     * application!
     *
     * REMIND: This is only needed by developers while using the simulator!
     */
    public boolean handleEvent(HmeEvent event)
    {
        switch (event.getOpCode()) {
          case EVT_DEVICE_INFO:
            HmeEvent.DeviceInfo info = (HmeEvent.DeviceInfo)event;

            //
            // If we are running in the simulator display jpg.
            //
            if (((String)info.getMap().get("platform")).startsWith("sim-")) {
                getRoot().setResource("blue.jpg");
            } 
            break;
        }
        return super.handleEvent(event);
    }
    
}
