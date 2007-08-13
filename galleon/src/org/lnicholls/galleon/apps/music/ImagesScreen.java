package org.lnicholls.galleon.apps.music;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.net.URL;
import java.util.List;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.Yahoo;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

public class ImagesScreen extends DefaultScreen {
    private BList list;

    private BView mImageView;

    private Thread mImageThread;

    private Tracker mTracker;

    private List mResults;

    private int mPos;

    private BText mPosText;

    private BText mUrlText;

    public ImagesScreen(Music app, Tracker tracker) {
        super(app, "Images", true);

        getBelow().setResource(app.getImagesBackground(), RSRC_HALIGN_LEFT | RSRC_IMAGE_VFIT);

        mTracker = tracker;

        mImageView = new BView(this.getNormal(), BORDER_LEFT, TOP, BODY_WIDTH, getHeight() - SAFE_TITLE_V - TOP
                - 75);
        mImageView.setVisible(false);

        mPosText = new BText(getNormal(), BORDER_LEFT, getHeight() - SAFE_TITLE_V - 60, BODY_WIDTH, 30);
        mPosText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
        mPosText.setFont("default-18-bold.font");
        mPosText.setColor(Color.CYAN);
        mPosText.setShadow(true);

        mUrlText = new BText(getNormal(), SAFE_TITLE_H, getHeight() - SAFE_TITLE_V - 78, BODY_WIDTH, 15);
        mUrlText.setFlags(RSRC_HALIGN_CENTER | RSRC_VALIGN_BOTTOM);
        mUrlText.setFont("default-12-bold.font");
        mUrlText.setColor(Color.WHITE);
        mUrlText.setShadow(true);

        setFooter("search.yahoo.com");

        mBusy.setVisible(true);

        /*
         * list = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H + 10,
         * (getHeight() - SAFE_TITLE_V) - 60, (int) Math .round((getWidth() -
         * (SAFE_TITLE_H * 2)) / 2), 90, 35);
         * //list.setBarAndArrows(BAR_HANG, BAR_DEFAULT, H_LEFT, null);
         * list.add("Back to player"); setFocusDefault(list);
         */

        BButton button = new BButton(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 55, (int) Math
                .round((getWidth() - (SAFE_TITLE_H * 2)) / 2), 35);
        button.setResource(createText("default-24.font", Color.white, "Return to player"));
        button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
        setFocus(button);
    }
    
    public Music getApp() {
        return (Music)super.getApp();
    }

    public void updateImage() {
        Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
        Audio audio = null;
        try {
            List list = null;
            if (nameFile.isFile())
                list = AudioManager.findByPath(((File) nameFile.getValue()).getCanonicalPath());
            else
                list = AudioManager.findByPath((String) nameFile.getValue());
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(Music.class, ex);
        }
        final Audio lyricsAudio = audio;

        mImageThread = new Thread() {
            public void run() {
                try {
                    synchronized (this) {
                        mBusy.setVisible(true);
                        getBApp().flush();
                    }

                    if (mResults == null || mResults.size() == 0) {
                        mResults = Yahoo.getImages("\"" + lyricsAudio.getArtist() + "\" music");
                        mPos = 0;
                    }
                    if (mResults.size() == 0) {
                        synchronized (this) {
                            try {
                                setPainting(false);
                                mBusy.setVisible(false);
                                getBApp().flush();
                            } finally {
                                setPainting(true);
                            }
                        }
                        return;
                    }

                    NameValue nameValue = (NameValue) mResults.get(mPos);
                    Image image = Tools.getImage(new URL(nameValue.getValue()), -1, -1);

                    if (image != null) {
                        synchronized (this) {
                            try {
                                setPainting(false);
                                if (mImageView.getResource() != null)
                                {
                                    mImageView.getResource().flush();
                                    mImageView.getResource().remove();
                                }
                                mUrlText.setValue(nameValue.getName());
                                mImageView.setVisible(true);
                                mImageView.setTransparency(1f);
                                mImageView.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                mImageView.setTransparency(0f, getResource("*500"));
                                image.flush();
                                image = null;
                            } finally {
                                setPainting(true);
                            }
                        }
                    } else {
                        mResults.remove(mPos);
                    }

                } catch (Exception ex) {
                    Tools.logException(Music.class, ex, "Could not retrieve image");
                    mResults.remove(mPos);
                } finally {
                    synchronized (this) {
                        try {
                            setPainting(false);
                            if (mResults != null && mResults.size() > 0)
                                mPosText.setValue(String.valueOf(mPos + 1) + " of "
                                        + String.valueOf(mResults.size()));
                            else
                                mPosText.setValue("No images found");
                            mBusy.setVisible(false);
                        } finally {
                            setPainting(true);
                        }
                        getBApp().flush();
                    }
                }
            }

            public void interrupt() {
                synchronized (this) {
                    super.interrupt();
                }
            }
        };
        mImageThread.start();
    }

    public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
        updateImage();

        return super.handleEnter(arg, isReturn);
    }

    public boolean handleExit() {
        try {
            setPainting(false);
            if (mImageThread != null && mImageThread.isAlive()) {
                mImageThread.interrupt();
                mImageThread = null;
                mResults.clear();
                mResults = null;
            }
        } finally {
            setPainting(true);
        }
        return super.handleExit();
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_SELECT:
            postEvent(new BEvent.Action(this, "pop"));
            return true;
        case KEY_UP:
        case KEY_DOWN:
        case KEY_CHANNELUP:
            if (mResults != null && mResults.size() > 0) {
                getBApp().play("pageup.snd");
                getBApp().flush();
                mPos = mPos - 1;
                if (mPos == -1)
                    mPos = mResults.size() - 1;
            }
            updateImage();
            return true;
        case KEY_CHANNELDOWN:
            if (mResults != null && mResults.size() > 0) {
                getBApp().play("pagedown.snd");
                getBApp().flush();
                mPos = mPos + 1;
                if (mPos == mResults.size())
                    mPos = 0;
            }
            updateImage();
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }
}