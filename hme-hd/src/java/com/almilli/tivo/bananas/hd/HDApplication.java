package com.almilli.tivo.bananas.hd;

import com.almilli.tivo.hme.hd.Resolution;
import com.almilli.tivo.hme.hd.ResolutionInfo;
import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.IHmeConstants;
import com.tivo.hme.interfaces.ILogger;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.HmeException;
import com.tivo.hme.sdk.io.ChunkedInputStream;
import com.tivo.hme.sdk.io.ChunkedOutputStream;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

public class HDApplication extends BApplication {
    private static final Log log = LogFactory.getLog(HDApplication.class);
    
    public static final int CMD_RECEIVER_SET_RESOLUTION = 62;

    private ChunkedOutputStream output;
    private int protocolVersion;
    private IContext context;
    private ResolutionInfo resolutionInfo;
    private int width;
    private int height;
    private boolean resolutionReceived;
    private boolean applicationInitialized;
    private Timer timer;
    private Resolution desiredResolution;
    protected boolean initialized;

    protected void setContext(IContext context, int version) {
        if (log.isDebugEnabled()) {
            log.debug("initContext version="+version);
        }
        this.protocolVersion = version;
        
        resolutionInfo = createResolutionInfo(640, 480, 1, 1);

        if (context.getOutputStream() instanceof ChunkedOutputStream) {
            this.output = (ChunkedOutputStream)context.getOutputStream();
        } else {
            this.output = new ChunkedOutputStream(context.getOutputStream(), IHmeConstants.TCP_BUFFER_SIZE);
        }
        
        super.setContext(context, version);

        this.width = getRoot().getWidth();
        this.height = getRoot().getHeight();
    }
    
    public void init(IContext context) throws Exception {
        this.context = context;
        super.init(context);
        
        if (log.isDebugEnabled()) {
            log.debug("init context resolutionReceived="+resolutionReceived);
        }
        
        if (resolutionReceived) {
            initApp();
        }
    }
    
    public boolean handleChunk(InputStream in) 
    {
        boolean doMore = true;
        
        // flush any data that was generated from previous handling of
        // event
        flush();

        ChunkedInputStream chunkInStr = null;
        if (in instanceof ChunkedInputStream)
        {
            chunkInStr = (ChunkedInputStream)in;
        }
        else
        {
            chunkInStr = new ChunkedInputStream(in); //, IHmeConstants.TCP_BUFFER_SIZE);
        }

        if ( protocolVersion < VERSION_0_40 ) 
        {
            chunkInStr.setUseVString( false );
        }
        else {
            chunkInStr.setUseVString( true );
        }            

        int opcode = -1;
        try {
            opcode = (int)chunkInStr.readVInt();
        } catch (IOException e) {
            // receiver closed - ignore
        }

        if (opcode == -1) {
            doMore = false;
            return doMore;
        }
        
        HmeEvent evt = null;
        try
        {
            switch (opcode) {
              case EVT_DEVICE_INFO: evt = new HmeEvent.DeviceInfo(chunkInStr); break;
              case EVT_APP_INFO:    
                  evt = new HmeEvent.ApplicationInfo(chunkInStr); 
                  initApp();
                  break;
                  
              case EVT_RSRC_INFO:   evt = new HmeEvent.ResourceInfo(chunkInStr, this); break;
              case EVT_KEY:         evt = new HmeEvent.Key(chunkInStr); break;
              case EVT_IDLE:        evt = new HmeEvent.Idle(chunkInStr); break;
              case EVT_FONT_INFO:   
                  evt = new HmeEvent.FontInfo(chunkInStr);
                  initApp();
                  break;
                  
              case EVT_INIT_INFO:   
                  evt = new HmeEvent.InitInfo(chunkInStr); 
                  initApp();
                  break;
                  
              case ResolutionInfo.EVT_RES_INFO:
                  log.debug("handleChunk: ResolutionInfo");
                  this.resolutionInfo = new ResolutionInfo(chunkInStr);
                  evt = resolutionInfo;
                  Resolution res = resolutionInfo.getCurrentResolution();
                  if(res.getWidth() != getRoot().getWidth() || res.getHeight() != getRoot().getHeight())
                  {
                      getRoot().setSize(res.getWidth(), res.getHeight());
                      width = getRoot().getWidth();
                      height = getRoot().getHeight();
                      rootBoundsChanged(getRoot().getBounds());
                  }
                  resolutionReceived = true;
                  initApp();
                  break;
            }
            chunkInStr.readTerminator();
        }
        catch (IOException e) {
            evt = null;
            e.printStackTrace();
        }

        if (evt == null) {
            log(ILogger.LOG_DEBUG, "unknown event opcode : " + opcode);
        }
        else
        {
            dispatchEvent(evt);
        }
        // flush any data that was generated from handling of event
        flush();
        return doMore;
    }

