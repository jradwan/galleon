package com.almilli.tivo.bananas.hd;

import com.tivo.hme.bananas.BKeyboard;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;


public class HDKeyboard extends BKeyboard {
    public static final int DEFAULT_INPUT_WIDTH = 880;
    
    private static Map<Integer, Keyboard> standardKeyboards = new HashMap<Integer, Keyboard>();


    public HDKeyboard(BView parent, int x, int y, int width, int height) {
        this(parent, x, y, width, height, getStandardKeyboard(STANDARD_KEYBOARD_LOWERCASE), 
                true, DEFAULT_INPUT_WIDTH, true);
    }
    
    public HDKeyboard(BView parent, int x, int y, int width, int height, int keyboardType, boolean tips) {
        this(parent, x, y, width, height, getStandardKeyboard(keyboardType), 
                tips, DEFAULT_INPUT_WIDTH, true);
        
        if (keyboardType == EMAIL_KEYBOARD) {
            linkKeyboardToState(UPPERCASE, getStandardKeyboard(STANDARD_KEYBOARD_EMAIL_UPPERCASE));
            linkKeyboardToState(SYMBOL, getStandardKeyboard(STANDARD_KEYBOARD_EMAIL_SYMBOL));
        }
    }


    public HDKeyboard(BView parent, int x, int y, int width, int height, Keyboard keyboard, boolean tips, int textEntryWidth, boolean visible) {
        super(parent, x, y, width, height, keyboard, false, textEntryWidth, visible);

        //replace the keyboards
        linkKeyboardToState(LOWERCASE, getStandardKeyboard(STANDARD_KEYBOARD_LOWERCASE));
        linkKeyboardToState(UPPERCASE, getStandardKeyboard(STANDARD_KEYBOARD_UPPERCASE));
        linkKeyboardToState(SYMBOL, getStandardKeyboard(STANDARD_KEYBOARD_SYMBOL));

        //NOTE: this is a big hack because the API is not extendable
        String prefix = HDKeyboard.class.getPackage().getName().replace('.', '/') + "/images/";
        
        //move the keyboard down
        BView kbBgImage = (BView)getChild(0);
        kbBgImage.setLocation(0, 80);
        
        //modify the resource of the text input fields
        BView dataField = (BView)getChild(getChildCount()-1);
        dataField.setBounds(0, 0, textEntryWidth, 70);
        
        BView dataFieldLeft = (BView)dataField.getChild(0);
        dataFieldLeft.setBounds(0, 0, textEntryWidth - 40, 70);
        dataFieldLeft.clearResource();
        dataFieldLeft.setResource(prefix + "keyboard-datafield.png", RSRC_HALIGN_LEFT);
        
        BView dataFieldRight = (BView)dataField.getChild(1);
        dataFieldRight.setBounds(textEntryWidth - 40, 0, 40, 70);
        dataFieldRight.clearResource();
        dataFieldRight.setResource(prefix + "keyboard-datafield.png", RSRC_HALIGN_RIGHT);

        BText textField = (BText)dataField.getChild(2);
        textField.setBounds(20, 2, dataField.getWidth() - 40, 70);
        textField.setFont("default-36.font");

        if (tips) {
            BView tipsView = new BView(this, 600, 120, 276, 278);
            tipsView.setResource(prefix + "keyboard-tips.png");
        }
    }

    public static Point getKeyboardSize(int keyboardType, boolean tips)
    {
        return getKeyboardSize(getStandardKeyboard(keyboardType), tips, -1);
    }

    public static Point getKeyboardSize(Keyboard keyboard, boolean tips, int textEntryWidth)
    {
        Point p = BKeyboard.getKeyboardSize(keyboard, false, textEntryWidth);
        if (tips) {
            p.x += 300;
        }
        p.y += 45;
        return p;
    }

