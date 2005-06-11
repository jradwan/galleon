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


import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Button;import java.awt.Color;
import java.awt.image.*;import java.io.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;import com.tivo.hme.sdk.*;
import com.tivo.hme.util.*;
import com.tivo.hme.http.server.*;
import com.tivo.hme.http.share.*;import org.mozilla.javascript.*;import org.jdom.Document;
import org.jdom.*;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class Widget {

    final static Button mediaTrackerComp = new Button();    private static final int SCALE_FACTOR = 1;

    public Widget(WidgetLoader widgetLoader) {        mWidgetLoader = widgetLoader;
        mTimer = new Timer();    }        public View getWindow(View parent)    {
        Context context = Context.enter();
        try
        {
            String code = (String)mWidgetLoader.getCode();
            code = fixXml(code);
            ByteArrayInputStream inputStream = new ByteArrayInputStream(code.getBytes());
        
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(inputStream);

            // Get the root widget element
            Element root = doc.getRootElement();

            // Get window information
            Element windowElement = root.getChild("window");
            String name = getAttribute(windowElement, "name");
            int width = Integer.parseInt(getAttribute(windowElement, "width")) * SCALE_FACTOR;
            //System.out.println("width="+width);
            int height = Integer.parseInt(getAttribute(windowElement, "height")) * SCALE_FACTOR;
            //System.out.println("height="+height);
            
            //System.out.println("Creating windowView: "+(parent.width-width)/2+","+(parent.height-height)/2+","+width+","+height);
            View windowView = new View(parent,(parent.getWidth()-width)/2,(parent.getHeight()-height)/2,width,height);
            Konfabulator.mWindow = windowView;
            
            mScope = context.initStandardObjects();
            
            Object wrappedOut = Context.javaToJS(System.out, mScope); 
            ScriptableObject.putProperty(mScope, "out", wrappedOut);
            
            Object wrappedApp = Context.javaToJS(parent.getApp(), mScope); 
            ScriptableObject.putProperty(mScope, "app", wrappedApp);
            
            Play play = new Play(parent.getApp());
            Object wrappedPlay = Context.javaToJS(play, mScope); 
            ScriptableObject.putProperty(mScope, "player", wrappedPlay);
            String function = "function play(s) { player.play(s); };";
            Object result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            function = "function suppressUpdates() { app.root.setPainting(false); };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            function = "function updateNow() { app.flush(); app.root.setPainting(true);  };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            function = "function focusWidget() {  };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            function = "function savePreferences() {  };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            function = "function print(value) { out.println(value); };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            
            Include include = new Include(mWidgetLoader,mScope);
            Object wrappedInclude = Context.javaToJS(include, mScope); 
            ScriptableObject.putProperty(mScope, "includer", wrappedInclude);
            function = "function include(file) { includer.load(file); };";
            result = context.evaluateString(mScope, function, "<cmd>", 1, null);
            
            ScriptableObject.defineClass(mScope, ScriptWindow.class);
            Scriptable window = context.newObject(mScope, "Window");
            mScope.put(name, mScope, window);
            
            ScriptableObject.defineClass(mScope, ScriptPreferences.class);
            Scriptable preferences = context.newObject(mScope, "Preferences");
            List customPreferences = root.getChildren("preference");
            Iterator iterator = customPreferences.iterator();
            while (iterator.hasNext()) 
            {
              Element preferenceElement = (Element) iterator.next();
              String preferenceName = getAttribute(preferenceElement, "name");
              String defaultValue = getAttribute(preferenceElement, "defaultValue");
              //System.out.println(preferenceName+"="+defaultValue);
              ScriptableObject.defineProperty(preferences,preferenceName, new NameValue(preferenceName,defaultValue), ScriptableObject.PERMANENT);            
            }
            mScope.put("preferences", mScope, preferences);
            
            ScriptableObject.defineClass(mScope, ScriptSystem.class);
            Scriptable script = context.newObject(mScope, "System");
            ScriptableObject.defineProperty(script, "event", new Event(), ScriptableObject.PERMANENT);            
            
            mScope.put("system", mScope, script);
            
            ScriptableObject.defineClass(mScope, ScriptUrl.class);
            
            // TODO Cannot be static; how to handle multiple clients?
            //ScriptImage wrapper = new ScriptImage(windowView, mWidgetLoader);
            
            ScriptableObject.defineClass(mScope, ScriptImage.class);
            //Scriptable ScriptImage = context.newObject(mScope, "Image");
            //((ScriptImage)ScriptImage).setWidgetLoader(mWidgetLoader);
            
            // Get image information
            ArrayList elements = new ArrayList();
            List images = root.getChildren("image");
            iterator = images.iterator();
            while (iterator.hasNext()) 
            {
                Element imageElement = (Element) iterator.next();
                elements.add(imageElement);
            }
            
            Element[] elementsArray = (Element[])elements.toArray(new Element[0]);
            Arrays.sort(elementsArray, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            Element element1 = (Element) o1;
                            Element element2 = (Element) o2;
                            int z1 = 0;
                            int z2 = 0;
                            try
                            {
                                String value = getAttribute(element1, "zOrder");   
                                if (value!=null)
                                {
                                    z1 = Integer.parseInt(value);
                                }
                                value = getAttribute(element2, "zOrder");   
                                if (value!=null)
                                {
                                    z2 = Integer.parseInt(value);
                                }
                            }
                            catch (Exception ex) {}
                            
                            if (z1 < z2)
                                return -1;
                            else if (z1 > z2)
                                return 1;
                            else
                                return 0;
                        }
                    });            
            
            for (int i=0;i<elementsArray.length;i++)
            {  
                Element imageElement = elementsArray[i];                
                String imageName = getAttribute(imageElement, "name");
                //System.out.println("imageName="+imageName);
                if (getAttribute(imageElement, "width")!=null)
                    width = Integer.parseInt(getAttribute(imageElement, "width")) * SCALE_FACTOR;
                else    
                    width = -1;
                //System.out.println("width="+width);
                if (getAttribute(imageElement, "height")!=null)
                    height = Integer.parseInt(getAttribute(imageElement, "height")) * SCALE_FACTOR;
                else
                    height = -1;    
                //System.out.println("height="+height);
                int hOffset = 0;
                if (getAttribute(imageElement, "hOffset")!=null)
                    hOffset = Integer.parseInt(getAttribute(imageElement, "hOffset")) * SCALE_FACTOR;
                //System.out.println("hOffset="+hOffset);                        
                int vOffset = 0;
                if (getAttribute(imageElement, "vOffset")!=null)
                    vOffset = Integer.parseInt(getAttribute(imageElement, "vOffset")) * SCALE_FACTOR;
                //System.out.println("vOffset="+vOffset);
                int opacity = 255;
                if (getAttribute(imageElement, "opacity")!=null)
                    opacity = Integer.parseInt(getAttribute(imageElement, "opacity"));
                
                String src = getAttribute(imageElement, "src");
            
                ScriptImage scriptImage = (ScriptImage)context.newObject(mScope, "Image");
                scriptImage.jsSet_hOffset(hOffset);
                scriptImage.jsSet_vOffset(vOffset);
                scriptImage.jsSet_opacity(opacity);
                scriptImage.jsSet_src(src);
                
                mScope.put(imageName, mScope, scriptImage);
            }                    
            
            ScriptableObject.defineClass(mScope, ScriptText.class);
            
            elements = new ArrayList();
            List texts = root.getChildren("text");
            iterator = texts.iterator();
            while (iterator.hasNext()) 
            {
              Element textElement = (Element) iterator.next();
              elements.add(textElement);
            }
            
            elementsArray = (Element[])elements.toArray(new Element[0]);
            Arrays.sort(elementsArray, new Comparator() {
                        public int compare(Object o1, Object o2) {
                            Element element1 = (Element) o1;
                            Element element2 = (Element) o2;
                            int z1 = 0;
                            int z2 = 0;
                            try
                            {
                                String value = getAttribute(element1, "zOrder");   
                                if (value!=null)
                                {
                                    z1 = Integer.parseInt(value);
                                }
                                value = getAttribute(element2, "zOrder");   
                                if (value!=null)
                                {
                                    z2 = Integer.parseInt(value);
                                }
                            }
                            catch (Exception ex) {}
                            
                            if (z1 < z2)
                                return -1;
                            else if (z1 > z2)
                                return 1;
                            else
                                return 0;
                        }
                    });            
            
            for (int i=0;i<elementsArray.length;i++)
            {  
              Element textElement = elementsArray[i];
              Scriptable text = context.newObject(mScope, "Text");
              String textName = getAttribute(textElement, "name"); 
              //System.out.println("textName="+textName);
              if (textName!=null)  
                ((ScriptText)text).jsSet_name(textName);
              String value = getAttribute(textElement, "font"); 
              if (value!=null)  
                ((ScriptText)text).jsSet_font(value);
              value = getAttribute(textElement, "style"); 
              if (value!=null)  
                ((ScriptText)text).jsSet_style(value);
              value = getAttribute(textElement, "size"); 
              if (value!=null)  
                ((ScriptText)text).jsSet_size(Integer.parseInt(value));
              value = getAttribute(textElement, "color"); 
              if (value!=null) 
                ((ScriptText)text).jsSet_color(value);
              value = getAttribute(textElement, "alignment"); 
              if (value!=null)  
                ((ScriptText)text).jsSet_alignment(value);
              value = getAttribute(textElement, "hOffset");   
              if (value!=null)  
                ((ScriptText)text).jsSet_hOffset(Integer.parseInt(value));
              value = getAttribute(textElement, "vOffset");   
              if (value!=null)                  
                ((ScriptText)text).jsSet_vOffset(Integer.parseInt(value));
              value = getAttribute(textElement, "zOrder");   
              if (value!=null)  
              ((ScriptText)text).jsSet_zOrder(Integer.parseInt(value));
              value = getAttribute(textElement, "data"); 
              if (value!=null)  
                ((ScriptText)text).jsSet_data(value);

              mScope.put(textName, mScope, text);
            }            
        
            List actions = root.getChildren("action");
            iterator = actions.iterator();
            while (iterator.hasNext()) 
            {
              Element actionElement = (Element) iterator.next();
              String trigger = getAttribute(actionElement, "trigger"); 
              //System.out.println("trigger="+trigger);
              if (trigger.equalsIgnoreCase("onload"))
              {
                String file = getAttribute(actionElement, "file"); 
                
                String onLoad = "";
                if (file!=null)
                    onLoad = (String)mWidgetLoader.getResource(file);
                else    
                    onLoad = actionElement.getText();
                
                result = context.evaluateString(mScope, onLoad, "<cmd>", 1, null);

                System.err.println(context.toString(result));                
              }  
              else  
              if (trigger.equalsIgnoreCase("ontimer"))
              {
                String onTimer = actionElement.getText();
                int delay = (int) Math.round(Float.parseFloat(getAttribute(actionElement, "interval", "100"))*1000);
                //System.out.println("delay="+delay);
                schedule(new ReloadTask(mScope, onTimer), delay);
              }
              else  
              if (trigger.equalsIgnoreCase("onKeyDown"))
              {
                mOnKeyDown = actionElement.getText();
              }    
            }
            
            //windowView.setResource(Color.white);
            return windowView;
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
        }        
        return null;
    }
    
