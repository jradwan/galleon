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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Iterator;
import java.util.Vector;
import java.util.Random;
import java.util.StringTokenizer;

import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.HibernateUtil;
import org.lnicholls.galleon.media.MediaManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.FileFilters;
import org.lnicholls.galleon.util.FileSystemContainer;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.FileItem;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.database.PersistentValue;

import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.IHmeEventHandler;
import com.tivo.hme.sdk.IHmeProtocol;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.sdk.StreamResource;
import com.tivo.hme.sdk.HmeEvent.ResourceInfo;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.interfaces.IArgumentList;

public class DefaultApplication extends BApplication {

    private static final Logger log = Logger.getLogger(DefaultApplication.class.getName());

    public static String TRACKER = "org.lnicholls.galleon.widget.DefaultApplication.Tracker.List";
    
    public static String SEPARATOR = ",";

    protected Resource mBusyIcon;

    protected Resource mStarIcon;

    public void init(IContext context) throws Exception {
        super.init(context);

        mBusyIcon = getSkinImage(null, "busy");
        mStarIcon = getSkinImage(null, "star");

        mCallbacks = new ArrayList();

        mPlayer = new Player(this);
    }

    public Resource getStarIcon() {
        return mStarIcon;
    }

    public Resource getBusyIcon() {
        return mBusyIcon;
    }

    protected Resource getSkinImage(String screen, String key) {
        ByteArrayOutputStream baos = Server.getServer().getSkin().getImage(this.getClass().getName(), screen, key);
        if (mLastObject != null && mLastResource != null) {
            if (mLastObject == baos) {
                return mLastResource;
            }
        }
        mLastObject = baos;

        if (baos != null) {
            /*
             * if (image.getWidth() > 640 || image.getHeight() > 480) image = ImageManipulator.getScaledImage(image,
             * 640, 480);
             */
            try {
                mLastResource = createImage(baos.toByteArray());
                return mLastResource;
            } catch (Exception ex) {
                Tools.logException(DefaultApplication.class, ex);
            }
        }
        mLastResource = createImage(Tools.getDefaultImage());
        return mLastResource;
    }

    protected void dispatchEvent(HmeEvent event) {
        switch (event.getOpCode()) {
        case EVT_KEY:
            HmeEvent.Key e = (HmeEvent.Key) event;
            if (handleCallback(e))
                return;
        }
        super.dispatchEvent(event);
    }

    public boolean handleAction(BView view, Object action) {
        if (action.equals("pop")) {
            pop();
            remove();
            return true;
        }
        return super.handleAction(view, action);
    }

