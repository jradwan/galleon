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

public class PersistentValueManager {

    private static Logger log = Logger.getLogger(PersistentValueManager.class.getName());

    public static interface Callback {
        public void visit(Session session, PersistentValue persistentValue);
    }

    public static PersistentValue retrievePersistentValue(PersistentValue persistentValue) throws HibernateException {
        return retrievePersistentValue(persistentValue.getId());
    }

    public static PersistentValue retrievePersistentValue(Integer id) throws HibernateException {

        PersistentValue result = null;
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            result = (PersistentValue) session.load(PersistentValue.class, id);
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

    public static PersistentValue createPersistentValue(PersistentValue persistentValue) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(persistentValue);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return persistentValue;
    }

    public static void updatePersistentValue(PersistentValue persistentValue) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(persistentValue);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static void deletePersistentValue(PersistentValue persistentValue) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(persistentValue);
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
            list = session.find("from org.lnicholls.galleon.database.PersistentValue");
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

            Query query = session.createQuery("from org.lnicholls.galleon.database.PersistentValue");
            ScrollableResults items = query.scroll();
            int counter = start;
            if (items.first()) {
                items.scroll(start);
                while (items.next() && (counter < end)) {
                    PersistentValue persistentValue = (PersistentValue) items.get(0);
                    list.add(persistentValue);
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
            Query q = session.createQuery("from org.lnicholls.galleon.database.PersistentValue");
            ScrollableResults items = q.scroll();
            if (items.first()) {
                items.beforeFirst();
                while (items.next())
                {
                    PersistentValue persistentValue = (PersistentValue) items.get(0);
                    callback.visit(session, persistentValue);
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

    public static String findValueByName(String name) throws HibernateException {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            List list = session.find("from org.lnicholls.galleon.database.PersistentValue as PersistentValue where PersistentValue.name=?", name,
                    Hibernate.STRING);

            tx.commit();

            if (list.size()>0)
            {
                PersistentValue persistentValue = (PersistentValue)list.get(0);
                return persistentValue.getValue();
            }
            return null;
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }
    
    public static PersistentValue findByName(String name) throws HibernateException {
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();

            List list = session.find("from org.lnicholls.galleon.database.PersistentValue as PersistentValue where PersistentValue.name=?", name,
                    Hibernate.STRING);

            tx.commit();

            if (list.size()>0)
            {
                return (PersistentValue)list.get(0);
            }
            return null;
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }    
}