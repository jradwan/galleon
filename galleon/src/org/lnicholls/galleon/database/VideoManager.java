package org.lnicholls.galleon.database;

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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import net.sf.hibernate.*;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.tool.hbm2ddl.SchemaExport;

public class VideoManager {

    private static Logger log = Logger.getLogger(VideoManager.class.getName());
    
    public static interface Callback {
        public void visit(Session session, Video video);
    }

    public static Video retrieveVideo(Video Video) throws HibernateException {
        return retrieveVideo(Video.getId());
    }

    public static Video retrieveVideo(Integer id) throws HibernateException {

        Video result = null;
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            result = (Video) session.load(Video.class, id);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return result;
    }

    public static Video createVideo(Video Video) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(Video);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return Video;
    }

    public static void updateVideo(Video Video) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(Video);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static void deleteVideo(Video Video) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(Video);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static List listAll() throws HibernateException {
        List list = new ArrayList();
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            list = session.createQuery("from org.lnicholls.galleon.database.Video").list();
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return list;
    }

    public static List listBetween(int start, int end) throws HibernateException {
        List list = new ArrayList();
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            Query query = session.createQuery("from org.lnicholls.galleon.database.Video");
            ScrollableResults items = query.scroll();
            int counter = start;
            if (items.first()) {
                items.scroll(start);
                while (items.next() && (counter < end)) {
                    Video Video = (Video) items.get(0);
                    list.add(Video);
                    counter++;
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
        return list;
    }

    public static void scroll(Callback callback) throws HibernateException {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            Query q = session.createQuery("from org.lnicholls.galleon.database.Video");
            ScrollableResults items = q.scroll();
            if (items.first()) {
                items.beforeFirst();
                while (items.next())
                {
                    Video video = (Video) items.get(0);
                    callback.visit(session, video);
                };
            }
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static List findByPath(String path) throws HibernateException {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            List list = session.createQuery("from org.lnicholls.galleon.database.Video as video where video.path=?")
                    .setString(0, path).list();

            tx.commit();

            return list;
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }
}