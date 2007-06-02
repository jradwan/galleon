//////////////////////////////////////////////////////////////////////
//
// File: BEvent.java
//
// Copyright (c) 2004, 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import com.tivo.hme.sdk.*;

/**
 * A wrapper class for bananas events.
 *
 * @author      Adam Doppelt
 */
public class BEvent extends HmeEvent implements IBananas
{
	/**
	 * Constructor
	 * @param opcode the opcode for the event
	 * @param id	 the resource id associated with the event
	 */
	protected BEvent( int opcode, int id )
	{
		super( opcode, id );
	}
	
    /**
     * An action occurred.
     */
    public static class Action extends BEvent
    {
        /**
         * The view that generated the action.
         */
        private BView view;

        /**
         * The action itself.
         */
        private Object action;
        
        /**
         * @return the view generating the action
         */
        public BView getView()
        {
        	return view;
        }
        
        /**
         * @return the action itself
         */
        public Object getAction()
        {
        	return action;
        }

        public Action(BView view, Object action)
        {
        	super( BEVT_ACTION, view.getID() );
            this.view = view;
            this.action = action;
        }
        public String toString()
        {
            return getID() + ".ACTION(" + action + ")";
        }
    }

    /**
     * Focus changed.
     */
    public static class Focus extends BEvent
    {
        /**
         * If true, this is the "gained focus" event. Otherwise this is the
         * "lost focus" event.
         */
        private boolean isGained;

        /**
         * The view that gained focus.
         */
        private BView gained;
        
        /**
         * The view that lost focus.
         */
        private BView lost;

		/**
		 * @return If true, this is the "gained focus" event. Otherwise this is the
         * "lost focus" event.
		 */
		public boolean isGained()
		{
			return isGained;
		}
		
		/**
         * @return The view that gained focus.
         */
        public BView getGained()
        {
        	return gained;
        }
        
        /**
         * @return The view that lost focus.
         */
		public BView getLost()
		{
			return lost;
		}
		
		public Focus(boolean isGained, BView gained, BView lost)
        {
        	super( BEVT_FOCUS, isGained ? gained.getID() : lost.getID() );
            this.isGained = isGained;
            this.gained = gained;
            this.lost = lost;
        }
        public String toString()
        {
            return getID() + ".FOCUS(" + isGained() + "," + gained + "," + lost + ")";
        }
    }
    
    /**
     * A screen was entered.
     */
    public static class ScreenEnter extends BEvent
    {
        /**
         * The screen being entered.
         */
        private BScreen screen;

        /**
         * The argument specified during the push/pop.
         */
        private Object arg;

        /**
         * If true, we are returning to this screen due to a pop.
         */
        private boolean isReturn;
        
        /**
         * @return The screen being entered.
         */
        public BScreen getScreen()
        {
        	return screen;
        }
        
        /**
         * @return The argument specified during the push/pop.
         */
        public Object getArg()
        {
        	return arg;
        }    

        /**
         * @return If true, we are returning to this screen due to a pop.
         */
        public boolean isReturn()
        {
        	return isReturn;
        }
        
        public ScreenEnter(BScreen screen, Object arg, boolean isReturn)
        {
        	super( BEVT_SCREEN_ENTER, screen.getID() );
            this.screen = screen;
            this.arg = arg;
            this.isReturn = isReturn;
        }
        public String toString()
        {
            return getID() + ".SCREEN_ENTER(" + arg + "," + isReturn + ")";
        }
    }

    /**
     * A screen was exited.
     */
    public static class ScreenExit extends BEvent
    {
        /**
         * The screen being exited.
         */
        private BScreen screen;
        
        /**
         * The screen being exited.
         */
        public BScreen getScreen()
        {
        	return screen;
        }
        
        public ScreenExit(BScreen screen)
        {
        	super( BEVT_SCREEN_EXIT, screen.getID() );
            this.screen = screen;
        }
        public String toString()
        {
            return getID() + ".SCREEN_EXIT()";
        }
    }
}
