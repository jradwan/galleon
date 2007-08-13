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
    
    public void refresh() {
        if (resource != null) {
            setPainting(false);
            try {
                removeAllTiles();
                
                boolean visible = getVisible();
                int width = getWidth();
                int height = getHeight();
                for (int x=0; x < width; x+=tileWidth) {
                    BView tile = new BView(this, x, 0, tileWidth, height, visible);
                    tile.setResource(resource, flags);
                    tiles.add(tile);
                }
                
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
