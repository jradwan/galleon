package org.lnicholls.galleon.apps.hdphotos;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.lnicholls.galleon.app.AppConfiguration;
import org.lnicholls.galleon.util.NameValue;

public class HDPhotosConfiguration implements AppConfiguration {
    private static final long serialVersionUID = -8190046507280613509L;

    private String name;
    
    private String flickrFavoriteUsers;
    
    private String flickrUsername;

    private boolean modified;

    private List<NameValue> paths = new ArrayList<NameValue>();

    public String getName() {
        return name;
    }

    public void setName(String value) {
        if (name != null && !name.equals(value))
            modified = true;
        name = value;
    }

    public List<NameValue> getPaths() {
        return paths;
    }

    public void setPaths(List<NameValue> value) {
        modified = true;
        paths = value;
    }

    public void addPath(NameValue nameValue) {
        modified = true;
        paths.add(nameValue);
    }
    
    public void setModified(boolean value) {
        modified = value;
    }

    public boolean isModified() {
        return modified;
    }

    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    public boolean isShared() {
        return false;
    }

    public void setShared(boolean value) {
    }

    public String getFlickrUsername() {
        return flickrUsername;
    }

    public void setFlickrUsername(String flickrUsername) {
        modified = true;
        this.flickrUsername = flickrUsername;
    }

    public String getFlickrFavoriteUsers() {
        return flickrFavoriteUsers;
    }

    public void setFlickrFavoriteUsers(String flickrUsers) {
        modified = true;
        this.flickrFavoriteUsers = flickrUsers;
    }
}
