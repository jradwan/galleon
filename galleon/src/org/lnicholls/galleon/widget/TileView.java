package org.lnicholls.galleon.widget;

import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;
import java.util.ArrayList;
import java.util.List;

public class TileView extends BView {
   
    private Resource resource;
    private int flags;
    private List<List<BView>> tiles = new ArrayList<List<BView>>();
    private int tileWidth;
    private int tileHeight;

    public TileView(BView parent, int x, int y, int width, int height, int tileWidth, int tileHeight, boolean visible) {
        super(parent, x, y, width, height, visible);
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
    }

    public TileView(BView parent, int x, int y, int width, int height, int tileWidth, int tileHeight) {
        this(parent, x, y, width, height, tileWidth, tileHeight, true);
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
            for (List<BView> row : tiles) {
                for (BView tile : row) {
                    tile.clearResource();
                }
                row.clear();
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
            
            if (width == 0 || height == 0) {
                removeAllTiles();
                return;
            }
            
            int cols = width/tileWidth + (((width%tileWidth) > 0) ? 1: 0);
            int rows = height/tileHeight + (((height%tileHeight) > 0) ? 1: 0);
            

            if (tiles.size() < rows) {
                //add missing rows and update other rows
                for (int i=0; i < rows; i++) {
                    if (i >= tiles.size()) {
                        List<BView> row = new ArrayList<BView>(cols);
                        updateRow(row, cols, i*tileHeight, visible);
                        tiles.add(row);
                    } else {
                        //update row
                        List<BView> row = tiles.get(i);
                        updateRow(row, cols, i*tileHeight, visible);
                    }
                }
                
            } else if (tiles.size() > rows) {
                //remove extra rows and update other rows
                for (int i=0; i < tiles.size(); i++) {
                    if (i >= rows) {
                        //remove extra row
                        List<BView> row = tiles.remove(i);
                        for (BView tile : row) {
                            tile.clearResource();
                        }
                        row.clear();
                    } else {
                        //update row
                        List<BView> row = tiles.get(i);
                        updateRow(row, cols, i*tileHeight, visible);
                    }
                }
            } else {
                //update all rows
                for (int i=0; i < tiles.size(); i++) {
                    List<BView> row = tiles.get(i);
                    updateRow(row, cols, i*tileHeight, visible);
                }
            }
        }
    }
    
    private void updateRow(List<BView> tiles, int cols, int y, boolean visible) {
        if (tiles.size() < cols) {
            for (int i=tiles.size(); i < cols; i++) {
                BView tile = new BView(this, i*tileWidth, y, tileWidth, tileHeight, visible);
                tile.setResource(resource, flags);
                tiles.add(tile);
            }
        } else if (tiles.size() > cols) {
            //remove the ones off the end
            for (int i=tiles.size()-1; i > cols; i--) {
                BView tile = tiles.remove(i);
                tile.clearResource();
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
            for (List<BView> row : tiles) {
                for (BView tile : row) {
                    tile.setVisible(visible, anim);
                }
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

    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }
    
}
