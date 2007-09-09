package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BScreen;
import com.tivo.hme.bananas.BView;
import org.lnicholls.galleon.util.BouncingAnimator;

public class BounceScreenSaver extends ShadeScreenSaver {
    
    private String imageResource = "org/lnicholls/galleon/widget/tivologo.png";
    
    private BouncingAnimator animator;
    
    public BounceScreenSaver() {
        setFadeDuration(100);
    }

    @Override
    public void activate() {
        super.activate();

        BScreen screen = getApp().getCurrentScreen();
        animator = new BouncingAnimator(screen, 0, 0, 200, 200,
                0, 0, screen.getWidth(), screen.getHeight()) {
            
            @Override
            protected void animate() {
                beforeAnimation();
                super.animate();
                afterAnimation();
            }
        };

        createBounceView(animator);

        animator.start();
    }
    
    protected void beforeAnimation() {}
    protected void afterAnimation() {}
    
    protected void createBounceView(BView parent) {
        BView view = new BView(parent, 0, 0, 142, 215);
        view.setResource(getApp().createImage(imageResource));
        parent.setSize(view.getWidth(), view.getHeight());
    }

    @Override
    public void deactivate() {
        super.deactivate();
        
        if (animator != null) {
            animator.stop();
            animator.clearResource();
            animator.remove();
            animator = null;
        }
    }

    public String getImageResource() {
        return imageResource;
    }

    public void setImageResource(String imageResource) {
        this.imageResource = imageResource;
    }

}