    public boolean handleKeyPress(int code, long rawcode) {
        switch (code) {
        case KEY_NUM1:
            if (mCurrentTrackerContext != null) {
                play("thumbsup.snd");
                
                PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(DefaultApplication.TRACKER);
                StringBuffer buffer = new StringBuffer();
                if (persistentValue!=null)
                {
                	buffer.append(persistentValue.getValue());
                }
                File file = new File(mCurrentTrackerContext);
    			if (file.exists()) {
    				if (file.isDirectory())
    				{
    					FileSystemContainer fileSystemContainer = new FileSystemContainer(mCurrentTrackerContext, true);
        				Tracker tracker = new Tracker(fileSystemContainer.getItems(FileFilters.audioDirectoryFilter), 0);
        				//mTracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
        				
        				String trackerString = getTrackerString(tracker);
        				if (buffer.length()==0)
        					buffer.append(trackerString);
        				else
        				{
        					buffer.append(SEPARATOR);
        					buffer.append(trackerString);
        				}
    				}
    				else
    				{
    	    			try
    	    			{
	    					Audio audio = getAudio(file.getCanonicalPath());
	    	    			if (buffer.length()==0)
	    	    				buffer.append(audio.getId());
	    	    			else
	    	    			{
	    	    				buffer.append(SEPARATOR);
	    	    				buffer.append(audio.getId());
	    	    			}
    	    			} catch (Exception ex) {
        					Tools.logException(DefaultApplication.class, ex);
        				}
    				}
    			} else {
    				// Organizer
    				List files = new ArrayList();
    				try {
    					Transaction tx = null;
    					Session session = HibernateUtil.openSession();
    					try {
    						tx = session.beginTransaction();
    						Query query = session.createQuery(mCurrentTrackerContext).setCacheable(true);
    						Audio audio = null;
    						ScrollableResults items = query.scroll();
    						if (items.first()) {
    							items.beforeFirst();
    							while (items.next()) {
    								audio = (Audio) items.get(0);
    								files.add(new FileItem(audio.getTitle(), new File(audio.getPath())));
    							}
    						}

    						tx.commit();
    					} catch (HibernateException he) {
    						if (tx != null)
    							tx.rollback();
    						throw he;
    					} finally {
    						HibernateUtil.closeSession();
    					}
    				} catch (Exception ex) {
    					Tools.logException(DefaultApplication.class, ex);
    				}

    				Tracker tracker = new Tracker(files, 0);
    				//mTracker.setRandom(musicPlayerConfiguration.isRandomPlayFolders());
    				String trackerString = getTrackerString(tracker);
    				if (buffer.length()==0)
    					buffer.append(trackerString);
    				else
    				{
    					buffer.append(SEPARATOR);
    					buffer.append(trackerString);
    				}
    			}
                PersistentValueManager.savePersistentValue(TRACKER, buffer.toString());
            }
            return true;
        case KEY_LEFT:
            play("pageup.snd");
            flush();
            setActive(false); // TODO Make default just pop
            return true;
        case KEY_PAUSE:
            //setSoundPlayed(true);
            play(null);
            mPlayer.pauseTrack();
            return true;
        case KEY_PLAY:
            mPlayer.playTrack();
            return true;
        case KEY_CHANNELUP:
            play("select.snd");
            flush();
            mPlayer.getPrevPos();
            mPlayer.startTrack();
            return true;
        case KEY_CHANNELDOWN:
            play("select.snd");
            flush();
            mPlayer.getNextPos();
            mPlayer.startTrack();
            return true;
        case KEY_SLOW:
            mPlayer.stopTrack();
            break;
        case KEY_FORWARD: 
        	mPlayer.fastForwardTrack();
            break;
        case KEY_REVERSE: 
        	mPlayer.rewindTrack();
            break;            
        case KEY_ADVANCE:
        	play("right.snd");
            flush();
        	mPlayer.jumpTrack();
            break;
        case KEY_CLEAR:
            setActive(false);
            break;
        }
        return super.handleKeyPress(code, rawcode);
    }

    public static class Tracker {
        public Tracker(List list, int pos) {
            setList(list);
            setPos(pos);
        }

        public void setPos(int pos) {
        	mPos = pos;
        }

        public int getPos() {
            return mPos;
        }

        public int getNextPos() {
            if (mList.size()>0)
            {
	        	if (mRandom) {
	                mPos = getRandomPos();
	            } else if (++mPos > (mList.size() - 1))
	                mPos = 0;
            }
            return mPos;
        }

        public int getPrevPos() {
        	if (mList.size()>0)
        	{
	        	if (mRandom) {
	                mPos = getRandomPos();
	            } else if (--mPos < 0)
	                mPos = mList.size() - 1;
        	}
            return mPos;
        }

        public void setList(List list) {
            if (list!=null)
            	mList = list;
        }

        public List getList() {
            return mList;
        }

        public boolean isRandom() {
            return mRandom;
        }

        public void setRandom(boolean value) {
            mRandom = value;
            if (mRandom)
            {
	            mRandomizer = new Random();
	            mRandomAvailable = null;
	            mPos = getRandomPos();
            }
        }

        private int getRandomPos() {
        	if (mList.size()>0)
        	{
	        	if (mList.size()>1)
	            {
	            	if (mRandomAvailable == null || mRandomAvailable.size()==0)
	            	{
	            		mRandomAvailable = new ArrayList(mList.size());
	            		for (int i=0;i<mList.size();i++)
	            			mRandomAvailable.add(new Integer(i));
	            	}
	            	
	            	int index = mRandomizer.nextInt(mRandomAvailable.size());
	            	int pos = ((Integer)mRandomAvailable.get(index)).intValue();
	            	mRandomAvailable.remove(index);
	                return pos;
	            }
	            else
	                return mRandomizer.nextInt(mList.size());
        	}
        	return 0;
        }
        
        public Object clone()
        {
        	ArrayList list = new ArrayList();
        	list.addAll(mList);
        	return new Tracker(list, mPos);
        }
        
        private List mList = new ArrayList();

        private int mPos = -1;

        private boolean mRandom;

        private Random mRandomizer = new Random();
        
