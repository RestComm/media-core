/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.drivers.asr;

import java.util.Map;

/**
 * Manages a set of ASR drivers from multiple providers.
 * 
 * @author gdubina
 *
 */
public interface AsrDriverManager {

    /**
     * Registers a new ASR driver.
     * 
     * @param name The name of the driver.
     * @param clazz The type of the driver.
     * @param config The configuration parameters of the driver.
     */
    void registerDriver(String name, String clazz, Map<String, String> config);

    /**
     * Loads a driver by name.
     * 
     * @param name The name of the driver.
     * @return A new driver instance.
     * @throws UnknownAsrDriverException The driver name is unrecognized.
     * @throws AsrDriverConfigurationException The driver was badly configured.
     */
    AsrDriver getDriver(String name) throws UnknownAsrDriverException, AsrDriverConfigurationException;

}