    public static Keyboard getStandardKeyboard(int type) {
        Keyboard kb = standardKeyboards.get(type);
        if (kb != null) {
            return kb;
        }
        String prefix = HDKeyboard.class.getPackage().getName().replace('.', '/') + "/images/";
        
        Cell[][] cells;
        switch (type) {
        case BKeyboard.STANDARD_KEYBOARD_LOWERCASE:
            cells = new Cell[9][5];

            // row 1
            cells[0][0] = new Cell(CELL_TYPE_DEL, null, 0, 2, 105, 54);
            cells[1][0] = new Cell(CELL_TYPE_CLR, null, 105, 2, 124, 54);
            cells[2][0] = new Cell(CELL_TYPE_CHAR, " ", 231, 2, 108, 54);
            cells[3][0] = new Cell(CELL_TYPE_UPPERCASE, null, 349, 2, 108, 54);
            cells[4][0] = new Cell(CELL_TYPE_SYM, null, 460, 2, 108, 54);
            
            // row 2
            cells[0][1] = new Cell(CELL_TYPE_CHAR, "a", 12, 65, 60, 54);
            cells[1][1] = new Cell(CELL_TYPE_CHAR, "b", 72, 65, 60, 54);
            cells[2][1] = new Cell(CELL_TYPE_CHAR, "c", 132, 65, 60, 54);
            cells[3][1] = new Cell(CELL_TYPE_CHAR, "d", 192, 65, 60, 54);
            cells[4][1] = new Cell(CELL_TYPE_CHAR, "e", 252, 65, 60, 54);
            cells[5][1] = new Cell(CELL_TYPE_CHAR, "f", 312, 65, 60, 54);
            cells[6][1] = new Cell(CELL_TYPE_CHAR, "g", 372, 65, 60, 54);
            cells[7][1] = new Cell(CELL_TYPE_CHAR, "h", 432, 65, 60, 54);
            cells[8][1] = new Cell(CELL_TYPE_CHAR, "i", 492, 65, 60, 54);
            
            // row 3
            cells[0][2] = new Cell(CELL_TYPE_CHAR, "j", 12, 125, 60, 54);
            cells[1][2] = new Cell(CELL_TYPE_CHAR, "k", 72, 125, 60, 54);
            cells[2][2] = new Cell(CELL_TYPE_CHAR, "l", 132, 125, 60, 54);
            cells[3][2] = new Cell(CELL_TYPE_CHAR, "m", 192, 125, 60, 54);
            cells[4][2] = new Cell(CELL_TYPE_CHAR, "n", 252, 125, 60, 54);
            cells[5][2] = new Cell(CELL_TYPE_CHAR, "o", 312, 125, 60, 54);
            cells[6][2] = new Cell(CELL_TYPE_CHAR, "p", 372, 125, 60, 54);
            cells[7][2] = new Cell(CELL_TYPE_CHAR, "q", 432, 125, 60, 54);
            cells[8][2] = new Cell(CELL_TYPE_CHAR, "r", 492, 125, 60, 54);
            
            // row 4
            cells[0][3] = new Cell(CELL_TYPE_CHAR, "s", 12, 189, 60, 54);
            cells[1][3] = new Cell(CELL_TYPE_CHAR, "t", 72, 189, 60, 54);
            cells[2][3] = new Cell(CELL_TYPE_CHAR, "u", 132, 189, 60, 54);
            cells[3][3] = new Cell(CELL_TYPE_CHAR, "v", 192, 189, 60, 54);
            cells[4][3] = new Cell(CELL_TYPE_CHAR, "w", 252, 189, 60, 54);
            cells[5][3] = new Cell(CELL_TYPE_CHAR, "x", 312, 189, 60, 54);
            cells[6][3] = new Cell(CELL_TYPE_CHAR, "y", 372, 189, 60, 54);
            cells[7][3] = new Cell(CELL_TYPE_CHAR, "z", 432, 189, 60, 54);
            cells[8][3] = new Cell(CELL_TYPE_CHAR, "0", 492, 189, 60, 54);
            
            // row 5
            cells[0][4] = new Cell(CELL_TYPE_CHAR, "1", 12, 249, 60, 60);
            cells[1][4] = new Cell(CELL_TYPE_CHAR, "2", 72, 249, 60, 60);
            cells[2][4] = new Cell(CELL_TYPE_CHAR, "3", 132, 249, 60, 60);
            cells[3][4] = new Cell(CELL_TYPE_CHAR, "4", 192, 249, 60, 60);
            cells[4][4] = new Cell(CELL_TYPE_CHAR, "5", 252, 249, 60, 60);
            cells[5][4] = new Cell(CELL_TYPE_CHAR, "6", 312, 249, 60, 60);
            cells[6][4] = new Cell(CELL_TYPE_CHAR, "7", 372, 249, 60, 60);
            cells[7][4] = new Cell(CELL_TYPE_CHAR, "8", 432, 249, 60, 60);
            cells[8][4] = new Cell(CELL_TYPE_CHAR, "9", 492, 249, 60, 60);
            
            String lc_top_img = prefix + "keyboard-top-DELCLRSPC_ABC_SYM.png";
            String lc_top_undo_img = prefix + "keyboard-top-DELUNDOSPC_ABC_SYM.png";
            String lc_middle_img = prefix + "keyboard-middle-lc.png";
            String lc_bottom_img = prefix + "keyboard-bottom-empty.png";
            
            kb = new Keyboard(cells, 
                    lc_top_img, lc_top_undo_img, 70,
                    lc_middle_img, 240,
                    lc_bottom_img, 22,
                    578);
            
            standardKeyboards.put(new Integer(type), kb);
            return kb;
            
        case STANDARD_KEYBOARD_UPPERCASE:
            cells = new Cell[9][5];

            // row 1
            cells[0][0] = new Cell(CELL_TYPE_DEL, null, 0, 2, 105, 54);
            cells[1][0] = new Cell(CELL_TYPE_CLR, null, 105, 2, 124, 54);
            cells[2][0] = new Cell(CELL_TYPE_CHAR, " ", 231, 2, 108, 54);
            cells[3][0] = new Cell(CELL_TYPE_UPPERCASE, null, 349, 2, 108, 54);
            cells[4][0] = new Cell(CELL_TYPE_SYM, null, 460, 2, 108, 54);
            
            // row 2
            cells[0][1] = new Cell(CELL_TYPE_CHAR, "A", 12, 65, 60, 54);
            cells[1][1] = new Cell(CELL_TYPE_CHAR, "B", 72, 65, 60, 54);
            cells[2][1] = new Cell(CELL_TYPE_CHAR, "C", 132, 65, 60, 54);
            cells[3][1] = new Cell(CELL_TYPE_CHAR, "D", 192, 65, 60, 54);
            cells[4][1] = new Cell(CELL_TYPE_CHAR, "E", 252, 65, 60, 54);
            cells[5][1] = new Cell(CELL_TYPE_CHAR, "F", 312, 65, 60, 54);
            cells[6][1] = new Cell(CELL_TYPE_CHAR, "G", 372, 65, 60, 54);
            cells[7][1] = new Cell(CELL_TYPE_CHAR, "H", 432, 65, 60, 54);
            cells[8][1] = new Cell(CELL_TYPE_CHAR, "I", 492, 65, 60, 54);
            
            // row 3
            cells[0][2] = new Cell(CELL_TYPE_CHAR, "J", 12, 125, 60, 54);
            cells[1][2] = new Cell(CELL_TYPE_CHAR, "K", 72, 125, 60, 54);
            cells[2][2] = new Cell(CELL_TYPE_CHAR, "L", 132, 125, 60, 54);
            cells[3][2] = new Cell(CELL_TYPE_CHAR, "M", 192, 125, 60, 54);
            cells[4][2] = new Cell(CELL_TYPE_CHAR, "N", 252, 125, 60, 54);
            cells[5][2] = new Cell(CELL_TYPE_CHAR, "O", 312, 125, 60, 54);
            cells[6][2] = new Cell(CELL_TYPE_CHAR, "P", 372, 125, 60, 54);
            cells[7][2] = new Cell(CELL_TYPE_CHAR, "Q", 432, 125, 60, 54);
            cells[8][2] = new Cell(CELL_TYPE_CHAR, "R", 492, 125, 60, 54);
            
            // row 4
            cells[0][3] = new Cell(CELL_TYPE_CHAR, "S", 12, 189, 60, 54);
            cells[1][3] = new Cell(CELL_TYPE_CHAR, "T", 72, 189, 60, 54);
            cells[2][3] = new Cell(CELL_TYPE_CHAR, "U", 132, 189, 60, 54);
            cells[3][3] = new Cell(CELL_TYPE_CHAR, "V", 192, 189, 60, 54);
            cells[4][3] = new Cell(CELL_TYPE_CHAR, "W", 252, 189, 60, 54);
            cells[5][3] = new Cell(CELL_TYPE_CHAR, "X", 312, 189, 60, 54);
            cells[6][3] = new Cell(CELL_TYPE_CHAR, "Y", 372, 189, 60, 54);
            cells[7][3] = new Cell(CELL_TYPE_CHAR, "Z", 432, 189, 60, 54);
            cells[8][3] = new Cell(CELL_TYPE_CHAR, "0", 492, 189, 60, 54);
            
            // row 5
            cells[0][4] = new Cell(CELL_TYPE_CHAR, "1", 12, 249, 60, 60);
            cells[1][4] = new Cell(CELL_TYPE_CHAR, "2", 72, 249, 60, 60);
            cells[2][4] = new Cell(CELL_TYPE_CHAR, "3", 132, 249, 60, 60);
            cells[3][4] = new Cell(CELL_TYPE_CHAR, "4", 192, 249, 60, 60);
            cells[4][4] = new Cell(CELL_TYPE_CHAR, "5", 252, 249, 60, 60);
            cells[5][4] = new Cell(CELL_TYPE_CHAR, "6", 312, 249, 60, 60);
            cells[6][4] = new Cell(CELL_TYPE_CHAR, "7", 372, 249, 60, 60);
            cells[7][4] = new Cell(CELL_TYPE_CHAR, "8", 432, 249, 60, 60);
            cells[8][4] = new Cell(CELL_TYPE_CHAR, "9", 492, 249, 60, 60);        
            
            String uc_top_img = prefix + "keyboard-top-DELCLRSPCabcSYM.png";
            String uc_top_undo_img = prefix + "keyboard-top-DELUNDOSPCabcSYM.png";
            String uc_middle_img = prefix + "keyboard-middle-uc.png";
            String uc_bottom_img = prefix + "keyboard-bottom-empty.png";

            kb = new Keyboard(cells, 
                    uc_top_img, uc_top_undo_img, 70,
                    uc_middle_img, 240,
                    uc_bottom_img, 20,
                    578);
            standardKeyboards.put(new Integer(type), kb);
            return kb;


        case STANDARD_KEYBOARD_SYMBOL:
            cells = new Cell[8][5];
            
            // row 1
            cells[0][0] = new Cell(CELL_TYPE_DEL, null, 0, 2, 105, 54);
            cells[1][0] = new Cell(CELL_TYPE_CLR, null, 105, 2, 124, 54);
            cells[2][0] = new Cell(CELL_TYPE_CHAR, " ", 231, 2, 108, 54);
            cells[3][0] = new Cell(CELL_TYPE_LOWERCASE, null, 349, 2, 108, 54);
            cells[4][0] = new Cell(CELL_TYPE_UPPERCASE, null, 460, 2, 108, 54);

            
            // row 2
            cells[0][1] = new Cell(CELL_TYPE_CHAR, ".", 12, 61, 60, 56);
            cells[1][1] = new Cell(CELL_TYPE_CHAR, ",", 72, 61, 60, 56);
            cells[2][1] = new Cell(CELL_TYPE_CHAR, "@", 144, 61, 60, 56);
            cells[3][1] = new Cell(CELL_TYPE_CHAR, "~", 216, 61, 60, 56);
            cells[4][1] = new Cell(CELL_TYPE_CHAR, "#", 294, 61, 60, 56);
            cells[5][1] = new Cell(CELL_TYPE_CHAR, "&", 366, 61, 60, 56);
            cells[6][1] = new Cell(CELL_TYPE_CHAR, "(", 436, 61, 60, 56);
            cells[7][1] = new Cell(CELL_TYPE_CHAR, ")", 496, 61, 60, 56);
            
            // row 3
            cells[0][2] = new Cell(CELL_TYPE_CHAR, ":", 12, 125, 60, 56);
            cells[1][2] = new Cell(CELL_TYPE_CHAR, ";", 72, 125, 60, 56);
            cells[2][2] = new Cell(CELL_TYPE_CHAR, "*", 144, 125, 60, 56);
            cells[3][2] = new Cell(CELL_TYPE_CHAR, "?", 216, 125, 60, 56);
            cells[4][2] = new Cell(CELL_TYPE_CHAR, "!", 294, 125, 60, 56);
            cells[5][2] = new Cell(CELL_TYPE_CHAR, "_", 366, 125, 60, 56);
            cells[6][2] = new Cell(CELL_TYPE_CHAR, "[", 436, 125, 60, 56);
            cells[7][2] = new Cell(CELL_TYPE_CHAR, "]", 496, 125, 60, 56);
            
            // row 4
            cells[0][3] = new Cell(CELL_TYPE_CHAR, "'", 12, 185, 60, 56);
            cells[1][3] = new Cell(CELL_TYPE_CHAR, "\"", 72, 185, 60, 56);
            cells[2][3] = new Cell(CELL_TYPE_CHAR, "`", 144, 185, 60, 56);
            cells[3][3] = new Cell(CELL_TYPE_CHAR, "-", 216, 185, 60, 56);
            cells[4][3] = new Cell(CELL_TYPE_CHAR, "+", 294, 185, 60, 56);
            cells[5][3] = new Cell(CELL_TYPE_CHAR, "=", 366, 185, 60, 56);
            cells[6][3] = new Cell(CELL_TYPE_CHAR, "{", 436, 185, 60, 56);
            cells[7][3] = new Cell(CELL_TYPE_CHAR, "}", 496, 185, 60, 56);
            
            // row 5
            cells[0][4] = new Cell(CELL_TYPE_CHAR, "/", 12, 245, 60, 56);
            cells[1][4] = new Cell(CELL_TYPE_CHAR, "\\", 72, 245, 60, 56);
            cells[2][4] = new Cell(CELL_TYPE_CHAR, "|", 144, 245, 60, 56);
            cells[3][4] = new Cell(CELL_TYPE_CHAR, "^", 216, 245, 60, 56);
            cells[4][4] = new Cell(CELL_TYPE_CHAR, "%", 294, 245, 60, 56);
            cells[5][4] = new Cell(CELL_TYPE_CHAR, "$", 366, 245, 60, 56);
            cells[6][4] = new Cell(CELL_TYPE_CHAR, "<", 436, 245, 60, 56);
            cells[7][4] = new Cell(CELL_TYPE_CHAR, ">", 496, 245, 60, 56);
            
            String sym_top_img = prefix + "keyboard-top-DELCLRSPCabcABC.png";
            String sym_top_undo_img = prefix + "keyboard-top-DELUNDOSPCabcABC.png";
            String sym_middle_img = prefix + "keyboard-middle-sym.png";
            String sym_bottom_img = prefix + "keyboard-bottom-empty.png";

            kb = new Keyboard(cells, 
                    sym_top_img, sym_top_undo_img, 70,
                    sym_middle_img, 240,
                    sym_bottom_img, 20,
                    578);
            
            standardKeyboards.put(new Integer(type), kb);
            return kb;
        }
        return null;
    }

}