        private ArrayList mRandomAvailable = null;
    }
    
    public static String getTrackerString(Tracker tracker)
    {
    	StringBuffer buffer = new StringBuffer();
    	if (tracker!=null)
    	{
	    	Iterator iterator = tracker.getList().iterator();
	    	while (iterator.hasNext())
	    	{
	    		try
	    		{
	    			Item nameFile = (Item) iterator.next();
	    			File file = (File) nameFile.getValue();
	    			Audio audio = getAudio(file.getCanonicalPath());
	    			if (buffer.length()==0)
	    				buffer.append(audio.getId());
	    			else
	    			{
	    				buffer.append(SEPARATOR);
	    				buffer.append(audio.getId());
	    			}
	    		}
	    		catch (Exception ex) {}
	    	}
    	}
    	return buffer.toString();
    }

    public static class Player implements IHmeEventHandler, IHmeProtocol {
        public static final int PLAY = 0;

        public static final int PAUSE = 1;

        public static final int STOP = 2;
        
        public static final int FORWARD = 3;
        
        public static final int REWIND = 4;

        public Player(DefaultApplication defaultApplication) {
            mDefaultApplication = defaultApplication;
        }

        public void playTrack(String url) {
            //System.out.println("playTrack:"+url);
            if (mStreamResource != null) {
                //System.out.println("playTrack: remove");
                mStreamResource.removeHandler(this);
                mStreamResource.remove();
                //mStreamResource.close();
                mDefaultApplication.flush();
            }
            mPlayerState = PLAY;
            mSpeedIndex = 3;
            mStreamResource = mDefaultApplication.createStream(url, "audio/mp3", true);
            log.debug("mStreamResource=" + mStreamResource.getID());
            mStreamResource.addHandler(this);
        }

        public void playTrack() {
            //System.out.println("playTrack:");
            if (mPlayerState != PLAY) {
                if (mStreamResource != null) {
                	mSpeedIndex = 3;
                	mPlayerState = PLAY;
                	mStreamResource.play();
                } else
                    startTrack();
            }
        }

        public void startTrack() {
            //System.out.println("startTrack:");
            if (mTracker != null && mTracker.getList().size()>0) {
                try {
                    if (mTracker.getPos() == -1)
                        getNextPos();
                    Item nameFile = (Item) mTracker.getList().get(mTracker.getPos());
                    Audio audio = null;
                    if (nameFile.isFile()) {
                        File file = (File) nameFile.getValue();
                        audio = getAudio(file.getCanonicalPath());
                    } else
                        audio = getAudio((String) nameFile.getValue());

                    if (audio != null) {
                        setTitle("");
                        stopTrack();
                        String url = mDefaultApplication.getContext().getBaseURI().toString();
                        try {
                            if (nameFile.isFile())
                                url += URLEncoder.encode(mDefaultApplication.hashCode() + "/" + audio.getId() + ".mp3",
                                        "UTF-8");
                            else
                                url += URLEncoder.encode(mDefaultApplication.hashCode() + "/" + audio.getId()
                                        + ".http.mp3", "UTF-8");
                        } catch (UnsupportedEncodingException ex) {
                            Tools.logException(DefaultApplication.class, ex, url);
                        }
                        try {
                            audio.setPlayCount(audio.getPlayCount() + 1);
                            audio.setDatePlayed(new Date());
                            AudioManager.updateAudio(audio);
                        } catch (Exception ex) {
                            Tools.logException(DefaultApplication.class, ex);
                        }

                        mCurrentAudio = audio;
                        playTrack(url);
                    }
                } catch (Exception ex) {
                    Tools.logException(DefaultApplication.class, ex);
                }
            }
        }

        public void pauseTrack() {
            //System.out.println("pauseTrack:");
            if (mPlayerState != STOP) {
                if (mPlayerState == PAUSE) {
                    playTrack();
                } else {
                    if (mStreamResource != null) {
                        mPlayerState = PAUSE;
                        mSpeedIndex = 3;
                        mStreamResource.pause();
                    }
                }
            }
        }

        public void stopTrack() {
            //System.out.println("stopTrack:");
            if (mPlayerState != STOP) {
                if (mStreamResource != null) {
                    mPlayerState = STOP;

                    mStreamResource.removeHandler(this);
                    mSpeedIndex = 3;
                    mStreamResource.setSpeed(0.0f);
                    //mStreamResource.remove();
                    //mStreamResource.close();
                    mStreamResource = null;
                    mDefaultApplication.flush();
                    reset();
                }
            }
        }
        
