package org.lnicholls.galleon;

import java.io.*;
import java.lang.reflect.*;

import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.util.FileGatherer;
import org.lnicholls.galleon.util.JarClassLoader;
import org.lnicholls.galleon.util.Tools;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

import net.jimmc.jshortcut.JShellLink;

public class test1
{
    static class testlink
    {
        public testlink()
        {
            try {
                File file = new File("c:/test.lnk");
                JShellLink windowsShortcut = new JShellLink(file.getParent(), file.getName().substring(0,
                        file.getName().lastIndexOf(".")));
                windowsShortcut.load();
                file = new File(windowsShortcut.getPath());
                System.out.println(file.getAbsolutePath());
            } catch (Exception ex) {
                ex.printStackTrace();
            }            
        }
    }
    
        public static void main(String[] args) throws Exception    {
        URL[] urls = new URL[1];
        urls[0] = new URL("file://d:/galleon/lib/jshortcut.jar");
        URLClassLoader urlClassLoader = new URLClassLoader(urls);
        
        JarClassLoader jarClassLoader = new JarClassLoader(new File("d:/galleon/lib/jshortcut.jar"));
        
        //Class theClass = Class.forName("org.lnicholls.galleon.test1$testlink", true, jarClassLoader);
        //testlink testlink = (testlink)theClass.newInstance();
        //System.out.println("b= "+testlink.getClass().getClassLoader());
        
        Class theClass = Class.forName("net.jimmc.jshortcut.JShellLink", true, jarClassLoader);
        
        Class[] parameterTypes = new Class[2];
        parameterTypes[0] = java.lang.String.class;
        parameterTypes[1] = java.lang.String.class;
        Constructor constructor = theClass.getConstructor(parameterTypes);
        Object[] parameters = new Object[2];
        File file = new File("c:/test.lnk");
        parameters[0] = file.getParent();
        parameters[1] = file.getName().substring(0, file.getName().lastIndexOf("."));
        Object stub = constructor.newInstance(parameters);
        
        //JShellLink JShellLink = (JShellLink)theClass.newInstance();
        System.out.println("b= "+stub.getClass().getClassLoader());
        
        Method method = theClass.getMethod("load", null);
        method.invoke(stub, null);
        method = theClass.getMethod("getPath", null);
        
        System.out.println("b= "+method.invoke(stub, null));
        
        //testlink testlink = new testlink();    }
}
