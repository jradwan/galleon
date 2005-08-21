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

package org.lnicholls.galleon.data;

import java.awt.image.BufferedImage;
import java.awt.image.PixelGrabber;
import java.io.InputStream;
import java.io.StringReader;
import java.util.Iterator;

import javax.imageio.ImageIO;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.lnicholls.galleon.server.DataConfiguration;
import org.lnicholls.galleon.server.ServerConfiguration;
import org.lnicholls.galleon.util.GalleonException;
import org.lnicholls.galleon.util.Tools;

public class Users {

	private static String HOST = "localhost";

	private static Logger log = Logger.getLogger(Users.class.getName());

	private static HttpClient httpclient = new HttpClient();

	static {
		httpclient.getParams().setParameter("http.socket.timeout", new Integer(30000));
		httpclient.getParams().setParameter("http.useragent", "Galleon " + Tools.getVersion());
	}

	public static Object getCode() {
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/code.php");
		post.setFollowRedirects(false);

		try {
			int code = httpclient.executeMethod(post);
			if (code == 200) {
				InputStream inputStream = post.getResponseBodyAsStream();
				BufferedImage image = ImageIO.read(inputStream);
				image = (BufferedImage) Tools.getImage(image);
				PixelGrabber grabber = new PixelGrabber(image, 0, 0, -1, -1, true);
				grabber.grabPixels();
				return grabber.getPixels();
			}
		} catch (Exception ex) {
			log.error("Could not contact server", ex);
		} finally {
			post.releaseConnection();
		}
		return null;
	}

	public static void createUser(DataConfiguration dataConfiguration) throws Exception {
		log.debug("createUser: " + dataConfiguration);
		// TODO https
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/duplicateUser.php");
		post.setFollowRedirects(false);
		NameValuePair username = new NameValuePair("username", dataConfiguration.getUsername());
		post.setQueryString(new NameValuePair[] { username });
		post.addParameter(username);

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not update user", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not contact server", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}

		post = new PostMethod("http://" + HOST + "/galleon/xml/createUser.php");
		post.setFollowRedirects(false);
		username = new NameValuePair("username", dataConfiguration.getUsername());
		NameValuePair password = new NameValuePair("password", Tools.decrypt(dataConfiguration.getPassword()));
		NameValuePair anonymous = new NameValuePair("anon", Boolean.toString(dataConfiguration.isAnonymous()));
		NameValuePair security = new NameValuePair("code", dataConfiguration.retrieveCode());
		//post.setQueryString(new NameValuePair[] { username, password, security, anonymous });
		post.addParameter(username);
		post.addParameter(password);
		post.addParameter(security);
		post.addParameter(anonymous);

		//log.debug(post.getQueryString());

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}

				if (root.elements().size() > 0) {
					throw new GalleonException("Server error");
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not create user", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not contact server", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}
		
		logout(dataConfiguration);
	}

	public static void updateUser(DataConfiguration dataConfiguration, ServerConfiguration serverConfiguration) throws Exception {
		log.debug("updateUser: " + dataConfiguration);
		// TODO https
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/updateUser.php");
		post.setFollowRedirects(false);
		NameValuePair username = new NameValuePair("username", dataConfiguration.getUsername());
		NameValuePair oldPassword = new NameValuePair("oldPassword", Tools.decrypt(serverConfiguration.getDataConfiguration().getPassword()));
		NameValuePair password = new NameValuePair("password", Tools.decrypt(dataConfiguration.getPassword()));
		NameValuePair security = new NameValuePair("code", dataConfiguration.retrieveCode());
		NameValuePair anonymous = new NameValuePair("anon", Boolean.toString(dataConfiguration.isAnonymous()));
		//post.setQueryString(new NameValuePair[] { username, oldPassword, password, security, anonymous });
		post.addParameter(username);
		post.addParameter(oldPassword);
		post.addParameter(password);
		post.addParameter(security);
		post.addParameter(anonymous);

		//log.debug(post.getQueryString());

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}

				if (root.elements().size() > 0) {
					throw new GalleonException("Server error");
				}

				for (Iterator i = root.elementIterator(); i.hasNext();) {
					Element element = (Element) i.next();
					System.out.println("element=" + element);
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not update user", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not contact server", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}
		
		logout(dataConfiguration);
	}
	
	public static void deleteUser(DataConfiguration dataConfiguration) throws Exception {
		log.debug("deleteUser: " + dataConfiguration);
		// TODO https
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/deleteUser.php");
		post.setFollowRedirects(false);
		NameValuePair username = new NameValuePair("username", dataConfiguration.getUsername());
		NameValuePair password = new NameValuePair("password", Tools.decrypt(dataConfiguration.getPassword()));
		NameValuePair security = new NameValuePair("code", dataConfiguration.retrieveCode());
		//post.setQueryString(new NameValuePair[] { username, password, security });
		post.addParameter(username);
		post.addParameter(password);
		post.addParameter(security);

		//log.debug(post.getQueryString());

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}

				if (root.elements().size() > 0) {
					throw new GalleonException("Server error");
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not delete user", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not contact server", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}
		
		logout(dataConfiguration);
	}
	
	public static void login(DataConfiguration dataConfiguration) throws Exception {
		log.debug("login: " + dataConfiguration);
		// TODO https
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/login.php");
		post.setFollowRedirects(false);
		NameValuePair username = new NameValuePair("username", dataConfiguration.getUsername());
		NameValuePair password = new NameValuePair("password", Tools.decrypt(dataConfiguration.getPassword()));
		//post.setQueryString(new NameValuePair[] { username, password });
		post.addParameter(username);
		post.addParameter(password);

		//log.debug(post.getQueryString());

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}

				if (root.elements().size() > 0) {
					throw new GalleonException("Server error");
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not login", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not login", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}
	}
	
	public static void logout(DataConfiguration dataConfiguration) throws Exception {
		log.debug("logout: " + dataConfiguration);
		PostMethod post = new PostMethod("http://" + HOST + "/galleon/xml/logout.php");
		post.setFollowRedirects(false);

		log.debug(post.getQueryString());

		try {
			if (httpclient.executeMethod(post) == 200) {
				String strGetResponseBody = post.getResponseBodyAsString();
				log.debug(strGetResponseBody);

				SAXReader saxReader = new SAXReader();
				StringReader stringReader = new StringReader(strGetResponseBody);
				Document document = saxReader.read(stringReader);

				Element root = document.getRootElement();
				Element error = root.element("error");
				if (error != null) {
					String code = Tools.getAttribute(error, "code");
					String reason = Tools.getAttribute(error, "reason");
					throw new GalleonException(reason, Integer.parseInt(code));
				}

				if (root.elements().size() > 0) {
					throw new GalleonException("Server error");
				}
			}
		} catch (GalleonException ex) {
			log.error("Could not logout", ex);
			throw ex;
		} catch (Exception ex) {
			log.error("Could not logout", ex);
			throw new GalleonException("Server error");
		} finally {
			post.releaseConnection();
		}
	}
}