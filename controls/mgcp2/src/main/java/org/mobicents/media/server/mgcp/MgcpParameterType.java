/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp;

/**
 * List of possible types of MGCP parameters.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpParameterType {
    
    CALL_ID("C"), 
    MODE("M"), 
    ENDPOINT_ID("Z"), 
    SECOND_ENDPOINT_ID("Z2"), 
    SDP("sdp"), 
    CONNECTION_ID("I"), 
    SECOND_CONNECTION_ID("I2"),
    REQUEST_ID("X"),
    REQUESTED_EVENTS("R"),
    REQUESTED_SIGNALS("S"),
    NOTIFIED_ENTITY("N"), 
    OBSERVED_EVENT("O"), 
    CONNECTION_PARAMS("P"), 
    LOCAL_CONNECTION_OPTS("L"),
    REASON_CODE("E"), 
    BEARER_INFORMATION("B"),
    REQUESTED_INFO("F"),
    REMOTE_CONNECTION_DESCRIPTION("RC"), 
    LOCAL_CONNECTION_DESCRIPTION("LC");
    
    private final String code;

    private MgcpParameterType(String id) {
        this.code = id;
    }

    public String getCode() {
        return code;
    }
    
    public static MgcpParameterType fromCode(String code) {
        for (MgcpParameterType value : values()) {
            if(value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }

}
