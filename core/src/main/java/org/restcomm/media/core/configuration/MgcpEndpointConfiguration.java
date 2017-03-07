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

package org.restcomm.media.core.configuration;

import org.restcomm.media.spi.RelayType;

/**
 * Configuration parameters of an MGCP Endpoint.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointConfiguration {
    
    public static String DEFAULT_RELAY_TYPE = RelayType.MIXER.name();

    private String name;
    private RelayType relayType;

    public MgcpEndpointConfiguration() {
        this.name = "";
        this.relayType = RelayType.MIXER;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Endpoint name cannot be empty.");
        }
        this.name = name.toLowerCase();
    }
    
    public void setRelayType(String relayType) {
        if(relayType == null) {
            throw new IllegalArgumentException("Relay Type cannot be undefined.");
        }
        this.relayType = RelayType.fromName(relayType);
    }
    
    public RelayType getRelayType() {
        return relayType;
    }

}
