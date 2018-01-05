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
        
package org.restcomm.media.bootstrap.ioc.provider.asr;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.asr.AsrEngineProvider;
import org.restcomm.media.asr.AsrEngineProviderImpl;
import org.restcomm.media.asr.driver.AsrDriverManagerImpl;
import org.restcomm.media.core.configuration.DriverConfiguration;
import org.restcomm.media.core.configuration.MediaServerConfiguration;
import org.restcomm.media.core.configuration.SubsystemsConfiguration;
import org.restcomm.media.drivers.asr.AsrDriverManager;
import org.restcomm.media.scheduler.PriorityQueueScheduler;

import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AsrEngineProviderGuiceProvider implements Provider<AsrEngineProvider> {

    private static final Logger logger = LogManager.getLogger(AsrEngineProviderGuiceProvider.class);

    private final PriorityQueueScheduler scheduler;
    private final MediaServerConfiguration configuration;

    @Inject
    public AsrEngineProviderGuiceProvider(PriorityQueueScheduler scheduler, MediaServerConfiguration configuration) {
        super();
        this.scheduler = scheduler;
        this.configuration = configuration;
    }

    @Override
    public AsrEngineProvider get() {
        final AsrDriverManager mng = new AsrDriverManagerImpl();
        final SubsystemsConfiguration subsystemsConfiguration = configuration.getSubsystemsConfiguration();
        final Collection<DriverConfiguration> drivers = subsystemsConfiguration.getDrivers("asr");
        if (drivers == null || drivers.isEmpty()) {
            logger.warn("Asr drivers are not configured. Speech recognition functionality will not work");
        } else {
            for (final DriverConfiguration driver : drivers) {
                final String driverName = driver.getDriverName();
                final String className = driver.getClassName();
                mng.registerDriver(driverName, className, driver.getParameters());
                logger.info("Driver \'" + driverName + "' (" + className + ") is successfully registered");
            }
        }
        final int silenceLevel = configuration.getResourcesConfiguration().getSpeechDetectorSilenceLevel();
        return new AsrEngineProviderImpl(scheduler, mng, silenceLevel);
    }
    
}
