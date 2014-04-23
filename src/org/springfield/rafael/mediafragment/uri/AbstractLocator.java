/* 
* AbstractLocator.java
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

package org.springfield.rafael.mediafragment.uri;

import java.net.URLDecoder;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.restlet.Client;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.Method;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.springfield.rafael.mediafragment.uri.AbstractLocator;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;

/**
 * Abstract locator handling
 * 
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment.uri
 *
 */
public class AbstractLocator {
	private String abstractPath;
	private String queryString;
	private Status status;
	private Form queryForm;
	private ClientInfo clientInfo;

	private static final Logger LOG = Logger.getLogger(AbstractLocator.class);
	
	/**
	 * Abstractor locator matches a video path to a specific video instance
	 * based on client parameters like device, resolution, etc
	 * Further more conditional access is applied, a valid ticket need to be 
	 * supplied in order to play out. 
	 * 
	 * @param abstractPath
	 * @param queryForm
	 * @param clientInfo
	 */
	public AbstractLocator(String abstractPath, Form queryForm, ClientInfo clientInfo) {
		this.abstractPath = abstractPath.endsWith("/") ? abstractPath.substring(0, abstractPath.length()-1) : abstractPath;
		this.queryString = URLDecoder.decode(queryForm.getQueryString());
		this.queryForm = queryForm;
		this.clientInfo = clientInfo;
		
		GlobalConfiguration conf = GlobalConfiguration.getInstance();
		this.abstractPath = conf.getProperty("server-basepath") + this.abstractPath;

		String ticket = queryForm.getFirstValue("ticket", true);
		LOG.debug("ticket = "+ticket);
		LOG.debug("abstract path = "+abstractPath);
		if (ticket == null) {
			//TODO: check with smithers if ticket is required for this video
			status = Status.CLIENT_ERROR_FORBIDDEN;
			return;
		} else {
			String wowzaUri = conf.getProperty("wowza-server-uri");

			Request request = new Request(Method.GET, wowzaUri+"/acl/ticket/"+ticket);
			Client client = new Client(Protocol.HTTP);
			Response response = client.handle(request);
			
			//TODO: add support for ip whitelisting instead of ticketing
			
			try {
				Document fsxml = DocumentHelper.parseText(response.getEntityAsText());
				String uri = fsxml.selectSingleNode("//properties/uri") == null ? null : fsxml.selectSingleNode("//properties/uri").getText();
				//String fsxmlStatus = fsxml.selectSingleNode("//properties/status") == null ? null : fsxml.selectSingleNode("//properties/status").getText();
				
				String domainPath = abstractPath.indexOf("/domain/") == -1 ? abstractPath : abstractPath.substring(abstractPath.indexOf("/domain/"));
				
				if (uri != null && uri.startsWith(domainPath)) {
					status = Status.REDIRECTION_SEE_OTHER;
					return;
				}
				status = Status.CLIENT_ERROR_FORBIDDEN;
				return;
			} catch (DocumentException e) {
				status = Status.SERVER_ERROR_INTERNAL;
				return;
			}
		}		
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getUri() {
		int rawvideo = 2;
		
		//TODO: check with smithers for qualities of the video (bitrate, width, height, etc) to determine correct raw
		
		// check user agent for iPhone or iPad
		String ua = clientInfo.getAgent();
		if (ua.contains("iPhone") || ua.contains("iPad")) {
			rawvideo = 2;
		}		
		
		 // check if resolution parameter was set
		if (queryForm.getFirstValue("resolution") != null) {
			String[] resolution = queryForm.getFirstValue("resolution").split(",");
			if (resolution.length == 2) {
				int width = Integer.parseInt(resolution[0]);
				int height = Integer.parseInt(resolution[1]);
				
				if (width < 640 || height < 480) {
					rawvideo = 5;
				} else if (width < 1280 || height < 720) {
					rawvideo = 2;
				} else if (width < 1920 || height < 1080) {
					rawvideo = 3;
				} else if (width >= 1920 && height >= 1080) {
					rawvideo = 4;
				} else {
					rawvideo = 2;
				}
			}			
		}
		
		
		 // check if a direct raw video was requested		 
		if (queryForm.getFirstValue("rawvideo") != null) {
			rawvideo = Integer.parseInt(queryForm.getFirstValue("rawvideo"));
		}
		return abstractPath+"/rawvideo/"+rawvideo+"/raw.mp4?"+queryString;
	}
}
