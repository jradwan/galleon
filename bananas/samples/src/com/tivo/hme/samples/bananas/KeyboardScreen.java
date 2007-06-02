//////////////////////////////////////////////////////////////////////
//
// File: KeyboardScreen.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.samples.bananas;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BKeyboard;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.bananas.BKeyboard.KeyboardEvent;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.io.FastInputStream;
import com.tivo.hme.sdk.io.HmeInputStream;

/**
 * A set of three sub-screens demonstrating ways to use the BKeyboard widget
 * <p>
 * 1) A plain "enter some text" screen using the default keyboard<br>
 * 2) An email entry screen that updates an email address<br>
 * 3) A custom screen with a scrolling list of words that updates as you type
 * <p>
 *
 * @author Carl Haynes
 */
public class KeyboardScreen extends BananasSampleScreen
{
    // the menu list
    KeyboardList list;
    
    // this string will get updated by the KeyboardEmailScreen 
    String email = "george@whitehouse.gov";
    
    // the three keyboard screens we will be creating
    PlainKeyboardScreen kbts;
    KeyboardEmailScreen kbes;
    KeyboardListScreen kbls;
    
    /**
     * constructor
     */
    public KeyboardScreen(BApplication app)
    {
        super(app);
        
        // create a list and populate it with the keyboard screen descriptions
        
        list = new KeyboardList(getNormal(), SAFE_TITLE_H, 200, getNormal().getWidth() - (SAFE_TITLE_H * 2), 300, 40);
        
        list.add("Plain keyboard");
        list.add("Email keyboard (" + email + ")");
        list.add("No tip area, list on side");
        
        setFocus(list);
    }

    /**
     * On enter, we check the argument and update the email address if it was
     * passed back from the email test screen
     */
    public boolean handleEnter(Object arg, boolean isReturn) {
        if (isReturn == true && arg != null && arg instanceof String) {
            BView view = list.getRow(1);
            if (view != null) {
                if (view.getChildCount() > 0 && (view.getChild(0) instanceof BText)) {
                    BText txt = (BText)view.getChild(0);
                    email = arg.toString();
                    txt.setValue("Email keyboard (" + email + ")");
                }
            }
        }
        return super.handleEnter(arg, isReturn);
    }

    /**
     * Push the selected screen
     */
    public boolean handleAction(BView view, Object action) {
        if ("push".equals(action)) {
            int idx = list.getFocus();
            switch(idx) {
                case 0:
                    if (kbts == null) {
                        kbts  = new PlainKeyboardScreen(getBApp());
                    }
                    getBApp().push(kbts, TRANSITION_LEFT);
                    break;
                case 1:
                    if (kbes == null) {
                        kbes = new KeyboardEmailScreen(getBApp());
                    }
                    getBApp().push(kbes, TRANSITION_LEFT, email);
                    break;
                case 2:
                    if (kbls == null) {
                        kbls = new KeyboardListScreen(getBApp());
                    }
                    getBApp().push(kbls, TRANSITION_LEFT);
                    break;
            }
            return true;
        }        
        return super.handleAction(view, action);
    }
    
    /**
     * 
     */
    public boolean handleKeyPress(int code, long rawcode) {
        if (code == KEY_LEFT) {
            getBApp().pop();
            return true;
        }
        else if (code == KEY_SELECT) {
            postEvent(new BEvent.Action(this, "push"));
            return true;
        }
       return super.handleKeyPress(code, rawcode);
    }

    /**
     * The list appearing on our menu page
     * 
     *
     * @author Carl Haynes
     */
    class KeyboardList extends BList {

        /**
         * @param parent
         * @param x
         * @param y
         * @param width
         * @param height
         * @param rowHeight
         */
        public KeyboardList(BView parent, int x, int y, int width, int height, int rowHeight)
        {
            super(parent, x, y, width, height, rowHeight);
            setBarAndArrows(BAR_HANG, BAR_DEFAULT, null, "push");
        }

        /* (non-Javadoc)
         * @see com.tivo.hme.bananas.BList#createRow(com.tivo.hme.bananas.BView, int)
         */
        protected void createRow(BView parent, int index)
        {
            BText text = new BText(parent, 30, 0, parent.getWidth()-20, parent.getHeight());
            text.setFlags(RSRC_HALIGN_LEFT);
            text.setShadow(true);
            text.setValue(get(index));     
        }
    }
    
