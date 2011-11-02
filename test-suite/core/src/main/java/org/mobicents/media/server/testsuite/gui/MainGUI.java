/*
 * MainGUI.java
 */

package org.mobicents.media.server.testsuite.gui;



import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

/**
 * The main class of the application.
 */
public class MainGUI extends SingleFrameApplication {
	
	private static final Logger logger = Logger.getLogger(MainGUI.class);

    /**
     * At startup create and show the main frame of the application.
     */
    @Override protected void startup() {

        show(new MainGUIView(this));
    }

    /**
     * This method is to initialize the specified window by injecting resources.
     * Windows shown in our application come fully initialized from the GUI
     * builder, so this additional configuration is not needed.
     */
    @Override protected void configureWindow(java.awt.Window root) {
    }

    /**
     * A convenient static getter for the application instance.
     * @return the instance of MainGUI
     */
    public static MainGUI getApplication() {
        return Application.getInstance(MainGUI.class);
    }

    /**
     * Main method launching the application.
     */
    public static void main(String[] args) {
    	configLog4j();
        launch(MainGUI.class, args);
    }
    
    private static void configLog4j(){
    	InputStream inStreamLog4j = MainGUI.class.getClassLoader().getResourceAsStream("log4j.properties");
		Properties propertiesLog4j = new Properties();
		try {
			propertiesLog4j.load(inStreamLog4j);
			PropertyConfigurator.configure(propertiesLog4j);
		} catch (Exception e) {
			e.printStackTrace();
		}

		logger.debug("log4j configured");

    }
}
