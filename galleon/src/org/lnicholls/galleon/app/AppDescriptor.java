package org.lnicholls.galleon.app;

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
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.net.*;
import java.io.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.lnicholls.galleon.util.Tools;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Plugin descriptor class extracts the plugin.xml file from the plugin jar and determines the plugin properties
 */
public class AppDescriptor {
    private static Logger log = Logger.getLogger(AppDescriptor.class.getName());

    public AppDescriptor(File jar) throws IOException, AppException {
        mJar = jar;
        
        mZipFile = new JarFile(jar);
        Enumeration entries = mZipFile.entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = (ZipEntry) entries.nextElement();
            String name = entry.getName();
            if (log.isDebugEnabled())
                log.debug("zip entry:" + name);
            if (name.equals("plugin.xml")) {
                getDescriptor(entry);
                break;
            }
        }
        mZipFile.close();
    }

    private void getDescriptor(ZipEntry entry) throws AppException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        //factory.setValidating(true);
        //factory.setNamespaceAware(true);
        InputStream in = null;
        try {
            DocumentBuilder builder = factory.newDocumentBuilder();
            in = mZipFile.getInputStream(entry);
            Document document = builder.parse(in);
            if (log.isDebugEnabled())
                log.debug("document:" + document.getNodeName());

            // <plugin>
            Node domNode = document.getFirstChild();

            if (domNode.getNodeName().equalsIgnoreCase("plugin")) {
                NamedNodeMap namedNodeMap = domNode.getAttributes();
                if (namedNodeMap != null) {
                    // Check for required attributes
                    Node attribute = namedNodeMap.getNamedItem("name");
                    if (attribute != null)
                        setName(attribute.getNodeValue());
                    else
                        throw new AppException("Missing name attribute for plugin descriptor: " + mJar.toString());
                    attribute = namedNodeMap.getNamedItem("class");
                    if (attribute != null)
                        setClassName(attribute.getNodeValue());
                    else
                        throw new AppException("Missing class attribute for plugin descriptor: " + mJar.toString());
                    attribute = namedNodeMap.getNamedItem("version");
                    if (attribute != null)
                        setVersion(attribute.getNodeValue());
                    else
                        throw new AppException("Missing version attribute for plugin descriptor: " + mJar.toString());

                    // Check for non-required attributes
                    attribute = namedNodeMap.getNamedItem("icon");
                    if (attribute != null)
                        setIcon(attribute.getNodeValue());
                    attribute = namedNodeMap.getNamedItem("releaseDate");
                    if (attribute != null)
                        setReleaseDate(attribute.getNodeValue());
                    attribute = namedNodeMap.getNamedItem("documentation");
                    if (attribute != null)
                        setDocumentation(attribute.getNodeValue());
                }
            }

            // <description>, <author>
            for (int i = 0; i < domNode.getChildNodes().getLength(); i++) {
                Node node = domNode.getChildNodes().item(i);

                if (node.getNodeType() == Node.ELEMENT_NODE) {
                    if (node.getNodeName().equalsIgnoreCase("description"))
                        setDescription(getText(node));
                    else if (node.getNodeName().equalsIgnoreCase("author")) {
                        NamedNodeMap namedNodeMap = node.getAttributes();
                        if (namedNodeMap != null) {
                            Node attribute = namedNodeMap.getNamedItem("name");
                            // Required attributes
                            if (attribute != null)
                                setAuthorName(attribute.getNodeValue());
                            else
                                throw new AppException("Missing author name attribute for plugin descriptor: "
                                        + mJar.toString());
                            attribute = namedNodeMap.getNamedItem("email");
                            if (attribute != null)
                                setAuthorEmail(attribute.getNodeValue());
                            else
                                throw new AppException("Missing author email attribute for plugin descriptor: "
                                        + mJar.toString());

                            attribute = namedNodeMap.getNamedItem("homepage");
                            if (attribute != null)
                                setAuthorHomepage(attribute.getNodeValue());
                        }
                    }
                }
            }
        } catch (SAXParseException spe) {
            // Error generated by the parser
            log.error("Parsing error, line " + spe.getLineNumber() + ", uri " + spe.getSystemId());
            log.error("   " + spe.getMessage());
            Tools.logException(AppDescriptor.class, spe);

            // Use the contained exception, if any
            Exception x = spe;
            if (spe.getException() != null)
                x = spe.getException();
            Tools.logException(AppDescriptor.class, x);

        } catch (SAXException sxe) {
            // Error generated during parsing)
            Exception x = sxe;
            if (sxe.getException() != null)
                x = sxe.getException();
            Tools.logException(AppDescriptor.class, x);
        } catch (ParserConfigurationException pce) {
            // Parser with specified options can't be built
            log.error("Cannot get context" + entry.getName());
            Tools.logException(AppDescriptor.class, pce);
        } catch (IOException ioe) {
            // I/O error
            log.error("Cannot get context" + entry.getName());
            Tools.logException(AppDescriptor.class, ioe);
        } finally {
            if (in != null) {
                try {
                    in.close();
                    in = null;
                } catch (Exception ex) {
                }
            }
        }
    }

    private String getText(Node node) {
        StringBuffer result = new StringBuffer();
        if (!node.hasChildNodes())
            return "";

        NodeList list = node.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node subnode = list.item(i);
            if (subnode.getNodeType() == Node.TEXT_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.CDATA_SECTION_NODE) {
                result.append(subnode.getNodeValue());
            } else if (subnode.getNodeType() == Node.ENTITY_REFERENCE_NODE) {
                // Recurse into the subtree for text
                // (and ignore comments)
                result.append(getText(subnode));
            }
        }
        return result.toString();
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();
        synchronized (buffer) {
            buffer.append("name=" + getName() + "\n");
            buffer.append("jar=" + mJar + "\n");
            buffer.append("icon=" + getIcon() + "\n");
            buffer.append("className=" + getClassName() + "\n");
            buffer.append("version=" + getVersion() + "\n");
            buffer.append("releaseDate=" + getReleaseDate() + "\n");
            buffer.append("description=" + getDescription() + "\n");
            buffer.append("documentation=" + getDocumentation() + "\n");
            buffer.append("authorName=" + getAuthorName() + "\n");
            buffer.append("authorEmail=" + getAuthorEmail() + "\n");
            buffer.append("authorHomepage=" + getAuthorHomepage());
        }
        return buffer.toString();
    }

    public void setName(String value) {
        mName = value;
    }

    public String getName() {
        return mName;
    }

    public void setIcon(String value) {
        mIcon = value;
    }

    public String getIcon() {
        return mIcon;
    }

    public void setClassName(String value) {
        mClassName = value;
    }

    public String getClassName() {
        return mClassName;
    }

    public void setVersion(String value) {
        mVersion = value;
    }

    public String getVersion() {
        return mVersion;
    }

    public void setReleaseDate(String value) {
        mReleaseDate = value;
    }

    public String getReleaseDate() {
        return mReleaseDate;
    }

    public void setDescription(String value) {
        mDescription = value;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDocumentation(String value) {
        mDocumentation = value;
    }

    public String getDocumentation() {
        return mDocumentation;
    }

    public void setAuthorName(String value) {
        mAuthorName = value;
    }

    public String getAuthorName() {
        return mAuthorName;
    }

    public void setAuthorEmail(String value) {
        mAuthorEmail = value;
    }

    public String getAuthorEmail() {
        return mAuthorEmail;
    }

    public void setAuthorHomepage(String value) {
        mAuthorHomepage = value;
    }

    public String getAuthorHomepage() {
        return mAuthorHomepage;
    }
    
    private JarFile mZipFile;

    private File mJar;

    private String mName;

    private String mIcon;

    private String mClassName;

    private String mVersion;

    private String mReleaseDate;

    private String mDescription;

    private String mDocumentation;

    private String mAuthorName;

    private String mAuthorEmail;

    private String mAuthorHomepage;
}