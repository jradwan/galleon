package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.View;
import java.awt.Color;
import org.lnicholls.galleon.util.ScreenSaver;

public class ShadeScreenSaver implements ScreenSaver {
    private BApplication app;
    private Color color = Color.black;
    private float transparency = 0.15f;
    private int fadeDuration = 5000;
    
    private View shades;

    public void activate() {
        BScreen screen = app.getCurrentScreen();
        Resource resource = screen.getResource("*" + fadeDuration);
        shades = new TileView(screen, 0, 0, screen.getWidth(), screen.getHeight(), 650, 360);
        shades.setTransparency(1.0f);
        shades.setResource(color);
        shades.setTransparency(transparency, resource);
        screen.flush();
    }

    public void deactivate() {
        shades.clearResource();
        shades = null;
    }

    public void init(BApplication app) {
        this.app = app;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public int getFadeDuration() {
        return fadeDuration;
    }

    public void setFadeDuration(int fadeDuration) {
        this.fadeDuration = fadeDuration;
    }

    public float getTransparency() {
        return transparency;
    }

    public void setTransparency(float transparency) {
        this.transparency = transparency;
    }

}
