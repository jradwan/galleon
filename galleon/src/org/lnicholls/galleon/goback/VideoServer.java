package org.lnicholls.galleon.goback;

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

import com.tivo.hme.host.http.server.*;
import com.tivo.hme.host.http.share.Query;
import com.tivo.hme.host.io.FastInputStream;
import com.tivo.hme.host.io.FastOutputStream;
import com.tivo.hme.host.util.Config;
import com.tivo.hme.host.util.Cookies;
import com.tivo.hme.interfaces.*;
import java.io.*;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.log4j.Logger;

import org.lnicholls.galleon.database.VideoManager;
import org.lnicholls.galleon.server.*;
import org.lnicholls.galleon.util.*;
import org.lnicholls.galleon.util.FileSystemContainer.Item;
import org.lnicholls.galleon.widget.DefaultApplication;
import org.lnicholls.galleon.widget.DefaultApplication.Tracker;
import org.lnicholls.galleon.database.Video;
import org.lnicholls.galleon.database.VideoManager;

public class VideoServer extends HttpServer {

	private static Logger log = Logger.getLogger(VideoServer.class.getName());

	public VideoServer(Config config) throws IOException {
		super(config);
		config.put("http.acceptor.name", "VideoServer");
		
		InetAddress inetAddress = InetAddress.getLocalHost();
		mHost = inetAddress.getHostName();
		
		mFileDateFormat = new SimpleDateFormat();
        mFileDateFormat.applyPattern("EEE MMM d yyyy hh mma");
        mTimeDateFormat = new SimpleDateFormat();
        mTimeDateFormat.applyPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"); //2005-02-23T11:59:58Z
        mCalendar = new GregorianCalendar();
		
		start();
	}

	protected void handleException(Object obj, Throwable throwable) {
		if (throwable instanceof SocketException)
			return;
		else
			return;
	}

	void reply(HttpRequest httprequest, int i, String s) throws IOException {
		log.debug(httprequest.getInetAddress().getHostAddress() + " " + httprequest.getURI() + " HTTP "
						+ httprequest.getMethod() + " - " + i + " - " + s);
		httprequest.reply(i, s);
	}

	public void handle(HttpRequest httprequest) throws IOException {
		String s = httprequest.getURI();
		log.debug(s);
		
		ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
		ItemURL itemURL = new ItemURL(s);
		
		if (itemURL.getParameter(Constants.COMMAND)!=null && itemURL.getParameter(Constants.COMMAND).equalsIgnoreCase(Constants.COMMAND_QUERY_CONTAINER))
		{
			try {
				PrintWriter printWriter = null;
				try {
					httprequest.reply(200, "Success");
					String container = getContainer(itemURL);
					log.debug(container);
					OutputStream outputstream = httprequest.getOutputStream(container.length());
					printWriter = new PrintWriter(outputstream);
		            printWriter.print(container);
				} finally {
					if (printWriter!=null)
						printWriter.close();
				}
			} catch (IOException ioexception1) {
				log.debug(httprequest.getInetAddress().getHostAddress() + " I/O Exception handling " + " HTTP "
								+ httprequest.getMethod() + " " + s + ": " + ioexception1.getMessage());
			}
		}
		else
		{
			if (itemURL.getPath().startsWith("/TiVoConnect/TivoNowPlaying/Galleon/"))
			{
				String path = itemURL.getPath().substring("/TiVoConnect/TivoNowPlaying/Galleon/".length());
				log.debug("path="+path);
				
				File file = new File(serverConfiguration.getRecordingsPath()+"/"+path);
				if (file.exists())
				{
					try {
						PrintWriter printWriter = null;
						try {
							httprequest.reply(200, "Success");
							
							/*
							StringBuffer buffer = new StringBuffer();
							byte[] buf = new byte[1024];
							int amount = 0;
							try {
								InputStream input = new FileInputStream(file);
								while ((amount = input.read(buf)) > 0) {
									buffer.append(new String(buf, 0, amount));
								}
								input.close();
							} catch (Exception ex) {
								Tools.logException(Tools.class, ex);
							}
							String details = buffer.toString();
							*/
							
							String details = getVideoDetails(file);
							log.debug(details);
							OutputStream outputstream = httprequest.getOutputStream(details.length());
							printWriter = new PrintWriter(outputstream);
				            printWriter.print(details);
						} finally {
							if (printWriter!=null)
								printWriter.close();
						}
						return;
					} catch (IOException ioexception1) {
						log.debug(httprequest.getInetAddress().getHostAddress() + " I/O Exception handling " + " HTTP "
										+ httprequest.getMethod() + " " + s + ": " + ioexception1.getMessage());
					}
				}
			}
			
			try {
				String path = itemURL.getPath().substring("/TiVoConnect/Galleon/".length());
				log.debug("path="+path);
				
				File file = new File(serverConfiguration.getRecordingsPath()+"/"+path);
				if (!file.exists())
				{
					// Handle shortcuts 
					// TODO fix handling paths properly
					File directory = new File(serverConfiguration.getRecordingsPath());
					FileSystemContainer fileSystemContainer = new FileSystemContainer(directory.getCanonicalPath(), true);
					//List files = fileSystemContainer.getItemsSorted(FileFilters.videoDirectoryFilter);
					List files = fileSystemContainer.getItemsSorted(FileFilters.videoFilter);
					for (int i=0;i<files.size();i++)
					{
						Item nameFile = (Item) files.get(i);
						File match  = (File)nameFile.getValue();
						if (match.getName().equals(path))
						{
							file = match;
							break;
						}
					}
				}
				
				if (file.exists())
				{
					httprequest.reply(200, "Success");
					InputStream inputstream = new FileInputStream(file);
					try {
						OutputStream outputstream = httprequest.getOutputStream(inputstream.available());
						byte abyte0[] = new byte[4380];
						int i;
						while ((i = inputstream.read(abyte0, 0, abyte0.length)) > 0)
							outputstream.write(abyte0, 0, i);
					} finally {
						inputstream.close();
					}
				}
				else
					httprequest.reply(404, "File not found");
				
			} catch (IOException ioexception1) {
				log.debug(httprequest.getInetAddress().getHostAddress() + " I/O Exception handling " + " HTTP "
								+ httprequest.getMethod() + " " + s + ": " + ioexception1.getMessage());
			}
		}
	}
	
