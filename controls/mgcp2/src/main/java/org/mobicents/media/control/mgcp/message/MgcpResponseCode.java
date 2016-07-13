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
        
package org.mobicents.media.control.mgcp.message;

/**
 * Enumeration of MGCP response codes.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public enum MgcpResponseCode {

    ACKNOWLEDGEMENT(0, "Response Acknowledgement"),
    TRANSACTION_BEEN_EXECUTED(100, "The transaction is currently being executed"),
    TRANSACTION_HAS_BEEN_QUEUED(101, "The transaction has been queued for execution"),
    TRANSACTION_WAS_EXECUTED(200, "The requested transaction was executed normally"),
    CONNECTION_WAS_DELETED(250, "The connection was deleted"),
    TRANSIENT_ERROR(400, "The transaction could not be executed, due to some unspecified transient error"),
    INSUFFICIENT_RESOURCES(403, "The transaction could not be executed, because the endpoint does not have sufficient resources at this time"),
    INSUFFICIENT_BANDWIDTH(404, "Insufficient bandwidth at this time"),
    ENDPOINT_RESTARTING(405, "The transaction could not be executed, because the endpoint is restarting"),
    TIMEOUT(406, "Transaction time-out"),
    ABORTED(407, "Transaction aborted"),
    OVERLOADED(409, "The transaction could not be executed because of internal overload"),
    ENDPOINT_NOT_AVAILABLE(410, "No endpoint available"),
    ENDPOINT_UNKNOWN(500, "The transaction could not be executed, because the endpoint is unknown"),
    ENDPOINT_NOT_READY(501, "The transaction could not be executed, because the endpoint is not ready"),
    ENDPOINT_DOES_NOT_HAVE_RESOURCES(502, "The transaction could not be executed, because the endpoint does not have sufficient resources"),
    WILDCARD_TOO_COMPLICATED(503, "ALL_OF wildcard too complicated"),
    UNKNOWN_OR_UNSUPPORTED_COMMAND(504, "Unknown or unsupported command"),
    UNSUPPORTED_SDP(505, "Unsupported RemoteConnectionDescriptor"),
    REMOTE_SDP_AND_LOCAL_OPTION_CONFLICT(506, "Unable to satisfy both LocalConnectionOptions and RemoteConnectionDescriptor"),
    UNSUPPROTED_FUNCTIONALITY(507, "Unsupported functionality"),
    UNKNOWN_QUARANTINE_HANDLING(508, "Unknown or unsupported quarantine handling"),
    ERROR_IN_SDP(509, "Error in RemoteConnectionDescriptor"),
    PROTOCOL_ERROR(510, "The transaction could not be executed, because some unspecified protocol error was detected"),
    UNKNOWN_EXTENSION(511, "The transaction could not be executed, because the command contained an unrecognized extension"),
    CANNOT_DETECT_EVENT(512, "The transaction could not be executed, because the gateway is not equipped to detect one of the requested events"),
    CANNOT_GENERATE_SIGNAL(513, "The transaction could not be executed, because the gateway is not equipped to generate one of the requested signals"),
    CANNOT_SEND_ANNOUNCEMENT(514, "The transaction could not be executed, because the gateway cannot send the specified announcement"),
    INCORRECT_CONNECTION_ID(515, "The transaction refers to an incorrect connection-id (may have been already deleted)"),
    INCORRECT_CALL_ID(516, "The transaction refers to an unknown call-id, or the call-id supplied is incorrect"),
    INVALID_OR_UNSUPPORTED_MODE(517, "Unsupported or invalid mode"),
    UNKNOWN_PACKAGE(518, "Unsupported or unknown package"),
    INTERNAL_INCONSISTENCY_IN_LOCAL_SDP(524, "Internal inconsistency in LocalConnectionOptions"),
    MISSING_REMOTE_CONNECTION_DESCRIPTOR(527, "Missing RemoteConnectionDescriptor"),
    CODEC_NEGOTIATION_FAILURE(534, "Codec negotiation failure"), 
    EVENT_OR_SIGNAL_ERROR(538, "Event/signal parameter error");

    private final int code;
    private final String message;

    private MgcpResponseCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int code() {
        return code;
    }

    public String message() {
        return this.message;
    }

    public static final MgcpResponseCode fromCode(int code) {
        for (MgcpResponseCode responseCode : values()) {
            if (responseCode.code == code) {
                return responseCode;
            }
        }
        return null;
    }
}
