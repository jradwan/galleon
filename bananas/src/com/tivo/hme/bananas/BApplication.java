//////////////////////////////////////////////////////////////////////
//
// File: BApplication.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.bananas.ext.HmeEventListener;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.ILogger;
import com.tivo.hme.sdk.Application;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.View;
import java.util.Vector;
import javax.swing.event.EventListenerList;

/**
 * A bananas application.
 *
 * @author      Adam Doppelt
 */
abstract public class BApplication extends Application implements IBananas
{
    /**
     * The animation used to transition between screens.
     */
    public final static String SLIDE_ANIM = "*400,1";
    public final static String FADE_ANIM = "*1000,1";

    /**
     * The layer for screens.
     */
    private BView normal;

    /**
     * The layer "below" the screens. This is a good place to put a background.
     */
    private BView below;

    /**
     * The layer "above" the screens. This is a good place to put foreground
     * objects.
     */
    private BView above;

    /**
     * Skin generator.
     */
    private BSkin skin;
    
    /**
     * A stack of screens. Use push to add a new screen and pop to return to the
     * previous screen.
     */
    private Vector stack;

    /** If true, a sound was played recently. */
    private boolean soundPlayed;

    /** If true, a push/pop occurred recently. */
    private boolean screenChanged;
    
    private EventListenerList listeners;

    /**
     * Init the application. Subclasses must call super.init().
     * 
     * @param context provided for convenience
     */
    public void init(IContext context) throws Exception
    {
        initImpl(context);
    }
    
    protected void initImpl(IContext context) throws Exception
    {
        super.init(context);
        below  = new BView(getRoot());
        normal = new BView(getRoot());
        above  = new BView(getRoot());
        stack = new Vector();
        skin = new BSkin(this);
    }
    
    public int getSafeActionHorizontal() {
        return SAFE_ACTION_H;
    }
    
    public int getSafeActionVertical() {
        return SAFE_ACTION_V;
    }
    
    public int getSafeTitleHorizontal() {
        return SAFE_TITLE_H;
    }
    
    public int getSafeTitleVertical() {
        return SAFE_TITLE_V;
    }

    //
    // accessors
    //
    
    /**
     * Gets the {@link BView} that contains the screens.
     * @return the 'normal' BView.
     */
    public BView getNormal()
    {
    	return normal;
    }
    
    /**
     * Gets the {@link BView} that contains views that appear "below" all
     * screens.
     * @return the 'below' BView.
     */
    public BView getBelow()
    {
    	return below;
    }
    
    /**
     * Gets the {@link BView} that contains views that appear "above" all
     * screens.
     * @return the 'above' BView.
     */
    public BView getAbove()
    {
    	return above;
    }

    /**
     * Get the current screen. Returns null if there is no screen.
     * @return The current screen or null if there is no screen.
     */
    public BScreen getCurrentScreen()
    {
        if (stack.size() > 0) {
            return ((StackFrame)stack.lastElement()).screen;
        }
        return null;
    }

    /**
     * Get the depth of the stack.
     */
    public int getStackDepth()
    {
        return stack.size();
    }

    /**
     * Get the current skin generator.
     * @return the current skin generator.
     */
    public BSkin getSkin()
    {
    	return skin;
    }
    
    /**
     * Set the skin generator. Widgets created after setSkin is called will use
     * the new skin.
     * @param skin the skin generator.
     */
    public void setSkin(BSkin skin)
    {
        this.skin = skin;
    }
    
    public void addHmeEventListener(HmeEventListener listener) {
        if (listeners == null) {
            listeners = new EventListenerList();
        }
        listeners.add(HmeEventListener.class, listener);
    }
    
    public void removeHmeEventListener(HmeEventListener listener) {
        if (listeners != null) {
            listeners.remove(HmeEventListener.class, listener);
        }
    }
    
    protected void fireEventReceived(HmeEvent event) {
        if (listeners != null) {
            HmeEventListener[] list = listeners.getListeners(HmeEventListener.class);
            for (HmeEventListener listener : list) {
                try {
                    listener.eventReceived(this, event);
                } catch (Throwable t) {
                    log(ILogger.LOG_WARNING, 
                            "An exception occurred during listener eventReceived: " + 
                            t.getMessage());
                }
            }
        }
    }

