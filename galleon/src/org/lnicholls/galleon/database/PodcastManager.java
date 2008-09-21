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
import java.util.Iterator;
import java.util.List;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.ScrollableResults;
import org.hibernate.classic.Session;
import org.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
public class PodcastManager {
	private static Logger log = Logger
			.getLogger(PodcastManager.class.getName());
	public static interface Callback {
		public void visit(Session session, Podcast Podcast);
	}
	public static Podcast retrievePodcast(Podcast Podcast)
			throws HibernateException {
		return retrievePodcast(new Integer(Podcast.getId()));
	}
	public static Podcast retrievePodcast(int id) throws HibernateException {
		return retrievePodcast(new Integer(id));
	}
	public static Podcast retrievePodcast(Integer id) throws HibernateException {
		Podcast result = null;
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			result = (Podcast) session.load(Podcast.class, id);
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
	public static Podcast createPodcast(Podcast Podcast)
			throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			session.save(trim(Podcast));
			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
		return Podcast;
	}
	public static void updatePodcast(Podcast Podcast) throws HibernateException {
		if (Podcast.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.update(trim(Podcast));
	
				tx.commit();
	
			} catch (HibernateException he) {
	
				log.debug(Podcast.getPath());
	
				if (tx != null)
	
					tx.rollback();
	
				throw he;
	
			} finally {
	
				HibernateUtil.closeSession();
	
			}
		}
	}
	public static void deletePodcast(Podcast Podcast) throws HibernateException {
		if (Podcast.getId()!=0)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				session.delete(Podcast);
	
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
	public static List<Podcast> listAll() throws HibernateException {
		List<Podcast> list = new ArrayList<Podcast>();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			list = session.createQuery(
					"from org.lnicholls.galleon.database.Podcast").list();
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
	@SuppressWarnings("unchecked")
	public static List<Podcast> listAllSubscribed() throws HibernateException {
		List<Podcast> list = new ArrayList<Podcast>();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Podcast as podcast where podcast.status=?")
					.setInteger(0,
					Podcast.STATUS_SUBSCRIBED).list();
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
	public static List<Podcast> listBetween(int start, int end)
			throws HibernateException {
		List<Podcast> list = new ArrayList<Podcast>();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			Query query = session
					.createQuery("from org.lnicholls.galleon.database.Podcast");
			ScrollableResults items = query.scroll();
			int counter = start;
			if (items.first()) {
				items.scroll(start);
				while (items.next() && (counter < end)) {
					Podcast Podcast = (Podcast) items.get(0);
					list.add(Podcast);
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
					.createQuery("from org.lnicholls.galleon.database.Podcast");
			ScrollableResults items = q.scroll();
			if (items.first()) {
				items.beforeFirst();
				while (items.next()) {
					Podcast Podcast = (Podcast) items.get(0);
					callback.visit(session, Podcast);
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
	public static List<Podcast> findByPath(String path) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Podcast> list = session
					.createQuery(
					"from org.lnicholls.galleon.database.Podcast as Podcast where Podcast.path=?")
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
	@SuppressWarnings("unchecked")
	public static List<Podcast> findByOrigen(String origen) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Podcast> list = session
					.createQuery(
					"from org.lnicholls.galleon.database.Podcast as Podcast where Podcast.origen=?")
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
	@SuppressWarnings("unchecked")
	public static List<Podcast> findByTitle(String title) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Podcast> list = session
					.createQuery(
					"from org.lnicholls.galleon.database.Podcast as Podcast where Podcast.title=?")
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
	@SuppressWarnings("unchecked")
	public static List<Podcast> findByExternalId(String id) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Podcast> list = session
					.createQuery(
							"from org.lnicholls.galleon.database.Podcast as Podcast where Podcast.externalId=?")
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
	@SuppressWarnings("unchecked")
	public static List<Podcast> listTitles() throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			List<Podcast> list = session
					.createQuery(
					"select Podcast.title from org.lnicholls.galleon.database.Podcast as Podcast")
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
	public static List<NameValue> getPodcasts() throws HibernateException
	{
		List<NameValue> names = new ArrayList<NameValue>();
		try {
			List<Podcast> podcasts = listAllSubscribed();
			if (podcasts != null && podcasts.size() > 0) {
				for (Iterator<Podcast> i = podcasts.iterator(); i.hasNext(); /* Nothing */) {
					Podcast podcast = i.next();
					names.add(new NameValue(podcast.getTitle(), podcast
							.getPath()));
				}
			}
		} catch (Exception ex) {
			Tools.logException(PodcastManager.class, ex);
		}
		return names;
	}
	public static void setPodcasts(List<NameValue> list) throws HibernateException
	{
		try {
			List<Podcast> podcasts = listAllSubscribed();
			if (podcasts != null && podcasts.size() > 0) {
				Iterator<NameValue> iterator = list.iterator();
				while (iterator.hasNext())
				{
					NameValue nameValue = (NameValue) iterator.next();
					boolean found = false;
					for (Iterator<Podcast> i = podcasts.iterator(); i.hasNext(); /* Nothing */) {
						Podcast podcast = (Podcast) i.next();
						if (podcast.getPath().equals(nameValue.getValue()))
						{
							podcast.setTitle(nameValue.getName());
							updatePodcast(podcast);
							found = true;
							break;
						}
					}
					if (!found)
					{
						Podcast podcast = new Podcast(nameValue.getName(),
								Podcast.STATUS_SUBSCRIBED,
								nameValue.getValue(), 0, new ArrayList<PodcastTrack>());
						createPodcast(podcast);
					}
				}
				// Remove podcasts no longer on list
				for (Iterator<Podcast> i = podcasts.iterator(); i.hasNext(); /* Nothing */) {
					Podcast podcast = i.next();
					boolean found = false;
					iterator = list.iterator();
					while (iterator.hasNext())
					{
						NameValue nameValue = (NameValue) iterator.next();
						if (podcast.getPath().equals(nameValue.getValue()))
						{
							found = true;
							break;
						}
					}
					if (!found)
					{
						podcast.setStatus(Podcast.STATUS_DELETED);
						updatePodcast(podcast);
					}
				}
			}
			else
			{
				Iterator<NameValue> iterator = list.iterator();
				while (iterator.hasNext())
				{
					NameValue nameValue = iterator.next();
					Podcast podcast = new Podcast(nameValue.getName(),
							Podcast.STATUS_SUBSCRIBED, nameValue.getValue(), 0,
							new ArrayList<PodcastTrack>());
					createPodcast(podcast);
				}
			}
		} catch (Exception ex) {
			Tools.logException(PodcastManager.class, ex);
		}
	}
	private static Podcast trim(Podcast podcast)
	{
		if (podcast!=null)
		{
			podcast.setAuthor(Tools.trim(podcast.getAuthor(), 255));
			podcast.setCategory(Tools.trim(podcast.getCategory(), 255));
			podcast.setDescription(Tools.trim(podcast.getDescription(), 4096));
			podcast.setExternalId(Tools.trim(podcast.getExternalId(), 255));
			podcast.setKeywords(Tools.trim(podcast.getKeywords(), 255));
			podcast.setLink(Tools.trim(podcast.getLink(), 1024));
			podcast.setOrigen(Tools.trim(podcast.getOrigen(), 30));
			podcast.setPath(Tools.trim(podcast.getPath(), 1024));
			podcast.setSubtitle(Tools.trim(podcast.getSubtitle(), 4096));
			podcast.setSummary(Tools.trim(podcast.getSummary(), 4096));
			podcast.setTitle(Tools.trim(podcast.getTitle(), 255));
			List<PodcastTrack> list = (List<PodcastTrack>)podcast.getTracks();
			Iterator<PodcastTrack> iterator = list.iterator();
			while (iterator.hasNext())
			{
				PodcastTrack track = (PodcastTrack) iterator.next();
				track.setAuthor(Tools.trim(track.getAuthor(), 255));
				track.setCategory(Tools.trim(track.getCategory(), 255));
				track.setDescription(Tools.trim(track.getDescription(), 4096));
				track.setGuid(Tools.trim(track.getGuid(), 255));
				track.setKeywords(Tools.trim(track.getKeywords(), 255));
				track.setLink(Tools.trim(track.getLink(), 1024));
				track.setMimeType(Tools.trim(track.getMimeType(), 50));
				track.setSubtitle(Tools.trim(track.getSubtitle(), 4096));
				track.setSummary(Tools.trim(track.getSummary(), 4096));
				track.setTitle(Tools.trim(track.getTitle(), 255));
				track.setUrl(Tools.trim(track.getUrl(), 1024));
			}
		}
		return podcast;
	}
}
