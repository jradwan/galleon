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
import org.lnicholls.galleon.util.Tools;
public class ImageAlbumsManager {
	private static Logger log = Logger.getLogger(ImageAlbumsManager.class
			.getName());
	public static interface Callback {
		public void visit(Session session, ImageAlbums ImageAlbums);
	}
	public static ImageAlbums retrieveImageAlbums(ImageAlbums ImageAlbums)
			throws HibernateException {
		return retrieveImageAlbums(new Integer(ImageAlbums.getId()));
	}
	public static ImageAlbums retrieveImageAlbums(int id) throws HibernateException {
		return retrieveImageAlbums(new Integer(id));
	}
	public static ImageAlbums retrieveImageAlbums(Integer id)
			throws HibernateException {
		ImageAlbums result = null;
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			result = (ImageAlbums) session.load(ImageAlbums.class, id);
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
	public static ImageAlbums createImageAlbums(ImageAlbums ImageAlbums)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(trim(ImageAlbums));
			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
		return ImageAlbums;
	}
	public static void updateImageAlbums(ImageAlbums ImageAlbums)
			throws HibernateException {
		if (ImageAlbums.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.update(trim(ImageAlbums));
	
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
	public static void deleteImageAlbums(ImageAlbums ImageAlbums)
			throws HibernateException {
		if (ImageAlbums.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.delete(ImageAlbums);
	
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
					"from org.lnicholls.galleon.database.ImageAlbums").list();
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
					.createQuery("from org.lnicholls.galleon.database.ImageAlbums");
			ScrollableResults items = query.scroll();
			int counter = start;
			if (items.first()) {
				items.scroll(start);
				while (items.next() && (counter < end)) {
					ImageAlbums ImageAlbums = (ImageAlbums) items.get(0);
					list.add(ImageAlbums);
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
					.createQuery("from org.lnicholls.galleon.database.ImageAlbums");
			ScrollableResults items = q.scroll();
			if (items.first()) {
				items.beforeFirst();
				while (items.next()) {
					ImageAlbums ImageAlbums = (ImageAlbums) items.get(0);
					callback.visit(session, ImageAlbums);
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
							"from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.path=?")
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
							"from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.origen=?")
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
							"from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.title=?")
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
							"from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.externalId=?")
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
	public static List listAlbums() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"select ImageAlbums.title from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.isRoll=false")
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
	public static List listRolls() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"select ImageAlbums.title from org.lnicholls.galleon.database.ImageAlbums as ImageAlbums where ImageAlbums.isRoll=true")
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
	public static void deleteImageAlbumsPictures(ImageAlbums ImageAlbums)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List list = session
					.createQuery(
							"from org.lnicholls.galleon.database.ImageAlbumsPictures as ImageAlbumsPictures where ImageAlbumsPictures.id=?")
					.setInteger(0, ImageAlbums.getId()).list();
			if (list != null && list.size() > 0)
			{
				for (int i = 0; i < list.size(); i++)
				{
					ImageAlbumsPictures imageAlbumsPictures = (ImageAlbumsPictures) list
							.get(0);
					session.delete(imageAlbumsPictures);
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
	}
	private static ImageAlbums trim(ImageAlbums imageAlbums)
	{
		if (imageAlbums!=null)
		{
			imageAlbums.setExternalId(Tools.trim(imageAlbums.getExternalId(), 255));
	
			imageAlbums.setOrigen(Tools.trim(imageAlbums.getOrigen(), 30));
	
			imageAlbums.setTitle(Tools.trim(imageAlbums.getTitle(), 255));
		}
		return imageAlbums;
	}
}
