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

package org.mobicents.media.control.mgcp.pkg;

import java.util.HashMap;
import java.util.Map;

/**
 * Representation of the SignalRequests parameter of an MGCP RQNT command.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SignalRequests {

    private final String packageName;
    private final String signalType;
    private final Map<String, String> parameters;

    public SignalRequests(String packageName, String signalType) {
        super();
        this.packageName = packageName;
        this.signalType = signalType;
        this.parameters = new HashMap<>(5);
    }

    public String getPackageName() {
        return packageName;
    }

    public String getSignalType() {
        return signalType;
    }

    public void addParameter(String name, String value) {
        this.parameters.put(name, value);
    }

    public String getParameter(String name) {
        return this.parameters.get(name);
    }
    
}
