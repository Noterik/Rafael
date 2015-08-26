/* 
* GlobalConfiguration.java
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

package org.springfield.rafael.mediafragment.config;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.DailyRollingFileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.springfield.rafael.mediafragment.config.GlobalConfiguration;

/**
 * Global configuration
 * 
 * @author Pieter van Leeuwen
 * @copyright Copyright: Noterik B.V. 2013
 * @package org.springfield.rafael.mediafragment.config
 *
 */

public class GlobalConfiguration {	
	private Properties properties;
	
	private static GlobalConfiguration instance;
	private static String CONFIG_FILE = "config/config.xml";
	public static final String PACKAGE_ROOT = "org.springfield.rafael.mediafragment";

	/**
	 * Instance 
	 */
	static {
		instance = new GlobalConfiguration();
	}
	
	/** 
	 * Constructor
	 */
	public GlobalConfiguration() {
		properties = new Properties();
	}
	
	/**
	 * Get instance 
	 * 
	 * @return globalconfiguration instance
	 */
	public static GlobalConfiguration getInstance() { return instance; }

	/**
	 * Initialze config 
	 * 
	 * @param contextPath
	 */
	public void initConfig(String contextPath) {
		try {
			File file = new File(contextPath+CONFIG_FILE);
			if (file.exists()) {
				properties.loadFromXML(new BufferedInputStream(new FileInputStream(file)));
			} else {
				System.out.println("ERROR: cannot load configuration file: "+contextPath+CONFIG_FILE);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		properties.put("contextPath", contextPath);		
		initLogging(contextPath);
	}
	
	public String getProperty(String key) {
		return properties.getProperty(key, "");
	}
	
	/**
	 * Initialize logging
	 * 
	 * @param contextPath
	 */
	private void initLogging(String contextPath) {
		System.out.println("Rafael: Initializing logging");

		// enable appenders
		String logPath = contextPath.substring(0,contextPath.indexOf("webapps"));
		logPath += "logs/rafael/rafael.log";	

		try {
			// default layout
			Layout layout = new PatternLayout("%-5p: %d{yyyy-MM-dd HH:mm:ss,SSS} %c %x - %m%n");
				
			// rolling file appender
			DailyRollingFileAppender appender1 = new DailyRollingFileAppender(layout,logPath,"'.'yyyy-MM-dd");
			BasicConfigurator.configure(appender1);
				
			// console appender 
			ConsoleAppender appender2 = new ConsoleAppender(layout);
			BasicConfigurator.configure(appender2);
		}
		catch(IOException e) {
			System.out.println("Got an exception while initializing the logging configuration");
			e.printStackTrace();
		}
		
		/*
		 *  turn off all logging, and enable ERROR logging for the root package
		 *  use restlet.LoggingResource to enable specific logging
		 */
		Logger.getRootLogger().setLevel(Level.OFF);
		Logger.getLogger(PACKAGE_ROOT).setLevel(Level.INFO);
			
		System.out.println("Rafael: Initializing logging done");
	}
}
