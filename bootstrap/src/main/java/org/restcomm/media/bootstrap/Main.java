/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2018, Telestax Inc and individual contributors
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

package org.restcomm.media.bootstrap;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) created on 04/01/2018
 */
public class Main {

    private static final Logger log = Logger.getLogger(Main.class);

    private final static String HOME_DIR = "MMS_HOME";
    private static final String LOGGER_CONF = "/conf/log4j.xml";
    private static final String MS_CONF = "/conf/mediaserver.xml";

    private static Bootstrapper bootstrapper;

    public static void main(String[] args) throws Throwable {
        // Load HOME_DIR
        final String home = loadHomeDir();
        if (log.isInfoEnabled()) {
            log.info("Home directory: " + home);
        }

        // Load Logger configuration
        try {
            initLogger(home + LOGGER_CONF);
            if (log.isInfoEnabled()) {
                log.info("Loaded logger configuration from " + home + LOGGER_CONF);
            }
        } catch (Exception e) {
            log.warn("Could not load configuration file for Logger. Defaults will be used.");
        }

        // Bootstrap the Media Server and add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(new ShutdownTask()));
        loadBootstrapper(home + MS_CONF);

        // Call the Garbage Collector
        if (log.isInfoEnabled()) {
            log.info("Called the Garbage collector to clear init data");
        }
        System.gc();
    }

    /**
     * Gets the Media Server Home directory.
     *
     * @return the path to the home directory.
     */
    private static String loadHomeDir() {
        String mmsHomeDir = System.getProperty(HOME_DIR);
        if (mmsHomeDir == null) {
            mmsHomeDir = System.getenv(HOME_DIR);
            if (mmsHomeDir == null) {
                mmsHomeDir = ".";
            }
        }
        return mmsHomeDir;
    }

    /**
     * Loads configuration for Logger.
     *
     * @param filepath The path of the configuration file.
     */
    private static void initLogger(String filepath) throws Exception {
        final File file = new File(filepath);
        if (file.exists()) {
            DOMConfigurator.configure(file.toURI().toURL());
        } else {
            throw new FileNotFoundException("No such logger configuration file " + filepath);
        }
    }

    /**
     * Configures and bootstraps the Media Server
     *
     * @param filepath
     * @throws Exception
     */
    private static void loadBootstrapper(String filepath) throws Exception {
        Main.bootstrapper = new GuiceBootstrapper(filepath);
        Main.bootstrapper.deploy();
    }

    private static class ShutdownTask implements Runnable {

        public void run() {
            if (log.isInfoEnabled()) {
                log.info("Media Server is shutting down.");
            }

            if (Main.bootstrapper != null) {
                Main.bootstrapper.undeploy();
                Main.bootstrapper = null;
            }

            if (log.isInfoEnabled()) {
                log.info("Media Server is shut down.");
            }
        }
    }

}
