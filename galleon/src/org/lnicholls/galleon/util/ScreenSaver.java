package org.lnicholls.galleon.util;

import com.tivo.hme.bananas.BApplication;
import java.util.Map;

public interface ScreenSaver {
    public void init(BApplication app);
    public void activate();
    public void deactivate();
}
