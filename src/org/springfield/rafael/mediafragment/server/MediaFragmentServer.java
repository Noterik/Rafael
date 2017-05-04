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
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.CacheDirective;
import org.restlet.data.CharacterSet;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.ServerInfo;
import org.restlet.data.Status;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.engine.header.Header;
import org.restlet.engine.header.HeaderConstants;
import org.restlet.representation.FileRepresentation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;
import org.springfield.rafael.mediafragment.server.MediaFragmentServer;
import org.springfield.rafael.mediafragment.uri.AbstractLocator;
import org.springfield.rafael.mediafragment.Fragment;
import org.springfield.rafael.mediafragment.MediaTypes;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;
import org.springfield.rafael.mediafragment.fs.Media;

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
	private static final String SERVER_INFO = "Rafael/0.2.5";
	private static final String[] VIDEO_EXTENSIONS = {"mp4", "m4v"};
	private static final String[] AUDIO_EXTENSIONS = {"m4a"};
	private static final String os = System.getProperty("os.name").toLowerCase();
	
	/**
	 * Handle GET request
	 */ 
	@Get
	public void handleGet() {
		GlobalConfiguration conf = GlobalConfiguration.getInstance();
		String basePath = conf.getProperty("basepath");
		String tempPath = conf.getProperty("temp-mediafragment-path");
		
		String fileIdentifier = getIdentifier(Request.getCurrent().getResourceRef().getPath(), conf.getProperty("contextPath"));
		LOG.info("identifier = "+fileIdentifier);
		
		// set server info
		ServerInfo serverInfo = new ServerInfo();
		serverInfo.setAgent(SERVER_INFO);			
		getResponse().setServerInfo(serverInfo);
		
		File resource = new File(basePath+fileIdentifier);
		
		MediaTypes mType;
		
		// check if requested file exists
		if (resource.exists() && resource.isFile()) {
			// check if we support this type
			String extension = fileIdentifier.lastIndexOf(".") == -1 ? "" : fileIdentifier.substring(fileIdentifier.lastIndexOf(".")+1);
			if (!supportedExtension(extension)) {
				getResponse().setStatus(Status.SERVER_ERROR_NOT_IMPLEMENTED);
				return;
			} else {
			    if (isAudioExtension(extension)) {
				mType = MediaTypes.AUDIO;
			    } else {
				mType = MediaTypes.VIDEO;
			    }
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
			
			Series<Header> series = ((HttpRequest) getRequest()).getHeaders();
			Header range = series.getFirst("range");
			
			File media = new File(filePath);
			
			if (fileIdentifier.indexOf("/domain/euscreen") > -1 || (fileIdentifier.indexOf("/domain/dans/") > -1 && new Media(fileIdentifier, mType).isPrivate())) {
			
				/** TODO: Abstract ticket handling in separate class **/
				String ticket = queryForm.getFirstValue("ticket", true, "").toLowerCase();
				LOG.debug("ticket = "+ticket);

				//Range request
				if (range != null) {
					String byteRange = range.getValue();
					LOG.debug("Requested byte range "+byteRange);
					byteRange = byteRange.substring(byteRange.indexOf("=")+1);
					long start = Long.parseLong(byteRange.substring(0, byteRange.indexOf("-")));
					long end = -1;
					if (byteRange.indexOf("-") < byteRange.length()-1) {
						end = Long.parseLong(byteRange.substring(byteRange.indexOf("-")+1));
					}
					
					if (end == -1l) {
						end = media.length();
					}
					
					LOG.debug("start = "+start+" end = "+end);
					
					//Safari fix that does multiple 0-1 requests
					if (start == 0l && end > 1l) {
						//only allowed with ticket allows
						String wowzaUri = conf.getProperty("wowza-server-uri");
	
						//get ticket
						StringRepresentation entity = new StringRepresentation("<fsxml><properties><uri>"+fileIdentifier+"</uri></properties></fsxml>");
						entity.setMediaType(MediaType.TEXT_XML);
						Request request = new Request(Method.PUT, wowzaUri+"/acl/ticketaccess/"+ticket, entity);
						Context context = new Context();
						Series<Parameter> parameters = context.getParameters();
						parameters.add("socketTimeout", "1000");
						context.setParameters(parameters);
						Client client = new Client(context, Protocol.HTTP);
						Response response = client.handle(request);
					
						LOG.debug("response = "+response);
						LOG.debug(response.getEntityAsText());
						
						try {
							Document fsxml = DocumentHelper.parseText(response.getEntityAsText());
							boolean allowed = fsxml.selectSingleNode("//properties/allowed") == null ? false : Boolean.parseBoolean(fsxml.selectSingleNode("//properties/allowed").getText());
							
							if (!allowed) {
								status = Status.CLIENT_ERROR_FORBIDDEN;
								getResponse().setStatus(status);
								return;
							}
						} catch (DocumentException e) {
							status = Status.SERVER_ERROR_INTERNAL;
							getResponse().setStatus(status);
							return;
						}
						request.release();
						response.release();
					} else {
						//for now unlimited allowed
					}				
				} else {
					//entire request only allowed once
					String wowzaUri = conf.getProperty("wowza-server-uri");
					
					//get ticket
					StringRepresentation entity = new StringRepresentation("<fsxml><properties><uri>"+fileIdentifier+"</uri></properties></fsxml>");
					entity.setMediaType(MediaType.TEXT_XML);
					Request request = new Request(Method.PUT, wowzaUri+"/acl/ticketaccess/"+ticket, entity);
					Context context = new Context();
					Series<Parameter> parameters = context.getParameters();
					parameters.add("socketTimeout", "1000");
					context.setParameters(parameters);
					Client client = new Client(context, Protocol.HTTP);
					Response response = client.handle(request);
					
					LOG.debug("response = "+response);
					LOG.debug(response.getEntityAsText());
						
					try {
						Document fsxml = DocumentHelper.parseText(response.getEntityAsText());
						boolean allowed = fsxml.selectSingleNode("//properties/allowed") == null ? false : Boolean.parseBoolean(fsxml.selectSingleNode("//properties/allowed").getText());
						
						if (!allowed) {
							status = Status.CLIENT_ERROR_FORBIDDEN;
							getResponse().setStatus(status);
							return;
						}
					} catch (DocumentException e) {
						status = Status.SERVER_ERROR_INTERNAL;
						getResponse().setStatus(status);
						return;
					}
					request.release();
					response.release();
				}
			}
			
			FileRepresentation rep;

			rep = new FileRepresentation(media, mType.getType());
						    
			getResponse().setEntity(rep);
			
			if (fileIdentifier.indexOf("/domain/euscreen") > -1 || (fileIdentifier.indexOf("/domain/dans/") > -1 && new Media(fileIdentifier, mType).isPrivate())) {			
				Series<Header> responseHeaders = (Series<Header>) getResponse().getAttributes().get(HeaderConstants.ATTRIBUTE_HEADERS);
				if (responseHeaders == null) {
					responseHeaders = new Series(Header.class); 
					getResponse().getAttributes().put(HeaderConstants.ATTRIBUTE_HEADERS, responseHeaders); 
				}

				responseHeaders.add(new Header("Pragma", "no-cache"));
				
				getResponse().getEntity().setExpirationDate(new Date(0));
				getResponse().setCacheDirectives(new ArrayList<CacheDirective>());
				getResponse().getCacheDirectives().add(CacheDirective.noCache());				
			}
				        	        
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
		//running windows
		if(os.contains("windows")){
			contextPath = contextPath.replace("\\", "/");
		}
		
		LOG.debug("Uri = "+uri+" contextPath  = "+contextPath);
		
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
		
		for (int i = 0; i < VIDEO_EXTENSIONS.length; i++) {
			if (VIDEO_EXTENSIONS[i].equals(extension)) { 
				return true;
			}
		}	
		for (int i = 0; i < AUDIO_EXTENSIONS.length; i++) {
			if (AUDIO_EXTENSIONS[i].equals(extension)) { 
				return true;
			}
		}
		return false;
	}
	
	/**
	 * check if this file is containing an audio extension
	 * 
	 * @param extension
	 * @return
	 */
	private boolean isAudioExtension(String extension) {
	    extension = extension.toLowerCase();
	    
	    for (int i = 0; i < AUDIO_EXTENSIONS.length; i++) {
		if (AUDIO_EXTENSIONS[i].equals(extension)) { 
			return true;
		}
	    }		
	    return false;
	}
	
	/**
	 * check if this is containing a video extension
	 * 
	 * @param extension
	 * @return
	 */
	private boolean isVideoExtension(String extension) {
	    extension = extension.toLowerCase();
	    
	    for (int i = 0; i < VIDEO_EXTENSIONS.length; i++) {
		if (VIDEO_EXTENSIONS[i].equals(extension)) { 
			return true;
		}
	    }		
	    return false;
	}	
}