package org.lnicholls.galleon.widget;

/*
 * Copyright (C) 2005 Leon Nicholls
 * 
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with this program; if not, write to the Free
 * Software Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * 
 * See the file "COPYING" for more details.
 */

import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.apps.music.Music;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.server.MusicPlayerConfiguration;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;

import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.Resource;

public class MusicInfo extends BView {

    private static Logger log = Logger.getLogger(MusicInfo.class.getName());

    public MusicInfo(BView parent, int x, int y, int width, int height, boolean visible) {
        super(parent, x, y, width, height, visible);
        
        mTimeFormat = new SimpleDateFormat();
        mTimeFormat.applyPattern("mm:ss");

        int start = 0;

        mCover = new BView(this, this.width - 210, 135, 200, 200, false);

        mTitleText = new BText(this, 0, start, this.width, 70);
        mTitleText.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_TOP);
        mTitleText.setFont("default-30-bold.font");
        mTitleText.setColor(Color.CYAN);
        mTitleText.setShadow(true);

        start += 70;

        mSongText = new BText(this, 0, start, this.width, 20);
        mSongText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
        mSongText.setFont("default-18-bold.font");
        mSongText.setShadow(true);

        mDurationText = new BText(this, 0, start, this.width, 20);
        mDurationText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
        mDurationText.setFont("default-18-bold.font");
        mDurationText.setShadow(true);

        start += 20;

        mAlbumText = new BText(this, 0, start, this.width, 20);
        mAlbumText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
        mAlbumText.setFont("default-18-bold.font");
        mAlbumText.setShadow(true);

        mYearText = new BText(this, 0, start, this.width, 20);
        mYearText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
        mYearText.setFont("default-18-bold.font");
        mYearText.setShadow(true);

        start += 20;

        mArtistText = new BText(this, 0, start, this.width, 20);
        mArtistText.setFlags(RSRC_HALIGN_LEFT | RSRC_VALIGN_TOP);
        mArtistText.setFont("default-18-bold.font");
        mArtistText.setShadow(true);

        mGenreText = new BText(this, 0, start, this.width, 20);
        mGenreText.setFlags(RSRC_HALIGN_RIGHT | RSRC_VALIGN_TOP);
        mGenreText.setFont("default-18-bold.font");
        mGenreText.setShadow(true);

        mStars = new BView[5];
        for (int i = 0; i < 5; i++) {
            mStars[i] = new BView(this, 0 + (i * 40), 150, 34, 34, true);
            mStars[i].setResource(((DefaultApplication) getApp()).getStarIcon(), RSRC_IMAGE_BESTFIT);
            mStars[i].setTransparency(0.6f);
        }
    }

    public void setAudio(final Audio audio) {
        if (audio != null) {
            setPainting(false);
            try {
                mTitleText.setValue(audio.getTitle());
                if (audio.getPath().startsWith("http"))
                {
                    mSongText.setValue("Stream: " + Tools.trim(audio.getTitle(), 80));
                    mDurationText.setVisible(false);
                    mAlbumText.setVisible(false);
                    mYearText.setVisible(false);
                    mArtistText.setVisible(false);
                    mGenreText.setVisible(false);
                }
                else
                {
                    mSongText.setValue("Song: " + Tools.trim(audio.getTitle(), 40));
                    mDurationText.setValue("Duration: " + mTimeFormat.format(new Date(audio.getDuration())));
                    mAlbumText.setValue("Album: " + Tools.trim(audio.getAlbum(), 40));
                    mYearText.setValue("Year: " + String.valueOf(audio.getDate()));
                    mArtistText.setValue("Artist: " + Tools.trim(audio.getArtist(), 40));
                    mGenreText.setValue("Genre: " + audio.getGenre());
                    mDurationText.setVisible(true);
                    mAlbumText.setVisible(true);
                    mYearText.setVisible(true);
                    mArtistText.setVisible(true);
                    mGenreText.setVisible(true);
                }

                setRating(audio);

                if (mAudio == null || !audio.getId().equals(mAudio.getId()))
                {
                    clearCover();
                
                    if (mCoverThread != null && mCoverThread.isAlive())
                        mCoverThread.interrupt();
    
                    if (!audio.getPath().startsWith("http")) {
                        mCoverThread = new Thread() {
                            public void run() {
                                try {
                                    MusicPlayerConfiguration musicPlayerConfiguration = Server.getServer()
                                            .getMusicPlayerConfiguration();
                                    java.awt.Image image = Mp3File.getCover(audio, musicPlayerConfiguration.isUseAmazon(),
                                            musicPlayerConfiguration.isUseFile());
                                    if (image != null) {
                                        synchronized (this) {
                                            mCover.setResource(createImage(image), RSRC_IMAGE_BESTFIT);
                                            mCover.setVisible(true);
                                            mCover.setTransparency(1.0f);
                                            mCover.setTransparency(0.0f, mAnim);
                                            getBApp().flush();
                                        }
                                    }
                                } catch (Exception ex) {
                                    Tools.logException(MusicInfo.class, ex, "Could retrieve cover");
                                }
                            }
    
                            public void interrupt() {
                                synchronized (this) {
                                    super.interrupt();
                                }
                            }
                        };
                        mCoverThread.start();
                    }
                }

            } finally {
                setPainting(true);
            }
            mAudio = audio;
        }
    }

    public void setTitle(String value) {
        setPainting(false);
        try {
            mTitleText.setValue(value);
        } finally {
            setPainting(true);
        }
    }

    private void clearCover() {
        mCover.setVisible(false);
        if (mCover.resource != null)
            mCover.resource.remove();
    }

    private void setRating(Audio audio) {
        if (audio != null) {
            for (int i = 0; i < 5; i++) {
                if (i < audio.getRating())
                    mStars[i].setTransparency(0.0f);
                else
                    mStars[i].setTransparency(0.6f);
            }
        }
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_THUMBSDOWN:
            if (mAudio != null && mAudio.getRating() > 0) {
                getBApp().play("thumbsdown.snd");
                getBApp().flush();
                try {
                    mAudio.setRating(Math.max(mAudio.getRating() - 1, 0));
                    AudioManager.updateAudio(mAudio);
                } catch (Exception ex) {
                    Tools.logException(Music.class, ex);
                }
                setRating(mAudio);
            } else {
                getBApp().play("bonk.snd");
                getBApp().flush();
            }
            return true;
        case KEY_THUMBSUP:
            if (mAudio != null && mAudio.getRating() < 5) {
                getBApp().play("thumbsup.snd");
                getBApp().flush();
                try {
                    mAudio.setRating(Math.min(mAudio.getRating() + 1, 5));
                    AudioManager.updateAudio(mAudio);
                } catch (Exception ex) {
                    Tools.logException(Music.class, ex);
                }
                setRating(mAudio);
            } else {
                getBApp().play("bonk.snd");
                getBApp().flush();
            }
            return true;
        }
        return super.handleKeyPress(code, rawcode);
    }

    private Audio mAudio;

    private SimpleDateFormat mTimeFormat;

    private Resource mAnim = getResource("*2000");

    private BView mCover;

    private BText mTitleText;

    private BText mSongText;

    private BText mArtistText;

    private BText mAlbumText;

    private BText mDurationText;

    private BText mYearText;

    private BText mGenreText;

    private Tracker mTracker;

    private BView[] mStars;

    private Thread mCoverThread;
}