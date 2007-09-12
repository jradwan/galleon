package org.lnicholls.galleon.apps.iPhoto;

import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Vector;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.apps.photos.Photos;
import org.lnicholls.galleon.database.Image;
import org.lnicholls.galleon.media.ImageManipulator;
import org.lnicholls.galleon.media.JpgFile;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.Grid;


public class PGrid extends Grid {
    private static final Logger log = Logger.getLogger(PGrid.class);
    private iPhoto app;
    
    public PGrid(iPhoto app, BView parent, int x, int y, int width, int height,
            int rowHeight) {
        super(parent, x, y, width, height, rowHeight, true, app.isHighDef());
        this.app = app;
        mThreads = new Vector();
    }
    public PGrid(iPhoto app, BView parent, int x, int y, int width, int height) {
        super(parent, x, y, width, height, app.isHighDef() ? (int) (height / 4.9) : (int) (height / 2.9), true, app.isHighDef());
        this.app = app;
        mThreads = new Vector();
    }
	public void createCell(final BView parent, int row, int column,
            boolean selected) {
        ArrayList photos = (ArrayList) get(row);
        if (column < photos.size()) {
            final Item nameFile = (Item) photos.get(column);
            if (nameFile.isFolder()) {
                BView folderImage = new BView(parent, 0, 0, parent
                        .getWidth(), parent.getHeight());
                folderImage.setResource(app.getLargeFolderIcon());
                folderImage.flush();
                BText nameText = new BText(parent, 0,
                        parent.getHeight() - 25, parent.getWidth(), 25);
                nameText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
                nameText.setFont("default-18-bold.font");
                nameText.setShadow(true);
                nameText.setValue(nameFile.getName());
                parent.flush();
            } else {
                // TODO Handle: iPhoto[#1,uri=null]
                // handleApplicationError(4,view 1402 not found)
                Thread thread = new Thread() {
                    public void run() {
                        try {
                            synchronized (this) {
                                parent.setResource(Color.GRAY);
                                parent.setTransparency(0.5f);
                                parent.flush();
                            }
                            Image image = null;
                            synchronized (this) {
                                image = Photos.getImage(((File) nameFile
                                        .getValue()).getCanonicalPath());
                            }
                            BufferedImage thumbnail = null;
                            synchronized (this) {
                                thumbnail = JpgFile.getThumbnail(image);
                            }
                            if (thumbnail != null) {
                                synchronized (this) {
                                    if (image.getRotation() != 0) {
                                        thumbnail = ImageManipulator
                                                .rotate(thumbnail, parent
                                                        .getWidth(), parent
                                                .getHeight(), image
                                                        .getRotation());
                                    }
                                    parent.setResource(
                                            createImage(thumbnail),
                                            RSRC_IMAGE_BESTFIT);
                                    parent.setTransparency(0.0f);
                                    parent.flush();
                                    Tools.clearResource(parent);
                                }
                            }
                        } catch (Throwable ex) {
                            log.error(ex);
                        } finally {
                            mThreads.remove(this);
                        }
                    }
                    public void interrupt() {
                        synchronized (this) {
                            super.interrupt();
                        }
                    }
                };
                mThreads.add(thread);
                thread.start();
            }
        }
    }
    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_SELECT:
            postEvent(new BEvent.Action(this, "push"));
            return true;
        case KEY_CHANNELUP:
        case KEY_CHANNELDOWN:
            boolean result = super.handleKeyPress(code, rawcode);
            if (!result) {
                getBApp().play("bonk.snd");
                getBApp().flush();
            }
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }
    public void shutdown() {
        try {
            setPainting(false);
            Iterator iterator = mThreads.iterator();
            while (iterator.hasNext()) {
                Thread thread = (Thread) iterator.next();
                if (thread.isAlive()) {
                    thread.interrupt();
                }
            }
            mThreads.clear();
        } catch (Exception ex) {
            Tools.logException(iPhoto.class, ex, "Could not shutdown");
        } finally {
            setPainting(true);
        }
    }
    private Vector mThreads;
}