    public void setReceiverResolution(Resolution res) {
        try {
            if (timer == null) {
                timer = new Timer("Resolution Changer", true);
            }
            timer.schedule(new TimerTask() {
                public void run() {
                    if (!resolutionReceived) {
                        log.error("Timeout waiting for resolution change.");
                    }
                }
            }, 30000);
            
            //request the resolution change
            if (log.isDebugEnabled()) {
                log.debug("Resolution change requested: " + res);
            }
            resolutionReceived = false;
            cmdReceiverSetResolution(res.getWidth(), res.getHeight(),
                    res.getPixelAspectNumerator(), res.getPixelAspectDenominator());
            
        } catch (Exception e) {
            log(ILogger.LOG_DEBUG, "Error changing receiver resolution: " + e.getMessage());
        }
    }


    void cmdReceiverSetResolution(int width, int height, int aspectNumerator, int aspectDenominator) {
        try {
            output.writeVInt(CMD_RECEIVER_SET_RESOLUTION);
            output.writeVInt(1);
            
            output.writeVInt(width);
            output.writeVInt(height);
            output.writeVInt(aspectNumerator);
            output.writeVInt(aspectDenominator);
            
            output.writeTerminator();
            output.flush();
        } catch (Throwable t) {
            throw new HmeException(t);
        }
    }

    public ResolutionInfo getResolutionInfo() {
        return resolutionInfo;
    }
    
    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    ResolutionInfo createResolutionInfo(int width, int height, int aspectNumerator, int aspectDenominator) {
        Resolution resolution = new Resolution(width, height, aspectNumerator, aspectDenominator);
        List<Resolution> supported = new ArrayList<Resolution>(1);
        supported.add(resolution);
        return new ResolutionInfo(resolution, supported);
    }
    
    private void initApp() {
        if (applicationInitialized) {
	    if (log.isDebugEnabled()) {
		log.debug("Already initialized, not re-initing.");
	    }
            return;
        }
        applicationInitialized = true;
        
        try {
            initSkin();

            if (log.isDebugEnabled()) {
                log.debug("calling initApp(IContext)");
            }
            initApp(context);
        } catch (Throwable e) {
            log.error("An error occurred during application initialization.", e);
        }
    }


    public int getSafeActionHorizontal() {
        return resolutionInfo.getCurrentResolution().getSafeActionHorizontal();
    }

    public int getSafeActionVertical() {
        return resolutionInfo.getCurrentResolution().getSafeActionVertical();
    }

    public int getSafeTitleHorizontal() {
        return resolutionInfo.getCurrentResolution().getSafeTitleHorizontal();
    }

    public int getSafeTitleVertical() {
        return resolutionInfo.getCurrentResolution().getSafeTitleVertical();
    }
    
    public void rootBoundsChanged(Rectangle r) {
        if (log.isDebugEnabled()) {
            log.debug("rootBoundsChanged r="+r);
        }
        getBelow().setBounds(r.x, r.y, r.width, r.height);
        getNormal().setBounds(r.x, r.y, r.width, r.height);
        getAbove().setBounds(r.x, r.y, r.width, r.height);
        initSkin();
    }
    
    private void initSkin() {
        //change the skin to a custom skin
        CustomSkin skin = new CustomSkin(this);
        setSkin(skin);
        
        boolean highDef = getHeight() > 480;
        if (highDef) {
            String prefix = HDApplication.class.getPackage().getName().replace('.', '/') + "/images/";
    
            //add the highlight bar
            Properties props = new Properties();
            props.put("cap", "50");
            props.put("padh", "25");
            props.put("padv", "4");
            skin.add("bar", 1280, 60, prefix + "bar_hd.png", props);
            
            //add the keyboard highlight
            skin.add("keyboard-focus", 144, 100, prefix + "keyboard-focus.png");
    
            //add the arrows
            skin.add("up", 48, 40, prefix + "up_hd.png");
            skin.add("down", 48, 40, prefix + "down_hd.png");
            skin.add("left", 35, 30, prefix + "left_hd.png");
            skin.add("right", 35, 30, prefix + "right_hd.png");
        }
    }
    
    public void initApp(IContext context) throws Exception {
        // from MovieRentalApplication:

        // Check the current resolution. If needed, tell receiver to switch to
        // the desired resolution and return. When the receiver finally gets to
        // the desired resolution, call the application's initService method.
        ResolutionInfo resInfo = getResolutionInfo();
        Resolution currentRes = resInfo.getCurrentResolution();
        desiredResolution = resInfo.getPreferredResolution();

        if (log.isInfoEnabled()) {
            log.info("Current resolution is: " + currentRes);
        }

        if (currentRes.equals(desiredResolution)) {
            if (!initialized) {
                initService();
            }
        } else {
            if (log.isInfoEnabled()) {
                log.info("Changing resolution to: " + desiredResolution);
            }
            // switch the the preferred resolution
            setReceiverResolution(desiredResolution);
        }
    }

    /**
     * This event handler snoops on resolution changes and reinitializes the
     * service when it sees one.
     */
    public boolean handleEvent(HmeEvent event) {
        if (event instanceof ResolutionInfo) {
            ResolutionInfo resInfo = (ResolutionInfo) event;

            if (log.isInfoEnabled()) {
                log.info("Received resolution event: " + resInfo);
            }
            if (!initialized && desiredResolution != null
                && resInfo.getCurrentResolution().equals(desiredResolution)) {
                initService();
            }
        }
        return super.handleEvent(event);
    }

    public void initService() {
        // TODO Auto-generated method stub
    }
}
