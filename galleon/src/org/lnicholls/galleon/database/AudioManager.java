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

public class AudioManager {

    private static Logger log = Logger.getLogger(AudioManager.class.getName());

    public static interface Callback {
        public void visit(Session session, Audio audio);
    }

    public static Audio retrieveAudio(Audio audio) throws HibernateException {
        return retrieveAudio(audio.getId());
    }

    public static Audio retrieveAudio(Integer id) throws HibernateException {

        Audio result = null;
        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            result = (Audio) session.load(Audio.class, id);
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

    public static Audio createAudio(Audio audio) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.save(audio);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
        return audio;
    }

    public static void updateAudio(Audio audio) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.update(audio);
            tx.commit();
        } catch (HibernateException he) {
            if (tx != null)
                tx.rollback();
            throw he;
        } finally {
            HibernateUtil.closeSession();
        }
    }

    public static void deleteAudio(Audio audio) throws HibernateException {

        Session session = HibernateUtil.openSession();
        Transaction tx = null;
        try {
            tx = session.beginTransaction();
            session.delete(audio);
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
            list = session.find("from org.lnicholls.galleon.database.Audio");
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

            Query query = session.createQuery("from org.lnicholls.galleon.database.Audio");
            ScrollableResults items = query.scroll();
            int counter = start;
            if (items.first()) {
                items.scroll(start);
                while (items.next() && (counter < end)) {
                    Audio audio = (Audio) items.get(0);
                    list.add(audio);
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
            Query q = session.createQuery("from org.lnicholls.galleon.database.Audio");
            ScrollableResults items = q.scroll();
            if (items.first()) {
                items.beforeFirst();
                while (items.next())
                {
                    Audio audio = (Audio) items.get(0);
                    callback.visit(session, audio);
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

            List list = session.find("from org.lnicholls.galleon.database.Audio as Audio where Audio.path=?", path,
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

    /*
     * public List listAllBlogNamesAndItemCounts(int max) throws HibernateException {
     * 
     * Session session = HibernateUtil.openSession(); Transaction tx = null; List result = null; try { tx =
     * session.beginTransaction(); Query q = session.createQuery("select blog.id, blog.name, count(blogItem) " + "from
     * Blog as blog " + "left outer join blog.items as blogItem " + "group by blog.name, blog.id " + "order by
     * max(blogItem.datetime)"); q.setMaxResults(max); result = q.list(); tx.commit(); } catch (HibernateException he) {
     * if (tx != null) tx.rollback(); throw he; } finally { HibernateUtil.closeSession(); } return result; }
     * 
     * public Blog getBlogAndAllItems(Long blogid) throws HibernateException {
     * 
     * Session session = HibernateUtil.openSession(); Transaction tx = null; Blog blog = null; try { tx =
     * session.beginTransaction(); Query q = session.createQuery("from Blog as blog " + "left outer join fetch
     * blog.items " + "where blog.id = :blogid"); q.setParameter("blogid", blogid); blog = (Blog) q.list().get(0);
     * tx.commit(); } catch (HibernateException he) { if (tx != null) tx.rollback(); throw he; } finally {
     * HibernateUtil.closeSession(); } return blog; }
     * 
     * public List listBlogsAndRecentItems() throws HibernateException {
     * 
     * Session session = HibernateUtil.openSession(); Transaction tx = null; List result = null; try { tx =
     * session.beginTransaction(); Query q = session.createQuery("from Blog as blog " + "inner join blog.items as
     * blogItem " + "where blogItem.datetime > :minDate");
     * 
     * Calendar cal = Calendar.getInstance(); cal.roll(Calendar.MONTH, false); q.setCalendar("minDate", cal);
     * 
     * result = q.list(); tx.commit(); } catch (HibernateException he) { if (tx != null) tx.rollback(); throw he; }
     * finally { HibernateUtil.closeSession(); } return result; }
     */

    /*
     * 
     * Query q = sess.createQuery("select cat.name, cat from DomesticCat cat " + "order by cat.name"); ScrollableResults
     * cats = q.scroll(); if ( cats.first() ) {
     *  // find the first name on each page of an alphabetical list of cats by name firstNamesOfPages = new ArrayList();
     * do { String name = cats.getString(0); firstNamesOfPages.add(name); } while ( cats.scroll(PAGE_SIZE) );
     *  // Now get the first page of cats pageOfCats = new ArrayList(); cats.beforeFirst(); int i=0; while( ( PAGE_SIZE >
     * i++ ) && cats.next() ) pageOfCats.add( cats.get(1) );
     *  }
     */

    /*
     * public static List findByTitle(Session session, java.lang.String title) throws SQLException, HibernateException {
     * List finds = session.find("from org.lnicholls.galleon.database.Audio as Audio where Audio.title=?", title,
     * Hibernate.STRING); return finds; }
     * 
     * public static List findByPath(Session session, java.lang.String filePath) throws SQLException, HibernateException {
     * List finds = session.find("from org.lnicholls.galleon.database.Audio as Audio where Audio.filePath=?", filePath,
     * Hibernate.STRING); return finds; }
     * 
     * public static List findAll(Session session) throws SQLException, HibernateException { List finds =
     * session.find("from Audio in class org.lnicholls.galleon.database.Audio"); return finds; }
     */
    /*
     * Criteria crit = session.createCriteria(fromData.getClass()).add( Expression.between(readKey, fromReadValue,
     * toReadValue));
     * 
     * crit.setMaxResults(5); crit.setFirstResult( ( 5 * get_cur_page_number() ) );
     * 
     * list = crit.list();
     */
}

/*
 * 
 * public class Page {
 * 
 * private List results; private int pageSize; private int page;
 * 
 * public Page(Query query, int page, int pageSize) {
 * 
 * this.page = page; this.pageSize = pageSize; results = query.setFirstResult(page * pageSize)
 * .setMaxResults(pageSize+1) .list();
 *  }
 * 
 * public boolean isNextPage() { return results.size() > pageSize; }
 * 
 * public boolean isPreviousPage() { return page > 0; }
 * 
 * public List getList() { return isNextPage() ? results.subList(0, pageSize-1) : results; }
 *  }
 */
