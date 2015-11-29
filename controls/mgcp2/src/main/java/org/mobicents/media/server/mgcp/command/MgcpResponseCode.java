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

package org.mobicents.media.server.mgcp.command;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public interface MgcpResponseCode {

    public final static int ACKNOWLEDGEMENT = 0;

    // 1XX
    public final static int TRANSACTION_BEEN_EXECUTED = 100;
    public final static int TRANSACTION_HAS_BEEN_QUEUED = 101;

    // 2XX
    public final static int TRANSACTION_WAS_EXECUTED = 200;
    public final static int CONNECTION_WAS_DELETED = 250;

    // 4XX
    public final static int TRANSIENT_ERROR = 400;
    public final static int INSUFFICIENT_RESOURCES = 403;
    public final static int INSUFFICIENT_BANDWIDTH = 404;
    public final static int ENDPOINT_RESTARTING = 405;
    public final static int TIMEOUT = 406;
    public final static int ABORTED = 407;
    public final static int OVERLOADED = 409;
    public final static int ENDPOINT_NOT_AVAILABLE = 410;

    // 5XX
    public final static int ENDPOINT_UNKNOWN = 500;
    public final static int ENDPOINT_NOT_READY = 501;
    public final static int ENDPOINT_DOES_NOT_HAVE_RESOURCES = 502;
    public final static int WILDCARD_TOO_COMPLICATED = 503;
    public final static int UNKNOWN_OR_UNSUPPORTED_COMMAND = 504;
    public final static int UNSUPPORTED_SDP = 505;
    public final static int REMOTE_SDP_AND_LOCAL_OPTION_CONFLICT = 506;
    public final static int UNSUPPROTED_FUNCTIONALITY = 507;
    public final static int UNKNOWN_QUARANTINE_HANDLING = 508;
    public final static int ERROR_IN_SDP = 509;
    public final static int PROTOCOL_ERROR = 510;
    public final static int UNKNOWN_EXTENSION = 511;
    public final static int CAN_NOT_DETECT_EVENT = 512;
    public final static int CAN_NOT_GENERATE_SIGNAL = 513;
    public final static int CAN_NOT_SEND_ANNOUNCEMENT = 514;
    public final static int INCORRECT_CONNECTION_ID = 515;
    public final static int INCORRECT_CALL_ID = 516;
    public final static int INVALID_OR_UNSUPPORTED_MODE = 517;
    public final static int INTERNAL_INCONSISTENCY_IN_LOCAL_SDP = 524;
    public final static int MISSING_SDP_OFFER = 527;

}