        public void jumpTrack() {
            //System.out.println("stopTrack:");
            if (mPlayerState != STOP) {
                if (mStreamResource != null) {
                	mPlayerState = PLAY;
                	
                	int skip = mTotal/4;
                	if (mCurrentPosition < 1*skip)
                	{
                		mStreamResource.setPosition(1*skip);
                	}
                	else
            		if (mCurrentPosition < 2*skip)
            		{
            			mStreamResource.setPosition(2*skip);
            		}
            		else                		
            		if (mCurrentPosition < 3*skip)
            		{
            			mStreamResource.setPosition(3*skip);
            		}
                	else                		
            		if (mCurrentPosition < 4*skip)
            		{
            			mStreamResource.setPosition(4*skip);
            		}
            		else
            		{
            			mStreamResource.setPosition(0);
            		}
                	
                	mSpeedIndex = 3;
                	mDefaultApplication.play("select.snd");
                	mDefaultApplication.flush();
                }
            }
        }
        
        void fastForwardTrack()
        {
            if (mStreamResource != null) 
            {
            	mPlayerState = FORWARD;
                if (mSpeedIndex < 6 && mSpeedIndex >= 3)
                {
                    mSpeedIndex++;
                    if (mSpeedIndex == 4) {
                    	mDefaultApplication.play("speedup1.snd");
                    } else if (mSpeedIndex == 5) {
                    	mDefaultApplication.play("speedup2.snd");
                    } else if (mSpeedIndex == 6) {
                    	mDefaultApplication.play("speedup3.snd");
                    } else {
                    	mDefaultApplication.play("slowdown1.snd");
                    }
                                            
                }
                else
                {
                	mPlayerState = PLAY;
                	mSpeedIndex = 3;
                    mDefaultApplication.play("slowdown1.snd");
                }
                mStreamResource.setSpeed(mSpeeds[mSpeedIndex]);
            }
        }

        void rewindTrack()
        {
            if (mStreamResource != null) 
            {
            	mPlayerState = REWIND;
            	if (mSpeedIndex > 0 && mSpeedIndex <= 3)
                {
                    mSpeedIndex--;
                    if (mSpeedIndex == 2) {
                    	mDefaultApplication.play("speedup1.snd");
                    } else if (mSpeedIndex == 1) {
                    	mDefaultApplication.play("speedup2.snd");
                    } else if (mSpeedIndex == 0) {
                    	mDefaultApplication.play("speedup3.snd");
                    } else {
                    	mDefaultApplication.play("slowdown1.snd");
                    }
                }
                else
                {
                	mPlayerState = PLAY;
                	mSpeedIndex = 3;
                    mDefaultApplication.play("slowdown1.snd");
                }
                mStreamResource.setSpeed(mSpeeds[mSpeedIndex]);
            }
        }
        
        public void postEvent(HmeEvent event) {
            //System.out.println("postEvent:"+event);
            // TODO Implement listeners
            HmeEvent.ResourceInfo info = (HmeEvent.ResourceInfo) event;
            switch (event.getOpCode()) {
            case StreamResource.EVT_RSRC_INFO:
            	if (info.getStatus() == RSRC_STATUS_PLAYING || info.getStatus() == RSRC_STATUS_SEEKING) {
                    String pos = (String) info.getMap().get("pos");
                    if (pos != null) {
                        try {
                            StringTokenizer tokenizer = new StringTokenizer(pos, "/");
                            if (tokenizer.countTokens() == 2) {
                                mCurrentPosition = Integer.parseInt(tokenizer.nextToken());
                                mTotal = Integer.parseInt(tokenizer.nextToken());
                            }
                        } catch (Exception ex) {
                        }
                    }
                    String bitrate = (String) info.getMap().get("bitrate");
                    if (bitrate != null) {
                        try {
                            mBitrate = (int) Math.round(Float.parseFloat(bitrate) / 1024);
                        } catch (Exception ex) {
                        }
                    }
                }
                if (mDefaultApplication.getCurrentScreen().getFocus() != null)
                    mDefaultApplication.getCurrentScreen().getFocus().handleEvent(new BEvent.Action(mDefaultApplication
                            .getCurrentScreen().getFocus(), ResourceInfo.statusToString(info.getStatus())));
                fireEvent(new ApplicationEvent(info, ResourceInfo.statusToString(info.getStatus()), getCurrentAudio(), mCurrentPosition, mTotal, mBitrate));                
                break;
            case StreamResource.EVT_RSRC_STATUS:
                if (info.getStatus() == RSRC_STATUS_PLAYING || info.getStatus() == RSRC_STATUS_SEEKING) {
                    if (mDefaultApplication.getCurrentScreen().getFocus() != null)
                        mDefaultApplication.getCurrentScreen().getFocus().handleEvent(new BEvent.Action(mDefaultApplication
                                .getCurrentScreen().getFocus(), "ready"));
                } else if (info.getStatus() >= RSRC_STATUS_CLOSED) {
                    //System.out.println("postEvent:RSRC_STATUS_CLOSED");
                    if (mPlayerState != STOP) {
                        stopTrack();

                        reset();

                        getNextPos();
                        startTrack();

                        if (mDefaultApplication.getCurrentScreen().getFocus() != null)
                            mDefaultApplication.getCurrentScreen().getFocus().handleEvent(new BEvent.Action(
                                    mDefaultApplication.getCurrentScreen().getFocus(), "stopped"));
                        
                        fireEvent(new ApplicationEvent(info, "stopped", getCurrentAudio(), mCurrentPosition, mTotal, mBitrate));                        
                    }
                }
                break;
            }
        }

