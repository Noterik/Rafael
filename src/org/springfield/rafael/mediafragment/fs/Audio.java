/* 
* Audio.java
* 
* Copyright (c) 2017 Noterik B.V.
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
package org.springfield.rafael.mediafragment.fs;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.restlet.Client;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.data.Status;
import org.restlet.representation.StringRepresentation;
import org.restlet.util.Series;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;

/**
 * Audio.java
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2016
 * @package org.springfield.rafael.mediafragment.fs
 * 
 */
public class Audio {
	private String identifier;
	private GlobalConfiguration conf;
	private static final Logger LOG = Logger.getLogger(Audio.class);
	
	public Audio(String identifier) {
		this.identifier = identifier;
		conf = GlobalConfiguration.getInstance();
	}
	
	/**
	 * Determine if this audio is marked as private
	 * in the Springfield file system and for that 
	 * reason requires a ticket
	 * 
	 * @param identifier
	 */
	public boolean isPrivate() {
		String springfieldUri = conf.getProperty("springfield-uri");
		
		identifier = identifier.substring(identifier.indexOf("/domain/"), identifier.lastIndexOf("/rawaudio/"));
		
		LOG.info("Checking audio is Private for uri "+identifier);
		Request request = new Request(Method.GET, springfieldUri+identifier);
		Context context = new Context();
		Series<Parameter> parameters = context.getParameters();
		parameters.add("socketTimeout", "1000");
		context.setParameters(parameters);
		Client client = new Client(context, Protocol.HTTP);
		Response response = client.handle(request);
		
		LOG.debug("response = "+response);
		LOG.debug(response.getEntityAsText());
		
		boolean privateAudio = true;
		
		try {
			Document fsxml = DocumentHelper.parseText(response.getEntityAsText());
			privateAudio = fsxml.selectSingleNode("//properties/private") == null ? true : Boolean.parseBoolean(fsxml.selectSingleNode("//properties/private").getText());
		} catch (DocumentException e) {
			
		}
		request.release();
		response.release();
		
		LOG.debug("Is audio private ? "+String.valueOf(privateAudio));
		
		return privateAudio;
	}
}
