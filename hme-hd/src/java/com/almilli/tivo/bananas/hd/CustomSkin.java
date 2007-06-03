package com.almilli.tivo.bananas.hd;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BSkin;
import com.tivo.hme.sdk.Resource;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


public class CustomSkin extends BSkin {
    private Map<String, ResourceDef> resourceMap = new HashMap<String, ResourceDef>();
    private BApplication app;

    public CustomSkin(BApplication app) {
        super(app);
        this.app = app;
    }
    
    public void add(String name, int width, int height, String resource) {
        resourceMap.put(name, new ResourceDef(name, width, height, resource));
    }
    
    public void add(String name, int width, int height, String resource, Properties props) {
        resourceMap.put(name, new ResourceDef(name, width, height, resource, props));
    }
    
    public Element get(String name) {
        ResourceDef info = resourceMap.get(name);
        if (info != null) {
            return info.getElement();
        }
        return super.get(name);
    }
    
    private class ResourceDef {
        private String name;
        private int width;
        private int height;
        private String resourcePath;
        private Element element;
        private Properties props;
        
        public ResourceDef(String name, int width, int height, String resourcePath) {
            this(name, width, height, resourcePath, null);
        }
        
        public ResourceDef(String name, int width, int height, String resourcePath, Properties props) {
            this.name = name;
            this.width = width;
            this.height = height;
            this.resourcePath = resourcePath;
            this.props = props;
        }
        
        public Element getElement() {
            if (element == null) {
                Resource resource = app.getResource(resourcePath);
                element = new Element(CustomSkin.this, name, width, height, resource);
                if (props != null) {
                    //set all the properties
                    Enumeration e = props.propertyNames();
                    while (e.hasMoreElements()) {
                        String name = (String)e.nextElement();
                        element.set(name, props.getProperty(name));
                    }
                }
            }
            return element;
        }
    }

}
