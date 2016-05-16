/* 
* HeaderFilter.java
* 
* Copyright (c) 2016 Noterik B.V.
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

import org.apache.log4j.Logger;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.Restlet;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.routing.Filter;

/**
 * HeaderFilter.java
 *
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2016
 * @package org.springfield.rafael.mediafragment.restlet
 * 
 */
public class HeaderFilter extends Filter {
	private static final Logger LOG = Logger.getLogger(HeaderFilter.class);
	
	public HeaderFilter() {
		
	}
	
	public HeaderFilter(Context context) {
		super(context);
	}
	
	public HeaderFilter(Context context, Restlet next) {
		super(context, next);
	}
	
	@Override
	protected int beforeHandle(Request request, Response response) {
		LOG.debug("Intercepting filter");
		
		handleHttpIfRange(request);
		handleHttpIfModifiedSince(request);
		handleHttpIfUnModifiedSince(request);
		
		return CONTINUE;
	}
	
	/**
	 * This function removes the if-range header that Chrome uses
	 * Our restlet version cannot handle the data value that Chrome sends
	 * (only ETag) causing to return a 200 response with the full response.
	 * Chrome doesn't likes this and cancels the response
	 * 
	 * @param request
	 */
	private final void handleHttpIfRange(Request request) {
		((HttpRequest) request).getHeaders().removeAll("if-range");
	}
	
	/**
	 * This function removes the if-modified-since header that Chrome uses
	 * Our restlet version returns a 404 with the full response.
	 * Chrome doesn't like this and cancels the response
	 * 
	 * @param request
	 */
	private final void handleHttpIfModifiedSince(Request request) {
		((HttpRequest) request).getHeaders().removeAll("if-modified-since");
	}
	
	/**
	 * This function removes the if-unmodified-since header that IE uses
	 * Our restlet version returns a 404 with the full response
	 * IE doesn't like this and cancels the response
	 * 
	 * @param request
	 */
	private final void handleHttpIfUnModifiedSince(Request request) {
		((HttpRequest) request).getHeaders().removeAll("if-unmodified-since");
	}
}
