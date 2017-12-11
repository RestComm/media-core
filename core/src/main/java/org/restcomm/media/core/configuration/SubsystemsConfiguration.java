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

package org.restcomm.media.core.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.naming.ConfigurationException;

import org.apache.commons.lang3.StringUtils;

/**
 * @author anikiforov
 */
public class SubsystemsConfiguration {
    private final Map<String, SubsystemConfiguration> subsystems = new HashMap<>();

    public Collection<DriverConfiguration> getDrivers(final String subsystemName) {
        final SubsystemConfiguration subsystem = subsystems.get(subsystemName);
        if (subsystem != null) {
            return subsystem.getDrivers();
        }
        return Collections.emptyList();
    }

    public DriverConfiguration getDriver(final String subsystemName, final String driverName) {
        final SubsystemConfiguration subsystem = subsystems.get(subsystemName);
        if (subsystem != null) {
            return subsystem.getDriver(driverName);
        }
        return null;
    }

    public void addSubsystem(final String subsystemName, final SubsystemConfiguration subsystemConfig)
            throws ConfigurationException {
        if (StringUtils.isEmpty(subsystemName)) {
            throw new IllegalArgumentException("Subsystem name shouldn't be empty");
        } else if (subsystemConfig == null) {
            throw new IllegalArgumentException("Driver config shouldn't be null");
        } else if (subsystems.containsKey(subsystemName)) {
            throw new ConfigurationException("Subsystem '" + subsystemName + "' is already configured");
        } else {
            subsystems.put(subsystemName, subsystemConfig);
        }
    }
}
