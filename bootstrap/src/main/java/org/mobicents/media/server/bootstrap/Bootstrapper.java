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

package org.mobicents.media.server.bootstrap;

import org.apache.log4j.Logger;
import org.mobicents.media.core.configuration.MediaServerConfiguration;
import org.mobicents.media.server.bootstrap.configuration.ConfigurationLoader;
import org.mobicents.media.server.bootstrap.configuration.XmlConfigurationLoader;
import org.mobicents.media.server.bootstrap.ioc.CoreModule;
import org.mobicents.media.server.bootstrap.ioc.MediaModule;
import org.mobicents.media.server.bootstrap.ioc.MgcpModule;
import org.mobicents.media.server.bootstrap.main.RestCommMediaServer;
import org.mobicents.media.server.spi.MediaServer;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Bootstrapper that reads from a configuration file and initializes the Media Server.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class Bootstrapper {

    private static final Logger log = Logger.getLogger(Bootstrapper.class);
    private final String filepath;
    private final ConfigurationLoader configurationLoader;
    private MediaServer mediaServer;

    public Bootstrapper(String filepath) {
        this.filepath = filepath;
        this.configurationLoader = new XmlConfigurationLoader();
    }

    public void start() {
        try {
            MediaServerConfiguration conf = configurationLoader.load(this.filepath);
            Injector injector = Guice.createInjector(new CoreModule(conf), new MediaModule(), new MgcpModule());
            this.mediaServer = injector.getInstance(RestCommMediaServer.class);
            this.mediaServer.start();
        } catch (Exception e) {
            log.error("Bootstrap aborted. Reason: " + e.getMessage());
        }
    }

    public void stop() {
        if (mediaServer != null) {
            this.mediaServer.stop();
        }
    }

}
