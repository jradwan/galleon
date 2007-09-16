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
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.apache.log4j.Logger;
public class ImageAlbumManager {
	private static Logger log = Logger.getLogger(ImageAlbumManager.class
			.getName());
	public static interface Callback {
		public void visit(Session session, ImageAlbum imageAlbum);
	}
	public static ImageAlbum retrieveImageAlbum(ImageAlbum imageAlbum)
			throws HibernateException {
		return retrieveImageAlbum(new Integer(imageAlbum.getId()));
	}
	public static ImageAlbum retrieveImageAlbum(int id)	throws HibernateException {
		return retrieveImageAlbum(new Integer(id));
	}
public static ImageAlbum retrieveImageAlbum(Integer id)
			throws HibernateException {
		ImageAlbum result = null;
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			result = (ImageAlbum) session.load(ImageAlbum.class, id);
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
	public static ImageAlbum createImageAlbum(ImageAlbum imageAlbum)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(imageAlbum);
			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
		return imageAlbum;
	}
	public static void updateImageAlbum(ImageAlbum imageAlbum)
			throws HibernateException {
		if (imageAlbum.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				session.update(imageAlbum);
				tx.commit();
			} catch (HibernateException he) {
				if (tx != null)
					tx.rollback();
				throw he;
			} finally {
				HibernateUtil.closeSession();
			}
		}
	}
	public static void deleteImageAlbum(ImageAlbum imageAlbum)
			throws HibernateException {
		if (imageAlbum.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
			Transaction tx = null;
			try {
				tx = session.beginTransaction();
				session.delete(imageAlbum);
				tx.commit();
			} catch (HibernateException he) {
				if (tx != null)
					tx.rollback();
				throw he;
			} finally {
				HibernateUtil.closeSession();
			}
		}
	}
	public static List listAll() throws HibernateException {
		List list = new ArrayList();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			list = session.createQuery(
					"from org.lnicholls.galleon.database.ImageAlbum").list();
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
	public static List listBetween(int start, int end)
			throws HibernateException {
		List list = new ArrayList();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Query query = session
					.createQuery("from org.lnicholls.galleon.database.ImageAlbum");
			ScrollableResults items = query.scroll();
			int counter = start;
			if (items.first()) {
				items.scroll(start);
				while (items.next() && (counter < end)) {
					ImageAlbum ImageAlbum = (ImageAlbum) items.get(0);
					list.add(ImageAlbum);
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
			Query q = session
					.createQuery("from org.lnicholls.galleon.database.ImageAlbum");
			ScrollableResults items = q.scroll();
			if (items.first()) {
				items.beforeFirst();
				while (items.next()) {
					ImageAlbum ImageAlbum = (ImageAlbum) items.get(0);
					callback.visit(session, ImageAlbum);
				}
				;
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
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbum as ImageAlbum where ImageAlbum.path=?")
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
	public static List findByOrigen(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbum as ImageAlbum where ImageAlbum.origen=?")
					.setString(0,
					origen).list();
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
	public static List findByTitle(String title) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbum as ImageAlbum where ImageAlbum.title=?")
					.setString(0,
					title).list();
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
	public static List findByExternalId(String id) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbum as ImageAlbum where ImageAlbum.externalId=?")
					.setString(
					0, id).list();
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
	public static List listTitles() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"select imagealbum.title from org.lnicholls.galleon.database.ImageAlbum as imagealbum")
					.list();
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
