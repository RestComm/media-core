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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author anikiforov
 */
public class DriverConfiguration {

    private final String driverName;
    private final String className;
    private final Map<String, String> parameters;

    public DriverConfiguration(final String driverName, final String className) {
        if (StringUtils.isEmpty(driverName)) {
            throw new IllegalArgumentException("Driver name shouldn't be empty");
        } else if (StringUtils.isEmpty(className)) {
            throw new IllegalArgumentException("Class name shouldn't be empty");
        }

        this.driverName = driverName;
        this.className = className;
        this.parameters = new HashMap<>();
    }

    public String getDriverName() {
        return driverName;
    }

    public String getClassName() {
        return className;
    }

    public Map<String, String> getParameters() {
        return Collections.unmodifiableMap(parameters);
    }

    public void addParameter(final String paramName, final String paramValue) {
        if (StringUtils.isEmpty(paramName)) {
            throw new IllegalArgumentException("Parameter name shouldn't be empty");
        } else if (StringUtils.isEmpty(paramValue)) {
            throw new IllegalArgumentException("Parameter value shouldn't be empty");
        } else if (parameters.containsKey(paramName)) {
            throw new IllegalArgumentException("Parameter " + paramName + " is already specified");
        }

        parameters.put(paramName, paramValue);
    }
}
