package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeEventHandler;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.HmeEvent.FontInfo;
import org.lnicholls.galleon.database.Audio;

public class MusicScreenSaver extends BounceScreenSaver {
    private Audio audio;

    private BView containerView;
    private BView imageView;
    private BText textView;
    private FontInfo fontInfo;
    
    private boolean imageModified;
    private Resource imageRes;
    private int imageWidth;
    private int imageHeight;

    @Override
    public void activate() {
        super.activate();
        updateInfo();
    }

    @Override
    public boolean isWakeEvent(HmeEvent event) {
        if (event instanceof HmeEvent.Key) {
            HmeEvent.Key keyEvent = (HmeEvent.Key)event;
            int code = keyEvent.getCode();
            if (code == IHmeProtocol.KEY_VOLUMEDOWN || code == IHmeProtocol.KEY_VOLUMEUP ||
                    code == IHmeProtocol.KEY_TIVO) {
                //don't wake up on volume key presses or special keys
                return false;
            }
        }
        return super.isWakeEvent(event);
    }
    
//    @Override
    protected void createBounceView(BView parent) {
        imageView = new BView(parent, 0, 0, 142, 215);
        imageView.setResource(getApp().createImage(getImageResource()));

        Resource fontRes = parent.createFont("default.ttf", IHmeProtocol.FONT_PLAIN, 24, 
                    IHmeProtocol.FONT_METRICS_BASIC | IHmeProtocol.FONT_METRICS_GLYPH);
        fontRes.addHandler(new IHmeEventHandler() {
            public void postEvent(HmeEvent event) {
                switch (event.getOpCode()) { 
                case IHmeProtocol.EVT_FONT_INFO:
                    fontInfo = (FontInfo)event;
                    updateInfo();
                }
            }
        });
        
        textView = new BText(parent, 0, imageView.getHeight(), imageView.getWidth(), 30);
        textView.setFont(fontRes);
        textView.setValue("Label");

        parent.setSize(imageView.getWidth(), imageView.getHeight() + 30);
        containerView = parent;
    }

    public Audio getAudio() {
        return audio;
    }

    public void setAudio(Audio audio) {
        this.audio = audio;
        updateInfo();
    }
    
    public void setImage(Resource imageRes, int width, int height) {
        if (imageRes == null) {
            imageRes = getApp().createImage(getImageResource());
            width = 142;
            height = 215;
        }
        this.imageRes = imageRes;
        this.imageWidth = width;
        this.imageHeight = height;
        this.imageModified = true;
    }

    @Override
    protected void beforeAnimation() {
        if (imageModified) {
            updateInfo();
        }
    }
    
    private void updateInfo() {
        if (containerView == null || fontInfo == null) {
            return;
        }
        try {
            containerView.setPainting(false);
            String title = "";
            if (audio != null) {
                title = audio.getTitle();
            }
            textView.setValue(title);
    
            if (imageModified) {
                imageModified = false;
                if (imageRes == null) {
                    imageRes = getApp().createImage(getImageResource());
                }
                if (imageWidth > 150) {
                    imageHeight = imageWidth*150/imageHeight;
                    imageWidth = 150;
                    
                } else if (imageHeight > 250) {
                    imageWidth = imageHeight*250/imageWidth;
                    imageHeight = 250;
                }
                imageView.setSize(imageWidth, imageHeight);
                imageView.setResource(imageRes, IHmeProtocol.RSRC_IMAGE_BESTFIT);
            }
            
            
            int width = Math.max(imageView.getWidth(), fontInfo.measureTextWidth(title));
            
            //change the size
            imageView.setLocation((width-imageView.getWidth())/2, 0);
            textView.setBounds(0, imageView.getHeight(), width, textView.getHeight());
            containerView.setSize(width, imageView.getHeight() + textView.getHeight());
        } finally {
            containerView.setPainting(true);
        }
    }

}
