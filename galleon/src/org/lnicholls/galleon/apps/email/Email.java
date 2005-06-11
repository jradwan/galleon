package org.lnicholls.galleon.apps.email;

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
import java.io.IOException;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppContext;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.apps.email.EmailConfiguration.Account;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.ReloadCallback;
import org.lnicholls.galleon.util.ReloadTask;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultScreen;
import org.lnicholls.galleon.widget.ScrollText;

import com.tivo.hme.bananas.BButton;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BList;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.http.server.HttpRequest;
import com.tivo.hme.sdk.Resource;
import com.tivo.hme.util.ArgumentList;

public class Email extends DefaultApplication {

    private static Logger log = Logger.getLogger(Email.class.getName());

    public final static String TITLE = "Email";

    private Resource mMenuBackground;

    private Resource mInfoBackground;

    private Resource mViewerBackground;

    private Resource mFolderIcon;

    private Resource mItemIcon;

    protected void init(Context context) {
        super.init(context);

        mMenuBackground = getSkinImage("menu", "background");
        mInfoBackground = getSkinImage("info", "background");
        mViewerBackground = getSkinImage("viewer", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        mItemIcon = getSkinImage("menu", "item");

        EmailConfiguration emailConfiguration = (EmailConfiguration) ((EmailFactory) getContext().getFactory()).getAppContext()
                .getConfiguration();

        if (emailConfiguration.getAccounts().size() == 1) {
            Account account = (Account) emailConfiguration.getAccounts().get(0);
            List mail = (List) ((EmailFactory) getContext().getFactory()).mAccounts.get(account.getName());
            push(new EmailAccountMenuScreen(this, account, mail, true), TRANSITION_NONE);
        } else
            push(new EmailMenuScreen(this), TRANSITION_NONE);
    }

    public class EmailMenuScreen extends DefaultMenuScreen {
        public EmailMenuScreen(Email app) {
            super(app, "Email");

            getBelow().setResource(mMenuBackground);

            EmailConfiguration emailConfiguration = (EmailConfiguration) ((EmailFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();
            List accounts = emailConfiguration.getAccounts();
            Account[] feedArray = (Account[]) accounts.toArray(new Account[0]);
            Arrays.sort(feedArray, new Comparator() {
                public int compare(Object o1, Object o2) {
                    Account account1 = (Account) o1;
                    Account account2 = (Account) o2;

                    return -account1.getName().compareTo(account2.getName());
                }
            });

            for (int i = 0; i < feedArray.length; i++) {
                Account account = (Account) feedArray[i];
                mMenuList.add(account);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                Account account = (Account) mMenuList.get(mMenuList.getFocus());

                List stories = (List) ((EmailFactory) getContext().getFactory()).mAccounts.get(account.getName());
                EmailAccountMenuScreen emailAccountMenuScreen = new EmailAccountMenuScreen((Email) getBApp(), account,
                        stories);
                getBApp().push(emailAccountMenuScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            icon.setResource(mFolderIcon);

            Account account = (Account) mMenuList.get(index);
            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(account.getName(), 40));
        }
    }

    public class EmailAccountMenuScreen extends DefaultMenuScreen {
        
        public EmailAccountMenuScreen(Email app, Account account, List list) {
            this(app, account, list, false);
        }
        
        public EmailAccountMenuScreen(Email app, Account account, List list, boolean first) {
            super(app, null);
            
            mFirst = first;

            getBelow().setResource(mMenuBackground);

            EmailConfiguration emailConfiguration = (EmailConfiguration) ((EmailFactory) getContext().getFactory())
                    .getAppContext().getConfiguration();

            setTitle(account.getName());

            for (int i = 0; i < list.size(); i++) {
                EmailItem item = (EmailItem) list.get(i);
                mMenuList.add(item);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push")) {
                load();
                EmailItem item = (EmailItem) mMenuList.get(mMenuList.getFocus());

                EmailScreen rssScreen = new EmailScreen((Email) getBApp(), item);
                getBApp().push(rssScreen, TRANSITION_LEFT);
                getBApp().flush();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 10, 3, 30, 30);
            icon.setResource(mItemIcon);

            EmailItem item = (EmailItem) mMenuList.get(index);
            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(cleanHTML(item.getSubject()), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_LEFT:
                if (!mFirst)
                {
                    postEvent(new BEvent.Action(this, "pop"));
                    return true;
                }
            }
            return super.handleKeyPress(code, rawcode);
        }

        private BView mImage;
        
        private boolean mFirst;
    }

    public class EmailScreen extends DefaultScreen {

        public EmailScreen(Email app, EmailItem item) {
            super(app, Tools.trim(item.getSubject(), 28), false);

            getBelow().setResource(mInfoBackground);

            int start = TOP - 30;

            BText fromText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
            fromText.setFlags(RSRC_HALIGN_LEFT);
            fromText.setFont("default-24-bold.font");
            fromText.setColor(Color.GREEN);
            fromText.setShadow(Color.black, 2);
            fromText.setValue("From: " + item.getFrom());

            start += 30;

            SimpleDateFormat dateFormat = new SimpleDateFormat();
            dateFormat.applyPattern("EEE M/dd H:mm");

            BText dateText = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 20);
            dateText.setFlags(RSRC_HALIGN_LEFT);
            dateText.setFont("default-18-bold.font");
            dateText.setColor(Color.GREEN);
            dateText.setShadow(true);
            dateText.setValue("Date: " + dateFormat.format(item.getDate()));

            start += 25;

            mScrollText = new ScrollText(getNormal(), SAFE_TITLE_H, start, BODY_WIDTH - 10, getHeight() - 2 * SAFE_TITLE_V - 193,
                    cleanHTML(item.getBody()));

            /*
             * mList = new DefaultOptionList(this.getNormal(), SAFE_TITLE_H, (getHeight() - SAFE_TITLE_V) - 40, (width -
             * (SAFE_TITLE_H * 2)) / 2, 90, 35); mList.add("Back to menu"); setFocusDefault(mList);
             */

            BButton button = new BButton(getNormal(), SAFE_TITLE_H + 10, (getHeight() - SAFE_TITLE_V) - 55, (int) Math
                    .round((getWidth() - (SAFE_TITLE_H * 2)) / 2), 35);
            button.setResource(createText("default-24.font", Color.white, "Return to menu"));
            button.setBarAndArrows(BAR_HANG, BAR_DEFAULT, "pop", null, null, null, true);
            setFocus(button);
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch (code) {
            case KEY_SELECT:
                postEvent(new BEvent.Action(this, "pop"));
                return true;
            case KEY_UP:
            case KEY_DOWN:
            case KEY_CHANNELUP:
            case KEY_CHANNELDOWN:
                return mScrollText.handleKeyPress(code, rawcode);
            }
            return super.handleKeyPress(code, rawcode);
        }

        private BList mList;

        private ScrollText mScrollText;
    }

    private static class EmailItem {
        public EmailItem(String subject, String from, String body, Date date) {
            mSubject = subject;
            mFrom = from;
            mBody = body;
            mDate = date;
        }

        public String getSubject() {
            return mSubject;
        }

        public String getFrom() {
            return mFrom;
        }

        public String getBody() {
            return mBody;
        }

        public Date getDate() {
            return mDate;
        }

        private String mSubject = "";

        private String mFrom;

        private String mBody;

        private Date mDate;
    }

    public static class EmailFactory extends AppFactory {

        final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";

        public EmailFactory(AppContext appContext) {
            super(appContext);

            Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());
            Security.setProperty("ssl.SocketFactory.provider", "org.lnicholls.galleon.util.DummySSLSocketFactory");

            EmailConfiguration emailConfiguration = (EmailConfiguration) getAppContext().getConfiguration();
            Server.getServer().scheduleShortTerm(new ReloadTask(new ReloadCallback() {
                public void reload() {
                    try {
                        updateAccounts();
                    } catch (Exception ex) {
                        log.error("Could not download email", ex);
                    }
                }
            }), emailConfiguration.getReload());
        }

        public void setAppContext(AppContext appContext) {
            super.setAppContext(appContext);

            updateAccounts();
        }

        private void updateAccounts() {
            final EmailConfiguration emailConfiguration = (EmailConfiguration) getAppContext().getConfiguration();

            new Thread(){
                public void run()
                {
                    Iterator iterator = emailConfiguration.getAccounts().iterator();
                    while (iterator.hasNext()) {
                        Account account = (Account) iterator.next();
                        List mail = (List) mAccounts.get(account.getName());
                        if (mail == null) {
                            mail = new ArrayList();
                            mAccounts.put(account.getName(), mail);
                        }
                        try {
                            Properties props = new Properties();
        
                            if (account.getProtocol().equals("pop3s")) {
                                props.setProperty("mail.pop3.socketFactory.class", SSL_FACTORY);
                                props.setProperty("mail.pop3.socketFactory.fallback", "false");
                                props.setProperty("mail.pop3.port", "995");
                                props.setProperty("mail.pop3.socketFactory.port", "995");
                            } else if (account.getProtocol().equals("imaps")) {
                                props.setProperty("mail.imap.socketFactory.class", SSL_FACTORY);
                                props.setProperty("mail.imap.socketFactory.fallback", "true");
                                props.setProperty("mail.imap.port", "993");
                                props.setProperty("mail.imap.socketFactory.port", "993");
                            }
        
                            Session session = Session.getDefaultInstance(props, null);
                            Store store = session.getStore(account.getProtocol());
                            store.connect(account.getServer(), Tools.decrypt(account.getUsername()), Tools.decrypt(account
                                    .getPassword()));
                            // TODO Make this configurable too?
                            Folder folder = store.getFolder("INBOX");
                            folder.open(Folder.READ_ONLY);
        
                            // Get stats
                            int countEmail = folder.getMessageCount();
                            // TODO Should we look at this first?
                            int newCount = folder.getNewMessageCount();
                            int unreadCount = folder.getUnreadMessageCount();
        
                            mail.clear();
        
                            int count = 0;
                            for (int i = 1; i <= countEmail; i++) {
                                boolean unread = false;
        
                                Message message = folder.getMessage(i);
                                String[] statusHeader = message.getHeader("Status");
        
                                count = count + 1;
        
                                if (statusHeader != null && statusHeader.length > 0) {
                                    if (statusHeader[0].equals("")) {
                                        // New message
                                        unread = true;
                                    } else if (statusHeader[0].equals("O")) {
                                        // Unread message
                                        unread = true;
                                    }
                                } else {
                                    if (message.isSet(Flags.Flag.RECENT)) {
                                        // New message
                                        unread = true;
                                    } else if (!message.isSet(Flags.Flag.SEEN)) {
                                        // Unread message
                                        unread = true;
                                    }
                                }
        
                                if (unread) {
                                    String title = message.getSubject() == null ? "none" : message.getSubject();
                                    String from = "";
                                    String description = "";
                                    Address[] address;
                                    if ((address = message.getFrom()) != null) {
                                        if (address.length > 0) {
                                            from = address[0].toString();
                                        }
                                    }
                                    if (message.isMimeType("text/plain")) {
                                        description = (String) message.getContent();
                                    } else if (message.isMimeType("multipart/*")) {
                                        Multipart mp = (Multipart) message.getContent();
                                        for (int p = 0; p < mp.getCount(); p++) {
                                            Part part = mp.getBodyPart(p);
                                            if (part.isMimeType("text/plain")) {
                                                description = (String) part.getContent();
                                                break;
                                            }
                                        }
                                    }
                                    EmailItem emailItem = new EmailItem(message.getSubject(), from, description, message
                                            .getSentDate());
                                    mail.add(emailItem);
                                }
                            }
                            folder.close(false);
        
                            store.close();
                        } catch (Exception ex) {
                            Tools.logException(Email.class, ex, "Could not reload email");
                        }
                    }
                }
            }.start();
        }

        protected void init(ArgumentList args) {
            super.init(args);
        }
        
        public void handleHTTP(HttpRequest http, String uri) throws IOException {
            if (uri.equals("icon.png")) {
                EmailConfiguration emailConfiguration = (EmailConfiguration) getAppContext().getConfiguration();

                boolean hasMail = false;
                Iterator iterator = emailConfiguration.getAccounts().iterator();
                while (iterator.hasNext()) {
                    Account account = (Account) iterator.next();
                    List mail = (List) mAccounts.get(account.getName());
                    if (mail!=null && mail.size()>0)
                    {
                        hasMail = true;
                        break;
                    }
                }
                
                if (hasMail) {
                    super.handleHTTP(http, "alerticon.png");
                    return;
                }
            }
            super.handleHTTP(http, uri);
        }

        private static Hashtable mAccounts = new Hashtable();
    }

    private static String cleanHTML(String data) {
        String result = "";
        data = data.replaceAll("\n", " ");
        int pos1 = data.indexOf("<");
        if (pos1 != -1) {
            while (pos1 != -1) {
                int pos2 = data.indexOf(">");
                if (pos2 == -1) {
                    result = result + data;
                    break;
                }
                result = result + data.substring(0, pos1);
                data = data.substring(pos2 + 1);
                pos1 = data.indexOf("<");
            }
        } else
            result = data;
        return StringEscapeUtils.unescapeHtml(result);
    }
}