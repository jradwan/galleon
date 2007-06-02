//////////////////////////////////////////////////////////////////////
//
// File: BKeyboard.java
//
// Copyright (c) 2005 TiVo Inc.
//
//////////////////////////////////////////////////////////////////////

package com.tivo.hme.bananas;

import java.awt.*;
import java.util.*;

import com.tivo.hme.sdk.HmeEvent;

/**
 * A keyboard widget to allow users to enter text. 
 * <p>
 * The keyboard is simply a two dimensional grid of characters and an associated 
 * image, this grid can be one of the default grids included in the widget or 
 * the developer can make his own.
 * <p>
 * The keyboard has 3 states: <br>
 * &nbsp;&nbsp;1) LOWERCASE - the default state<br>
 * &nbsp;&nbsp;2) UPPERCASE - when the CAPS key is selected<br>
 * &nbsp;&nbsp;3) SYMBOL - when the NUM key is selected<br>
 * <p>
 * The user can set a grid for any of these states through the  
 * setKeyBoardStateCells() method.
 * <p>
 * 
 *
 * @author Carl Haynes
 */
public class BKeyboard extends BView
{ 
    // an event opcode used for apps that wish to handle 
    // keyboard events
    private int EVT_KBD_VALUE = HmeEvent.EVT_RESERVED + 2;
    
    // standard keyboard types
    public static final int PLAIN_KEYBOARD = 1;
    public static final int EMAIL_KEYBOARD = 4;
    
    // standard bananas keyboards
    public static final int STANDARD_KEYBOARD_LOWERCASE = 1;
    public static final int STANDARD_KEYBOARD_UPPERCASE = 2;
    public static final int STANDARD_KEYBOARD_SYMBOL = 3;
    public static final int STANDARD_KEYBOARD_EMAIL_LOWERCASE = 4;
    public static final int STANDARD_KEYBOARD_EMAIL_UPPERCASE = 5;
    public static final int STANDARD_KEYBOARD_EMAIL_SYMBOL = 6;
  
    // 
    // each key (or cell) has a type and a string
    // to indicate how to handle the key
    //
    
    public static final int CELL_TYPE_DEL       = 1;
    public static final int CELL_TYPE_CLR       = 2;
    public static final int CELL_TYPE_SYM       = 3;
    public static final int CELL_TYPE_UNDO      = 4;
    public static final int CELL_TYPE_LOWERCASE = 5;
    public static final int CELL_TYPE_UPPERCASE = 6;
    public static final int CELL_TYPE_CHAR      = 7;
    public static final int CELL_TYPE_EMPTY     = 8;
  
    //     
    // a keyboard can be in one of 4 states, 
    // LOWERCASE, UPPERCASE, or SYMBOL
    // and there needs to be a keyboard grid associated
    // with each of these states, the grid can be 
    // swapped out at any time
    //     
       
    public static final int LOWERCASE = 0;
    public static final int UPPERCASE = 1;
    public static final int SYMBOL = 2;
    protected Keyboard keyboardStates[] = {
            getStandardKeyboard(STANDARD_KEYBOARD_LOWERCASE),   // LOWERCASE
            getStandardKeyboard(STANDARD_KEYBOARD_UPPERCASE),   // UPPERCASE
            getStandardKeyboard(STANDARD_KEYBOARD_SYMBOL),      // SYMBOL
    };
    
    /**
     * The current keyboard state
     */
    protected int keyboardState = LOWERCASE;
        
    /**
     * background view onto which the keys will be placed.
     */
    protected BView kbBgImage;
    protected BView kbBgImage_top;
    protected BView kbBgImage_middle;
    protected BView kbBgImage_bottom;
    
    /**
     * current value of the keyboard
     */
    protected String word = "";

    /**
     * current value of the keyboard
     */
    protected String undoValue = null;
    
    /**
     * 
     */
    public static final int DEFAULT_INPUT_WIDTH = 440;
    public static final int INPUT_WIDTH_SAME_AS_WIDGET = -1;
    
    /**
     * focus manager associated with keyboard
     */
    protected BFocusMgr focusMgr = new BFocusMgr();
    protected boolean highlightVis = false;
    
    /**
     * currently focused letter
     */
    protected LetterView focused = null;
    
    /**
     * all keys have a standard height
     */
    protected int rowHeight = 30;   
    
    /**
     * speed in which highlight moves
     */
    protected static final String animation = null;
    
    /**
     * A set of highlights to be shared across all rows.
     */
    protected BHighlights keyboardHighlights;
    
    /**
     * The datafield in which the current value will be displayed
     */
    
    protected DisplayText textField;
    
    /**
     * a hash of standard keyboards
     */
    protected static HashMap standardKeyboards = new HashMap();
    
    /**
     * Constructor create plain keyboard with a tips area.
     *
     */
    public BKeyboard(BView parent, int x, int y, int width, int height)
    {
        this(parent, x, y, width, height, getStandardKeyboard(STANDARD_KEYBOARD_LOWERCASE), 
                true, DEFAULT_INPUT_WIDTH, true);
    }

    /**
     * Constructor for creating  either a plain or an email keyboard
     * with an optional tips area.
     * <p>
     * keyboardType can be one of: 
     * <p>
     * PLAIN_KEYBOARD<br>
     * EMAIL_KEYBOARD<br>
     */
    public BKeyboard(BView parent, int x, int y, int width, int height, 
            int keyboardType, boolean tips)
    {
        this(parent, x, y, width, height, getStandardKeyboard(keyboardType), 
                tips, DEFAULT_INPUT_WIDTH, true);
        
        if (keyboardType == EMAIL_KEYBOARD) {
            linkKeyboardToState(UPPERCASE, getStandardKeyboard(STANDARD_KEYBOARD_EMAIL_UPPERCASE));
            linkKeyboardToState(SYMBOL, getStandardKeyboard(STANDARD_KEYBOARD_EMAIL_SYMBOL));
        }
    }

    
    /**
     * Constructor 
     */
    public BKeyboard(BView parent, int x, int y, int width, int height,
            Keyboard keyboard, boolean tips, int textEntryWidth, boolean visible)
    {
        super(parent, x, y, width, height, visible);
        
        if (keyboard == null) {
            throw new IllegalArgumentException("Keyboard can not be null");
        }
        
        // set the normal state to be the grid passed in
        keyboardStates[LOWERCASE] = keyboard;
        
        setFocusable(true);
        
        this.keyboardHighlights = new BHighlights(new HighlightsLayout());
        
        int kbwidth = getKeyboardSize(keyboard, tips, textEntryWidth).x;
        int kbheight = getKeyboardSize(keyboard, tips, textEntryWidth).y;
        
        // set up views into which we will place our images
        kbBgImage = new BView(this, 0, 40, keyboard.imageWidth, keyboard.imageHeight);
        int t = 0;
        kbBgImage_top = new BView(kbBgImage, 0, t, keyboard.imageWidth, keyboard.topImageHeight);
        t += keyboard.topImageHeight;
        kbBgImage_middle = new BView(kbBgImage, 0, t, keyboard.imageWidth, keyboard.middleImageHeight);
        t += keyboard.middleImageHeight;
        kbBgImage_bottom = new BView(kbBgImage, 0, t, keyboard.imageWidth, keyboard.bottomImageHeight);
        
        setKeyboard(LOWERCASE);
        
        BSkin.Element e = getBApp().getSkin().get(H_KEYBOARD);        
        BHighlight h = new BHighlight(e.getName(), null, -5, -10);
        h.setAbove(true);
        h.setStretchWidth(focused.getWidth() + 10);
        keyboardHighlights.set(h);
        
        
        refreshHighlights(animation);
        
        if (tips) {
            BView tipsView = new BView(this, 300, 60, 137, 138);
            tipsView.setResource("com/tivo/hme/bananas/keyboard-tips.png");
        }
        
        if (textEntryWidth == INPUT_WIDTH_SAME_AS_WIDGET) {
            textEntryWidth = this.getWidth();
        }
        
        BView dataField = new BView(this, 0, 0, textEntryWidth, 35);
        
        BView dataFieldLeft = new BView(dataField, 0, 0, textEntryWidth -20, 35);
        dataFieldLeft.setResource("com/tivo/hme/bananas/keyboard-datafield.png", RSRC_HALIGN_LEFT);
        
        BView dataFieldRight = new BView(dataField, textEntryWidth -20, 0, 20, 35);
        dataFieldRight.setResource("com/tivo/hme/bananas/keyboard-datafield.png", RSRC_HALIGN_RIGHT);
        
        textField = new DisplayText(dataField, 10, 1, dataField.getWidth() - 20, 35);
        textField.setFlags(RSRC_HALIGN_LEFT);
    }
    
