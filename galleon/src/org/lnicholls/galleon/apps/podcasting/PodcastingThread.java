package org.lnicholls.galleon.apps.podcasting;

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

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import net.sf.hibernate.HibernateException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.database.Audio;
import org.lnicholls.galleon.database.AudioManager;
import org.lnicholls.galleon.database.Podcast;
import org.lnicholls.galleon.database.PodcastManager;
import org.lnicholls.galleon.database.PodcastTrack;
import org.lnicholls.galleon.media.Mp3File;
import org.lnicholls.galleon.server.Constants;
import org.lnicholls.galleon.util.ProgressListener;
import org.lnicholls.galleon.util.Tools;

import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;

public class PodcastingThread extends Thread implements Constants, ProgressListener {
	private static Logger log = Logger.getLogger(PodcastingThread.class.getName());

	public PodcastingThread(PodcastingConfiguration podcastingConfiguration) {
		super("PodcastingThread");

		mPodcastingConfiguration = podcastingConfiguration;

		setPriority(Thread.MIN_PRIORITY);
	}

	public void run() {
		while (true) {
			try {
				log.debug("podcastingthread run");
				List list = null;
				synchronized (this) {
					try {
						list = PodcastManager.listAll();
					} catch (Exception ex) {
						Tools.logException(PodcastingThread.class, ex);
					}
				}

				if (list != null && list.size() > 0) {
					for (Iterator i = list.iterator(); i.hasNext(); /* Nothing */) {
						Podcast podcast = (Podcast) i.next();
						
						Thread.sleep(100); // give the CPU some breathing time

						if (podcast.getStatus()==podcast.STATUS_ERROR)
						{
							Date current = new Date();
							if (current.getTime()-podcast.getDateUpdated().getTime()>1000*60*60)
							{
								podcast.setStatus(Podcast.STATUS_SUBSCRIBED);
								PodcastManager.updatePodcast(podcast);
							}
						}
						else
						if (podcast.getStatus()==podcast.STATUS_SUBSCRIBED)							
						{
							synchronized (this) {
								try {
									podcast = PodcastManager.retrievePodcast(podcast);
								} catch (Exception ex) {
									Tools.logException(PodcastingThread.class, ex, "Retrieve podcast failed");
								}
							}

							List podcastTracks = podcast.getTracks();
							if (podcastTracks != null && podcastTracks.size() > 0) {
								PodcastTrack[] tracks = new PodcastTrack[podcastTracks.size()];
								int pos = 0;
								for (Iterator j = podcastTracks.iterator(); j.hasNext(); /* Nothing */) {
									PodcastTrack track = (PodcastTrack) j.next();
									tracks[pos++] = track;
								}
								
								// Update podcast
								if (Podcasting.getPodcast(podcast)!=null) {
									synchronized (this) {
										try {
											PodcastManager.updatePodcast(podcast);
										} catch (Exception ex) {
											Tools.logException(Podcasting.class, ex);
										}
									}
									// Remove tracks that dont exist anymore
									for (int k = 0; k < tracks.length; k++) {
										boolean found = false;
										for (Iterator l = podcast.getTracks().iterator(); l.hasNext(); /* Nothing */) {
											PodcastTrack track = (PodcastTrack) l.next();
											if (tracks[k].getUrl()!=null && track.getUrl()!=null && tracks[k].getUrl().equals(track.getUrl())) {
												found = true;
												break;
											}
										}

										if (!found) {
											deleteAudio(podcast, tracks[k]);
											synchronized (this) {
												try {
													podcast.getTracks().remove(tracks[k]);
													PodcastManager.updatePodcast(podcast);
												} catch (Exception ex) {
													Tools.logException(Podcasting.class, ex);
												}
											}
										}
									}

									podcastTracks = podcast.getTracks();
									tracks = new PodcastTrack[podcastTracks.size()];
									pos = 0;
									for (Iterator j = podcastTracks.iterator(); j.hasNext(); /* Nothing */) {
										PodcastTrack track = (PodcastTrack) j.next();
										tracks[pos++] = track;
									}
								}
								else
								{
									podcast.setStatus(Podcast.STATUS_ERROR);
									podcast.setDateUpdated(new Date());
									PodcastManager.updatePodcast(podcast);
									break;
								}

								// Sort the tracks by date
								Arrays.sort(tracks, new Comparator() {
									public int compare(Object o1, Object o2) {
										PodcastTrack track1 = (PodcastTrack) o1;
										PodcastTrack track2 = (PodcastTrack) o2;
										if (track1.getPublicationDate() != null && track2.getPublicationDate() != null)
											return -(int)(track1.getPublicationDate().getTime()-track2.getPublicationDate().getTime());
										else
											return 0;
									}
								});

								PodcastTrack track = null;

								// Find a track selected by the user
								for (int j = 0; j < tracks.length; j++) {
									if (tracks[j].getStatus() == PodcastTrack.STATUS_QUEUED
											|| tracks[j].getStatus() == PodcastTrack.STATUS_DOWNLOADING) {
										track = tracks[j];
										break;
									}
								}
								if (track == null && podcast.getStatus() == Podcast.STATUS_SUBSCRIBED) {
									// Pick the latest that hasnt been
									// downloaded
									int count = 0;
									for (int j = tracks.length - 1; j >= 0; j--) {
										if (tracks[j].getStatus() != PodcastTrack.STATUS_DOWNLOAD_CANCELLED
												&& tracks[j].getStatus() != PodcastTrack.STATUS_DELETED
												&& tracks[j].getStatus() != PodcastTrack.STATUS_DOWNLOADED
												&& tracks[j].getStatus() != PodcastTrack.STATUS_PLAYED) {
											track = tracks[j];
										} else if (tracks[j].getStatus() == PodcastTrack.STATUS_DOWNLOADED
												|| tracks[j].getStatus() == PodcastTrack.STATUS_PLAYED) {
											count++;
										}
									}
									if (track != null) {
										int max = mPodcastingConfiguration.getDownload();
										if (max != -1) {
											if (count >= max) {
												PodcastTrack deleted = null;
												// Remove the oldest track that
												// has already been played
												for (int j = tracks.length - 1; j >= 0; j--) {
													if (tracks[j].getStatus() == PodcastTrack.STATUS_PLAYED) {
														PodcastTrack played = tracks[j];
														try {
															played = podcast.getTrack(played.getUrl());
															Audio audio = played.getTrack();
															synchronized (this) {
																if (audio != null && audio.getDatePlayed() != null) {
																	boolean delete = false;
																	if (track.getPublicationDate() == null) {
																		if (new Date().getTime()
																				- audio.getDatePlayed().getTime() > audio
																				.getDuration()) {
																			delete = true;
																		}
																	} else if (new Date().getTime()
																			- audio.getDatePlayed().getTime() > audio
																			.getDuration()
																			&& track.getPublicationDate().after(
																					played.getPublicationDate())) {
																		delete = true;
																	}

																	if (delete) {
																		deleteAudio(podcast, played);
																		deleted = played;
																		break;
																	}
																}
															}
														} catch (Exception ex) {
															Tools.logException(PodcastingThread.class, ex,
																	"Track update failed");
														}
													}
												}
												if (deleted == null)
													continue;
											}
										}
									}
								}
								if (track != null) {
									synchronized (this) {
										try {
											track = podcast.getTrack(track.getUrl());
										} catch (Exception ex) {
											Tools.logException(PodcastingThread.class, ex, "Track update failed");
										}
									}

									GetMethod get = null;
									try {
										HttpClient client = new HttpClient();
										get = new GetMethod(track.getUrl());
										client.executeMethod(get);

										if (get.getStatusCode() != 200)
											continue;

										synchronized (this) {
											try {
												track.setStatus(PodcastTrack.STATUS_DOWNLOADING);
												track.setSize(get.getResponseContentLength());
												PodcastManager.updatePodcast(podcast);
												track = podcast.getTrack(track.getUrl());
											} catch (HibernateException ex) {
												Tools.logException(PodcastingThread.class, ex, "Track update failed");
											}
										}

										InputStream input = get.getResponseBodyAsStream();

										String path = System.getProperty("data") + File.separator + "podcasts"
												+ File.separator + clean(podcast.getTitle());
										File dir = new File(path);
										if (!dir.exists()) {
											dir.mkdirs();
										}
										path = dir.getCanonicalPath();
										String name = clean(track.getTitle() + ".mp3");
										log.info("Downloading: " + track.getTitle());
										File file = new File(path + File.separator + name);
										WritableByteChannel channel = new FileOutputStream(file, false).getChannel();

										long total = 0;
										double diff = 0.0;
										ByteBuffer buf = ByteBuffer.allocateDirect(1024 * 4);
										byte[] bytes = new byte[1024 * 4];
										int amount = 0;
										int index = 0;
										long start = System.currentTimeMillis();
										long last = start;
										while (amount >= 0 && track.getStatus() == PodcastTrack.STATUS_DOWNLOADING) {
											if (index == amount) {
								                amount = input.read(bytes);
								                index = 0;
								                total = total + amount;
								            }
								            while (index < amount && buf.hasRemaining()) {
								                buf.put(bytes[index++]);
								            }
								    
								            buf.flip();
								            int numWritten = channel.write(buf);
								            if (buf.hasRemaining()) {
								                buf.compact();
								            } else {
								                buf.clear();
								            }
											
											if ((System.currentTimeMillis() - last > 10000) && (total > 0)) {
												synchronized (this) {
													try {
														podcast = PodcastManager.retrievePodcast(podcast);
														track = podcast.getTrack(track.getUrl());

														if (track.getStatus() == PodcastTrack.STATUS_DOWNLOADING) {
															diff = (System.currentTimeMillis() - start) / 1000.0;
															if (diff > 0) {
																track.setDownloadSize(total);
																track.setDownloadTime((int) diff);
																PodcastManager.updatePodcast(podcast);
																track = podcast.getTrack(track.getUrl());
															}
														}
													} catch (Exception ex) {
														Tools.logException(PodcastingThread.class, ex,
																"Track update failed");
													}
												}
												last = System.currentTimeMillis();
											}
											Thread.sleep(200); // give the CPU some breathing time
										}
										diff = (System.currentTimeMillis() - start) / 1000.0;
										 channel.close();

										if (track.getStatus() != PodcastTrack.STATUS_DOWNLOADING)
											continue;

										if (diff != 0)
											log.info("Download rate=" + (total / 1024) / diff + " KBps");
										synchronized (this) {
											try {
												Audio audio = null;
												List audios = AudioManager.findByPath(file.getCanonicalPath());
												if (audios != null && audios.size() > 0) {
													audio = (Audio) audios.get(0);
													if (audio != null) {
														audio.setOrigen("Podcast");
														if (track.getDuration()!=null)
															audio.setDuration(track.getDuration().longValue());
														AudioManager.updateAudio(audio);
													}
												} else {
													try
													{
														audio = Mp3File.getAudio(file.getCanonicalPath());
													} catch (Exception ex) {
														Tools.logException(PodcastingThread.class, ex, "Track tags failed");
													}
													if (audio != null) {
														audio.setOrigen("Podcast");
														if (track.getDuration()!=null)
															audio.setDuration(track.getDuration().longValue());
														AudioManager.createAudio(audio);
													}
												}
												if (audio != null) {
													track.setTrack(audio);
													track.setStatus(PodcastTrack.STATUS_DOWNLOADED);
													PodcastManager.updatePodcast(podcast);
												}
											} catch (Exception ex) {
												Tools.logException(PodcastingThread.class, ex, "Track update failed");
											}
										}
									} catch (MalformedURLException ex) {
										Tools.logException(PodcastingThread.class, ex, track.getUrl());
									} catch (Exception ex) {
										Tools.logException(PodcastingThread.class, ex, track.getUrl());
									} finally {
										get.releaseConnection();
									}
								}
							}
						}
						else
						{
							PodcastManager.deletePodcast(podcast);
						}
					}
				}
				synchronized(this)
            	{
            		wait(1000 * 60 * 60);
            	}
			} catch (InterruptedException ex) {
				Tools.logException(PodcastingThread.class, ex);
			} // handle silently for waking up
			catch (Exception ex2) {
				Tools.logException(PodcastingThread.class, ex2);
				try {
					sleep(1000 * 60);
				} catch (Exception ex) {
				}
			}
		}
	}

