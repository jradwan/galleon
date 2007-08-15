package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import java.util.ArrayList;
import java.util.List;

public class HTileView extends BView {
   
    private Resource resource;
    private int flags;
    private List<BView> tiles = new ArrayList<BView>();
    private int tileWidth;

    public HTileView(BView parent, int x, int y, int w, int h, int tileWidth, boolean visible) {
        super(parent, x, y, w, y, visible);
        this.tileWidth = tileWidth;
    }

    public HTileView(BView parent, int x, int y, int w, int h, int tileWidth) {
        super(parent, x, y, w, h);
        this.tileWidth = tileWidth;
    }

    @Override
    public void setResource(Resource res, int flags) {
        if (res == null) {
            removeAllTiles();
            this.resource = null;
        } else {
            this.resource = res;
            this.flags = flags;
            refresh();
        }
    }
    
    private void removeAllTiles() {
        if (!tiles.isEmpty()) {
            //remove all tiles
            for (BView tile : tiles) {
                tile.clearResource();
            }
            tiles.clear();
        }
    }
    
    @Override
    public void setBounds(int x, int y, int width, int height, Resource animation) {

        boolean sizeChanged = (this.getWidth() != width || this.getHeight() != height);
        super.setBounds(x, y, width, height, animation);
        
        if (sizeChanged) {
            setPainting(false);
            try {
                update();
            } finally {
                setPainting(true);
            }
        }
    }
    
    private void update() {
        if (resource != null) {
            boolean visible = getVisible();
            int width = getWidth();
            int height = getHeight();
            int tilesNeeded = width/tileWidth + (((width%tileWidth) > 0) ? 1: 0);
            if (tiles.size() < tilesNeeded) {
                for (int i=tiles.size(); i < tilesNeeded; i++) {
                    BView tile = new BView(this, i*tileWidth, 0, tileWidth, height, visible);
                    tile.setResource(resource, flags);
                    tiles.add(tile);
                }
            } else if (tiles.size() > tilesNeeded) {
                //remove the ones off the end
                for (int i=tiles.size(); i > tilesNeeded; i--) {
                    BView tile = tiles.remove(i);
                    tile.clearResource();
                }
            }
        }
    }

    public void refresh() {
        if (resource != null) {
            setPainting(false);
            try {
                removeAllTiles();
                update();
                
            } finally {
                setPainting(true);
            }
        }
    }

    @Override
    public void setVisible(boolean visible, Resource anim) {
        super.setVisible(visible, anim);

        setPainting(false);
        try {
            for (BView tile : tiles) {
                tile.setVisible(visible, anim);
            }
        } finally {
            setPainting(true);
        }
    }

    public int getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }
    
}
