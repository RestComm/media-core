/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
 * by the @authors tag. 
 *
 * This is free software), you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation), either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY), without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software), if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
        
package org.mobicents.media.control.mgcp;

/**
 * Enumeration of MGCP response codes.
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpResponseCode {

    ACKNOWLEDGEMENT(0),
    TRANSACTION_BEEN_EXECUTED(100),
    TRANSACTION_HAS_BEEN_QUEUED(101),
    TRANSACTION_WAS_EXECUTED(200),
    CONNECTION_WAS_DELETED(250),
    TRANSIENT_ERROR(400),
    INSUFFICIENT_RESOURCES(403),
    INSUFFICIENT_BANDWIDTH(404),
    ENDPOINT_RESTARTING(405),
    TIMEOUT(406),
    ABORTED(407),
    OVERLOADED(409),
    ENDPOINT_NOT_AVAILABLE(410),
    ENDPOINT_UNKNOWN(500),
    ENDPOINT_NOT_READY(501),
    ENDPOINT_DOES_NOT_HAVE_RESOURCES(502),
    WILDCARD_TOO_COMPLICATED(503),
    UNKNOWN_OR_UNSUPPORTED_COMMAND(504),
    UNSUPPORTED_SDP(505),
    REMOTE_SDP_AND_LOCAL_OPTION_CONFLICT(506),
    UNSUPPROTED_FUNCTIONALITY(507),
    UNKNOWN_QUARANTINE_HANDLING(508),
    ERROR_IN_SDP(509),
    PROTOCOL_ERROR(510),
    UNKNOWN_EXTENSION(511),
    CAN_NOT_DETECT_EVENT(512),
    CAN_NOT_GENERATE_SIGNAL(513),
    CAN_NOT_SEND_ANNOUNCEMENT(514),
    INCORRECT_CONNECTION_ID(515),
    INCORRECT_CALL_ID(516),
    INVALID_OR_UNSUPPORTED_MODE(517),
    INTERNAL_INCONSISTENCY_IN_LOCAL_SDP(524),
    MISSING_REMOTE_CONNECTION_DESCRIPTOR(527);
    
    private final int code;
    
    private MgcpResponseCode(int code) {
        this.code = code;
    }
    
    public int getCode() {
        return code;
    }
    
    public static final MgcpResponseCode fromCode(int code) {
        for (MgcpResponseCode responseCode : values()) {
            if(responseCode.code == code) {
                return responseCode;
            }
        }
        return null;
    }
}
