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
import org.restcomm.media.bootstrap.configuration.ConfigurationLoader;
import org.restcomm.media.bootstrap.configuration.XmlConfigurationLoader;
import org.restcomm.media.bootstrap.ioc.AsrModule;
import org.restcomm.media.bootstrap.ioc.CoreModule;
import org.restcomm.media.bootstrap.ioc.MediaModule;
import org.restcomm.media.bootstrap.ioc.MgcpModule;
import org.restcomm.media.core.configuration.MediaServerConfiguration;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.restcomm.media.spi.MediaServer;

/**
 * Bootstrapper that reads from a configuration file and initializes the Media Server.
 *
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class GuiceBootstrapper implements Bootstrapper {

    private static final Logger log = Logger.getLogger(GuiceBootstrapper.class);
    private final String filepath;
    private final ConfigurationLoader configurationLoader;
    private MediaServer mediaServer;

    public GuiceBootstrapper(String filepath) {
        this.filepath = filepath;
        this.configurationLoader = new XmlConfigurationLoader();
    }

    public void deploy() throws Exception {
        MediaServerConfiguration conf = configurationLoader.load(this.filepath);
        Injector injector = Guice.createInjector(new CoreModule(conf), new MediaModule(), new MgcpModule(), new AsrModule());
        this.mediaServer = injector.getInstance(StandaloneMediaServer.class);
        this.mediaServer.start();
    }

    public void undeploy() {
        if (mediaServer != null) {
            this.mediaServer.stop();
        }
    }

}