//charCodeAt(i):s=115
//charCodeAt(i):r=114
//charCodeAt(i):p=112
//charCodeAt(i):l=108        public void play()
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null && mOnKeyDown!=null)
            {                //System.out.println("Sending play.....");
                Scriptable system = (Scriptable)mScope.get("system", mScope);
                ScriptableObject.defineProperty(system, "event", new Event("115"), ScriptableObject.PERMANENT);            
                Object result = context.evaluateString(mScope, mOnKeyDown, "<onKeyDown>", 1, null);
                //Konfabulator.mWindow.getApp().flush();
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }        
    }
    
    public void pause()
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null && mOnKeyDown!=null)
            {
                //System.out.println("Sending pause.....");
                Scriptable system = (Scriptable)mScope.get("system", mScope);
                ScriptableObject.defineProperty(system, "event", new Event("112"), ScriptableObject.PERMANENT);            
                Object result = context.evaluateString(mScope, mOnKeyDown, "<onKeyDown>", 1, null);
                //Konfabulator.mWindow.getApp().flush();
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }            
    }
    
    public void start()
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null && mOnKeyDown!=null)
            {
                //System.out.println("Sending start.....");
                Scriptable system = (Scriptable)mScope.get("system", mScope);
                ScriptableObject.defineProperty(system, "event", new Event("108"), ScriptableObject.PERMANENT);            
                Object result = context.evaluateString(mScope, mOnKeyDown, "<onKeyDown>", 1, null);
                //Konfabulator.mWindow.getApp().flush();                
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }            
    }
    
    public void resetScore()
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null && mOnKeyDown!=null)
            {
                //System.out.println("Sending reset.....");
                Scriptable system = (Scriptable)mScope.get("system", mScope);
                ScriptableObject.defineProperty(system, "event", new Event("114"), ScriptableObject.PERMANENT);            
                Object result = context.evaluateString(mScope, mOnKeyDown, "<onKeyDown>", 1, null);
                //Konfabulator.mWindow.getApp().flush();                
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }            
    }
    
    public void left(int increment)
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null)
            {
                //System.out.println("Sending left.....");
                Object result = context.evaluateString(mScope, "system.event.hOffset", "<cmd>", 1, null);
                int currentOffset = Integer.parseInt(context.toString(result));
                if (currentOffset<18)
                    currentOffset = 18;
                //Object result = context.evaluateString(mScope, "system.event.hOffset = system.event.hOffset -5;", "<left>", 1, null);
                ScriptSystem system = (ScriptSystem)mScope.get("system", mScope);
                Event event = new Event();
                event.sethOffset(currentOffset-increment);
                ScriptableObject.defineProperty(system, "event", event, ScriptableObject.PERMANENT);            
                //Konfabulator.mWindow.getApp().flush();                
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }            
    }
    
    public void right(int increment)
    {        //Konfabulator.mWindow.getApp().root.setPainting(false);
        Context context = Context.enter();
        try
        {
            if (mScope!=null)
            {                //System.out.println("Sending right.....");
                
                Object result = context.evaluateString(mScope, "system.event.hOffset = system.event.hOffset +"+increment+";", "<right>", 1, null);
                
                /*
                Object result = context.evaluateString(mScope, "system.event.hOffset", "<cmd>", 1, null);
                int currentOffset = Integer.parseInt(context.toString(result));
                ScriptSystem system = (ScriptSystem)mScope.get("system", mScope);
                Event event = new Event();
                event.sethOffset(currentOffset+1);
                ScriptableObject.defineProperty(system, "event", event, ScriptableObject.PERMANENT);            
                */
                //Konfabulator.mWindow.getApp().flush();                
            }        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        } finally {
            // Exit from the context.
            Context.exit();
            //Konfabulator.mWindow.getApp().root.setPainting(true);
        }            
    }                                    private static String fixXml(String xml)    {
        // handle <widget version=1.0>        int pos = xml.indexOf("version=1.0");
        if (pos!=-1)
        {            String prefix = xml.substring(0,pos);            xml = prefix + "version=\"1.0\""+xml.substring(pos+"version=1.0".length());
        }        // handle &&
        pos = xml.indexOf("&&");
        if (pos!=-1)
        {            xml = xml.replaceAll("&&","&amp;&amp;");
        }                //encoding="macintosh"        pos = xml.indexOf("encoding=\"macintosh\"");
        if (pos!=-1)
        {            String prefix = xml.substring(0,pos);            xml = prefix + "encoding=\"UTF-8\""+xml.substring(pos+"encoding=\"macintosh\"".length());
        }
        
        return xml;    }
    
    private static String getAttribute(Element element, String attr)
    {        return getAttribute(element, attr, null);
    }        private static String getAttribute(Element element, String attr, String defaultValue)
    {        Attribute attribute = element.getAttribute(attr);        if (attribute!=null)        {
            return attribute.getValue();        }        else        {
            Element child = element.getChild(attr);
            if (child!=null)
            {                return child.getText();
            }        }        return defaultValue;
    }    
    
    public static void main(String args[]) throws Exception
    {
     // Creates and enters a Context. The Context stores information
     // about the execution environment of a script.
     Context cx = Context.enter();
     try {
         // Initialize the standard objects (Object, Function, etc.)
         // This must be done before scripts can be executed. Returns
         // a scope object that we use in later calls.
         Scriptable scope = cx.initStandardObjects();

        // Add a global variable "out" that is a JavaScript reflection
        // of //System.out
        Object jsOut = Context.javaToJS(System.out, scope);
        ScriptableObject.putProperty(scope, "out", jsOut);         
        
        
        // Use the Counter class to define a Counter constructor
        // and prototype in JavaScript.
        //ScriptableObject.defineClass(scope, Counter.class);
 
        // Create an instance of Counter and assign it to
        // the top-level variable "myCounter". This is
        // equivalent to the JavaScript code
        //    myCounter = new Counter(7);
        Object[] arg = { new Integer(7) };
        Scriptable myCounter = cx.newObject(scope, "Counter", arg);
        scope.put("myCounter", scope, myCounter);
        

         // Collect the arguments into a single string.
         String s = "";
         for (int i=0; i < args.length; i++) {
             s += args[i];
         }

         // Now evaluate the string we've colected.
         Object result = cx.evaluateString(scope, s, "<cmd>", 1, null);

         // Convert the result to a string and print it.
         System.err.println(cx.toString(result));

     } finally {
         // Exit from the context.
         Context.exit();
     }
    }            public void close()    {
        try {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
    }
    
    public static class ReloadTask extends TimerTask {                public ReloadTask(Scriptable scope, String code)        {
            mScope = scope;            mCode = code;        }
    
        public void run() {
            //Konfabulator.mWindow.getApp().root.setPainting(false);            Context context = Context.enter();
            try {                Object result = context.evaluateString(mScope, mCode, "<cmd>", 1, null);                //System.err.println(context.toString(result));
                
                //Konfabulator.mWindow.getApp().flush();            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                // Exit from the context.
                Context.exit();
                //Konfabulator.mWindow.getApp().root.setPainting(true);
            }
        }                private Scriptable mScope;        private String mCode;
    }        
    public synchronized void reset() {
        try {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
        }
        mTimer = new Timer();
    }            public synchronized void schedule(TimerTask task, long time) {
        if (time <= 0)
            time = 100;
        try {
            //System.out.println("mTimer="+mTimer);
            //System.out.println("task="+task);
            //System.out.println("time="+time);            mTimer.schedule(task, 0, time);
        } catch (IllegalStateException ex) {
            ex.printStackTrace();
            // Try again...
            reset();
            try {
                mTimer.schedule(task, 0, time);
            } catch (IllegalStateException ex2) {
                ex.printStackTrace();
            }
        }
    }    
    
   private WidgetLoader mWidgetLoader;
   private Image mMain;   private Timer mTimer;
   private Scriptable mScope;
   private String mOnKeyDown;}