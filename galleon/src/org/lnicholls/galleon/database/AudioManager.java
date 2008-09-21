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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.media.MediaRefreshThread;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
public class AudioManager {
	private static Logger log = Logger.getLogger(AudioManager.class.getName());
	public static interface Callback {
		public void visit(Session session, Audio audio);
	}
	public static Audio retrieveAudio(Audio audio) throws HibernateException {
		return retrieveAudio(new Integer(audio.getId()));
	}
	public static Audio retrieveAudio(int id) throws HibernateException {
		return retrieveAudio(new Integer(id));
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
			session.save(trim(audio));
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
		if (audio.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.update(trim(audio));
	
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
	public static void deleteAudio(Audio audio) throws HibernateException {
		if (audio.getId()!=0)
		{
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
	}
	@SuppressWarnings("unchecked")
	public static List<Audio> listAll() throws HibernateException {
		List<Audio> list = new ArrayList<Audio>();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			list = session.createQuery(
					"from org.lnicholls.galleon.database.Audio").list();
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
	public static List<Audio> listBetween(int start, int end)
			throws HibernateException {
		List<Audio> list = new ArrayList<Audio>();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Query query = session
					.createQuery("from org.lnicholls.galleon.database.Audio");
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
			Query q = session
					.createQuery("from org.lnicholls.galleon.database.Audio");
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByPath(String path) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Audio as Audio where Audio.path=?")
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
	public static Audio findByItem(Item nameFile) throws HibernateException, IOException {
		List<Audio> list = null;
		Audio audio = null;
		if (nameFile.isFile())
			list = findByPath(((File) nameFile.getValue()).getCanonicalPath());
		else
			list = findByPath((String) nameFile.getValue());
		if (list != null && list.size() > 0) {
			audio = list.get(0);
		}
		return audio;
	}
	@SuppressWarnings("unchecked")
	public static List<Audio> findByOrigen(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Audio as Audio where Audio.origen=?")
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
	@SuppressWarnings("unchecked")
	public static List<String> listGenres(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<String> list = new ArrayList<String>();
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByOrigenGenre(String origen, String genre)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = new ArrayList<Audio>();
			if (origen != null)
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.origen=? and audio.genre=?")
						.setString(0, origen).setString(1, genre).list();
			else
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.genre= ?")
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByOrigenRated(String origen)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = new ArrayList<Audio>();
			if (origen != null)
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.origen=? and audio.rating>0 order by rating desc")
						.setString(0, origen).list();
			else
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.rating>0 order by rating desc")
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByOrigenGenreOrdered(String origen, String genre)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = new ArrayList<Audio>();
			if (origen != null)
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.origen=? and audio.genre=? order by title")
						.setString(0, origen).setString(1, genre).list();
			else
				list = session
						.createQuery(
								"from org.lnicholls.galleon.database.Audio as audio where audio.genre= ? order by title")
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByTitle(String title) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Audio as audio where audio.title=?")
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
	@SuppressWarnings("unchecked")
	public static long countMP3s() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Long> list = session
					.createQuery(
							"select count(audio) from org.lnicholls.galleon.database.Audio as audio where substr(audio.path,1,4)<>'http'")
					.list();
			tx.commit();
			return ((Long) list.iterator().next()).longValue();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
	}
	@SuppressWarnings("unchecked")
	public static long countMP3sByOrigen(String origen)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Long> list = session
					.createQuery(
							"select count(audio) from org.lnicholls.galleon.database.Audio as audio where audio.origen=?")
					.setString(0, origen).list();
			tx.commit();
			return ((Long) list.iterator().next()).longValue();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
	}
	@SuppressWarnings("unchecked")
	public static List<Audio> findByExternalId(String id) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = session
					.createQuery(
					"from org.lnicholls.galleon.database.Audio as audio where audio.externalId=?")
					.setString(0, id)
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
	@SuppressWarnings("unchecked")
	public static List<Audio> findByTitleOrigenGenreExternalId(String title,
			String origen, String genre, String id) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Audio> list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Audio as audio where audio.title=? audio.origen=? and audio.genre=? and audio.externalId=?")
					.setString(0, title).setString(1, origen).setString(2,
							genre).setString(3, id).list();
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
	@SuppressWarnings("unchecked")
	public static void clean() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			java.util.List<Audio> list = session.createQuery(
					"from org.lnicholls.galleon.database.Audio").list();
			if (list != null && list.size() > 0) {
				int counter = 0;
				for (int i = 0; i < list.size(); i++) {
					Audio audio = list.get(i);
					if (!audio.getPath().startsWith("http")
							&& !(audio.getOrigen() != null && !audio
									.getOrigen().equals("Podcast"))) {
						File file = new File(audio.getPath());
						if (!file.exists()) {
							try {
								session.delete(audio);
								if (log.isDebugEnabled())
									log.debug("Removed: " + audio.getPath());
							} catch (Exception ex) {
								Tools
								.logException(MediaRefreshThread.class, ex,
										"Could not remove: "
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
	private static Audio trim(Audio audio)
	{
		if (audio!=null)
		{
			audio.setAlbum(Tools.trim(audio.getAlbum(), 255));
	
			audio.setArtist(Tools.trim(audio.getArtist(), 255));
	
			audio.setComments(Tools.trim(audio.getComments(), 4096));
	
			audio.setExternalId(Tools.trim(audio.getExternalId(), 255));
	
			audio.setGenre(Tools.trim(audio.getGenre(), 50));
	
			audio.setLyrics(Tools.trim(audio.getLyrics(), 4096));
	
			audio.setMimeType(Tools.trim(audio.getMimeType(), 50));
	
			audio.setOrigen(Tools.trim(audio.getOrigen(), 30));
	
			audio.setPath(Tools.trim(audio.getPath(), 1024));
	
			audio.setTitle(Tools.trim(audio.getTitle(), 255));
	
			audio.setTone(Tools.trim(audio.getTone(), 50));
		}
		return audio;
	}
}