    //
    // event handlers
    //

    /**
     * Dispatch an event by posting it to a resource or view.
     */
    protected void dispatchEvent(HmeEvent event)
    {
        //fire to the event listeners
        fireEventReceived(event);
        
        switch (event.getOpCode()) {
          case EVT_KEY:
            dispatchKeyEvent((HmeEvent.Key)event);
            break;
          default:
            super.dispatchEvent(event);
            break;
        }
    }

    /**
     * Dispatch a key event by posting it to the focused view. If necessary,
     * play the default sound.
     */
    protected void dispatchKeyEvent(HmeEvent.Key ir)
    {
        //
        // figure out where to send the key
        //

        BScreen screen = getCurrentScreen();
        if (screen == null)
        {
            // no screen to send key event to - play bonk on press only
            if (ir.getAction() == KEY_PRESS) {
                play("bonk.snd");
            }
            return;
        }

        View focus = (screen.getFocus() != null) ? screen.getFocus() : screen;

        //
        // send it
        //

        soundPlayed = false;
        screenChanged = false;

        focus.postEvent(ir);

        //
        // do we need to play a sound?
        //

        if (soundPlayed || ir.getAction() == KEY_RELEASE) {
            return;
        }

        boolean wasHandled;
        if (screenChanged) {
            wasHandled = true;
        } else {
            View nfocus = (screen.getFocus() != null) ? screen.getFocus() : screen;
            wasHandled = (focus != nfocus);
        }

        playSoundForKey(ir.getCode(), wasHandled, !screenChanged);
    }
    

    /**
     * Handle an event. Return true when the event is consumed.
     */
    public boolean handleEvent(HmeEvent event)
    {
        switch (event.getOpCode()) {
          case BEVT_ACTION: {
              BEvent.Action a = (BEvent.Action) event;
              return handleAction(a.getView(), a.getAction());
          }
        }
        return super.handleEvent(event);
    }

    /**
     * An action occurred. Return true when the action is consumed.
     * 
     * @param view the view that generated the action
     * @param action the action string
     */
    public boolean handleAction(BView view, Object action)
    {
        return false;
    }

    //
    // screen management
    //

    /**
     * Push a new screen onto the stack, no argument. The new screen will use an
     * animated transition to reveal itself.
     * 
     * @param screen the screen to push
     * @param transition one of the TRANSITION_XXX constants
     */
    public void push(BScreen screen, int transition)
    {
        push(screen, transition, null);
    }
    
    /**
     * Push a new screen onto the stack. If a transition is specified the new
     * screen will use an animated transition to reveal itself.
     * 
     * @param screen the screen to push
     * @param transition one of the TRANSITION_XXX constants
     * @param arg the argument to send when we enter the new screen
     */
    public void push(BScreen screen, int transition, Object arg)
    {
        getRoot().setPainting(false);
        try {
            //
            // exit old screen
            //

            StackFrame previous = null;
            if (stack.size() > 0) {
                previous = (StackFrame)stack.lastElement();
                previous.doExit();
            }

            //
            // enter new screen
            //

            StackFrame next = new StackFrame(screen, transition);
            stack.addElement(next);
            next.doEnter(arg, false);

            //
            // perform the transition. for the new screen, always adjust:
            //   location
            //   transparency

            switch (transition) {
              case TRANSITION_NONE:
                if (previous != null) {
                    previous.screen.setVisible(false);
                }
                screen.setLocation(-normal.getTranslationX(), normal.getTranslationY());
                screen.setTransparency(0);
                break;
                
              case TRANSITION_FADE:
                if (previous != null) {
                    previous.screen.setTransparency(1f, getResource(FADE_ANIM));
                }
                screen.setLocation(-normal.getTranslationX(), normal.getTranslationY());
                screen.setTransparency(1);
                screen.setTransparency(0, getResource(FADE_ANIM));                
                break;
                
              case TRANSITION_LEFT:
                screen.setLocation(-normal.getTranslationX() + normal.getWidth(), normal.getTranslationY());
                screen.setTransparency(0);
                normal.translate(-normal.getWidth(), 0, getResource(SLIDE_ANIM));
                break;
            }
            screen.setVisible(true);

            //
            // remember that the screen changed
            //

            screenChanged = true;
        } finally {
            getRoot().setPainting(true);
        }
    }

