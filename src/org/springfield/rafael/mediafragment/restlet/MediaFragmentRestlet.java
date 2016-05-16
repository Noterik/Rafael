/* 
* MediaFragmentRestlet.java
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

package org.springfield.rafael.mediafragment.restlet;

import org.restlet.Context;
import org.restlet.routing.Router;
import org.springfield.rafael.mediafragment.logging.LoggingResource;
import org.springfield.rafael.mediafragment.server.MediaFragmentServer;

/**
 * Redirects based on uri
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment.restlet
 *
 */
public class MediaFragmentRestlet extends Router {

	public MediaFragmentRestlet(Context cx) {
		super(cx);
		
		this.attach("/logging", LoggingResource.class);
		
		HeaderFilter headerFilter = new HeaderFilter(cx);
		this.attachDefault(headerFilter);
		headerFilter.setNext(MediaFragmentServer.class);
	}
}