        public void setTitle(String value) {
            mTitle = value;
            if (mDefaultApplication.getCurrentScreen().getFocus() != null)
                mDefaultApplication.getCurrentScreen().getFocus().handleEvent(new BEvent.Action(mDefaultApplication
                        .getCurrentScreen().getFocus(), "update"));
        }

        public void getNextPos() {
            if (mTracker != null) {
                int pos = mTracker.getNextPos();
                Item nameFile = (Item) mTracker.getList().get(pos);
                while (nameFile==null || nameFile.isFolder() || nameFile.isPlaylist()) {
                    pos = mTracker.getNextPos();
                    nameFile = (Item) mTracker.getList().get(pos);
                }
            }
        }

        public void getPrevPos() {
            //System.out.println("getPrevPos:");
            if (mTracker != null) {
                int pos = mTracker.getPrevPos();
                Item nameFile = (Item) mTracker.getList().get(pos);
                while (nameFile==null || nameFile.isFolder() || nameFile.isPlaylist()) {
                    pos = mTracker.getPrevPos();
                    nameFile = (Item) mTracker.getList().get(pos);
                }
            }
        }

        private void reset() {
            //System.out.println("reset:");
            mCurrentPosition = 0;
            mTotal = 0;
            mBitrate = 0;
            mTitle = "";
        }

        public void setTracker(Tracker tracker) {
        	if (mTracker != null)
                stopTrack();

            mTracker = tracker;
        }

        public Tracker getTracker() {
            return mTracker;
        }

        public int getCurrentPosition() {
            return mCurrentPosition;
        }

        public int getTotal() {
            return mTotal;
        }

        public int getBitrate() {
            return mBitrate;
        }

        public int getState() {
            return mPlayerState;
        }

        public String getTitle() {
            return mTitle;
        }

        public Audio getCurrentAudio() {
            return mCurrentAudio;
        }

        private DefaultApplication mDefaultApplication;

        private StreamResource mStreamResource;

        private Tracker mTracker;

        private int mPlayerState = STOP;

        private int mCurrentPosition;

        private int mTotal;

        private int mBitrate;

        private String mTitle = "";

        private Audio mCurrentAudio;
        
        private int mSpeedIndex = 3;
        
        private float[] mSpeeds = {-60.0f, -15.0f, -3.0f, 1.0f, 3.0f, 15.0f, 60.0f};
    }

    private static Audio getAudio(String path) {
        Audio audio = null;
        try {
            List list = AudioManager.findByPath(path);
            if (list != null && list.size() > 0) {
                audio = (Audio) list.get(0);
            }
        } catch (Exception ex) {
            Tools.logException(DefaultApplication.class, ex);
        }

        if (audio == null) {
            try {
                audio = (Audio) MediaManager.getMedia(path);
                if (audio!=null)
                {
                    AudioManager.createAudio(audio);
                }
            } catch (Exception ex) {
                Tools.logException(DefaultApplication.class, ex);
            }
        }
        return audio;
    }

