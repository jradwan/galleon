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

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;

import com.sun.image.codec.jpeg.ImageFormatException;
import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGImageDecoder;

import net.sf.hibernate.*;
import net.sf.hibernate.Query;
import net.sf.hibernate.Session;
import net.sf.hibernate.SessionFactory;
import net.sf.hibernate.Transaction;
import net.sf.hibernate.cfg.Configuration;
import net.sf.hibernate.tool.hbm2ddl.SchemaExport;

public class ThumbnailManager {

    private static Logger log = Logger.getLogger(ThumbnailManager.class.getName());

    public static interface Callback {
        public void visit(Session session, Thumbnail thumbnail);
    }

    public static Thumbnail retrieveThumbnail(Thumbnail thumbnail) throws HibernateException {
        return retrieveThumbnail(thumbnail.getId());
    }

    public static Thumbnail retrieveThumbnail(Integer id) throws HibernateException {

        Thumbnail result = null;
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            result = (Thumbnail) session.load(Thumbnail.class, id);
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

    public static Thumbnail createThumbnail(Thumbnail thumbnail) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(thumbnail);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return thumbnail;
    }

    public static void updateThumbnail(Thumbnail thumbnail) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(thumbnail);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static void deleteThumbnail(Thumbnail thumbnail) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(thumbnail);
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
            list = session.find("from org.lnicholls.galleon.database.Thumbnail");
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

            Query query = session.createQuery("from org.lnicholls.galleon.database.Thumbnail");
            ScrollableResults items = query.scroll();
            int counter = start;
            if (items.first()) {
                items.scroll(start);
                while (items.next() && (counter < end)) {
                    Thumbnail thumbnail = (Thumbnail) items.get(0);
                    list.add(thumbnail);
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
            Query q = session.createQuery("from org.lnicholls.galleon.database.Thumbnail");
            ScrollableResults items = q.scroll();
            if (items.first()) {
                items.beforeFirst();
                while (items.next())
                {
                    Thumbnail thumbnail = (Thumbnail) items.get(0);
                    callback.visit(session, thumbnail);
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

            List list = session.find("from org.lnicholls.galleon.database.Thumbnail as thumbnail where thumbnail.path=?", path,
                    Hibernate.STRING);

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
    
    public static Image findImageByPath(String path) throws HibernateException {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            List list = session.find("from org.lnicholls.galleon.database.Thumbnail as thumbnail where thumbnail.path=?", path,
                    Hibernate.STRING);
            
            Image image = null;
            if (list.size()>0)
            {
                Thumbnail thumbnail = (Thumbnail)list.get(0);
                Blob blob = thumbnail.getImage();
                try {
                    InputStream is = blob.getBinaryStream();
                    JPEGImageDecoder decoder = JPEGCodec.createJPEGDecoder(is);
                    image = decoder.decodeAsBufferedImage();
                } catch (ImageFormatException ex) {
                    Tools.logException(ThumbnailManager.class, ex);
                } catch (SQLException ex) {
                    Tools.logException(ThumbnailManager.class, ex);
                } catch (IOException ex) {
                    Tools.logException(ThumbnailManager.class, ex);
                }
            }            

            tx.commit();

            return image;
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }        
    }    
    
}