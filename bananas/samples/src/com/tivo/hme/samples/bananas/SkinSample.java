//////////////////////////////////////////////////////////////////////
//
// File: SkinSample.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.io.*;
import java.util.*;

import com.tivo.hme.bananas.*;
import com.tivo.hme.sdk.*;
import com.tivo.hme.sdk.util.*;
import com.tivo.hme.interfaces.IArgumentList;
import com.tivo.hme.interfaces.IContext;

/**
 * This sample shows the changes required to the standard bananas sample to use
 * a different skin. Note: The factory is not "required" to use a custom skin,
 * but it is part of this sample so you can easily see which images you should
 * edit to prepare a new skin, and use this sample as a means of testing the
 * skin.
 *
 * @author Carl Haynes
 * @author Brigham Stevens
 */
public class SkinSample extends BApplication
{
    //
    // This is the title of the application. The runtime environment will
    // discover the title here and use it as the name of the application.
    //
    
    public final static String TITLE = "Bananas Skin Sample";
    SkinSampleFactory factory;
    
    /**
     * Show the main screen for our sample app, as this is the first screen
     * shown, we will not show a transition to this screen
     */
    public void init(IContext context) throws Exception
    {
        super.init(context);

        //
        // Set the skin for this application.
        // All other widgets will use this skin.
        //
        factory = (SkinSampleFactory)getFactory();
        if (factory.skinZip != null) {
            try {
                System.out.println("Using BZipSkin");
                setSkin(new BZipSkin(this, factory.skinZip));
            } catch(IOException e) {
                System.out.println("FAILED to load skin");
                e.printStackTrace();
            }

        } else if (factory.skinDir != null) {
            System.out.println("Using BDirSkin");
            setSkin(new BDirSkin(this, factory.skinDir));

        } else if (factory.skinRes != null) {
            System.out.println("Using BResSkin");
            setSkin(new BResSkin(this, factory.skinRes));
        }

        BSkin skin = getSkin();
        getBelow().setResource(skin.get("background-main").getResource());
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
     * Factory is required to get command line argument of skin directory
     */
    public static class SkinSampleFactory extends Factory
    {
        
        String skinDir; // a folder containing resources for a new skin
        String skinRes;
        String skinZip;
        String bgResource;
        
	protected void init(IArgumentList args)
	{
            if (args.getRemainingCount() != 2) {
                usage();
            }

            String arg = args.shift();
            if("-zip".equals(arg)) {
                skinZip = args.shift();
                File f = new File(skinZip);
                if (!f.exists()) {
                    System.out.println("Zip File not found: " + skinZip);
                    usage();
                }
            } else if("-dir".equals(arg)) {
                skinDir = args.shift();
                File f = new File(skinDir);
                if (!f.exists()) {
                    System.out.println("Directory not found: " + skinDir);
                    usage();
                }
                
            } else if("-res".equals(arg)) {
                skinRes = args.shift();
                if (!skinRes.endsWith("/")) {
                    System.out.println("Invalid Resource Classpath: " + skinRes);
                    usage();
                }
            } else {
                System.out.println("Invalid Argument: " + arg);
                usage();
            }
        }

        void usage()
        {
            System.err.println("Usage: SkinSample -zip <skin Zip File>");
            System.err.println("Usage: SkinSample -dir <skin Directory>");
            System.err.println("Usage: SkinSample -res <skin Resource Path in app jar file>");
            System.exit(1);
        }
    }
}
