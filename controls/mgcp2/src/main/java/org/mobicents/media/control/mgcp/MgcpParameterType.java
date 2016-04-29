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

package org.mobicents.media.control.mgcp;

/**
 * Enumeration of MGCP parameter types.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpParameterType {

    CALL_ID("C"),
    MODE("M"),
    SECOND_ENDPOINT("Z2"),
    SDP("sdp"),
    CONNECTION_ID("I"),
    CONNECTION_ID2("I2"),
    ENDPOINT_ID("Z"),
    REQUEST_ID("X"),
    REQUESTED_EVENTS("R"),
    REQUESTED_SIGNALS("S"),
    NOTIFIED_ENTITY("N"),
    OBSERVED_EVENT("O"),
    CONNECTION_PARAMETERS("P"),
    LOCAL_CONNECTION_OPTIONS("L"),
    REASON_CODE("E"),
    BARER_INFORMATION("B"),
    REQUESTED_INFO("F"),
    REMOTE_CONNECTION_DESCRIPTION("RC"),
    LOCAL_CONNECTION_DESCRIPTION("LC");
    
    private final String code;
    
    private MgcpParameterType(String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static final MgcpParameterType fromCode(String code) {
        if(code != null && !code.isEmpty()) {
            for (MgcpParameterType type : values()) {
                if(type.code.equalsIgnoreCase(code)) {
                    return type;
                }
            }
        }
        return null;
    }
    
}
