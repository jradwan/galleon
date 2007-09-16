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
public class ImageAlbumsPicturesManager {
	private static Logger log = Logger.getLogger(ImageAlbumsPicturesManager.class
			.getName());
	public static interface Callback {
		public void visit(Session session, ImageAlbumsPictures ImageAlbumsPictures);
	}
	public static ImageAlbumsPictures retrieveImageAlbumsPictures(
			ImageAlbumsPictures ImageAlbumsPictures) throws HibernateException {
		return retrieveImageAlbumsPictures(new Integer(ImageAlbumsPictures.getId()));
	}
	public static ImageAlbumsPictures retrieveImageAlbumsPictures(int id) throws HibernateException {
		return retrieveImageAlbumsPictures(new Integer(id));
	}
	public static ImageAlbumsPictures retrieveImageAlbumsPictures(Integer id)
			throws HibernateException {
		ImageAlbumsPictures result = null;
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			result = (ImageAlbumsPictures) session.load(ImageAlbumsPictures.class, id);
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
	public static ImageAlbumsPictures createImageAlbumsPictures(
			ImageAlbumsPictures ImageAlbumsPictures) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(trim(ImageAlbumsPictures));
			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
		return ImageAlbumsPictures;
	}
	public static void updateImageAlbumsPictures(ImageAlbumsPictures ImageAlbumsPictures)
			throws HibernateException {
		if (ImageAlbumsPictures.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.update(trim(ImageAlbumsPictures));
	
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
	public static void deleteImageAlbumsPictures(ImageAlbumsPictures ImageAlbumsPictures)
			throws HibernateException {
		if (ImageAlbumsPictures.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.delete(ImageAlbumsPictures);
	
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
					"from org.lnicholls.galleon.database.ImageAlbumsPictures")
					.list();
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
					.createQuery("from org.lnicholls.galleon.database.ImageAlbumsPictures");
			ScrollableResults items = query.scroll();
			int counter = start;
			if (items.first()) {
				items.scroll(start);
				while (items.next() && (counter < end)) {
					ImageAlbumsPictures ImageAlbumsPictures = (ImageAlbumsPictures) items
							.get(0);
					list.add(ImageAlbumsPictures);
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
					.createQuery("from org.lnicholls.galleon.database.ImageAlbumsPictures");
			ScrollableResults items = q.scroll();
			if (items.first()) {
				items.beforeFirst();
				while (items.next()) {
					ImageAlbumsPictures ImageAlbumsPictures = (ImageAlbumsPictures) items
							.get(0);
					callback.visit(session, ImageAlbumsPictures);
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
	public static List findByImageAlbums(int id) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbumsPictures as ImageAlbumsPictures where ImageAlbumsPictures.imagealbums=?")
					.setInteger(0, id).list();
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
	private static ImageAlbumsPictures trim(ImageAlbumsPictures imageAlbumsPictures) {
		return imageAlbumsPictures;
	}
}
