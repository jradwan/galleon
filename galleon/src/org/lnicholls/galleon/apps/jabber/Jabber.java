package org.lnicholls.galleon.apps.jabber;
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
import java.awt.Font;
import java.awt.Point;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.apache.log4j.Logger;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.LabelText;
import org.lnicholls.galleon.widget.ScrollText;
import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BKeyboard;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.bananas.BKeyboard.KeyboardEvent;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;
public class Jabber extends DefaultApplication {
	private static Logger log = Logger.getLogger(Jabber.class.getName());
	public final static String TITLE = "Jabber";
	public final static String DEFAULT_KEYWORDS = "and;but;don't;no;or;the;yes;you;what;when;who;why";
	private Resource mMenuBackground;
	private Resource mInfoBackground;
	private Resource mOfflineIcon;
	private Resource mOnlineIcon;
	private Resource mMessageIcon;
	private Resource mDndIcon;
	public void init(IContext context) throws Exception {
		super.init(context);
		((JabberFactory) getFactory()).available();
		mMenuBackground = getSkinImage("menu", "background");
		mInfoBackground = getSkinImage("info", "background");
		mOfflineIcon = getSkinImage("menu", "offline");
		mOnlineIcon = getSkinImage("menu", "online");
		mMessageIcon = getSkinImage("menu", "message");
		mDndIcon = getSkinImage("menu", "dnd");
		push(new JabberMenuScreen(this), TRANSITION_NONE);
		initialize();
	}
	public void destroy() {
		((JabberFactory) getFactory()).unavailable();
	}
	public class JabberMenuScreen extends DefaultMenuScreen implements Notify {
		public JabberMenuScreen(Jabber app) {
			super(app, "Jabber");
			getBelow().setResource(mMenuBackground);
			updateView();
		}
		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			getBelow().setResource(mInfoBackground);
			updateView();
			((JabberFactory) getFactory()).registerPresenceListener(this);
			return super.handleEnter(arg, isReturn);
		}
		public boolean handleExit() {
			((JabberFactory) getFactory()).unregisterPresenceListener(this);
			return super.handleExit();
		}
		public void update()
		{
			updateView();
		}
		public void updateView() {
			List roster = ((JabberFactory) getFactory()).getRoster();
			if (roster != null) {
				int pos = mMenuList.getFocus();
				Iterator iterator = roster.iterator();
				while (iterator.hasNext()) {
					RosterEntry rosterEntry = (RosterEntry) iterator.next();
					((JabberFactory) getFactory())
							.unregisterMessageListener(rosterEntry.getUser());
				}
				mMenuList.clear();
				iterator = roster.iterator();
				while (iterator.hasNext()) {
					RosterEntry rosterEntry = (RosterEntry) iterator.next();
					mMenuList.add(rosterEntry);
					((JabberFactory) getFactory()).registerMessageListener(
							rosterEntry.getUser(), this);
				}
				mMenuList.setFocus(pos, false);
			}
			getBApp().flush();
		}
		public boolean handleAction(BView view, Object action) {
			if (action.equals("push")) {
				JabberConfiguration jabberConfiguration = (JabberConfiguration) ((JabberFactory) getFactory())
				.getAppContext().getConfiguration();
				RosterEntry rosterEntry = (RosterEntry) mMenuList.get(mMenuList
						.getFocus());
				JabberChatScreen jabberChatScreen = new JabberChatScreen(
						(Jabber) getBApp(), rosterEntry);
				getBApp().push(jabberChatScreen, TRANSITION_LEFT);
				getBApp().flush();
				return true;
			}
			return super.handleAction(view, action);
		}
		protected void createRow(BView parent, int index) {
			RosterEntry rosterEntry = (RosterEntry) mMenuList.get(index);
			BView icon = new BView(parent, 10, 3, 30, 30);
			List presence = ((JabberFactory) getFactory()).getPresence();
			Presence pres = (Presence) presence.get(index);
			if (pres != null && pres.getType() != null
					&& pres.getType() == Presence.Type.AVAILABLE)
			{
				// TODO Support other modes
				if (pres.getMode() != null
						&& pres.getMode() == Presence.Mode.DO_NOT_DISTURB)
					icon.setResource(mDndIcon);
				else
					icon.setResource(mOnlineIcon);
			}
			else
				icon.setResource(mOfflineIcon);
			BText name = new BText(parent, 50, 4, parent.getWidth() - 40,
					parent.getHeight() - 4);
			name.setShadow(true);
			name.setFlags(RSRC_HALIGN_LEFT);
			boolean hasMessage = ((JabberFactory) getFactory())
					.hasMessage(rosterEntry.getUser());
			if (hasMessage)
				name.setFont("default-24-italic.font");
			else
				name.setFont("default-24.font");
			String value = rosterEntry.getName();
			if (value == null || value.trim().length() == 0)
				value = rosterEntry.getUser();
			name.setValue(value);
		}
	}
	public class JabberChatScreen extends DefaultScreen implements Notify {
		public JabberChatScreen(Jabber app, RosterEntry rosterEntry) {
			super(app, false);
			setFooter("Press PLAY to send message, ADVANCE to accept suggestion");
			String value = rosterEntry.getName();
			if (value == null || value.trim().length() == 0)
			{
				value = rosterEntry.getUser();
				StringTokenizer tokenizer = new StringTokenizer(value, "@/");
				value = tokenizer.nextToken();
			}
			setTitle(value);
			mRosterEntry = rosterEntry;
			getBelow().setResource(mInfoBackground);
			int start = TOP;
			Point p = BKeyboard
					.getKeyboardSize(
							BKeyboard
									.getStandardKeyboard(BKeyboard.STANDARD_KEYBOARD_LOWERCASE),
							false, BKeyboard.INPUT_WIDTH_SAME_AS_WIDGET);
			mKeyboard = new BKeyboard(
					getNormal(),
					BORDER_LEFT,
					TOP,
					p.x,
					p.y,
					BKeyboard
					.getStandardKeyboard(BKeyboard.STANDARD_KEYBOARD_LOWERCASE),
					false,
					BKeyboard.INPUT_WIDTH_SAME_AS_WIDGET, true);
			setFocus(mKeyboard);
			mScrollText = new ScrollText(getNormal(), BORDER_LEFT + p.x + 10,
					TOP, getWidth()
							- (p.x + 10 + BORDER_LEFT + SAFE_TITLE_H + 10),
					275, "");
			mScrollText.setFont(ScrollText.DEFAULT_FONT.deriveFont(Font.BOLD,
					24));
			mScrollText.setColor(Color.WHITE);
			BButton button = new BButton(getNormal(), SAFE_TITLE_H + 10,
					(getHeight() - SAFE_TITLE_V) - 40, (int) Math
					.round((getWidth() - (SAFE_TITLE_H * 2)) / 2.5), 35);
			button.setResource(createText("default-24.font", Color.white,
					"Return to menu"));
			button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, H_UP,
					null, true);
			button.setFocusable(true);
			mText = new LabelText(getNormal(), BORDER_LEFT,
					(getHeight() - SAFE_TITLE_V) - 80, BODY_WIDTH / 2, 20, true);
			mText.setFlags(RSRC_HALIGN_LEFT);
			mText.setFont("default-18.font");
			mText.setShadow(true);
			mText.setColor(Color.YELLOW);
			mText.setLabel("Suggestion: ");
		}
		public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
			getBelow().setResource(mInfoBackground);
			updateView();
			if (!isReturn)
				((JabberFactory) getFactory()).registerMessageListener(
						mRosterEntry.getUser(), this);
			return super.handleEnter(arg, isReturn);
		}
		public boolean handleExit() {
			((JabberFactory) getFactory())
					.unregisterMessageListener(mRosterEntry.getUser());
			((JabberFactory) getFactory())
					.clearMessages(mRosterEntry.getUser());
			return super.handleExit();
		}
		public void update()
		{
			play("slowdown1.snd");
			updateView();
		}
		public void updateView() {
			List messages = ((JabberFactory) getFactory())
					.getMessages(mRosterEntry.getUser());
			StringBuffer buffer = new StringBuffer();
			if (messages != null) {
				Iterator iterator = messages.iterator();
				while (iterator.hasNext()) {
					Object object = iterator.next();
					if (object instanceof String)
					{
						String message = (String) object;
						buffer.insert(0, message + "\n\n");
					}
					else
					{
						Message message = (Message) object;
						buffer.insert(0, "> " + message.getBody() + "\n\n");
					}
				}
				mScrollText.setText(buffer.toString());
			}
			getBApp().flush();
		}
		public boolean handleAction(BView view, Object action) {
			if ("play".equals(action)) {
				if (mKeyboard.getValue().trim().length() > 0)
				{
					play("speedup1.snd");
					((JabberFactory) getFactory()).sendMessage(mRosterEntry
							.getUser(), mKeyboard.getValue());
					updateKeywords(mKeyboard.getValue());
					mKeyboard.setValue("");
					mText.setValue("");
					updateView();
				}
				return true;
			} else if ("left".equals(action)) {
				getBApp().pop();
				return true;
			}
			return super.handleAction(view, action);
		}
		private void updateKeywords(String message)
		{
			String words = DEFAULT_KEYWORDS;
			PersistentValue persistentValue = PersistentValueManager
					.loadPersistentValue(Jabber.class.getName() + "." + "words");
			if (persistentValue != null)
			{
				words = persistentValue.getValue();
			}
			else
			{
				PersistentValueManager.savePersistentValue(Jabber.class
						.getName()
						+ "." + "words", words);
			}
			StringTokenizer newTokenizer = new StringTokenizer(message, " ");
			StringTokenizer existingTokenizer = new StringTokenizer(words, ";");
			int count = newTokenizer.countTokens()
					+ existingTokenizer.countTokens();
			String[] array = new String[count];
			int pos = 0;
			while (newTokenizer.hasMoreTokens())
			{
				array[pos++] = clean(newTokenizer.nextToken());
			}
			while (existingTokenizer.hasMoreTokens())
			{
				array[pos++] = existingTokenizer.nextToken();
			}
			Arrays.sort(array, new Comparator() {
				public int compare(Object o1, Object o2) {
					String value1 = (String) o1;
					String value2 = (String) o2;
					return value1.compareTo(value2);
				}
			});
			String result = "";
			for (int i = 0; i < count; i++)
			{
				if (i == 0)
					result = array[i];
				else
					result = result + ";" + array[i];
			}
			PersistentValueManager.savePersistentValue(Jabber.class.getName()
					+ "." + "words", result);
		}
		private String clean(String value)
		{
			value = value.replaceAll(";", "");
			value = value.replaceAll(",", "");
			value = value.replaceAll(":", "");
			return value;
		}
		public boolean handleKeyPress(int code, long rawcode) {
			switch (code) {
			case KEY_ADVANCE:
				String current = mKeyboard.getValue();
				if (current.trim().length() > 0 && mClosest != null)
				{
					getBApp().play("select.snd");
					getBApp().flush();
					int pos = current.lastIndexOf(" ");
					if (pos == -1)
					{
						mKeyboard.setValue(mClosest + " ");
					}
					else
					{
						String before = current.substring(0, pos);
						mKeyboard.setValue(before + " " + mClosest + " ");
					}
				}
				return true;
			case KEY_UP:
				setFocus(mKeyboard);
				return true;
			case KEY_LEFT:
				getBApp().pop();
				return true;
			case KEY_SELECT:
				postEvent(new BEvent.Action(this, "pop"));
				return true;
			case KEY_CHANNELUP:
			case KEY_CHANNELDOWN:
				return mScrollText.handleKeyPress(code, rawcode);
			case KEY_PLAY:
				postEvent(new BEvent.Action(this, "play"));
				return true;
			}
			return super.handleKeyPress(code, rawcode);
		}
		public boolean handleEvent(HmeEvent event) {
			boolean result = super.handleEvent(event);
			if (event instanceof KeyboardEvent) {
				final KeyboardEvent localEvent = (KeyboardEvent) event;
				String message = localEvent.getValue();
				if (message != null)
				{
					if (message.trim().length() > 0
							&& message.charAt(message.length() - 1) != ' ')
					{
						String current = message;
						StringTokenizer tokenizer = new StringTokenizer(
								message, " ");
						while (tokenizer.hasMoreTokens())
							current = tokenizer.nextToken();
						String words = DEFAULT_KEYWORDS;
						PersistentValue persistentValue = PersistentValueManager
								.loadPersistentValue(Jabber.class.getName()
										+ "." + "words");
						if (persistentValue != null)
						{
							words = persistentValue.getValue();
						}
						else
						{
							PersistentValueManager.savePersistentValue(
									Jabber.class.getName() + "." + "words",
									words);
						}
						mClosest = null;
						tokenizer = new StringTokenizer(words, ";");
						while (tokenizer.hasMoreTokens())
						{
							String value = tokenizer.nextToken();
							if (value.startsWith(current))
							{
								mClosest = value;
								mText.setValue(mClosest);
								break;
							}
						}
						if (mClosest == null)
						{
							mText.setValue("");
						}
					}
				}
			}
			return result;
		}
		private RosterEntry mRosterEntry;
		private ScrollText mScrollText;
		private BKeyboard mKeyboard;
		private LabelText mText;
		private String mClosest;
	}
	public static class JabberFactory extends AppFactory {
		public void updateAppContext(AppContext appContext) {
			super.updateAppContext(appContext);
			unavailable();
			disconnect();
			connect();
			unavailable();
		}
		public void initialize() {
			new Thread() {
				public void run() {
					connect();
					unavailable();
				}
			}.start();
		}
		public void remove() {
			unavailable();
			disconnect();
		}
		public InputStream getStream(String uri) throws IOException {
			if (uri.toLowerCase().equals("icon.png")) {
				if (mMessages != null && mMessages.size() > 0)
					return super.getStream("message.png");
			}
			return super.getStream(uri);
		}
		private boolean isConnected() {
			try {
				return mConnection != null && mConnection.isConnected();
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex,
						"Could not connect to jabber");
			}
			return false;
		}
		private void connect() {
			JabberConfiguration jabberConfiguration = (JabberConfiguration) getAppContext()
					.getConfiguration();
			try {
				if (isConnected())
					disconnect();
				mRoster = new ArrayList();
				mPresence = new ArrayList();
				mConversations = new HashMap();
				mMessages = new ArrayList();
				mMessageListeners = new HashMap();
				mPresenceListeners = new ArrayList();
				mConnection = new XMPPConnection(jabberConfiguration
						.getServer());
				mConnection.addPacketListener(new PacketListener() {
					public void processPacket(Packet packet) {
						if (packet instanceof Message) {
							Message message = (Message) packet;
							StringTokenizer tokenizer = new StringTokenizer(
									message.getFrom(), "/");
							String from = tokenizer.nextToken();
							List messages = (List) mConversations.get(from);
							if (messages == null)
								messages = new ArrayList();
							messages.add(message);
							mConversations.put(from, messages);
							mMessages.add(message);
							fireMessageListeners(from);
						}
					}
				}, new PacketTypeFilter(Message.class));
				mConnection.login(jabberConfiguration.getUsername(), Tools
						.decrypt(jabberConfiguration.getPassword()),
				"TiVo");
				// Presence presence = new Presence(Presence.Type.AVAILABLE);
				// presence.setStatus("Messaging with Galleon");
				// mConnection.sendPacket(presence);
				final Roster roster = mConnection.getRoster();
				for (Iterator i = roster.getEntries(); i.hasNext();) {
					RosterEntry rosterEntry = (RosterEntry) i.next();
					mRoster.add(rosterEntry);
					mPresence.add(roster.getPresence(rosterEntry.getUser()));
				}
				roster.addRosterListener(new RosterListener() {
					public void entriesAdded(Collection addresses) {
					}
					public void entriesUpdated(Collection addresses) {
					}
					public void entriesDeleted(Collection addresses) {
					}
					public void presenceChanged(String user) {
						for (int i = 0; i < mRoster.size(); i++) {
							RosterEntry rosterEntry = (RosterEntry) mRoster
									.get(i);
							StringTokenizer tokenizer = new StringTokenizer(
									user, "/");
							String email = tokenizer.nextToken();
							if (rosterEntry.getUser().equals(email)) {
								Presence presence = roster.getPresence(user);
								mPresence.set(i, presence);
								firePresenceListeners();
								return;
							}
						}
					}
				});
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex,
						"Could not connect to jabber");
			}
		}
		private void disconnect() {
			try {
				if (mRoster != null) {
					mRoster.clear();
					mRoster = null;
				}
				if (mPresence != null) {
					mPresence.clear();
					mPresence = null;
				}
				if (mConversations != null) {
					mConversations.clear();
					mConversations = null;
				}
				if (mMessages != null) {
					mMessages.clear();
					mMessages = null;
				}
				if (mMessageListeners != null) {
					mMessageListeners.clear();
					mMessageListeners = null;
				}
				if (mPresenceListeners != null) {
					mPresenceListeners.clear();
					mPresenceListeners = null;
				}
				if (mConnection != null)
					mConnection.close();
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex,
						"Could not disconnect jabber");
			}
		}
		public List getRoster() {
			return mRoster;
		}
		public List getPresence() {
			return mPresence;
		}
		public HashMap getConversations() {
			return mConversations;
		}
		public void available() {
			try {
				if (!isConnected())
					connect();
				if (mConnection != null) {
					Presence presence = new Presence(Presence.Type.AVAILABLE);
					presence.setStatus("Messaging with Galleon");
					mConnection.sendPacket(presence);
				}
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex, "Could not set available");
			}
		}
		public void unavailable() {
			try {
				if (mConnection != null) {
					Presence presence = new Presence(Presence.Type.UNAVAILABLE);
					presence.setStatus("");
					mConnection.sendPacket(presence);
				}
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex,
						"Could not set unavailable");
			}
		}
		public boolean hasMessage(String user) {
			if (mMessages != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				Iterator iterator = mMessages.iterator();
				while (iterator.hasNext()) {
					Message message = (Message) iterator.next();
					tokenizer = new StringTokenizer(message.getFrom(), "/");
					String from = tokenizer.nextToken();
					if (from.equals(email))
						return true;
				}
			}
			return false;
		}
		public void sendMessage(String user, String message) {
			try
			{
				Chat chat = mConnection.createChat(user);
				chat.sendMessage(message);
			} catch (Exception ex) {
				Tools.logException(Jabber.class, ex, "Could not chat to: "
						+ user);
			}
			if (mConversations != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				List conversation = (List) mConversations.get(email);
				if (conversation == null)
				{
					conversation = new ArrayList();
					mConversations.put(email, conversation);
				}
				conversation.add(message);
			}
		}
		public void clearMessages(String user)
		{
			StringTokenizer tokenizer = new StringTokenizer(user, "/");
			String email = tokenizer.nextToken();
			if (mConversations != null) {
				List list = (List) mConversations.get(email);
				if (list != null)
					list.clear();
			}
			if (mMessages!=null)
			{
				ArrayList messages = new ArrayList();
	
				Iterator iterator = mMessages.listIterator();
	
				while (iterator.hasNext()) {
	
					Message message = (Message) iterator.next();
	
					tokenizer = new StringTokenizer(message.getFrom(), "/");
	
					String from = tokenizer.nextToken();
	
					if (!from.equals(email))
	
						messages.add(message);
	
				}
	
				mMessages.clear();
	
				mMessages = messages;
			}
		}
		
		public List getMessages(String user) {
			if (mConversations != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				return (List) mConversations.get(email);
			}
			return null;
		}
		public void registerMessageListener(String user, Notify notify) {
			if (user != null && mMessageListeners != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				mMessageListeners.put(email, notify);
			}
		}
		public void unregisterMessageListener(String user) {
			if (user != null && mMessageListeners != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				mMessageListeners.remove(email);
			}
		}
		public void registerPresenceListener(Notify notify) {
			if (mPresenceListeners != null) {
				mPresenceListeners.add(notify);
			}
		}
		public void unregisterPresenceListener(Notify notify) {
			if (mPresenceListeners != null) {
				mPresenceListeners.remove(notify);
			}
		}
		private void fireMessageListeners(String user) {
			if (user != null && mMessageListeners != null) {
				StringTokenizer tokenizer = new StringTokenizer(user, "/");
				String email = tokenizer.nextToken();
				Notify notify = (Notify) mMessageListeners.get(email);
				if (notify != null)
					notify.update();
			}
		}
		private void firePresenceListeners() {
			if (mPresenceListeners != null) {
				Iterator iterator = mPresenceListeners.iterator();
				while (iterator.hasNext()) {
					Notify notify = (Notify) iterator.next();
					notify.update();
				}
			}
		}
		private XMPPConnection mConnection;
		private List mRoster;
		private List mPresence;
		private List mMessages;
		private HashMap mConversations;
		private List mPresenceListeners;
		private HashMap mMessageListeners;
	}
	interface Notify {
		public void update();
	}
}