    public void addCallback(Callback callback) {
        mCallbacks.add(callback);
    }

    public boolean handleCallback(HmeEvent event) {
        for (int i = 0; i < mCallbacks.size(); i++) {
            Callback callback = (Callback) mCallbacks.get(i);
            if (callback.handleEvent(event)) {
                mCallbacks.remove(i);
                return true;
            }
        }

        return false;
    }

    public Player getPlayer() {
        return mPlayer;
    }

    public void setTracker(Tracker tracker) {
        mPlayer.setTracker(tracker);
        List list = tracker.getList();
        if (list.size() > 0) {
            Item nameFile = (Item) list.get(0);
            if (nameFile!=null && nameFile.isFile()) {
                File file = (File) nameFile.getValue();
                //Tools.savePersistentValue(DefaultApplication.TRACKER, file.getParent());
            }
        }
    }

    public Tracker getTracker() {
        return mPlayer.getTracker();
    }

    public Audio getCurrentAudio() {
        return mPlayer.getCurrentAudio();
    }

    public void setCurrentTrackerContext(String value) {
        mCurrentTrackerContext = value;
    }

    public boolean handleApplicationError(int errorCode, String errorText) {
        log.debug(this + " handleApplicationError(" + errorCode + "," + errorText + ")");
        return true;
    }

    /*
     * protected void writeStream(String filename) throws IOException { InputStream in = getApp().getStream(filename);
     * try { while (true) { int count = in.read(buf, 0, buf.length); if (count < 0) { break; } context.out.write(buf, 0,
     * count); } } finally { try { in.close(); } catch (IOException e) { } } }
     */
    
    
    public static class ApplicationEvent
    {
        public ApplicationEvent(HmeEvent.ResourceInfo event, String status, Audio audio, int currentPosition, int total, int bitrate)
        {
            mEvent = event;
            mStatus = status;
            mAudio = audio;
            mCurrentPosition = currentPosition;
            mTotal = total;
            mBitrate = bitrate;
        }
        
        public HmeEvent.ResourceInfo getEvent()
        {
            return mEvent;
        }
        
        public String getStatus()
        {
            return mStatus;
        }        
        
        public Audio getAudio()
        {
            return mAudio;
        }
        
        public int getCurrentPosition() {
            return mCurrentPosition;
        }

        public int getTotal() {
            return mTotal;
        }

        public int getBitrate() {
            return mBitrate;
        }
        
        private HmeEvent.ResourceInfo mEvent;
        
        private String mStatus;
        
        private Audio mAudio;
        
        private int mCurrentPosition;

        private int mTotal;

        private int mBitrate;
    }
    
    public static interface ApplicationEventListener
    {
        public void handleApplicationEvent(ApplicationEvent appEvent);
    }
    
    public static void addApplicationEventListener(ApplicationEventListener listener)
    {
        mApplicationEventListeners.add(listener);
    }

    public static void removeApplicationEventListener(ApplicationEventListener listener)
    {
        mApplicationEventListeners.remove(listener);
    }

    protected static void fireEvent(ApplicationEvent appEvent)
    {
        Vector listeners = (Vector)mApplicationEventListeners.clone();

         for (int i = 0; i < listeners.size(); i++)
         {
             ((ApplicationEventListener)listeners.get(i)).handleApplicationEvent(appEvent);
         }
         
    }
    
    public boolean handleActive(boolean active)
    {
    	return super.handleActive(active);
    }
    
    public boolean handleIdle( boolean isIdle )
    {
    	if (isIdle)
    	{
    		if (Server.getServer().getServerConfiguration().isDisableTimeout())
    		{
    			acknowledgeIdle(true);	
        		return true;
    		}
    		else
   			if (mHandleTimeout)
   			{
    			acknowledgeIdle(true);	
        		return true;
    		}
    	}
   		return super.handleIdle(isIdle);
    }
    
    public void setHandleTimeout(boolean value)
    {
    	mHandleTimeout = value;
    }
    
    public boolean getHandleTimeout()
    {
    	return mHandleTimeout;
    }

    // TODO Need to handle multiple apps
    private Player mPlayer;

    private ArrayList mCallbacks;

    private String mCurrentTrackerContext;

    private ByteArrayOutputStream mLastObject;

    private Resource mLastResource;
    
    private static Vector mApplicationEventListeners = new Vector();
    
    private boolean mHandleTimeout;
}