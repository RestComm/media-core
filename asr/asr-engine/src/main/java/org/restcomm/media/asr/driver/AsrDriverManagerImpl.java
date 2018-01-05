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

package org.restcomm.media.asr.driver;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.drivers.asr.AsrDriver;
import org.restcomm.media.drivers.asr.AsrDriverConfigurationException;
import org.restcomm.media.drivers.asr.AsrDriverManager;
import org.restcomm.media.drivers.asr.UnknownAsrDriverException;

/**
 * @author gdubina
 */
public class AsrDriverManagerImpl implements AsrDriverManager {

    private static final Logger logger = LogManager.getLogger(AsrDriverManager.class);

    private final Map<String, DriverInfo> providers;

    public AsrDriverManagerImpl() {
        this.providers = new HashMap<>(5);
    }

    @Override
    public void registerDriver(String name, String clazz, Map<String, String> config) {
        try {
            Class.forName(clazz);
            providers.put(name, new DriverInfo(clazz, config));
        } catch (ClassNotFoundException e) {
            logger.error("Failed to instantiate asr driver (className = " + clazz + ")", e);
            throw new IllegalArgumentException("Cannot register ASR driver " + clazz, e);
        }
    }

    @Override
    public AsrDriver getDriver(String name) throws UnknownAsrDriverException, AsrDriverConfigurationException {
        if (!providers.containsKey(name)) {
            logger.error(String.format("Provider with name '%s' is not registered", name));
            throw new UnknownAsrDriverException(String.format("Provider with name '%s' is not registered", name));
        }
        return newInstance(providers.get(name));
    }

    private AsrDriver newInstance(DriverInfo info) throws UnknownAsrDriverException, AsrDriverConfigurationException {
        try {
            final Class<?> clazz = Class.forName(info.driverClass);
            final AsrDriver object = (AsrDriver) clazz.newInstance();

            if (logger.isInfoEnabled()) {
                logger.info("Asr driver is successfully instantiated (className = " + info.driverClass + ")");
            }

            object.configure(info.config);
            if (logger.isTraceEnabled()) {
                final StringBuilder params = new StringBuilder();
                params.append("[");
                boolean isFirstEntry = true;
                for (final Map.Entry<String, String> entry : info.config.entrySet()) {
                    if (!isFirstEntry) {
                        params.append(", ");
                    }
                    params.append("'");
                    params.append(entry.getKey());
                    params.append("'='");
                    params.append(entry.getValue());
                    params.append("'");
                    isFirstEntry = false;
                }
                params.append("]");
                logger.trace("We have called AsrDriver.configure(" + params.toString() + ")");
            }
            return object;
        } catch (final ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            logger.error("Failed to instantiate asr driver (className = " + info.driverClass + ")", e);
            throw new UnknownAsrDriverException("Failed to instantiate asr driver " + info.driverClass);
        } catch (IllegalArgumentException e) {
            logger.error("Failed to configure asr driver (className = " + info.driverClass + ")", e);
            throw new AsrDriverConfigurationException("Failed to configure asr driver " + info.driverClass);
        }
    }

    private static class DriverInfo {

        private final String driverClass;
        private final HashMap<String, String> config;

        private DriverInfo(String driverClass, Map<String, String> config) {
            this.driverClass = driverClass;
            this.config = new HashMap<>();

            if (config != null) {
                this.config.putAll(config);
            }
        }

    }
}
