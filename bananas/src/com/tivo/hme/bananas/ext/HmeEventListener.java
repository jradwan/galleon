package com.tivo.hme.bananas.ext;

import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.HmeObject;
import java.util.EventListener;

public interface HmeEventListener extends EventListener {
    public void eventReceived(HmeObject object, HmeEvent event);
}