    /**
     * A set of 6 standard keyboards that are available for use, these keyboards
     * map to a set of TiVo standard keyboards.
     * <p>
     * type can be one of: 
     * <p>
     *      STANDARD_KEYBOARD_LOWERCASE<br>
     *      STANDARD_KEYBOARD_UPPERCASE<br>
     *      STANDARD_KEYBOARD_SYMBOL<br>
     *      STANDARD_KEYBOARD_EMAIL_LOWERCASE<br>
     *      STANDARD_KEYBOARD_EMAIL_UPPERCASE<br>
     *      STANDARD_KEYBOARD_EMAIL_SYMBOL<br>
     */
    static public Keyboard getStandardKeyboard(int type) {
        Keyboard kb = (Keyboard)standardKeyboards.get(new Integer(type));
        if (kb != null) {
            return kb;
        }
        
        switch (type) {
            case STANDARD_KEYBOARD_LOWERCASE:
                Cell LC_CELLS[][] = new Cell[9][5];
                
                // row 1
                LC_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                LC_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                LC_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                LC_CELLS[3][0] = new Cell(CELL_TYPE_UPPERCASE, null, 177, 6, 49, 28);
                LC_CELLS[4][0] = new Cell(CELL_TYPE_SYM, null, 232, 6, 50, 28);
                
                // row 2
                LC_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, "a", 6, 36, 30, 28);
                LC_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, "b", 36, 36, 30, 28);
                LC_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "c", 66, 36, 30, 28);
                LC_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "d", 96, 36, 30, 28);
                LC_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "e", 126, 36, 30, 28);
                LC_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "f", 156, 36, 30, 28);
                LC_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "g", 186, 36, 30, 28);
                LC_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, "h", 216, 36, 30, 28);
                LC_CELLS[8][1] = new Cell(CELL_TYPE_CHAR, "i", 246, 36, 30, 28);
                
                // row 3
                LC_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, "j", 6, 66, 30, 28);
                LC_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, "k", 36, 66, 30, 28);
                LC_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "l", 66, 66, 30, 28);
                LC_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "m", 96, 66, 30, 28);
                LC_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "n", 126, 66, 30, 28);
                LC_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "o", 156, 66, 30, 28);
                LC_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "p", 186, 66, 30, 28);
                LC_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "q", 216, 66, 30, 28);
                LC_CELLS[8][2] = new Cell(CELL_TYPE_CHAR, "r", 246, 66, 30, 28);
                
                // row 4
                LC_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "s", 6, 98, 30, 28);
                LC_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "t", 36, 98, 30, 28);
                LC_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "u", 66, 98, 30, 28);
                LC_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "v", 96, 98, 30, 28);
                LC_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "w", 126, 98, 30, 28);
                LC_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "x", 156, 98, 30, 28);
                LC_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "y", 186, 98, 30, 28);
                LC_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "z", 216, 98, 30, 28);
                LC_CELLS[8][3] = new Cell(CELL_TYPE_CHAR, "0", 246, 98, 30, 28);
                
                // row 5
                LC_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "1", 6, 128, 30, 30);
                LC_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "2", 36, 128, 30, 30);
                LC_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "3", 66, 128, 30, 30);
                LC_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "4", 96, 128, 30, 30);
                LC_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "5", 126, 128, 30, 30);
                LC_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "6", 156, 128, 30, 30);
                LC_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "7", 186, 128, 30, 30);
                LC_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, "8", 216, 128, 30, 30);
                LC_CELLS[8][4] = new Cell(CELL_TYPE_CHAR, "9", 246, 128, 30, 30);               
                       
                String lc_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPC_ABC_SYM.png";
                String lc_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPC_ABC_SYM.png";
                String lc_middle_img = "com/tivo/hme/bananas/keyboard-middle-lc.png";
                String lc_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-empty.png";

                kb = new Keyboard(LC_CELLS, 
                        lc_top_img, lc_top_undo_img, 35,
                        lc_middle_img, 120,
                        lc_bottom_img, 10,
                        289);
                standardKeyboards.put(new Integer(type), kb);
                return kb;
                
            case STANDARD_KEYBOARD_UPPERCASE:
                Cell UC_CELLS[][] = new Cell[9][5];
                
                // row 1
                UC_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                UC_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                UC_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                UC_CELLS[3][0] = new Cell(CELL_TYPE_LOWERCASE, null, 177, 6, 49, 28);
                UC_CELLS[4][0] = new Cell(CELL_TYPE_SYM, null, 232, 6, 50, 28);

                // row 2
                UC_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, "A", 6, 36, 30, 28);
                UC_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, "B", 36, 36, 30, 28);
                UC_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "C", 66, 36, 30, 28);
                UC_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "D", 96, 36, 30, 28);
                UC_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "E", 126, 36, 30, 28);
                UC_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "F", 156, 36, 30, 28);
                UC_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "G", 186, 36, 30, 28);
                UC_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, "H", 216, 36, 30, 28);
                UC_CELLS[8][1] = new Cell(CELL_TYPE_CHAR, "I", 246, 36, 30, 28);
                
                // row 3
                UC_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, "J", 6, 66, 30, 28);
                UC_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, "K", 36, 66, 30, 28);
                UC_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "L", 66, 66, 30, 28);
                UC_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "M", 96, 66, 30, 28);
                UC_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "N", 126, 66, 30, 28);
                UC_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "O", 156, 66, 30, 28);
                UC_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "P", 186, 66, 30, 28);
                UC_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "Q", 216, 66, 30, 28);
                UC_CELLS[8][2] = new Cell(CELL_TYPE_CHAR, "R", 246, 66, 30, 28);
                
                // row 4
                UC_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "S", 6, 98, 30, 28);
                UC_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "T", 36, 98, 30, 28);
                UC_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "U", 66, 98, 30, 28);
                UC_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "V", 96, 98, 30, 28);
                UC_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "W", 126, 98, 30, 28);
                UC_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "X", 156, 98, 30, 28);
                UC_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "Y", 186, 98, 30, 28);
                UC_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "Z", 216, 98, 30, 28);
                UC_CELLS[8][3] = new Cell(CELL_TYPE_CHAR, "0", 246, 98, 30, 28);
                
                // row 5
                UC_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "1", 6, 128, 30, 30);
                UC_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "2", 36, 128, 30, 30);
                UC_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "3", 66, 128, 30, 30);
                UC_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "4", 96, 128, 30, 30);
                UC_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "5", 126, 128, 30, 30);
                UC_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "6", 156, 128, 30, 30);
                UC_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "7", 186, 128, 30, 30);
                UC_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, "8", 216, 128, 30, 30);
                UC_CELLS[8][4] = new Cell(CELL_TYPE_CHAR, "9", 246, 128, 30, 30);               
                
                String uc_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPCabcSYM.png";
                String uc_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPCabcSYM.png";
                String uc_middle_img = "com/tivo/hme/bananas/keyboard-middle-uc.png";
                String uc_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-empty.png";

                kb = new Keyboard(UC_CELLS, 
                        uc_top_img, uc_top_undo_img, 35,
                        uc_middle_img, 120,
                        uc_bottom_img, 10,
                        289);
                standardKeyboards.put(new Integer(type), kb);
                return kb;

            case STANDARD_KEYBOARD_SYMBOL:
                Cell SYM_CELLS[][] = new Cell[8][5];
                // row 1               
                SYM_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                SYM_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                SYM_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                SYM_CELLS[3][0] = new Cell(CELL_TYPE_LOWERCASE, null, 177, 6, 49, 28);
                SYM_CELLS[4][0] = new Cell(CELL_TYPE_UPPERCASE, null, 232, 6, 50, 28);

                
                // row 2
                SYM_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, ".", 6, 34, 30, 28);
                SYM_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, ",", 36, 34, 30, 28);
                SYM_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "@", 72, 34, 30, 28);
                SYM_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "~", 108, 34, 30, 28);
                SYM_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "#", 147, 34, 30, 28);
                SYM_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "&", 183, 34, 30, 28);
                SYM_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "(", 218, 34, 30, 28);
                SYM_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, ")", 248, 34, 30, 28);
                
                // row 3
                SYM_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, ":", 6, 66, 30, 28);
                SYM_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, ";", 36, 66, 30, 28);
                SYM_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "*", 72, 66, 30, 28);
                SYM_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "?", 108, 66, 30, 28);
                SYM_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "!", 147, 66, 30, 28);
                SYM_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "_", 183, 66, 30, 28);
                SYM_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "[", 218, 66, 30, 28);
                SYM_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "]", 248, 66, 30, 28);
                
                // row 4
                SYM_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "'",   6, 96, 30, 28);
                SYM_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "\"", 36, 96, 30, 28);
                SYM_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "`",  72, 96, 30, 28);
                SYM_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "-", 108, 96, 30, 28);
                SYM_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "+", 147, 96, 30, 28);
                SYM_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "=", 183, 96, 30, 28);
                SYM_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "{", 218, 96, 30, 28);
                SYM_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "}", 248, 96, 30, 28);
                
                // row 5
                SYM_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "/",   6, 126, 30, 28);
                SYM_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "\\", 36, 126, 30, 28);
                SYM_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "|",  72, 126, 30, 28);
                SYM_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "^", 108, 126, 30, 28);
                SYM_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "%", 147, 126, 30, 28);
                SYM_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "$", 183, 126, 30, 28);
                SYM_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "<", 218, 126, 30, 28);
                SYM_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, ">", 248, 126, 30, 28);
                
                String sym_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPCabcABC.png";
                String sym_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPCabcABC.png";
                String sym_middle_img = "com/tivo/hme/bananas/keyboard-middle-sym.png";
                String sym_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-empty.png";

                kb = new Keyboard(SYM_CELLS, 
                        sym_top_img, sym_top_undo_img, 35,
                        sym_middle_img, 120,
                        sym_bottom_img, 10,
                        289);
                
                standardKeyboards.put(new Integer(type), kb);
                return kb;

            case STANDARD_KEYBOARD_EMAIL_LOWERCASE:
                Cell EMAIL_LC_CELLS[][] = new Cell[9][6];
                // row 1
                EMAIL_LC_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                EMAIL_LC_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                EMAIL_LC_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                EMAIL_LC_CELLS[3][0] = new Cell(CELL_TYPE_UPPERCASE, null, 177, 6, 49, 28);
                EMAIL_LC_CELLS[4][0] = new Cell(CELL_TYPE_SYM, null, 232, 6, 50, 28);

                
                // row 2
                EMAIL_LC_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, "a", 6, 36, 30, 28);
                EMAIL_LC_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, "b", 36, 36, 30, 28);
                EMAIL_LC_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "c", 66, 36, 30, 28);
                EMAIL_LC_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "d", 96, 36, 30, 28);
                EMAIL_LC_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "e", 126, 36, 30, 28);
                EMAIL_LC_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "f", 156, 36, 30, 28);
                EMAIL_LC_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "g", 186, 36, 30, 28);
                EMAIL_LC_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, "h", 216, 36, 30, 28);
                EMAIL_LC_CELLS[8][1] = new Cell(CELL_TYPE_CHAR, "i", 246, 36, 30, 28);
                
                // row 3
                EMAIL_LC_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, "j", 6, 66, 30, 28);
                EMAIL_LC_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, "k", 36, 66, 30, 28);
                EMAIL_LC_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "l", 66, 66, 30, 28);
                EMAIL_LC_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "m", 96, 66, 30, 28);
                EMAIL_LC_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "n", 126, 66, 30, 28);
                EMAIL_LC_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "o", 156, 66, 30, 28);
                EMAIL_LC_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "p", 186, 66, 30, 28);
                EMAIL_LC_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "p", 216, 66, 30, 28);
                EMAIL_LC_CELLS[8][2] = new Cell(CELL_TYPE_CHAR, "r", 246, 66, 30, 28);
                
                // row 4
                EMAIL_LC_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "s", 6, 98, 30, 28);
                EMAIL_LC_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "t", 36, 98, 30, 28);
                EMAIL_LC_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "u", 66, 98, 30, 28);
                EMAIL_LC_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "v", 96, 98, 30, 28);
                EMAIL_LC_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "w", 126, 98, 30, 28);
                EMAIL_LC_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "x", 156, 98, 30, 28);
                EMAIL_LC_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "y", 186, 98, 30, 28);
                EMAIL_LC_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "z", 216, 98, 30, 28);
                EMAIL_LC_CELLS[8][3] = new Cell(CELL_TYPE_CHAR, "0", 246, 98, 30, 28);
                
                // row 5
                EMAIL_LC_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "1", 6, 128, 30, 28);
                EMAIL_LC_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "2", 36, 128, 30, 28);
                EMAIL_LC_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "3", 66, 128, 30, 28);
                EMAIL_LC_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "4", 96, 128, 30, 28);
                EMAIL_LC_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "5", 126, 128, 30, 28);
                EMAIL_LC_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "6", 156, 128, 30, 28);
                EMAIL_LC_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "7", 186, 128, 30, 28);
                EMAIL_LC_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, "8", 216, 128, 30, 28);
                EMAIL_LC_CELLS[8][4] = new Cell(CELL_TYPE_CHAR, "9", 246, 128, 30, 28);               

                // row 6
                EMAIL_LC_CELLS[0][5] = new Cell(CELL_TYPE_CHAR, "@",    6,   166, 30, 28);
                EMAIL_LC_CELLS[1][5] = new Cell(CELL_TYPE_CHAR, ".com", 45,  166, 60, 28);
                EMAIL_LC_CELLS[2][5] = new Cell(CELL_TYPE_CHAR, ".org", 110, 166, 50, 28);
                EMAIL_LC_CELLS[3][5] = new Cell(CELL_TYPE_CHAR, ".net", 165, 166, 50, 28);
                EMAIL_LC_CELLS[4][5] = new Cell(CELL_TYPE_CHAR, "_",    220, 166, 30, 28);
                EMAIL_LC_CELLS[5][5] = new Cell(CELL_TYPE_CHAR, ".",    250, 166, 30, 28);

                String email_lc_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPC_ABC_SYM.png";
                String email_lc_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPC_ABC_SYM.png";
                String email_lc_middle_img = "com/tivo/hme/bananas/keyboard-middle-lc.png";
                String email_lc_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-email.png";

                kb = new Keyboard(EMAIL_LC_CELLS, 
                        email_lc_top_img, email_lc_top_undo_img, 35,
                        email_lc_middle_img, 120,
                        email_lc_bottom_img, 51,
                        289);
                
                standardKeyboards.put(new Integer(type), kb);
                return kb;
                
            case STANDARD_KEYBOARD_EMAIL_UPPERCASE:
                Cell EMAIL_UC_CELLS[][] = new Cell[9][7];
                // row 1
                EMAIL_UC_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                EMAIL_UC_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                EMAIL_UC_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                EMAIL_UC_CELLS[3][0] = new Cell(CELL_TYPE_LOWERCASE, null, 177, 6, 49, 28);
                EMAIL_UC_CELLS[4][0] = new Cell(CELL_TYPE_SYM, null, 232, 6, 50, 28);

                
                // row 2
                EMAIL_UC_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, "A", 6, 36, 30, 28);
                EMAIL_UC_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, "B", 36, 36, 30, 28);
                EMAIL_UC_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "C", 66, 36, 30, 28);
                EMAIL_UC_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "D", 96, 36, 30, 28);
                EMAIL_UC_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "E", 126, 36, 30, 28);
                EMAIL_UC_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "F", 156, 36, 30, 28);
                EMAIL_UC_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "G", 186, 36, 30, 28);
                EMAIL_UC_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, "H", 216, 36, 30, 28);
                EMAIL_UC_CELLS[8][1] = new Cell(CELL_TYPE_CHAR, "I", 246, 36, 30, 28);
                
                // row 3
                EMAIL_UC_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, "J", 6, 66, 30, 28);
                EMAIL_UC_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, "K", 36, 66, 30, 28);
                EMAIL_UC_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "L", 66, 66, 30, 28);
                EMAIL_UC_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "M", 96, 66, 30, 28);
                EMAIL_UC_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "N", 126, 66, 30, 28);
                EMAIL_UC_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "O", 156, 66, 30, 28);
                EMAIL_UC_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "P", 186, 66, 30, 28);
                EMAIL_UC_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "Q", 216, 66, 30, 28);
                EMAIL_UC_CELLS[8][2] = new Cell(CELL_TYPE_CHAR, "R", 246, 66, 30, 28);
                
                // row 4
                EMAIL_UC_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "S", 6, 98, 30, 28);
                EMAIL_UC_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "T", 36, 98, 30, 28);
                EMAIL_UC_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "U", 66, 98, 30, 28);
                EMAIL_UC_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "V", 96, 98, 30, 28);
                EMAIL_UC_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "W", 126, 98, 30, 28);
                EMAIL_UC_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "X", 156, 98, 30, 28);
                EMAIL_UC_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "Y", 186, 98, 30, 28);
                EMAIL_UC_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "X", 216, 98, 30, 28);
                EMAIL_UC_CELLS[8][3] = new Cell(CELL_TYPE_CHAR, "0", 246, 98, 30, 28);
                
                // row 5
                EMAIL_UC_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "1", 6, 128, 30, 30);
                EMAIL_UC_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "2", 36, 128, 30, 30);
                EMAIL_UC_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "3", 66, 128, 30, 30);
                EMAIL_UC_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "4", 96, 128, 30, 30);
                EMAIL_UC_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "5", 126, 128, 30, 30);
                EMAIL_UC_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "6", 156, 128, 30, 30);
                EMAIL_UC_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "7", 186, 128, 30, 30);
                EMAIL_UC_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, "8", 216, 128, 30, 30);
                EMAIL_UC_CELLS[8][4] = new Cell(CELL_TYPE_CHAR, "9", 246, 128, 30, 30);               
 
                // row 6
                EMAIL_UC_CELLS[0][5] = new Cell(CELL_TYPE_CHAR, "@",    6,   166, 30, 28);
                EMAIL_UC_CELLS[1][5] = new Cell(CELL_TYPE_CHAR, ".com", 45,  166, 60, 28);
                EMAIL_UC_CELLS[2][5] = new Cell(CELL_TYPE_CHAR, ".org", 110, 166, 50, 28);
                EMAIL_UC_CELLS[3][5] = new Cell(CELL_TYPE_CHAR, ".net", 165, 166, 50, 28);
                EMAIL_UC_CELLS[4][5] = new Cell(CELL_TYPE_CHAR, "_",    220, 166, 30, 28);
                EMAIL_UC_CELLS[5][5] = new Cell(CELL_TYPE_CHAR, ".",    250, 166, 30, 28);

                
                String email_uc_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPCabcSYM.png";
                String email_uc_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPCabcSYM.png";
                String email_uc_middle_img = "com/tivo/hme/bananas/keyboard-middle-uc.png";
                String email_uc_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-email.png";

                kb = new Keyboard(EMAIL_UC_CELLS, 
                        email_uc_top_img, email_uc_top_undo_img, 35,
                        email_uc_middle_img, 120,
                        email_uc_bottom_img, 51,
                        289);
                
                standardKeyboards.put(new Integer(type), kb);
                return kb;
                
            case STANDARD_KEYBOARD_EMAIL_SYMBOL:
                Cell EMAIL_SYM_CELLS[][] = new Cell[8][6];
                // row 1
                EMAIL_SYM_CELLS[0][0] = new Cell(CELL_TYPE_DEL, null, 2, 6, 48, 28);
                EMAIL_SYM_CELLS[1][0] = new Cell(CELL_TYPE_CLR, null, 51, 6, 65, 28);
                EMAIL_SYM_CELLS[2][0] = new Cell(CELL_TYPE_CHAR, " ", 119, 6, 47, 28);
                EMAIL_SYM_CELLS[3][0] = new Cell(CELL_TYPE_LOWERCASE, null, 177, 6, 49, 28);
                EMAIL_SYM_CELLS[4][0] = new Cell(CELL_TYPE_UPPERCASE, null, 232, 6, 50, 28);

                
                // row 2
                EMAIL_SYM_CELLS[0][1] = new Cell(CELL_TYPE_CHAR, ".", 6, 34, 30, 28);
                EMAIL_SYM_CELLS[1][1] = new Cell(CELL_TYPE_CHAR, ",", 36, 34, 30, 28);
                EMAIL_SYM_CELLS[2][1] = new Cell(CELL_TYPE_CHAR, "@", 72, 34, 30, 28);
                EMAIL_SYM_CELLS[3][1] = new Cell(CELL_TYPE_CHAR, "~", 108, 34, 30, 28);
                EMAIL_SYM_CELLS[4][1] = new Cell(CELL_TYPE_CHAR, "#", 147, 34, 30, 28);
                EMAIL_SYM_CELLS[5][1] = new Cell(CELL_TYPE_CHAR, "&", 183, 34, 30, 28);
                EMAIL_SYM_CELLS[6][1] = new Cell(CELL_TYPE_CHAR, "(", 218, 34, 30, 28);
                EMAIL_SYM_CELLS[7][1] = new Cell(CELL_TYPE_CHAR, ")", 248, 34, 30, 28);
                
                // row 3
                EMAIL_SYM_CELLS[0][2] = new Cell(CELL_TYPE_CHAR, ":", 6, 66, 30, 28);
                EMAIL_SYM_CELLS[1][2] = new Cell(CELL_TYPE_CHAR, ";", 36, 66, 30, 28);
                EMAIL_SYM_CELLS[2][2] = new Cell(CELL_TYPE_CHAR, "*", 72, 66, 30, 28);
                EMAIL_SYM_CELLS[3][2] = new Cell(CELL_TYPE_CHAR, "?", 108, 66, 30, 28);
                EMAIL_SYM_CELLS[4][2] = new Cell(CELL_TYPE_CHAR, "!", 147, 66, 30, 28);
                EMAIL_SYM_CELLS[5][2] = new Cell(CELL_TYPE_CHAR, "_", 183, 66, 30, 28);
                EMAIL_SYM_CELLS[6][2] = new Cell(CELL_TYPE_CHAR, "[", 218, 66, 30, 28);
                EMAIL_SYM_CELLS[7][2] = new Cell(CELL_TYPE_CHAR, "]", 248, 66, 30, 28);
                
                // row 4
                EMAIL_SYM_CELLS[0][3] = new Cell(CELL_TYPE_CHAR, "'",   6, 96, 30, 28);
                EMAIL_SYM_CELLS[1][3] = new Cell(CELL_TYPE_CHAR, "\"", 36, 96, 30, 28);
                EMAIL_SYM_CELLS[2][3] = new Cell(CELL_TYPE_CHAR, "`",  72, 96, 30, 28);
                EMAIL_SYM_CELLS[3][3] = new Cell(CELL_TYPE_CHAR, "-", 108, 96, 30, 28);
                EMAIL_SYM_CELLS[4][3] = new Cell(CELL_TYPE_CHAR, "+", 147, 96, 30, 28);
                EMAIL_SYM_CELLS[5][3] = new Cell(CELL_TYPE_CHAR, "=", 183, 96, 30, 28);
                EMAIL_SYM_CELLS[6][3] = new Cell(CELL_TYPE_CHAR, "{", 218, 96, 30, 28);
                EMAIL_SYM_CELLS[7][3] = new Cell(CELL_TYPE_CHAR, "}", 248, 96, 30, 28);
                
                // row 5
                EMAIL_SYM_CELLS[0][4] = new Cell(CELL_TYPE_CHAR, "/",   6, 126, 30, 28);
                EMAIL_SYM_CELLS[1][4] = new Cell(CELL_TYPE_CHAR, "\\", 36, 126, 30, 28);
                EMAIL_SYM_CELLS[2][4] = new Cell(CELL_TYPE_CHAR, "|",  72, 126, 30, 28);
                EMAIL_SYM_CELLS[3][4] = new Cell(CELL_TYPE_CHAR, "^", 108, 126, 30, 28);
                EMAIL_SYM_CELLS[4][4] = new Cell(CELL_TYPE_CHAR, "%", 147, 126, 30, 28);
                EMAIL_SYM_CELLS[5][4] = new Cell(CELL_TYPE_CHAR, "$", 183, 126, 30, 28);
                EMAIL_SYM_CELLS[6][4] = new Cell(CELL_TYPE_CHAR, "<", 218, 126, 30, 28);
                EMAIL_SYM_CELLS[7][4] = new Cell(CELL_TYPE_CHAR, ">", 248, 126, 30, 28);

                // row 6
                EMAIL_SYM_CELLS[0][5] = new Cell(CELL_TYPE_CHAR, "@",    6,   166, 30, 28);
                EMAIL_SYM_CELLS[1][5] = new Cell(CELL_TYPE_CHAR, ".com", 45,  166, 60, 28);
                EMAIL_SYM_CELLS[2][5] = new Cell(CELL_TYPE_CHAR, ".org", 110, 166, 50, 28);
                EMAIL_SYM_CELLS[3][5] = new Cell(CELL_TYPE_CHAR, ".net", 165, 166, 50, 28);
                EMAIL_SYM_CELLS[4][5] = new Cell(CELL_TYPE_CHAR, "_",    220, 166, 30, 28);
                EMAIL_SYM_CELLS[5][5] = new Cell(CELL_TYPE_CHAR, ".",    250, 166, 30, 28);

                String email_sym_top_img = "com/tivo/hme/bananas/keyboard-top-DELCLRSPCabcABC.png";
                String email_sym_top_undo_img = "com/tivo/hme/bananas/keyboard-top-DELUNDOSPCabcABC.png";
                String email_sym_middle_img = "com/tivo/hme/bananas/keyboard-middle-sym.png";
                String email_sym_bottom_img = "com/tivo/hme/bananas/keyboard-bottom-email.png";

                kb = new Keyboard(EMAIL_SYM_CELLS, 
                        email_sym_top_img, email_sym_top_undo_img, 35,
                        email_sym_middle_img, 120,
                        email_sym_bottom_img, 51,
                        289);
                
                standardKeyboards.put(new Integer(type), kb);
                return kb;
        }
        return null;
    }
    
    /**
     * Get the value of the widget, a string representing
     * what has been entered
     * 
     * @return the string that has been typed in
     */
    public String getValue() {
        return word;
    }
    
    /**
     * Set the value of the widget, all clients will be
     * notified imediately of the change
     * 
     * @param newWord New valuee of the widget
     */
    public void setValue(String newWord) {
        if (newWord == null) {
            newWord = "";
        }
        if (undoValue != null) {
            setUndoState(false);
        }
        this.word = newWord;
        notifyListeners();
    }
    
    
    /**
     * Set a keyboard to a keyboard state
     * 
     * a BKeyboard consists of 4 states:
     * 
     *  LOWERCASE;
     *  UPPERCASE;
     *  SYMBOL;
     *  NUMCAPSLOCK;
     * 
     * each state needs a keyboard associated with it
     * 
     * If you are changing a keybord of the current state, the
     * new keyboard will be displayed immediately
     * 
     * @param state The state of the keyboard
     * @param keyboard a keyboard
     */
    public void linkKeyboardToState(int state, Keyboard keyboard) {
        if (state < LOWERCASE || state > SYMBOL) {
            //TODO: should throw illegal arg exception
            return;
        }
        
        keyboardStates[state] = keyboard;
        
        // if we're replacing the curerent state then replace the keyboard
        if (keyboardState == state) {
            setKeyboard(state);
        }
    }
    
    /**
     * Set the state of the keyboard, this will cause a new key 
     * grid to be loaded.
     * 
     * @param state The new state of the keyboard
     */
    public void setKeyboard(int state) {  
        undoValue = null;
        
        if (state < LOWERCASE || state > SYMBOL) {
            keyboardState = LOWERCASE;
        }
        keyboardState = state;
        replaceKeyboard(keyboardStates[state]);
    }
    
    /**
     * Set a new key grid, remove the current views associated
     * with the old grid and replace them with new grid.
     * 
     * This will also attempt to keep the highlight in the same
     * place, it first looks for a key with the same value of the
     * old highlight and failing that tries to find a key in the 
     * location.
     * 
     * @param keyboard The grid which will be displayed
     */
    protected void replaceKeyboard(Keyboard keyboard) {
        
        // get the string of the currently focused item
        // after replacing the grid we will look for this and
        // reset the focus
        
        Rectangle oldFocusBounds = null;
        if (focused != null) {
            oldFocusBounds = focused.getBounds();
        }
        focused = null;
        
        setPainting(false);
        
        // set up views into which we will place our images
        kbBgImage.setSize(keyboard.imageWidth, keyboard.imageHeight);
        int t = 0;
        kbBgImage_top.setBounds(0, t, keyboard.imageWidth, keyboard.topImageHeight);
        t += keyboard.topImageHeight;
        kbBgImage_middle.setBounds(0, t, keyboard.imageWidth, keyboard.middleImageHeight);
        t += keyboard.middleImageHeight;
        kbBgImage_bottom.setBounds(0, t, keyboard.imageWidth, keyboard.bottomImageHeight);

        kbBgImage_top.setResource(keyboard.topImage);
        kbBgImage_middle.setResource(keyboard.middleImage);
        kbBgImage_bottom.setResource(keyboard.bottomImage);
        
        
        // remove current grid
        
        boolean tryAgain = true;
        while (tryAgain) {
            int numFound = 0;
            for (int i = 0 ; i < kbBgImage.getChildCount(); i++) {
                if (kbBgImage.getChild(i) instanceof LetterView) {
                    kbBgImage.getChild(i).setVisible(false);
                    kbBgImage.getChild(i).remove();
                    
                    ++numFound;
                    break;
                }
            }
            if (numFound == 0) {
                tryAgain = false;
            }
        }        
        
        // create new grid

        Cell newgrid[][] = keyboard.cells;
        // find the longest line
        int longest = 0;
        for (int i = 0 ; i < newgrid.length ; i++) {
            if (newgrid[i].length > longest) {
                longest = newgrid[i].length;
            }
        }
        // make a a grid of the views
        LetterView gridViews[][] = new LetterView[newgrid.length][longest];
        
        int view_y = 0;
        for (int i = 0 ; i < newgrid.length ; i++) {
            int view_x = 10;
            for (int j = 0 ; j < newgrid[i].length ; j++) {
                if (newgrid[i][j] != null && newgrid[i][j].type != CELL_TYPE_EMPTY) { 
                    int xpos = newgrid[i][j].x;
                    int ypos = newgrid[i][j].y;
                    int w = newgrid[i][j].width;
                    int h = newgrid[i][j].height;
                    
                    gridViews[i][j] = new LetterView(kbBgImage, newgrid[i][j]);
                    view_x += w;
                }                
            }
        }
        
        //
        // if we didn't set the focus by string, set it by location
        // 
        if (oldFocusBounds != null) {
            int centerX = oldFocusBounds.x + (oldFocusBounds.width /2);
            int centerY = oldFocusBounds.y + (oldFocusBounds.height /2);
            for (int i = 0 ; i < kbBgImage.getChildCount(); i++) {
                if (kbBgImage.getChild(i) instanceof LetterView) {
                    if (kbBgImage.getChild(i).getBounds().contains(centerX, centerY)) {
                        focused = (LetterView)kbBgImage.getChild(i);
                        break;
                    }
                }
            }
            
        }
        
            
        // if all else fails just set the focus to the first item
        if (focused == null) {
            focused = gridViews[0][0];
        }
                
        setPainting(true);
        flush();
        
        refreshHighlights(animation);
    }
    
    /**
     * 
     * @param undo
     */
    protected void setUndoState(boolean undo) {
        Keyboard kb = keyboardStates[keyboardState];
        if (undo == true) {
            String newImage = kb.topUndoImage;
            if (newImage == null) {
                return;
                
            }
            // scan first row for CLR, replace with UNDO
            Cell cells[][] = kb.cells;
            for (int j = 0 ; j < cells.length ; j++) {
                if (cells[j][0] != null && cells[j][0].type == CELL_TYPE_CLR) {
                    kbBgImage_top.setResource(newImage);
                    cells[j][0].type = CELL_TYPE_UNDO;
                    break;
                }
            }
        } else {
            String newImage = kb.topImage;
            // scan first row for UNDO, replace with CLR
            Cell cells[][] = kb.cells;
            for (int j = 0 ; j < cells.length ; j++) {
                if (cells[j][0] != null && cells[j][0].type == CELL_TYPE_UNDO) {
                    kbBgImage_top.setResource(newImage);
                    cells[j][0].type = CELL_TYPE_CLR;
                    break;
                }
            }    
        }
    }
    
    /**
     * move the hilite focus to the currently focused key
     */
    protected void refreshHighlights(String anim) {        
        if (anim != null) {
            keyboardHighlights.refresh(getResource(anim));
        } else {
            keyboardHighlights.refresh();
        }
    }

    /**
     * Helper method to return the size size of the keyboard 
     * based on the parameters that can be passed in to the 
     * constructor.
     * <p>
     * 
     * @param keyboardType the type of keyboard, currently either:<br>
     * &nbsp;&nbsp;&nbsp;PLAIN_KEYBOARD<br>
     * &nbsp;&nbsp;&nbsp;EMAIL_KEYBOARD<br>
     * @param tips True if the tips area ill be displayed
     * @return The minimum size of the widget
     */
    static public Point getKeyboardSize(int keyboardType, boolean tips) {
        return getKeyboardSize(getStandardKeyboard(keyboardType), tips, INPUT_WIDTH_SAME_AS_WIDGET);
    }
    
    
    /**
     * Helper method to return the size size of the keyboard 
     * based on the parameters that can be passed in to the 
     * constructor.
     * 
     * @param keyboard keyboard you are using, if null a standard lowercase 
     * keyboard will be used to measure
     * @param textEntryWidth Width of the text entry area
     * @param tips True if the tips area ill be displayed
     * @return The minimum size of the widget
     */
    static public Point getKeyboardSize(Keyboard keyboard, boolean tips, int textEntryWidth) {
        if (keyboard == null) {
            keyboard = getStandardKeyboard(STANDARD_KEYBOARD_LOWERCASE);
        }
        
        int kbwidth = keyboard.imageWidth;
        if (tips == true) {
            kbwidth += 150;
        }
        if (textEntryWidth != INPUT_WIDTH_SAME_AS_WIDGET && textEntryWidth > kbwidth) {
            kbwidth = textEntryWidth;
        }
        // height id keyboard image + text entry height + space between them
        int kbheight = keyboard.imageHeight + 40 + 5;
        
        return new Point(kbwidth, kbheight);
    }
    
    /**
     * Upon gaining the focus we want to make sure the highlight is visible, 
     * then hide it when we lose focus.
     */
    public boolean handleFocus(boolean gotFocus, BView gained, BView lost) {
        if (gotFocus && gained == this) {
            getScreen().setFocus(focused);
            highlightVis = true;
            refreshHighlights(animation);
        } else if (gotFocus == false && lost == this) {
            highlightVis = false;
            refreshHighlights(animation);
        } else {
            highlightVis = gained instanceof LetterView;
            refreshHighlights(animation);
        }
        
        return super.handleFocus(gotFocus,  gained,  lost);
    }
 
    /**
     * repeat keypresses  are treated as regular jey presses
     */
    public boolean handleKeyRepeat(int code, long rawcode) {     
        return handleKeyPress(code, rawcode);
    }

    /**
     * Use the keys to navigate and also allow the user
     * to press the number geys to enter numbers
     */
    public boolean handleKeyPress(int code, long rawcode) {        
        switch (code) {
            case KEY_RIGHT:
                BView newFocus = focusMgr.followArrow(focused, KEY_RIGHT);
                if (newFocus instanceof LetterView) {
                    focused = (LetterView)newFocus;
                    getScreen().setFocus(focused);
                    BHighlight h = keyboardHighlights.get(H_KEYBOARD);
                    h.setStretchWidth(focused.getWidth() + 15);
                    getBApp().play("updown.snd");
                    refreshHighlights(animation);
                } else {
                    getParent().postEvent(new BEvent.Action(this, "right"));
                }
                return true;
            case KEY_LEFT:
                newFocus = focusMgr.followArrow(focused, KEY_LEFT);
                if (newFocus instanceof LetterView) {
                    focused = (LetterView)newFocus;
                    getScreen().setFocus(focused);
                    BHighlight h = keyboardHighlights.get(H_KEYBOARD);
                    h.setStretchWidth(focused.getWidth() + 15);
                    
                    getBApp().play("updown.snd");
                    
                    refreshHighlights(animation);
                } else {
                    getParent().postEvent(new BEvent.Action(this, "left"));
                }
                return true;
            case KEY_UP:
                newFocus = focusMgr.followArrow(focused, KEY_UP);
                if (newFocus instanceof LetterView) {
                    focused = (LetterView)newFocus;
                    getScreen().setFocus(focused);
                    BHighlight h = keyboardHighlights.get(H_KEYBOARD);
                    
                    getBApp().play("updown.snd");
                    
                    h.setStretchWidth(focused.getWidth() + 15);
                    refreshHighlights(animation);
                } else {
                    getParent().postEvent(new BEvent.Action(this, "up"));
                }
                return true;
            case KEY_DOWN:
                newFocus = focusMgr.followArrow(focused, KEY_DOWN);
                if (newFocus instanceof LetterView) {
                    focused = (LetterView)newFocus;
                    getScreen().setFocus(focused);
                    
                    BHighlight h = keyboardHighlights.get(H_KEYBOARD);
                    h.setStretchWidth(focused.getWidth() + 15);
                    
                    getBApp().play("updown.snd");
                    
                    refreshHighlights(animation);
                } else {
                    getParent().postEvent(new BEvent.Action(this, "down"));
                }
                return true;
            case KEY_SELECT:
                
                if (focused != null) {
                    handleSelection(focused.getType(), focused.getValue());
                }
                return true;
            case KEY_THUMBSUP:
                handleSelection(CELL_TYPE_UPPERCASE, null);
                return true;
            case KEY_THUMBSDOWN:
                handleSelection(CELL_TYPE_LOWERCASE, null);
                return true;
            case KEY_REVERSE:
                handleSelection(CELL_TYPE_DEL, null);
                return true;
            case KEY_FORWARD:
                handleSelection(CELL_TYPE_CHAR, " ");
                return true;
            case KEY_NUM0:
                handleSelection(CELL_TYPE_CHAR, "0");
                return true;
            case KEY_NUM1:
                handleSelection(CELL_TYPE_CHAR, "1");
                return true;
            case KEY_NUM2:
                handleSelection(CELL_TYPE_CHAR, "2");
                return true;
            case KEY_NUM3:
                handleSelection(CELL_TYPE_CHAR, "3");
                return true;
            case KEY_NUM4:
                handleSelection(CELL_TYPE_CHAR, "4");
                return true;
            case KEY_NUM5:
                handleSelection(CELL_TYPE_CHAR, "5");
                return true;
            case KEY_NUM6:
                handleSelection(CELL_TYPE_CHAR, "6");
                return true;
            case KEY_NUM7:
                handleSelection(CELL_TYPE_CHAR, "7");
                return true;
            case KEY_NUM8:
                handleSelection(CELL_TYPE_CHAR, "8");
                return true;
            case KEY_NUM9:
                handleSelection(CELL_TYPE_CHAR, "9");
                return true;
            case KEY_CLEAR:
                handleSelection(CELL_TYPE_CLR, null);
                return true;     
        }
        return super.handleKeyPress(code, rawcode);
    }    

    /**
     * Handle an item selected from the keyboard, the character
     * selected is passed in
     *       
     */
    protected void handleSelection(int type, String value) {
        
        //
        // change keyboard and return
        //
        
        switch (type) {
            case CELL_TYPE_SYM:
                switch (keyboardState) {
                    case LOWERCASE:
                        setKeyboard(SYMBOL);
                        break;
                    case UPPERCASE:
                        setKeyboard(SYMBOL);
                        break;
                    case SYMBOL:
                        break;
                }
                getBApp().play("select.snd");
                return;
            case CELL_TYPE_LOWERCASE:
                    switch (keyboardState) {
                        case LOWERCASE:
                            break;
                        case UPPERCASE:
                            setKeyboard(LOWERCASE);
                            break;
                        case SYMBOL:
                            setKeyboard(LOWERCASE);
                            break;
                    }
                    getBApp().play("select.snd");
                    return;
                case CELL_TYPE_UPPERCASE:
                    switch (keyboardState) {
                        case LOWERCASE:
                            setKeyboard(UPPERCASE);
                            break;
                        case UPPERCASE:
                            break;
                        case SYMBOL:
                            setKeyboard(UPPERCASE);
                            break;
                    }
                    getBApp().play("select.snd");
                    return;
                case CELL_TYPE_DEL:
                    if (word.length() > 0) {
                        word = word.substring(0, word.length()-1);
                        notifyListeners();
                        getBApp().play("select.snd");
                   } else {
                       getBApp().play("bonk.snd");
                   }
                    return;
                case CELL_TYPE_UNDO:
                    setValue(undoValue);
                    undoValue = null;
                    setUndoState(false);
                    getBApp().play("select.snd");
                    return;
                case CELL_TYPE_CLR:
                    if (word.length() > 0) {
                        setUndoState(true);
                        undoValue = getValue();
                        word = "";
                        notifyListeners();
                        getBApp().play("select.snd");
                    } else {
                        getBApp().play("bonk.snd");
                    }
                    return;                
                case CELL_TYPE_CHAR:
                    setValue(word += value);
                    getBApp().play("select.snd");
                    break;
        }
    }
    
    /**
     * Tell all the clients that the text associated with the keyboard has changed
     *
     */
    protected void notifyListeners() {
        if (getValue() != null) {
            KeyboardEvent kbe = new KeyboardEvent(EVT_KBD_VALUE, this.getID(), getValue());;
            //BEvent.Action action = new BEvent.Action(this, getValue()); 
            postEvent(kbe);
        }
        
    }
    
    /**
     * handle a change in the value of the BKeyboard by
     * updating the related text field
     */
    public boolean handleEvent(HmeEvent event) {
        if (event instanceof KeyboardEvent) {
            textField.update((String)((KeyboardEvent)event).getValue());
        }
        return super.handleEvent(event);
    }
    
    /**
     * A BText that adds a "_" to the end of its display
     * 
     *
     * @author Carl Haynes
     */
    class DisplayText extends BText {
        
        /**
         * Constructor
         */
        public DisplayText(BView parent, int x, int y, int width, int height)
        {
            super(parent, x, y, width, height);
            
            update("");
        }
        
        /* (non-Javadoc)
         * @see com.tivo.hme.bananas.IKeyboardClient#update(java.lang.String)
         */
        public void update(String word)
        {
            setValue(word + "_");
        }
    }
    
    /**
     * A view which displays a letter on a keyboard
     * The view does not have a visual element, but
     * is used to provide an area in which to focus
     * 
     *
     * @author Carl Haynes
     */
    class LetterView extends BView {
        Cell cell;
        
        /**
         * Constructor
         */
        public LetterView(BView parent, Cell cell)
        {
            super(parent, cell.x, cell.y, cell.width, cell.height);
            this.cell = cell;
            setFocusable(true);
        }
 
        public int getType() {
            return cell.type;
        }
       
        //guarenteed to not be null
        public String getValue() {
            switch (cell.type) {
                case CELL_TYPE_UPPERCASE:
                    return "CAPS";
                case CELL_TYPE_SYM:
                    return "NUM";
                case CELL_TYPE_CLR:
                    return "CLR";
                case CELL_TYPE_EMPTY:
                    return "EMPTY";
                case CELL_TYPE_UNDO:
                    return "UNDO";
                case CELL_TYPE_CHAR:
                    if (cell.value == null) {
                        return "";
                    }
                    return cell.value;
            }
            return "";
        }
    
        public String toString() {
            return "" + cell.value;
        }
    }
    
    
    /**
     * A keyboard layout is defined by an image and an array of cells
     * representing each key
     * 
     *
     * @author Carl Haynes
     */
    public static class Keyboard {
        Cell cells[][];
        int imageWidth;
        int imageHeight;
        
        String topImage;
        String topUndoImage;
        int topImageHeight = 0;
        String middleImage;
        int middleImageHeight = 0;
        String bottomImage;
        int bottomImageHeight = 0;
        
        public Keyboard(Cell cells[][], 
                String topImage, 
                String topUndoImage,
                int topImageHeight,
                String middleImage,
                int middleImageHeight,
                String bottomImage, 
                int bottomImageHeight,
                int imageWidth) {
            this.cells = cells;

            this.topImage = topImage;
            this.topUndoImage = topUndoImage;
            this.topImageHeight = topImageHeight;
            this.middleImage = middleImage;
            this.middleImageHeight = middleImageHeight;
            this.bottomImage = bottomImage;
            this.bottomImageHeight = bottomImageHeight;
            
            this.imageWidth = imageWidth;
            this.imageHeight = topImageHeight + middleImageHeight + bottomImageHeight;
        }
        
    }
    
    /**
     * Each cell represents a key on a keyboard
     * 
     *
     * @author Carl Haynes
     */
    public static class Cell {
        int type = CELL_TYPE_EMPTY;
        String value = null;
        int x;
        int y;
        int width;
        int height;

        public Cell(int type, String value, int x, int y, int width, int height) {
            this.type = type;
            this.value = value;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;        
        }
    }
    
    /**
     * Handle the highlights layout for BKeyboard
     * 
     *
     * @author Carl Haynes
     */
    class HighlightsLayout implements IHighlightsLayout
    {
        /**
         * 
         */
        public BScreen getScreen()
        {
            return BKeyboard.this.getScreen();
        }
        
        /**
         * 
         */
        public BRect getHighlightBounds()
        {
            return getFocusBounds();
        }
        
        public BRect getFocusBounds()
        {
            if (focused != null)
            {
                BRect rect = focused.getHighlightBounds();
                return rect;
            }
            
            return toScreenBounds(new BRect(0, 0, getWidth(), rowHeight));
        }
        
        /**
         * 
         */
        public boolean getHighlightIsVisible(int visible)
        {
            return highlightVis;
        }
    }
    
    public static class KeyboardEvent extends HmeEvent {
        String value;
        
        /**
         * @param opcode
         * @param id
         */
        protected KeyboardEvent(int opcode, int id, String value)
        {
            super(opcode, id);  
            this.value = value;
        }
        
        public String getValue()
        {
            return value;
        }
        
        public void setValue(String value)
        {
            this.value = value;
        }
    }
}