	private String getContainer(ItemURL itemURL)
	{
		ServerConfiguration serverConfiguration = Server.getServer().getServerConfiguration();
		
		File directory = new File(serverConfiguration.getRecordingsPath());
		if (directory.exists())
		{
			if (itemURL.getParameter(Constants.COMMAND).equalsIgnoreCase(Constants.COMMAND_QUERY_CONTAINER))
			{
				if (itemURL.getParameter(Constants.PARAMETER_CONTAINER).equals("/"))
				{
					StringBuffer buffer = new StringBuffer();
					synchronized(buffer)
					{
						buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
						buffer.append("<TiVoContainer>\n");
						buffer.append("<Details>\n");
						buffer.append("<Title>"+mHost+"</Title>\n");
						buffer.append("<ContentType>x-container/tivo-server</ContentType>\n");
						buffer.append("<SourceFormat>x-container/folder</SourceFormat>\n");
						buffer.append("<TotalItems>1</TotalItems>\n");
						buffer.append("</Details>\n");
						buffer.append("<Item>\n");
						buffer.append("<Details>\n");
						buffer.append("<Title>"+mHost+"</Title>\n");
						buffer.append("<ContentType>x-container/tivo-videos</ContentType>\n");
						buffer.append("<SourceFormat>x-container/folder</SourceFormat>\n");
						buffer.append("<LastChangeDate>0x"+Tools.dateToHex(new Date(directory.lastModified()))+"</LastChangeDate>\n");
						buffer.append("</Details>\n");
						buffer.append("<Links>\n");
						buffer.append("<Content>\n");
						buffer.append("<Url>http://"+serverConfiguration.getIPAddress()+":"+Server.getServer().getHMOPort()+"/TiVoConnect?Command=QueryContainer&amp;Container=Galleon</Url>\n");
						buffer.append("<ContentType>x-container/tivo-videos</ContentType>\n");
						buffer.append("</Content>\n");
						buffer.append("</Links>\n");
						buffer.append("</Item>\n");
						buffer.append("<ItemStart>0</ItemStart>\n");
						buffer.append("<ItemCount>1</ItemCount>\n");
						buffer.append("</TiVoContainer>\n");
						buffer.append("<!-- Copyright (c) 2005 Leon Nicholls. All rights reserved.-->\n");			
					}
					return buffer.toString();
				}
				else
				if (itemURL.getParameter(Constants.PARAMETER_CONTAINER).startsWith("Galleon"))
				{
					try
					{
						if (!itemURL.getParameter(Constants.PARAMETER_CONTAINER).equals("Galleon"))
						{
							String recordingsPath = directory.getAbsolutePath();
							String container = itemURL.getParameter(Constants.PARAMETER_CONTAINER);
							StringTokenizer tokenizer = new StringTokenizer(container,"/");
							if (tokenizer.hasMoreTokens())
							{
								tokenizer.nextToken();  // galleon
								while (tokenizer.hasMoreTokens())
								{
									recordingsPath = recordingsPath + "/" + tokenizer.nextToken();
								}
							}
							directory = new File(recordingsPath);
						}
						
						FileSystemContainer fileSystemContainer = new FileSystemContainer(directory.getCanonicalPath(), true);
						//List files = fileSystemContainer.getItemsSorted(FileFilters.videoDirectoryFilter);
						List files = fileSystemContainer.getItemsSorted(FileFilters.videoFilter);
						log.debug("files="+files.size());
						
						int itemCount = files.size();
						if (itemURL.getParameter(Constants.PARAMETER_ITEM_COUNT)!=null)
						{
							itemCount = Integer.parseInt(itemURL.getParameter(Constants.PARAMETER_ITEM_COUNT));
						}
						
						if (files.size()>0)
						{
							StringBuffer buffer = new StringBuffer();
							synchronized(buffer)
							{
								int start = 0;
								String anchorItem = itemURL.getParameter(Constants.PARAMETER_ANCHOR_ITEM);
								log.debug("anchorItem="+anchorItem);
								if (anchorItem!=null)
								{
									ItemURL anchorItemURL = new ItemURL(anchorItem);
									String path = null;
									if (anchorItemURL.getParameter(Constants.COMMAND)!=null && anchorItemURL.getParameter(Constants.COMMAND).equalsIgnoreCase(Constants.COMMAND_QUERY_CONTAINER))
									{
										String container = anchorItemURL.getParameter(Constants.PARAMETER_CONTAINER).substring(itemURL.getParameter(Constants.PARAMETER_CONTAINER).length());
										if (container.startsWith("/"))
											container = container.substring(1);
										log.debug("container="+container);
										path = container;
										StringTokenizer tokenizer = new StringTokenizer(container,"/");
										while (tokenizer.hasMoreTokens())
										{
											path = tokenizer.nextToken();
										}
										log.debug("path0="+path);
									}
									else
									{
										path = anchorItemURL.getPath().substring("/TiVoConnect/Galleon/".length());
									}
									log.debug("path="+path);
									for (int i=0;i<files.size();i++)
									{
										Item nameFile = (Item) files.get(i);
										File file = (File)nameFile.getValue();
										log.debug("compare:"+file.getName()+" with "+path);
										if (file.getName().equals(path))
										{
											start = i;
											log.debug("start="+start);
											String anchorOffset = itemURL.getParameter(Constants.PARAMETER_ANCHOR_OFFSET);
											log.debug("anchorOffset="+anchorOffset);
											if (anchorOffset!=null)
											{
												try
												{
													start = start + Integer.parseInt(anchorOffset) + 1;
												}
												catch (Exception ex){}
											}
										}
									}
								}
								log.debug("start1="+start);
								if (start<0)
									start = 0;
								log.debug("start2="+start);
								
								int limit = start+itemCount;
								log.debug("limit1="+limit);
								if (limit>files.size())
								{
									limit = files.size();
									itemCount = limit-start;
								}
								log.debug("limit2="+limit);
								log.debug("itemCount="+itemCount);
								
								if (itemCount==1)
								{
									Item nameFile = (Item) files.get(start);
									String filename = nameFile.getName();
									File file = (File)nameFile.getValue();
									// TODO Hack for skipping folders
									if (nameFile.isFolder()) {
										start = start + 1;
										itemCount = limit-start;
									}
								}
								
								buffer.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\" ?>\n");
								buffer.append("<TiVoContainer>\n");
								buffer.append("<ItemStart>"+start+"</ItemStart>\n");
								buffer.append("<ItemCount>"+itemCount+"</ItemCount>\n");
								buffer.append("<Details>\n");
								buffer.append("<Title>"+mHost+"</Title>\n");
								buffer.append("<ContentType>x-container/tivo-videos</ContentType>\n");
								buffer.append("<SourceFormat>x-container/folder</SourceFormat>\n");
								buffer.append("<LastChangeDate>0x"+Tools.dateToHex(new Date(directory.lastModified()))+"</LastChangeDate>\n");
								buffer.append("<TotalItems>"+files.size()+"</TotalItems>\n");
								//buffer.append("<TotalDuration>26797000</TotalDuration>\n");
								buffer.append("</Details>\n");								
								
								for (int i=start;i<limit;i++)
								{
									Item nameFile = (Item) files.get(i);
									String filename = nameFile.getName();
									File file = (File)nameFile.getValue();
									if (nameFile.isFolder()) {
										buffer.append("<Item>\n");
										buffer.append("<Details>\n");
										buffer.append("<Title>"+Tools.escapeXMLChars(filename)+"</Title>\n");
										buffer.append("<ContentType>x-container/tivo-videos</ContentType>\n");
										buffer.append("<SourceFormat>x-container/folder</SourceFormat>\n");
										buffer.append("<LastChangeDate>0x"+Tools.dateToHex(new Date(file.lastModified()))+"</LastChangeDate>\n");
										buffer.append("</Details>\n");
										buffer.append("<Links>\n");
										buffer.append("<Content>\n");
										buffer.append("<Url>http://"+serverConfiguration.getIPAddress()+":"+Server.getServer().getHMOPort()+"/TiVoConnect?Command=QueryContainer&amp;Container="+URLEncoder.encode(itemURL.getParameter(Constants.PARAMETER_CONTAINER)+"/"+filename)+"</Url>\n");
										buffer.append("<ContentType>x-container/tivo-videos</ContentType>\n");
										buffer.append("</Content>\n");
										buffer.append("</Links>\n");
										buffer.append("</Item>\n");
									}
									else
									{
										if (file.getName().endsWith(".mpg"))
										{
											buffer.append("<Item>\n");
											buffer.append("<Details>\n");
											buffer.append("<Title>"+Tools.escapeXMLChars(filename)+"</Title>\n");
											buffer.append("<ContentType>video/mpeg</ContentType>\n");
											buffer.append("<SourceFormat>video/mpeg</SourceFormat>\n");
											buffer.append("<SourceSize>"+file.length()+"</SourceSize>\n");
											buffer.append("</Details>\n");
											buffer.append("<Links>\n");
											buffer.append("<Content>\n");
											buffer.append("<ContentType>video/mpeg</ContentType>\n");
											buffer.append("<Url>http://"+serverConfiguration.getIPAddress()+":"+Server.getServer().getHMOPort()+"/TiVoConnect/"+URLEncoder.encode(itemURL.getParameter(Constants.PARAMETER_CONTAINER)+"/"+Tools.escapeXMLChars(file.getName()))+"</Url>\n");
											buffer.append("</Content>\n");
											buffer.append("<CustomIcon>\n");
											buffer.append("<ContentType>image/*</ContentType>\n");
											buffer.append("<AcceptsParams>No</AcceptsParams>\n");
											buffer.append("<Url>urn:tivo:image:save-until-i-delete-recording</Url>\n");
											buffer.append("</CustomIcon>\n");
											buffer.append("</Links>\n");
											buffer.append("</Item>\n");
										}
										else
										{
											Video video = null;
											try {
							                    log.debug(file.getCanonicalPath());
												List list = VideoManager.findByPath(file.getCanonicalPath());
							                    if (list!=null && list.size()>0)
							                    {
							                    	video = (Video)list.get(0);
							                    }
							                    else
							                    {
							                    	String path = file.getAbsolutePath();
							                    	path = path.substring(0,1).toLowerCase()+path.substring(1);
							                    	log.debug(path);
							                    	list = VideoManager.findByPath(path);
								                    if (list!=null && list.size()>0)
								                    {
								                    	video = (Video)list.get(0);
								                    }	
							                    }
							                } catch (Exception ex) {
							                    log.error("Video retrieve failed", ex);
							                }
							                
							                log.debug("video="+video);
											
											buffer.append("<Item>\n");
											buffer.append("<Details>\n");
											if (video!=null)
												buffer.append("<Title>"+Tools.escapeXMLChars(video.getSeriesTitle())+"</Title>\n");
											else
												buffer.append("<Title>"+Tools.escapeXMLChars(filename)+"</Title>\n");
											buffer.append("<ContentType>video/x-tivo-mpeg</ContentType>\n");
											buffer.append("<SourceFormat>video/x-tivo-mpeg</SourceFormat>\n");
											if (video!=null)
												buffer.append("<CaptureDate>0x"+Tools.dateToHex(video.getOriginalAirDate())+"</CaptureDate>\n");
											else
												buffer.append("<CaptureDate>0x"+Tools.dateToHex(new Date(file.lastModified()))+"</CaptureDate>\n");
											if (video!=null)
												buffer.append("<Description>"+Tools.escapeXMLChars(video.getDescription())+"</Description>\n");
											else
												buffer.append("<Description></Description>\n");
											if (video!=null)
												buffer.append("<EpisodeTitle>"+Tools.escapeXMLChars(video.getEpisodeTitle())+"</EpisodeTitle>\n");
											else
												buffer.append("<EpisodeTitle>"+Tools.escapeXMLChars(filename)+"</EpisodeTitle>\n");
											if (video!=null)
											{
												String channel = String.valueOf(video.getChannelMajorNumber());
								                if (channel.equals("0"))
								                	channel = video.getChannel();
												buffer.append("<SourceChannel>"+channel+"</SourceChannel>\n");
											}
											else
												buffer.append("<SourceChannel>0</SourceChannel>\n");
											if (video!=null)
												buffer.append("<SourceStation>"+video.getStation()+"</SourceStation>\n");
											else
												buffer.append("<SourceStation>0</SourceStation>\n");
											buffer.append("<SourceSize>"+file.length()+"</SourceSize>\n");
											buffer.append("</Details>\n");
											buffer.append("<Links>\n");
											buffer.append("<Content>\n");
											buffer.append("<ContentType>video/x-tivo-mpeg</ContentType>\n");
											buffer.append("<Url>http://"+serverConfiguration.getIPAddress()+":"+Server.getServer().getHMOPort()+"/TiVoConnect/"+URLEncoder.encode(itemURL.getParameter(Constants.PARAMETER_CONTAINER)+"/"+Tools.escapeXMLChars(file.getName()))+"</Url>\n");
											buffer.append("</Content>\n");
											buffer.append("<CustomIcon>\n");
											buffer.append("<ContentType>image/*</ContentType>\n");
											buffer.append("<AcceptsParams>No</AcceptsParams>\n");
											buffer.append("<Url>urn:tivo:image:save-until-i-delete-recording</Url>\n");
											buffer.append("</CustomIcon>\n");
											buffer.append("<TiVoVideoDetails>\n");
											buffer.append("<ContentType>text/xml</ContentType>\n");
											buffer.append("<AcceptsParams>No</AcceptsParams>\n");
											buffer.append("<Url>/TiVoConnect/TivoNowPlaying/"+URLEncoder.encode(itemURL.getParameter(Constants.PARAMETER_CONTAINER)+"/"+Tools.escapeXMLChars(file.getName()))+"?Format=text%2Fxml</Url>\n");
											buffer.append("</TiVoVideoDetails>\n");
											buffer.append("</Links>\n");
											buffer.append("</Item>\n");
										}
									}
								}
								buffer.append("</TiVoContainer>\n");
								buffer.append("<!-- Copyright (c) 2005 Leon Nicholls. All rights reserved.-->\n");							
							}
							return buffer.toString();
						}
					} catch (Exception ex) {
						Tools.logException(VideoServer.class, ex);
					}
				}
			}
		}
		return "";
	}
	
	private String getVideoDetails(File file)
	{
		Video video = getVideo(file);
		if (video!=null)
		{
			StringBuffer buffer = new StringBuffer();
			synchronized(buffer)
			{
				buffer.append("<?xml version=\"1.0\" encoding=\"utf-8\"?><TvBusMarshalledStruct:TvBusEnvelope xmlns:xs=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:TvBusMarshalledStruct=\"http://tivo.com/developer/xml/idl/TvBusMarshalledStruct\" xmlns:TvPgdRecording=\"http://tivo.com/developer/xml/idl/TvPgdRecording\" xmlns:TvBusDuration=\"http://tivo.com/developer/xml/idl/TvBusDuration\" xmlns:TvPgdShowing=\"http://tivo.com/developer/xml/idl/TvPgdShowing\" xmlns:TvDbShowingBit=\"http://tivo.com/developer/xml/idl/TvDbShowingBit\" xmlns:TvBusDateTime=\"http://tivo.com/developer/xml/idl/TvBusDateTime\" xmlns:TvPgdProgram=\"http://tivo.com/developer/xml/idl/TvPgdProgram\" xmlns:TvDbColorCode=\"http://tivo.com/developer/xml/idl/TvDbColorCode\" xmlns:TvPgdSeries=\"http://tivo.com/developer/xml/idl/TvPgdSeries\" xmlns:TvDbShowType=\"http://tivo.com/developer/xml/idl/TvDbShowType\" xmlns:TvPgdChannel=\"http://tivo.com/developer/xml/idl/TvPgdChannel\" xmlns:TvDbTvRating=\"http://tivo.com/developer/xml/idl/TvDbTvRating\" xmlns:TvDbRecordQuality=\"http://tivo.com/developer/xml/idl/TvDbRecordQuality\" xmlns:TvDbBitstreamFormat=\"http://tivo.com/developer/xml/idl/TvDbBitstreamFormat\" xs:schemaLocation=\"http://tivo.com/developer/xml/idl/TvBusMarshalledStruct TvBusMarshalledStruct.xsd http://tivo.com/developer/xml/idl/TvPgdRecording TvPgdRecording.xsd http://tivo.com/developer/xml/idl/TvBusDuration TvBusDuration.xsd http://tivo.com/developer/xml/idl/TvPgdShowing TvPgdShowing.xsd http://tivo.com/developer/xml/idl/TvDbShowingBit TvDbShowingBit.xsd http://tivo.com/developer/xml/idl/TvBusDateTime TvBusDateTime.xsd http://tivo.com/developer/xml/idl/TvPgdProgram TvPgdProgram.xsd http://tivo.com/developer/xml/idl/TvDbColorCode TvDbColorCode.xsd http://tivo.com/developer/xml/idl/TvPgdSeries TvPgdSeries.xsd http://tivo.com/developer/xml/idl/TvDbShowType TvDbShowType.xsd http://tivo.com/developer/xml/idl/TvPgdChannel TvPgdChannel.xsd http://tivo.com/developer/xml/idl/TvDbTvRating TvDbTvRating.xsd http://tivo.com/developer/xml/idl/TvDbRecordQuality TvDbRecordQuality.xsd http://tivo.com/developer/xml/idl/TvDbBitstreamFormat TvDbBitstreamFormat.xsd\" xs:type=\"TvPgdRecording:TvPgdRecording\">\n");
				buffer.append("<recordedDuration>PT"+video.getDuration()/(60*1000)+"M</recordedDuration>\n");  //PT59M59S
				buffer.append("<vActualShowing>\n");
				buffer.append("<element>\n");
				buffer.append("<showingBits value=\"1027\"/>\n");
				buffer.append("<time>"+mTimeDateFormat.format(video.getDateRecorded())+"</time>\n");
				buffer.append("<duration>PT"+video.getDuration()/(60*1000)+"M</duration>\n");  // PT1H, PT30M
				if (video.getPartCount()!=null)
					buffer.append("<partCount>1</partCount>\n");
				if (video.getPartIndex()!=null)
					buffer.append("<partIndex>1</partIndex>\n");
				buffer.append("<program>\n");
				buffer.append("<vActor>\n");
				StringTokenizer tokenizer = new StringTokenizer(video.getActors(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+tokenizer.nextToken()+"</element>\n");	
				}
				buffer.append("</vActor>\n");
				buffer.append("<vAdvisory/>\n");
				buffer.append("<vChoreographer/>\n");
				buffer.append("<colorCode value=\""+video.getColorCode()+"\">COLOR</colorCode>\n");
				buffer.append("<description>"+Tools.escapeXMLChars(video.getDescription())+"</description>\n");
				buffer.append("<vDirector>\n");
				tokenizer = new StringTokenizer(video.getDirectors(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vDirector>\n");
				buffer.append("<episodeNumber>"+video.getEpisodeNumber()+"</episodeNumber>\n");
				buffer.append("<episodeTitle>"+Tools.escapeXMLChars(video.getEpisodeTitle())+"</episodeTitle>\n");
				buffer.append("<vExecProducer>\n");
				tokenizer = new StringTokenizer(video.getExecProducers(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vExecProducer>\n");
				buffer.append("<vProgramGenre>\n");
				tokenizer = new StringTokenizer(video.getProgramGenre(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vProgramGenre>\n");
				buffer.append("<vGuestStar/>\n");
				buffer.append("<vHost/>\n");
				buffer.append("<isEpisode>"+video.getEpisodic()+"</isEpisode>\n");
				buffer.append("<originalAirDate>"+mTimeDateFormat.format(video.getOriginalAirDate())+"</originalAirDate>\n");
				buffer.append("<vProducer>\n");
				tokenizer = new StringTokenizer(video.getProducers(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vProducer>\n");
				buffer.append("<series>\n");
				buffer.append("<isEpisodic>"+video.getEpisodic()+"</isEpisodic>\n");
				buffer.append("<vSeriesGenre>\n");
				tokenizer = new StringTokenizer(video.getSeriesGenre(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vSeriesGenre>\n");
				buffer.append("<seriesTitle>"+Tools.escapeXMLChars(video.getSeriesTitle())+"</seriesTitle>\n");
				buffer.append("</series>\n");
				String showTypeValue = String.valueOf(video.getShowTypeValue());
				if (video.getShowTypeValue()==null)
					showTypeValue = "5";
				buffer.append("<showType value=\""+showTypeValue+"\">"+video.getShowType()+"</showType>\n");
				buffer.append("<title>"+Tools.escapeXMLChars(video.getTitle())+"</title>\n");
				buffer.append("<vWriter>\n");
				tokenizer = new StringTokenizer(video.getWriters(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vWriter>\n");
				buffer.append("</program>\n");
				buffer.append("<channel>\n");
				buffer.append("<displayMajorNumber>"+video.getChannelMajorNumber()+"</displayMajorNumber>\n");
				buffer.append("<displayMinorNumber>"+video.getChannelMinorNumber()+"</displayMinorNumber>\n");
				buffer.append("<callsign>"+Tools.escapeXMLChars(video.getCallsign())+"</callsign>\n");
				buffer.append("</channel>\n");
				String ratingValue = String.valueOf(video.getRatingValue());
				if (video.getRatingValue()==null)
					ratingValue = "4";
				buffer.append("<tvRating value=\""+ratingValue+"\">"+video.getRating()+"</tvRating>\n");
				buffer.append("</element>\n");
				buffer.append("</vActualShowing>\n");
				buffer.append("<vBookmark/>\n");
				String recordingQualityValue = String.valueOf(video.getRecordingQualityValue());
				if (video.getRecordingQualityValue()==null)
					recordingQualityValue = "75";
				buffer.append("<recordingQuality value=\""+recordingQualityValue+"\">"+video.getRecordingQuality()+"</recordingQuality>\n");
				buffer.append("<showing>\n");
				buffer.append("<showingBits value=\"1027\"/>\n");
				buffer.append("<time>"+mTimeDateFormat.format(video.getDateRecorded())+"</time>\n");
				buffer.append("<duration>PT"+video.getDuration()/(60*1000)+"M</duration>\n");  // PT1H, PT30M
				if (video.getPartCount()!=null)
					buffer.append("<partCount>1</partCount>\n");
				if (video.getPartIndex()!=null)
					buffer.append("<partIndex>1</partIndex>\n");
				buffer.append("<program>\n");
				buffer.append("<vActor>\n");
				tokenizer = new StringTokenizer(video.getActors(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vActor>\n");
				buffer.append("<vAdvisory/>\n");
				buffer.append("<vChoreographer/>\n");
				buffer.append("<colorCode value=\""+video.getColorCode()+"\">COLOR</colorCode>\n");
				buffer.append("<description>"+Tools.escapeXMLChars(video.getDescription())+"</description>\n");
				buffer.append("<vDirector>\n");
				tokenizer = new StringTokenizer(video.getDirectors(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vDirector>\n");
				buffer.append("<episodeNumber>"+video.getEpisodeNumber()+"</episodeNumber>\n");
				buffer.append("<episodeTitle>"+Tools.escapeXMLChars(video.getEpisodeTitle())+"</episodeTitle>\n");
				buffer.append("<vExecProducer>\n");
				tokenizer = new StringTokenizer(video.getExecProducers(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vExecProducer>\n");
				buffer.append("<vProgramGenre>\n");
				tokenizer = new StringTokenizer(video.getProgramGenre(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vProgramGenre>\n");
				buffer.append("<vGuestStar>\n");
				tokenizer = new StringTokenizer(video.getGuestStars(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vGuestStar>\n");
				buffer.append("<vHost>\n");
				tokenizer = new StringTokenizer(video.getHosts(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vHost>\n");
				buffer.append("<isEpisode>"+video.getEpisodic()+"</isEpisode>\n");
				buffer.append("<originalAirDate>"+mTimeDateFormat.format(video.getOriginalAirDate())+"</originalAirDate>\n");
				buffer.append("<vProducer/>\n");
				buffer.append("<series>\n");
				buffer.append("<isEpisodic>"+video.getEpisodic()+"</isEpisodic>\n");
				buffer.append("<vSeriesGenre>\n");
				tokenizer = new StringTokenizer(video.getSeriesGenre(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vSeriesGenre>\n");
				buffer.append("<seriesTitle>"+Tools.escapeXMLChars(video.getSeriesTitle())+"</seriesTitle>\n");
				buffer.append("</series>\n");
				buffer.append("<showType value=\""+showTypeValue+"\">"+video.getShowType()+"</showType>\n");
				buffer.append("<title>"+video.getTitle()+"</title>\n");
				buffer.append("<vWriter>\n");
				tokenizer = new StringTokenizer(video.getWriters(),";");
				while (tokenizer.hasMoreTokens())
				{
					buffer.append("<element>"+Tools.escapeXMLChars(tokenizer.nextToken())+"</element>\n");	
				}
				buffer.append("</vWriter>\n");
				buffer.append("</program>\n");
				buffer.append("<channel>\n");
				buffer.append("<displayMajorNumber>"+video.getChannelMajorNumber()+"</displayMajorNumber>\n");
				buffer.append("<displayMinorNumber>"+video.getChannelMinorNumber()+"</displayMinorNumber>\n");
				buffer.append("<callsign>"+Tools.escapeXMLChars(video.getCallsign())+"</callsign>\n");
				buffer.append("</channel>\n");
				buffer.append("<tvRating value=\""+ratingValue+"\">"+video.getRating()+"</tvRating>\n");
				buffer.append("</showing>\n");
				buffer.append("<startTime>"+mTimeDateFormat.format(video.getStartTime())+"</startTime>\n");
				buffer.append("<stopTime>"+mTimeDateFormat.format(video.getStopTime())+"</stopTime>\n");
				/*
				buffer.append("<bitstreamFormat>\n");
				buffer.append("<vFormat>\n");
				buffer.append("<element>\n");
				buffer.append("<vByte>\n");
				buffer.append("<base64>EjQAAwABAjoBywxXAAAADwAAAAQAAAACAAAAAwEAAQ==</base64>\n");
				buffer.append("</vByte>\n");
				buffer.append("</element>\n");
				buffer.append("</vFormat>\n");
				buffer.append("</bitstreamFormat>\n");
				*/
				buffer.append("<expirationTime>"+mTimeDateFormat.format(video.getExpirationTime())+"</expirationTime>\n");
				buffer.append("</TvBusMarshalledStruct:TvBusEnvelope>\n");				
			}
			return buffer.toString();
		}
		return null;
	}
	
	private Video getVideo(File file)
	{
		Video video = null;
		try {
	        log.debug(file.getCanonicalPath());
			List list = VideoManager.findByPath(file.getCanonicalPath());
	        if (list!=null && list.size()>0)
	        {
	        	video = (Video)list.get(0);
	        }
	        else
	        {
	        	String path = file.getAbsolutePath();
	        	path = path.substring(0,1).toLowerCase()+path.substring(1);
	        	log.debug(path);
	        	list = VideoManager.findByPath(path);
	            if (list!=null && list.size()>0)
	            {
	            	video = (Video)list.get(0);
	            }	
	        }
	    } catch (Exception ex) {
	        log.error("Video retrieve failed", ex);
	    }
	    return video;
	}
	
	// /TiVoConnect?Command=QueryContainer&Container=%2F
	
	private String mHost = "Galleon";
	
	protected SimpleDateFormat mFileDateFormat;

    protected SimpleDateFormat mTimeDateFormat;

    protected GregorianCalendar mCalendar;
}
