package org.lnicholls.galleon.skin;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */


/** Utility class to store name/value pairs */
public class NameValue {
    public NameValue() {
    }

    public NameValue(String name, String value) {
        mName = name;
        mValue = value;
    }

    public String getName() {
        return mName;
    }

    public String getValue() {
        return mValue;
    }

    public void setName(String name) {
        mName = name;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public String toString() {
        return mName + "=" + mValue;
    }/*    
    public String jsGet_name() { return mName; }
    public void jsSet_name(String name) { mName = name; }            public String jsGet_value() { return mValue; }
    public void jsSet_value(String value) { mValue = value; }    */    

    private String mName;

    private String mValue;
}