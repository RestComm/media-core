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

import java.io.IOException;

import org.mobicents.media.core.call.Call;
import org.mobicents.media.core.call.CallManager;
import org.mobicents.media.server.mgcp.MgcpCommand;
import org.mobicents.media.server.mgcp.MgcpParameterType;
import org.mobicents.media.server.mgcp.MgcpRequest;
import org.mobicents.media.server.mgcp.exception.MgcpCommandException;
import org.mobicents.media.server.mgcp.naming.EndpointNamingWizard;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;
import org.mobicents.media.server.spi.ModeNotSupportedException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnection extends MgcpCommand implements Runnable {

    // MGCP error messages
    private static final String Z2_AND_SDP_PRESENT = "Z2 and SDP parameters are present in the same message";
    private static final String CALL_ID_MISSING = "Call ID (C) parameter is missing";
    private static final String MODE_MISSING = "Connection mode (M) parameter is missing";
    private static final String MODE_UNKNOWN = "Connection mode (M) parameter is unknown ";
    private static final String ENDPOINT_ID_MISSING = "Endpoint ID parameter is missing";
    private static final String ENDPOINT_ID_ALL = "Endpoint ID parameter cannot use wildcard * (ALL)";
    private static final String SECOND_ENDPOINT_ID_ALL = "Secondary Endpoint ID (Z2) parameter cannot use wildcard * (ALL)";
    private static final String UNKOWN_ENDPOINT_TYPE = "Unknown endpoint type ";
    private static final String ENDPOINT_NOT_FOUND = "Endpoint not found ";
    private static final String SDP_NEGOTIATION_FAILED = "Sdp negotiation failed";
    private static final String SDP_OFFER_FAILED = "Could not generate SDP offer";
    private static final String MODE_UNSUPPORTED = "Connection Mode (M) parameter unsupported ";

    // MGCP request and parameters
    private final MgcpRequest mgcpRequest;

    private Call call;
    private Endpoint primaryEndpoint, secondaryEndpoint;
    private Connection primaryConnection, secondaryConnection;

    public CreateConnection(MgcpRequest request) {
        this.mgcpRequest = request;
    }
    
    public String getPrimaryEndpointId() {
        if(this.primaryEndpoint == null) {
            return "";
        }
        return this.primaryEndpoint.getLocalName();
    }

    public String getSecondaryEndpointId() {
        if(this.secondaryEndpoint == null) {
            return "";
        }
        return this.secondaryEndpoint.getLocalName();
    }

    private void createConnection(MgcpRequest request) throws MgcpCommandException {
        // Retrieve parameters from request
        String callIdHex = request.getParameter(MgcpParameterType.CALL_ID);
        String endpointId = request.getEndpointId();
        EndpointType endpointType = EndpointNamingWizard.getEndpointType(endpointId);
        String secondEndpointId = request.getParameter(MgcpParameterType.SECOND_ENDPOINT_ID);
        EndpointType secondEndpointType = EndpointNamingWizard.getEndpointType(secondEndpointId);
        String mode = request.getParameter(MgcpParameterType.MODE);
        ConnectionMode connectionMode = ConnectionMode.fromDescription(mode);
        String lcOptions = request.getParameter(MgcpParameterType.LOCAL_CONNECTION_OPTS);
        String sdp = request.getSdp();

        // Validate command parameters
        if (callIdHex == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CALL_ID_MISSING);
        }

        if (mode == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, MODE_MISSING);
        }

        if (connectionMode == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, MODE_UNKNOWN + mode);
        }

        if (endpointId == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, ENDPOINT_ID_MISSING);
        }

        if (endpointId.endsWith(EndpointNamingWizard.WILDCARD_ALL)) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, ENDPOINT_ID_ALL);
        }

        if (endpointType == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, UNKOWN_ENDPOINT_TYPE + endpointId);
        }

        if (secondEndpointId != null && secondEndpointId.endsWith(EndpointNamingWizard.WILDCARD_ALL)) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, SECOND_ENDPOINT_ID_ALL);
        }

        if (secondEndpointId != null && secondEndpointType == null) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, UNKOWN_ENDPOINT_TYPE + secondEndpointId);
        }

        if (secondEndpointId != null && !sdp.isEmpty()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, Z2_AND_SDP_PRESENT);
        }

        // Execute command - CRCX
        // Retrieve or create call with requested ID
        int callId = Integer.parseInt(callIdHex, 16);

        this.call = CallManager.getInstance().getCall(callId);
        if (this.call == null) {
            this.call = CallManager.getInstance().createCall(callId);
        }

        // Retrieve or create primary endpoint
        if (endpointId.endsWith(EndpointNamingWizard.WILDCARD_ANY)) {
            this.primaryEndpoint = call.createEndpoint(endpointType);
        } else {
            this.primaryEndpoint = call.getEndpoint(endpointId);
            if (this.primaryEndpoint == null) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ENDPOINT_NOT_FOUND + endpointId);
            }
        }

        if (secondEndpointId != null) {
            // Retrieve or create secondary endpoint
            if (secondEndpointId.endsWith(EndpointNamingWizard.WILDCARD_ANY)) {
                this.secondaryEndpoint = call.createEndpoint(secondEndpointType);
            } else {
                this.secondaryEndpoint = call.getEndpoint(secondEndpointId);
                if (this.secondaryEndpoint == null) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ENDPOINT_NOT_FOUND
                            + secondEndpointId);
                }
            }

            // Create two local connections to wire both endpoints
            this.primaryConnection = primaryEndpoint.createConnection(ConnectionType.LOCAL, false);
            this.secondaryConnection = secondaryEndpoint.createConnection(ConnectionType.LOCAL, false);
        } else {
            // Create an RTP connection
            // XXX Check lcOptions to see if connection is local
            this.primaryConnection = this.primaryEndpoint.createConnection(ConnectionType.RTP, false);

            if (sdp != null) {
                // Connect to remote peer if SDP is present
                try {
                    this.primaryConnection.setOtherParty(sdp.getBytes());
                } catch (IOException e) {
                    throw new MgcpCommandException(MgcpResponseCode.UNSUPPORTED_SDP, SDP_NEGOTIATION_FAILED, e);
                }
            } else {
                // Generate SDP offer
                // XXX Check if call is WebRTC
                try {
                    this.primaryConnection.generateOffer(false);
                } catch (IOException e) {
                    throw new MgcpCommandException(MgcpResponseCode.ERROR_IN_SDP, SDP_OFFER_FAILED, e);
                }
            }
        }

        // Update connection mode
        try {
            this.primaryConnection.setMode(connectionMode);
        } catch (ModeNotSupportedException e) {
            throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, MODE_UNSUPPORTED + mode, e);
        }

    }

    private void sendResponse() {

    }

    private void rollback() {

    }
    
    public void execute() throws MgcpCommandException {
        createConnection(this.mgcpRequest);
    }

    @Override
    public void run() {
        try {
            createConnection(mgcpRequest);
        } catch (MgcpCommandException e) {
            rollback();
        } finally {
            sendResponse();
        }
    }

}
