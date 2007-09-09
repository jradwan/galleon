package org.lnicholls.galleon.util;

import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeEventHandler;
import com.tivo.hme.sdk.Resource;
import java.awt.Point;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class BouncingAnimator extends BView {
    private static final Log log = LogFactory.getLog(BouncingAnimator.class);
    private int minX;
    private int minY;
    private int maxX;
    private int maxY;
    
    private int speed = 3;
    
    private IHmeEventHandler handler;
    
    private int motionX;
    private int motionY;
    
    private Resource anim;
    
    private boolean animating;
    private int animatorId;

    public BouncingAnimator(BView parent, int x, int y, int width, int height, 
            int minX, int minY, int maxX, int maxY) {
        super(parent, x, y, width, height);
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        
        this.animatorId = hashCode();
        motionX = 1;
        motionY = 1;
        
        handler = new IHmeEventHandler() {
            public void postEvent(HmeEvent event) {
                processEvent(event);
            }
        };
    }
    
    
    public void start() {
        animating = true;
        setLocation((maxX-minX)/2, (maxY-minY)/2);
        motionX = 1;
        motionY = 1;
        getApp().addHandler(handler);
        animate();
    }
    
    public void stop() {
        getApp().removeHandler(handler);
        animating = false;
    }

    protected void animate() {
        int x = getX();
        int y = getY();
        int width = getWidth();
        int height = getHeight();
        Point p = getIntersection(x, y, motionX,  motionY, minX, minY, maxX, maxY);
        
        if (p == null) {
            log.info("No intersection");
            return;
        }
        
        //adjust it so the view is contained on the screen
        if (p.x + width > maxX) {
            p.y -= (p.x+width-maxX)*motionY/motionX;
            p.x -= (p.x+width-maxX);
        }
        if (p.y + height > maxY) {
            p.x -= (p.y+height-maxY)*motionX/motionY;
            p.y -= (p.y+height-maxY);
        }
        
        //next invert the motion for the bounce
        if (p.x <= minX || p.x + width >= maxX) {
            motionX = -motionX;
            
        }
        if (p.y <= minY || p.y + height >= maxY) {
            motionY = -motionY;
        }
        
        //calculate distance so we can calculate the speed of the animation so it is fluid
        int a = p.x - x;
        int b = p.y - y;
        int distance = (int)Math.sqrt(a*a + b*b);
        
        //remove the old animation
        if (anim != null) {
            anim.remove();
        }

        anim = getResource("*" + distance*50/speed);
        setLocation(p.x, p.y, anim);
        
        // send a marker event so we know when the animation is done
        HmeEvent evt = new HmeEvent.Key(getApp().getID(), 0, KEY_TIVO, animatorId);
        getApp().sendEvent(evt, anim);
        getApp().flush();
    }
    
    private Point getIntersection(int cx, int cy, int mx, int my, int rx1, int ry1, int rx2, int ry2) {
        float m = (float)my/mx;
        int b = cy - (int)(m*cx);
        int x, y;
        
        //check the top line intersection
        if (my > 0) {
            //it is moving upward
            y = ry2;
            x = (int)((y - b)/m);
            if (x >= rx1 && x <= rx2) {
                return new Point(x, y);
            }
        }

        //check the bottom line intersection
        if (my < 0) {
            //it is moving downward
            y = 0;
            x = (int)((y - b)/m);
            if (x >= rx1 && x <= rx2) {
                return new Point(x, y);
            }
        }

        //check the left line intersection
        if (mx < 0) {
            //it is moving leftward
            x = 0;
            y = (int)(m*x) + b;
            if (y >= ry1 && y <= ry2) {
                return new Point(x, y);
            }
        }

        //check the right line intersection
        if (mx > 0) {
            //it is moving rightward
            x = rx2;
            y = (int)(m*x) + b;
            if (y >= ry1 && y <= ry2) {
                return new Point(x, y);
            }
        }
        
        return null;
    }
    
    public void setMotionVector(int motionX, int motionY) {
        this.motionX = motionX;
        this.motionY = motionY;
    }
    
    public int getMotionX() {
        return motionX;
    }
    
    public int getMotionY() {
        return motionY;
    }

    protected void processEvent(HmeEvent event) {
        if (event.getOpCode() == EVT_KEY) {
            HmeEvent.Key e = (HmeEvent.Key) event;
            if (e.getCode() == KEY_TIVO) {
                int id = (int) e.getRawCode();
                if (animatorId == id) {
                    if (animating) {
                        animate();
                    }
                }
            }
        }
    }

}