    /**
     * Title of the screen
     */
    public String toString() 
    {
        return "BKeyboard";
    }
    
    /**
     * A Screen that displays a default keyboard
     *
     * @author Carl Haynes
     */
    static class PlainKeyboardScreen extends BananasSampleScreen
    {
        BKeyboard kb;
        
        /**
         * constructor
         */
        public PlainKeyboardScreen(BApplication app) {
            super(app);

            // get the size of a default keyboard
            Point p = BKeyboard.getKeyboardSize(BKeyboard.PLAIN_KEYBOARD, true);
            kb = new BKeyboard(getNormal(), 100, 140, p.x, p.y);
           
            setFocus(kb);
            
            // create a "return to menu" button
            BButton button = new BButton(getNormal(), SAFE_TITLE_H, getNormal().getHeight() - SAFE_TITLE_H-30, 400, 30);
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP, null, true);
            button.setResource(createText("default-24.font", Color.white, "Return to main menu"));
            button.setFocusable(true);
        }
        
        public boolean handleAction(BView view, Object action) {
            if ("pop".equals(action)) {
                getBApp().pop();
                return true;
            } else if ("left".equals(action)) {
                getBApp().pop();
                return true;                
            } 

            return super.handleAction(view, action);
        }
        
        /**
         * 
         */
        public boolean handleKeyPress(int code, long rawcode) {
            if (code == KEY_UP) {
                setFocus(kb);
                return true;
            } else if (code == KEY_SELECT) {
                getBApp().pop();
                return true;
            } else if (code == KEY_LEFT) {
                getBApp().pop();
                return true;
            }
           
            return super.handleKeyPress(code, rawcode);
        }
        
        
        /**
         * Title of the screen
         */
        public String toString() 
        {
            return "BKeyboard";
        }
        
    }
    
    /**
     * A screen to enter an email address, the screen passes the string 
     * back to the calling screen through the pop command
     */
    static class KeyboardEmailScreen extends BananasSampleScreen
    {
        BKeyboard kb;
     
        /**
         * @param app
         */
        public KeyboardEmailScreen(BApplication app)
        {
            super(app);
            
            // get the keyboard size
            Point p = BKeyboard.getKeyboardSize(BKeyboard.EMAIL_KEYBOARD, true);
            // create the keyboard
            kb = new BKeyboard(getNormal(), 100, 140, p.x, p.y, BKeyboard.EMAIL_KEYBOARD, 
                    true);
            
            setFocus(kb);  
            
            BButton button = new BButton(getNormal(), SAFE_TITLE_H, getNormal().getHeight() - SAFE_ACTION_H-40, 400, 30);
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP, null, true);
            button.setResource(createText("default-24.font", Color.white, "Return to main menu"));
            button.setFocusable(true);

        }
        
        /**
         * 
         */
        public boolean handleEnter(Object arg, boolean isReturn) {
            if (arg != null && arg instanceof String) {
                kb.setValue(arg.toString());
            }
            return super.handleEnter(arg, isReturn);
        }

        /**
         * 
         */
        public boolean handleAction(BView view, Object action) {
            if ("left".equals(action)) {
                getBApp().pop(kb.getValue());
                return true;
            }
            return super.handleAction(view, action);
        }

        /**
         * 
         */
        public boolean handleKeyPress(int code, long rawcode) {
            if (code == KEY_SELECT) {
                getBApp().pop();
            }
           
            return super.handleKeyPress(code, rawcode);
        }
        
        /**
         * Title of the screen
         */
        public String toString() 
        {
            return "BKeyboard";
        }
    }
    

    
    /**
     * A screen that updates a word list as letters are typed
     * 
     * It also illustrates creating a keyboard with no tip area on
     * the right side
     *
     * @author Carl Haynes
     */
    static class KeyboardListScreen extends BananasSampleScreen
    {
        WordList wordList;
        ArrayList wordArray;
        BKeyboard kb;
        
        /**
         * @param app
         */
        public KeyboardListScreen(BApplication app)
        {
            super(app);
            
            // get the keyboard size
            Point p = BKeyboard.getKeyboardSize(
                    BKeyboard.getStandardKeyboard(BKeyboard.STANDARD_KEYBOARD_LOWERCASE), 
                    false, BKeyboard.INPUT_WIDTH_SAME_AS_WIDGET);
            // create the keyboard
            kb = new BKeyboard(getNormal(), 100, 150, p.x, p.y, 
                    BKeyboard.getStandardKeyboard(BKeyboard.STANDARD_KEYBOARD_LOWERCASE), 
                    false, BKeyboard.INPUT_WIDTH_SAME_AS_WIDGET, true);
            
            setFocus(kb);

            readWords();

            // create the wordlist
            wordList = new WordList(getNormal(), 400, 150, getWidth() - (SAFE_ACTION_H+400), 210, 30);
            wordList.add("Start typing to");
            wordList.add("get word list");

            BButton button = new BButton(getNormal(), SAFE_TITLE_H, getNormal().getHeight() - SAFE_TITLE_H-30, 400, 30);
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP, null, true);
            button.setResource(createText("default-24.font", Color.white, "Return to main menu"));
            button.setFocusable(true);
        }
        
        /**
         * 
         */
        public boolean handleAction(BView view, Object action) {
            if ("left".equals(action)) {
                if (view.getParent() == wordList) {
                    setFocus(kb);
                    return true;
                } else {
                    getBApp().pop();
                }
            }
            return super.handleAction(view, action);
        }
        
        /**
         * handle a change in the value of the BKeyboard by
         * updating the related text field
         */
        public boolean handleEvent(HmeEvent event) {
            if (event instanceof KeyboardEvent) {
                update(((KeyboardEvent)event).getValue());
            }
            return super.handleEvent(event);
        }
        
        /**
         * pop on left or select
         */
        public boolean handleKeyPress(int code, long rawcode) {
            if (code == KEY_LEFT || code == KEY_SELECT) {
                getBApp().pop();             
                return true;
            }
            return super.handleKeyPress(code, rawcode);
        }
        
        /**
         * Title of the screen
         */
        public String toString() 
        {
            return "BKeyboard";
        }
        
        /** 
         *  Updates the list of words on the right hand side.
         */
        public void update(String word)
        {
            wordList.clear();
            if (word == null) {
                return;
            }
                        
            if (word.trim().length() == 0) {
                wordList.add("Start typing to");
                wordList.add("get word list");
                return;
            }
            
            int first = -1;
            for (int i = 0 ; i < wordArray.size() ; i++) {
                if (((String)wordArray.get(i)).startsWith(word.trim().toLowerCase())) {
                    first = i;
                    //wordList.add(wordArray.subList(i, wordArray.size()-1).toArray());
                    if (i + 5000 > wordArray.size()-1) {
                        wordList.add(wordArray.subList(i, i + (wordArray.size()-i)).toArray());
                    } else {
                        wordList.add(wordArray.subList(i, i + 5000).toArray());
                    }
                    return;
                }
            }
            wordList.add("No words starting");
            wordList.add("with " + word);
        }
        
        /**
         * Reads in a list of word from a file and shoves them into a hashtable.
         *
         */
        public void readWords() {

            wordArray = new ArrayList();
            
            FastInputStream fis = null;
            HmeInputStream his = null;
            try {
                URL url = new URL(getBApp().getContext().getBaseURI(),  "com/tivo/hme/samples/bananas/wordlist.txt");
                fis = new FastInputStream(url.openStream(), 255);
                his = new HmeInputStream(fis);
                String s;
                while ((s = his.readLine()) != null) {
                    wordArray.add(s);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    his.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
        
        
        /**
         * Simple list.
         *
         * @author Carl Haynes
         */
        class WordList extends BList {

            /**
             * constructor
             */
            public WordList(BView parent, int x, int y, int width, int height, int rowHeight)
            {
                super(parent, x, y, width, height, rowHeight);
                setBarAndArrows(BAR_DEFAULT, BAR_HANG, "left", null);
            }

            /* (non-Javadoc)
             * @see com.tivo.hme.bananas.BList#createRow(com.tivo.hme.bananas.BView, int)
             */
            protected void createRow(BView parent, int index)
            {
                BText text = new BText(parent, 30, 0, parent.getWidth()-20, parent.getHeight());
                text.setFlags(RSRC_HALIGN_LEFT);
                text.setShadow(true);
                text.setValue(get(index));            
            }   
        }
    }
}
