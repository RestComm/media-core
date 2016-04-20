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

import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.log4j.Logger;

/**
 * Bootstrapper that reads from a configuration file and initializes all beans necessary to the well functioning of the Media
 * Server.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class BootStrapper {

    private static final Logger log = Logger.getLogger(BootStrapper.class);

    private final String filepath;
    private final Configurations configurations;
    private XMLConfiguration configuration;

    public BootStrapper(String filepath) {
        this.filepath = filepath;
        this.configurations = new Configurations();
    }

    public void start() {
        try {
            log.info("Loading configuration file...");
            this.configuration = this.configurations.xml(filepath);
            log.info("... loaded configuration file successfully!");
        } catch (ConfigurationException e) {
            log.error("... failed to load configuration file!");
        }
    }

    public void stop() {
        log.info("Stopped Bootstrapper");
    }

}