	private void deleteAudio(Podcast podcast, PodcastTrack track) {
		if (podcast != null && track != null) {
			Audio audio = track.getTrack();
			if (audio != null) {
				if (audio.getPath() != null) {
					File file = new File(audio.getPath());
					if (file.exists()) {
						file.delete();
						log.info("Removing podcast file: " + file.getAbsolutePath());
					}
					synchronized (this) {
						try {
							track.setDownloadSize(0);
							track.setDownloadTime(0);
							track.setTrack(null);
							track.setStatus(PodcastTrack.STATUS_DELETED);
							PodcastManager.updatePodcast(podcast);
							AudioManager.deleteAudio(audio);
						} catch (Exception ex) {
							Tools.logException(PodcastingThread.class, ex, "Audio delete failed: " + audio.getPath());
						}
					}
				}
			}
		}
	}

	private String clean(String value) {
		value = value.replaceAll(":", "_");
		value = value.replaceAll("\\\\", "_");
		value = value.replaceAll("/", "_");
		value = value.replaceAll("\"", "_");
		value = value.replaceAll("<", "_");
		value = value.replaceAll(">", "_");
		value = value.replaceAll("=", "_");
		value = value.replaceAll("\\*", "_");
		value = value.replaceAll("\\?", "_");
		return value;
	}

	public void progress(String value) {
		if (log.isDebugEnabled())
			log.debug(value);
	}

	public void interrupt() {
		synchronized (this) {
			super.interrupt();
		}
	}
	
	public void update()
	{
		synchronized (this) {
            notifyAll();
        }
	}

	PodcastingConfiguration mPodcastingConfiguration;
}