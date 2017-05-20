/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.mobicents.media.server.tts.core;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;

import org.apache.commons.configuration.XMLConfiguration;
import org.apache.log4j.Logger;
import org.jboss.util.StringPropertyReplacer;


/**
 * LoadTTSEngine which parse configuration file to load TTS engine and handle text from user
 *   
 * @author Thanh Tran
 *
 */
public class LoadTTSEngine {
	private final static Logger logger = Logger.getLogger(LoadTTSEngine.class);

	// Define elements of configuration xml file
	private final static String 	ENGINE = "engine";
	private final static String 	CONFIGURATION_PATH = "configuration";
	
	// Configuration xml elements data
	private String 					myEnginePath;
	private String 					myConfigurationPath;
	private String					myFilePath;
	
	private TTSEngine				myTTSEngine;
	
	// Singleton pattern
	private static LoadTTSEngine	myInstance = null;
	
	public static LoadTTSEngine getInstance() {
		if ( null == myInstance ) {
			myInstance = new LoadTTSEngine();	
		}
		return myInstance;
	}

	public LoadTTSEngine() {
		// TODO Auto-generated constructor stub
		myEnginePath = "";
		myConfigurationPath = "";
		myFilePath = "";
	}
	
	public boolean parseRMSConfiguration(final String filePath) throws Exception {
		if ( filePath.isEmpty() ) {
			logger.warn("File path is NULL");
			return false;
		} else {
			if ( logger.isInfoEnabled() ) {
				logger.info("File path: " + filePath);
			}
			myFilePath = filePath;

			final URL configURL = getURL(filePath);
			final XMLConfiguration configuration = new XMLConfiguration(configURL);	

			myEnginePath = configuration.getString(ENGINE);
			myConfigurationPath = configuration.getString(CONFIGURATION_PATH);
			return true;
		}
	}
	
	public byte[] convertTextToSpeak(String content) {
		if ( myEnginePath.isEmpty() || myConfigurationPath.isEmpty() ) {
			logger.warn("Cannot handle TTS engine");
			return null; 
		} else {
			try {
				myTTSEngine = (TTSEngine) Class.forName(myEnginePath).getConstructor(String.class).newInstance(myConfigurationPath);
			} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
					| InvocationTargetException | NoSuchMethodException | SecurityException
					| ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if ( null != myTTSEngine ) {
				return myTTSEngine.speak(content);
			} else {
				return null;
			}		
		}
	}
	
	private URL getURL(String filePath) throws Exception {
		filePath = StringPropertyReplacer.replaceProperties(filePath, System.getProperties());
		File file = new File(filePath);
		if ( false == file.exists() ) {
			throw new IllegalArgumentException("No such file: " + filePath);
		}
		return file.toURI().toURL();
	}

	public String getEngine() {
		if ( myEnginePath.isEmpty() ) {
			logger.warn("Engine is NULL");
		} else {
			// Do nothing
		}
		return myEnginePath;
	}
	
	public String getConfigurationPat() {
		if ( myConfigurationPath.isEmpty() ) {
			logger.warn("Configuration path is NULL");
		} else {
			// Do nothing
		}
		return myConfigurationPath;
	}
	
	public String getFilePath() {
		if ( myFilePath.isEmpty() ) {
			logger.warn("File path is NULL");
		} else {
			// Do nothing
		}
		return myFilePath;
	}
}
