/* 
* MediaFragmentServer.java
* 
* Copyright (c) 2013 Noterik B.V.
* 
* This file is part of Rafael, related to the Noterik Springfield project.
*
* Rafael is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Rafael is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Rafael.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.springfield.rafael.mediafragment.server;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.restlet.Request;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.representation.FileRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.springfield.rafael.mediafragment.server.MediaFragmentServer;
import org.springfield.rafael.mediafragment.uri.AbstractLocator;
import org.springfield.rafael.mediafragment.Fragment;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;

/**
 * Media fragment server
 * 
 * Implementing Media Fragments URI 1.0 (basic) - temporal fragments only
 * 
 * Specs: http://www.w3.org/TR/2012/REC-media-frags-20120925/
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment.server
 *
 */

public class MediaFragmentServer extends ServerResource {
	private static final Logger LOG = Logger.getLogger(MediaFragmentServer.class);
	private static final String SERVER_INFO = "Rafael/0.1.4";
	private static final String[] SUPPORTED_EXTENSIONS = {"mp4", "m4v"};
	
	/**
	 * Handle GET request
	 */
	@Get
	public void handleGet() {
		GlobalConfiguration conf = GlobalConfiguration.getInstance();
		String basePath = conf.getProperty("basepath");
		String tempPath = conf.getProperty("temp-mediafragment-path");
		
		String fileIdentifier = getIdentifier(Request.getCurrent().getResourceRef().getPath(), conf.getProperty("contextPath"));
		LOG.debug("identifier = "+fileIdentifier);
		
		// set server info
		ServerInfo serverInfo = new ServerInfo();
		serverInfo.setAgent(SERVER_INFO);			
		getResponse().setServerInfo(serverInfo);
		
		File resource = new File(basePath+fileIdentifier);
		
		// check if requested file exists
		if (resource.exists() && resource.isFile()) {
			// check if we support this type
			String extension = fileIdentifier.lastIndexOf(".") == -1 ? "" : fileIdentifier.substring(fileIdentifier.lastIndexOf(".")+1);
			if (!supportedExtension(extension)) {
				getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
				return;
			}
			
			// get query
			Form queryForm = getRequest().getResourceRef().getQueryAsForm(CharacterSet.UTF_8);
			String t = queryForm.getFirstValue("t", true, "").toLowerCase();

			String filePath = null;
			Status status = null;
			// media fragment requested
			if (!t.equals("")) {			
				Fragment fragment = new Fragment(t, basePath, tempPath, fileIdentifier);
				status = fragment.getStatus();
				filePath = fragment.getFilename();
			} else {
				//whole file requested
				status = Status.SUCCESS_OK;
				filePath = basePath+fileIdentifier;
			}
			LOG.debug("Media Fragment status "+status);
			getResponse().setStatus(status);
			
			if (!status.equals(Status.SUCCESS_OK)) {
				return;
			}
			
			File video = new File(filePath);
			
			FileRepresentation rep = new FileRepresentation(video, MediaType.VIDEO_MP4);
	        getResponse().setEntity(rep);	        
	        return;
		} else if (resource.exists() && resource.isDirectory()) {					
			//resource is a folder, check for abstract locator	
			Pattern p = Pattern.compile("/domain/[^/]+/user/[^/]+/video/[0-9]+/?$");
			Matcher m = p.matcher(fileIdentifier);
			if (m.find()) {
				Form queryForm = getRequest().getResourceRef().getQueryAsForm(CharacterSet.UTF_8);
				AbstractLocator abstractLocator = new AbstractLocator(fileIdentifier, queryForm, getClientInfo());
				abstractLocator.getStatus();
				if (abstractLocator.getStatus() == Status.REDIRECTION_SEE_OTHER) {				
					LOG.debug("REDIRECT");
					LOG.debug("REDIRECT to "+abstractLocator.getUri());
					getResponse().redirectSeeOther(abstractLocator.getUri());
					return;
				} else {
					LOG.debug("NO REDIRECT");
					getResponse().setStatus(abstractLocator.getStatus());
					return;
				}
			}
			LOG.debug("FORBIDDEN");
			getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN);
			return;
		} else {
			LOG.debug("NOT FOUND");
			getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND);
			return;
		}
	}
	
	/**
	 * get file identifier from uri
	 * 
	 * @param uri - the requested uri
	 * @param contextPath - the context path
	 * @return file identifier
	 */
	private String getIdentifier(String uri, String contextPath) {
		String[] path = contextPath.split("/");

		return uri.substring(uri.indexOf(path[path.length-1])+path[path.length-1].length());
	}
	
	/**
	 * check if this file type is in the list of supported extensions
	 * 
	 * @param extension - extension of the file
	 * @return true if supported, otherwise false
	 */
	private boolean supportedExtension(String extension) {
		extension = extension.toLowerCase();
		
		for (int i = 0; i < SUPPORTED_EXTENSIONS.length; i++) {
			if (SUPPORTED_EXTENSIONS[i].equals(extension)) { 
				return true;
			}
		}		
		return false;
	}
}
