/* 
* MediaFragmentInitialListener.java
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

import java.util.Timer;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.springfield.rafael.mediafragment.restlet.MediaFragmentCleanerTask;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;

/**
 * Initial listener
 * 
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment.restlet
 *
 */
public class MediaFragmentInitialListener implements ServletContextListener {
	private Timer cleanerTimer = new Timer();
	private static final long CLEANER_INTERVAL = 15 * 60 * 1000; // every 15 minutes
	
	public void contextInitialized(ServletContextEvent event) {
		System.out.println("Rafael: context created");
		ServletContext servletContext = event.getServletContext();
		
		//load config
		GlobalConfiguration globalConfiguration = GlobalConfiguration.getInstance();
		globalConfiguration.initConfig(servletContext.getRealPath("/"));
		//schedule cleaner
		cleanerTimer.schedule(new MediaFragmentCleanerTask(), 0, CLEANER_INTERVAL);
	}
	
	public void contextDestroyed(ServletContextEvent event) {
		cleanerTimer.cancel();
		System.out.println("Rafael: context destroyed");
	}
}
