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

import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author anikiforov
 */
public class SubsystemConfiguration {

    private final String name;
    private final Map<String, DriverConfiguration> drivers;

    public SubsystemConfiguration(final String subsystemName) {
        this.name = subsystemName;
        this.drivers = new HashMap<>();
    }

    public String getName() {
        return name;
    }

    public DriverConfiguration getDriver(final String driverName) {
        return drivers.get(driverName);
    }

    public Collection<DriverConfiguration> getDrivers() {
        return drivers.values();
    }

    public void addDriver(final String driverName, final DriverConfiguration driverConf) {
        if (StringUtils.isEmpty(driverName)) {
            throw new IllegalArgumentException("Driver name shouldn't be empty");
        } else if (driverConf == null) {
            throw new IllegalArgumentException("Driver configuration shouldn't be null");
        } else if (drivers.containsKey(driverName)) {
            throw new IllegalArgumentException("Driver " + driverName + " is already specified");
        } else {
            drivers.put(driverName, driverConf);
        }
    }

}
