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
import net.sf.hibernate.HibernateException;
import net.sf.hibernate.Query;
import net.sf.hibernate.ScrollableResults;
import net.sf.hibernate.Session;
import net.sf.hibernate.Transaction;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;
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
	public static Video createVideo(Video video) throws HibernateException {
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			clean(video);
			session.save(trim(video));
			tx.commit();
		} catch (HibernateException he) {
			if (tx != null)
				tx.rollback();
			throw he;
		} finally {
			HibernateUtil.closeSession();
		}
		return video;
	}
	public static void updateVideo(Video video) throws HibernateException {
		if (video.getId()!=null)
		{
			Session session = HibernateUtil.openSession();
	
			Transaction tx = null;
	
			try {
	
				tx = session.beginTransaction();
	
				clean(video);
	
				session.update(trim(video));
	
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
	public static void deleteVideo(Video Video) throws HibernateException {
		if (Video.getId()!=null)
		{
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
	}
	public static List listAll() throws HibernateException {
		List list = new ArrayList();
		Session session = HibernateUtil.openSession();
		Transaction tx = null;
		try {
			tx = session.beginTransaction();
			list = session.createQuery(
					"from org.lnicholls.galleon.database.Video").list();
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
	public static List listAllTiVo() throws HibernateException {
		List list = new ArrayList();
		List all = listAll();
		Iterator iterator = all.iterator();
		while (iterator.hasNext())
		{
			Video video = (Video) iterator.next();
			if (video.getOrigen() == null)
				list.add(video);
		}
		/*
		 * 
		 * Session session = HibernateUtil.openSession();
		 * 
		 * Transaction tx = null;
		 * 
		 * try {
		 * 
		 * tx = session.beginTransaction();
		 * 
		 * list = session.createQuery("SELECT video FROM
		 * org.lnicholls.galleon.database.Video video LEFT JOIN video.origen
		 * origen WHERE origen IS NULL").list();
		 * 
		 * tx.commit();
		 *  } catch (HibernateException he) {
		 * 
		 * if (tx != null)
		 * 
		 * tx.rollback();
		 * 
		 * throw he;
		 *  } finally {
		 * 
		 * HibernateUtil.closeSession();
		 *  }
		 * 
		 */
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
					.createQuery("from org.lnicholls.galleon.database.Video");
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
			Query q = session
					.createQuery("from org.lnicholls.galleon.database.Video");
			ScrollableResults items = q.scroll();
			if (items.first()) {
				items.beforeFirst();
				while (items.next())
				{
					Video video = (Video) items.get(0);
					callback.visit(session, video);
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
							"from org.lnicholls.galleon.database.Video as video where video.path=?")
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
	private static void clean(Video video)
	{
		if (video.getDescription() != null)
		{
			if (video.getDescription().endsWith(
					"Copyright Tribune Media Services, Inc."))
				video.setDescription(video.getDescription().substring(
						0,
						video.getDescription().length()
								- "Copyright Tribune Media Services, Inc."
										.length()));
			video.setDescription(Tools.trim(video.getDescription(), 255));
		}
	}
	private static Video trim(Video video)
	{
		if (video != null) {
			video.setActors(Tools.trim(video.getActors(), 512));
			video.setAdvisories(Tools.trim(video.getAdvisories(), 255));
			video.setAudioCodec(Tools.trim(video.getAudioCodec(), 20));
			video.setBookmarks(Tools.trim(video.getBookmarks(), 255));
			video.setCallsign(Tools.trim(video.getCallsign(), 255));
			video.setChannel(Tools.trim(video.getChannel(), 255));
			video.setChoreographers(Tools.trim(video.getChoreographers(), 255));
			video.setColor(Tools.trim(video.getColor(), 20));
			video.setDescription(Tools.trim(video.getDescription(), 255));
			video.setDirectors(Tools.trim(video.getDirectors(), 255));
			video.setEpisodeTitle(Tools.trim(video.getEpisodeTitle(), 255));
			video.setExecProducers(Tools.trim(video.getExecProducers(), 255));
			video.setGuestStars(Tools.trim(video.getGuestStars(), 255));
			video.setHosts(Tools.trim(video.getHosts(), 255));
			video.setIcon(Tools.trim(video.getIcon(), 255));
			video.setMimeType(Tools.trim(video.getMimeType(), 50));
			video.setOrigen(Tools.trim(video.getOrigen(), 30));
			video.setPath(Tools.trim(video.getPath(), 1024));
			video.setProducers(Tools.trim(video.getProducers(), 255));
			video.setProgramGenre(Tools.trim(video.getProgramGenre(), 255));
			video.setRating(Tools.trim(video.getRating(), 255));
			video.setRecordingQuality(Tools.trim(video.getRecordingQuality(),
					255));
			video.setSeriesGenre(Tools.trim(video.getSeriesGenre(), 255));
			video.setSeriesTitle(Tools.trim(video.getSeriesTitle(), 255));
			video.setShowType(Tools.trim(video.getShowType(), 255));
			video.setSource(Tools.trim(video.getSource(), 255));
			video.setStation(Tools.trim(video.getStation(), 255));
			video.setTitle(Tools.trim(video.getTitle(), 255));
			video.setTone(Tools.trim(video.getTone(), 50));
			video.setUploaded(Tools.trim(video.getUploaded(), 255));
			video.setUrl(Tools.trim(video.getUrl(), 1024));
			video.setVideoCodec(Tools.trim(video.getVideoCodec(), 20));
			video
					.setVideoResolution(Tools.trim(video.getVideoResolution(),
							20));
			video.setWriters(Tools.trim(video.getWriters(), 255));
			video.setTivo(Tools.trim(video.getTivo(), 255));
		}
		return video;
	}
}