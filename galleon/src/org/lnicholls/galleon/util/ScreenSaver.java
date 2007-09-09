package org.lnicholls.galleon.util;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.sdk.HmeEvent;
import java.util.Map;

public interface ScreenSaver {
    public void init(BApplication app);
    public void activate();
    public void deactivate();
    public boolean isWakeEvent(HmeEvent event);
}
