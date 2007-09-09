package org.lnicholls.galleon.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

public class ScreenSaverConfiguration implements Serializable {

    private static final long serialVersionUID = 2250159492242142121L;
    
    private int duration = 120;
    private String screenSaverClass;
    private Map<String, String> attributeMap = new HashMap<String, String>();

    public void load(Element node) throws IOException {
        NamedNodeMap map = node.getAttributes();
        int size = map.getLength();
        for (int i=0; i < size; i++) {
            Attr att = (Attr)map.item(i);
            if (att.getName().equals("class")) {
                screenSaverClass = att.getValue();
                
            } else if (att.getName().equals("duration")) {
                duration = Integer.parseInt(att.getValue());
                
            } else {
                attributeMap.put(att.getName(), att.getValue());
            }
        }
    }
    
    public void save(String nodeName, Writer writer) throws IOException {
        writer.append("  <").append(nodeName);
        if (screenSaverClass != null) {
            writer.append(" class=\"").append(screenSaverClass);
        }
        writer.append(" duration=\"").append(Integer.toString(duration));
        writer.append('"');
        writer.append(' ');
        
        for (Map.Entry<String, String> entry : attributeMap.entrySet()) {
            writer.append(' ').append(entry.getKey());
            writer.append("=\"").append(entry.getValue());
            writer.append('"');
        }

        writer.append("/>\n");
    }

    public String getScreenSaverClass() {
        return screenSaverClass;
    }

    public void setScreenSaverClass(String screenSaverClass) {
        this.screenSaverClass = screenSaverClass;
    }

    public Map<String, String> getAttributeMap() {
        return attributeMap;
    }

    public void setAttributeMap(Map<String, String> attributeMap) {
        this.attributeMap = attributeMap;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }
}
