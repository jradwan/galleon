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

import java.io.File;
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

import org.lnicholls.galleon.media.MediaRefreshThread;
import org.lnicholls.galleon.util.Tools;

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
			audio.setComments(Tools.trim(audio.getComments(), 4096));
			audio.setLyrics(Tools.trim(audio.getLyrics(), 4096));
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
			audio.setComments(Tools.trim(audio.getComments(), 4096));
			audio.setLyrics(Tools.trim(audio.getLyrics(), 4096));
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
			list = session.createQuery("from org.lnicholls.galleon.database.Audio").list();
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
				while (items.next()) {
					Audio audio = (Audio) items.get(0);
					callback.visit(session, audio);
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

			List list = session.createQuery("from org.lnicholls.galleon.database.Audio as Audio where Audio.path=?")
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

			List list = session.createQuery("from org.lnicholls.galleon.database.Audio as Audio where Audio.origen=?")
					.setString(0, origen).list();

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

	public static List listGenres(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			List list = new ArrayList();
			if (origen != null) {
				list = session
						.createQuery(
								"select distinct audio.genre from org.lnicholls.galleon.database.Audio audio where audio.origen=? order by audio.genre asc")
						.setString(0, origen).list();
			} else
				list = session
						.createQuery(
								"select distinct audio.genre from org.lnicholls.galleon.database.Audio audio where (1=1) order by audio.genre asc")
						.setCacheable(true).list();

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

	public static List findByOrigenGenre(String origen, String genre) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			List list = new ArrayList();
			if (origen != null)
				list = session.createQuery(
						"from org.lnicholls.galleon.database.Audio as audio where audio.origen=? and audio.genre=?")
						.setString(0, origen).setString(1, genre).list();
			else
				list = session.createQuery("from org.lnicholls.galleon.database.Audio as audio where audio.genre= ?")
						.setString(0, genre).list();

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

			List list = session.createQuery("from org.lnicholls.galleon.database.Audio as audio where audio.title=?")
					.setString(0, title).list();

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

	public static int countMP3s() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			List list = session
					.createQuery(
							"select count(audio) from org.lnicholls.galleon.database.Audio as audio where substr(audio.path,1,4)<>'http'")
					.list();

			tx.commit();

			return ((Integer) list.iterator().next()).intValue();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
	}

	public static int countMP3sByOrigen(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			List list = session.createQuery(
					"select count(audio) from org.lnicholls.galleon.database.Audio as audio where audio.origen=?")
					.setString(0, origen).list();

			tx.commit();

			return ((Integer) list.iterator().next()).intValue();
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

			List list = session.createQuery(
					"from org.lnicholls.galleon.database.Audio as audio where audio.externalId=?").setString(0, id)
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

	public static void clean() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();

			java.util.List list = session.createQuery("from org.lnicholls.galleon.database.Audio").list();
			if (list != null && list.size() > 0) {
				int counter = 0;
				for (int i = 0; i < list.size(); i++) {
					Audio audio = (Audio) list.get(i);
					if (!audio.getPath().startsWith("http")
							&& !(audio.getOrigen() != null && !audio.getOrigen().equals("Podcast"))) {
						File file = new File(audio.getPath());
						if (!file.exists()) {
							try {
								session.delete(audio);

								if (log.isDebugEnabled())
									log.debug("Removed: " + audio.getPath());
							} catch (Exception ex) {
								Tools
										.logException(MediaRefreshThread.class, ex, "Could not remove: "
												+ audio.getPath());
							}
						}
					}
					audio = null;
					if (++counter % 100 == 0)
						System.gc();
					try {
						Thread.sleep(50); // give the CPU some breathing time
					} catch (Exception ex) {
					}
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
}