    /**
     * Pop back to the previous screen, no return argument.
     */
    public void pop()
    {
        pop(null);
    }
    
    /**
     * Pop back to the previous screen.
     * 
     * @param arg the argument to send when we enter the previous screen
     */
    public void pop(Object arg)
    {
        if (stack.size() <= 1) {
            return;
        }

        getRoot().setPainting(false);
        try {
            //
            // exit old screen
            //

            StackFrame top = (StackFrame)stack.lastElement();
            top.doExit();
            stack.removeElementAt(stack.size() - 1);

            //
            // enter new screen
            //

            StackFrame previous = (StackFrame)stack.lastElement();
            previous.doEnter(arg, true);        
        
            //
            // perform the REVERSE transition
            //
        
            switch (top.transition) {
              case TRANSITION_NONE:
                top.screen.setVisible(false);
                previous.screen.setVisible(true);
                break;
                
              case TRANSITION_FADE:
                top.screen.setTransparency(1f, getResource(FADE_ANIM));
                previous.screen.setTransparency(0f, getResource(FADE_ANIM));
                break;
                
              case TRANSITION_LEFT:
                top.screen.setVisible(false, getResource(SLIDE_ANIM));
                normal.translate(normal.getWidth(), 0, getResource(SLIDE_ANIM));
                break;
            }

            //
            // remember that the screen changed
            //

            screenChanged = true;
        } finally {
            getRoot().setPainting(true);
        }
    }

    //
    // sound framework
    //

    /**
     * Play the default sound for a given key press.
     * 
     * @param code the key code
     * @param wasHandled If true, the application handled the key. Some keys
     * will bonk when they are not handled.
     * @param inScreen If true, the key was used to move around within the
     * screen. The left/right keys play different sounds depending on whether
     * movement was within a screen or between screens.
     */
    public void playSoundForKey(int code, boolean wasHandled, boolean inScreen)
    {
        String snd = null;

        //
        // We divide the world into two sets of keys - primary and
        // secondary. Primary keys always make noise when pressed.
        //
        // If a primary key was handled, play the default sound. If a primary
        // key was not handled, bonk.
        //
        
        if (wasHandled) {
            switch (code) {
              case KEY_UP:           snd = "updown";     break;
              case KEY_DOWN:         snd = "updown";     break;
              case KEY_SELECT:       snd = "select";     break;
              case KEY_THUMBSUP:     snd = "thumbsup";   break;
              case KEY_THUMBSDOWN:   snd = "thumbsdown"; break;
              case KEY_CHANNELUP:    snd = "pageup";     break;
              case KEY_CHANNELDOWN:  snd = "pagedown";   break;

              case KEY_LEFT:   snd = inScreen ? "updown" : "pageup";   break;
              case KEY_RIGHT:  snd = inScreen ? "updown" : "pagedown"; break;
            }
        } else {
            switch (code) {            
              case KEY_UP:
              case KEY_DOWN:
              case KEY_LEFT:
              case KEY_RIGHT:
              case KEY_SELECT:
              case KEY_THUMBSUP:
              case KEY_THUMBSDOWN:
              case KEY_CHANNELUP:
              case KEY_CHANNELDOWN:
                snd = "bonk";
                break;
            }
        }
        if (snd != null) {
            play(snd + ".snd");
        }
    }

    /**
     * Play a sound. This is overridden so that BApplication can determine
     * whether or not to play a default sound when keys are dispatched.
     */
    public void play(String name)
    {
        super.play(name);
        soundPlayed = true;
    }

    /**
     * There is one StackFrame per pushed screen.
     */
    static class StackFrame
    {
        BScreen screen;
        int transition;

        StackFrame(BScreen screen, int transition)
        {
            this.screen = screen;
            this.transition = transition;
        }

        void doEnter(Object arg, boolean isReturn)
        {
            screen.postEvent(new BEvent.ScreenEnter(screen, arg, isReturn));
        }

        void doExit()
        {
            screen.postEvent(new BEvent.ScreenExit(screen));
        }
    }
}
