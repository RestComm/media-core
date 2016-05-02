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

package org.mobicents.media.control.mgcp.command;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(CreateConnectionCommand.class);

    private static final String WILDCARD_ANY = "*";
    private static final String WILDCARD_ALL = "$";

    // MGCP Components
    private final MgcpEndpointManager endpointManager;

    public CreateConnectionCommand(MgcpEndpointManager endpointManager) {
        // MGCP Components
        this.endpointManager = endpointManager;
    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        String z2 = request.getParameter(MgcpParameterType.SECOND_ENDPOINT);
        String sdp = request.getParameter(MgcpParameterType.SDP);

        // Z2 and SDP must not be together in same request
        if (z2 != null && sdp != null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), "Z2 and SDP present in message");
        }

        // Call ID
        String callId = request.getParameter(MgcpParameterType.CALL_ID);
        if (callId == null) {
            throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID.code(), "Call ID (C) is not specified");
        }

        // Connection Mode
        String mode = request.getParameter(MgcpParameterType.MODE);
        if (mode == null) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE.code(),
                    "Connection Mode (M) not specified");
        }

        // Endpoint Name
        String endpointId = request.getEndpointId().substring(0, request.getEndpointId().indexOf("@"));
        if (endpointId.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    "Wildcard ALL (*) is not supported");
        }

        // Secondary Endpoint Name
        String secondaryEndpointId = null;
        if (z2 != null) {
            secondaryEndpointId = z2.substring(0, request.getEndpointId().indexOf("@"));
            if (secondaryEndpointId.indexOf(WILDCARD_ANY) != -1) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                        "Wildcard ALL (*) is not supported");
            }
        }

        // Search primary endpoint
        int indexOfAll = endpointId.indexOf(WILDCARD_ALL);
        if (indexOfAll == -1) {
            // TODO Search for specific endpoint
        } else {
            // TODO Create new endpoint for a specific names pace
        }

        return null;
    }

    @Override
    protected MgcpResponse rollback(int code, String message) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void sendResponse(MgcpResponse response) {
        // TODO Auto-generated method stub

    }

}
