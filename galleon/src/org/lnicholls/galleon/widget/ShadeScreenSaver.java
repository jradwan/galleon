package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeProtocol;
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
        if (screen != null) {
            Resource anim = null;
            if (fadeDuration > 0) {
                anim = screen.getResource("*" + fadeDuration);
            }
            shades = new TileView(screen, 0, 0, screen.getWidth(), screen.getHeight(), 650, 360);
            shades.setTransparency(1.0f);
            shades.setResource(color);
            shades.setTransparency(transparency, anim);
            screen.flush();
        }
    }
    
    public boolean isWakeEvent(HmeEvent event) {
        if (event instanceof HmeEvent.Key) {
            HmeEvent.Key keyEvent = (HmeEvent.Key)event;
            int code = keyEvent.getCode();
            if (code == IHmeProtocol.KEY_TIVO) {
                //don't wake up on special keys
                return false;
            }
            return true;
        }
        return false;
    }

    public void deactivate() {
        if (shades != null) {
            shades.remove();
            shades.clearResource();
            shades = null;
        }
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

    protected BApplication getApp() {
        return app;
    }

}
