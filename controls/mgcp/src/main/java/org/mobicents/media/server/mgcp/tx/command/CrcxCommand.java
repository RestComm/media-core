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

package org.mobicents.media.server.mgcp.tx.command;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.mobicents.media.core.endpoints.EndpointPool;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.MgcpCall;
import org.mobicents.media.server.mgcp.controller.MgcpConnection;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.params.LocalConnectionOptions;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.mgcp.tx.Transaction;
import org.mobicents.media.server.mgcp.tx.cmd.MgcpCommandException;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.spi.EndpointType;
import org.mobicents.media.server.spi.ModeNotSupportedException;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.TooManyConnectionsException;
import org.mobicents.media.server.utils.Text;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CrcxCommand extends Action {

    private static final Logger LOGGER = Logger.getLogger(CrcxCommand.class);

    // Standard messages
    private final static Text CALLID_MISSING = new Text("Missing call identifier");
    private final static Text MODE_MISSING = new Text("Missing mode value");
    private final static Text WILDCARD_ALL_NOT_ALLOWED = new Text("Wildcard <all> not allowed here");
    private final static Text SDP_NEGOTIATION_FAILED = new Text("SDP_NEGOTIATION_FAILED");
    private final static Text ERROR_ENDPOINT_UNAVAILABLE = new Text("Endpoint not available");
    private final static Text ERROR_CONNECTION_UNAVAILABLE = new Text("Connection not available");
    private final static Text ERROR_GENERATE_SDP = new Text("Could not generate local connection descriptor.");
    private final static Text ERROR_UNSUPPORTED_MODE = new Text("Mode not supported.");
    private final static Text ERROR_JOIN_CONNECTIONS = new Text("Could not join connections");
    private final static Text SDP_AND_Z2_PRESENT = new Text("Second endpoint and remote SDP present in message");

    private final static Text SUCCESS = new Text("Success");

    // Task execution
    private final TaskChain taskChain;
    private final CreateConnection createConnectionTask;
    private final Respond respondTask;
    private final Rollback rollbackTask;

    // Command elements
    private MgcpEndpoint firstEndpoint;
    private MgcpEndpoint secondEndpoint;
    private MgcpConnection firstConnection;
    private MgcpConnection secondConnection;

    // Request parameters
    private int callId;
    private String[] firstEndpointName;
    private String[] secondEndpointName;
    private String connectionMode;
    private String sdp;
    private LocalConnectionOptions connectionOptions = new LocalConnectionOptions();

    public CrcxCommand(PriorityQueueScheduler scheduler) {
        // Task execution
        this.taskChain = new TaskChain(2, scheduler);
        this.createConnectionTask = new CreateConnection();
        this.respondTask = new Respond();
        this.rollbackTask = new Rollback();

        setActionHandler(taskChain);
        setRollbackHandler(rollbackTask);

        // Command elements and Request parameters
        reset();
    }

    @Override
    public void start(Transaction tx) {
        // clean state before new execution
        reset();

        // start command execution
        super.start(tx);
    }

    private void reset() {
        // Task chain
        this.taskChain.clean();
        this.taskChain.add(createConnectionTask);
        this.taskChain.add(respondTask);

        // Command elements
        this.firstEndpoint = null;
        this.secondEndpoint = null;
        this.firstConnection = null;
        this.secondConnection = null;

        // Request parameters
        this.callId = 0;
        this.firstEndpointName = new String[] { "", "" };
        this.secondEndpointName = new String[] { "", "" };
        this.sdp = "";
        this.connectionOptions.setValue(null);
    }

    private class CreateConnection extends Task {

        private void readRequestParameters(MgcpRequest request) {
            // Call Identifier
            Parameter callIdParam = request.getParameter(Parameter.CALL_ID);
            if (callIdParam == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CALLID_MISSING);
            } else {
                callId = callIdParam.getValue().hexToInteger();
            }

            // Primary Endpoint
            firstEndpointName = request.getEndpoint().toString().split("@");
            if (firstEndpointName[0].endsWith("*")) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, WILDCARD_ALL_NOT_ALLOWED);
            }

            // Secondary Endpoint
            Parameter secondEndpointParam = request.getParameter(Parameter.SECOND_ENDPOINT);
            if (secondEndpointParam != null) {
                secondEndpointName = secondEndpointParam.getValue().toString().split("@");
                if (secondEndpointName[0].endsWith("*")) {
                    throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, WILDCARD_ALL_NOT_ALLOWED);
                }
            }

            Parameter sdpParam = request.getParameter(Parameter.SDP);
            if (sdpParam != null) {
                if (secondEndpointParam != null) {
                    throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, SDP_AND_Z2_PRESENT);
                }
                sdp = sdpParam.toString();
            }

            // Connection Mode
            Parameter modeParam = request.getParameter(Parameter.MODE);
            if (modeParam == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, MODE_MISSING);
            } else {
                connectionMode = modeParam.toString();
            }

            // Local Connection Parameters
            Parameter lcOptionsParam = request.getParameter(Parameter.LOCAL_CONNECTION_OPTIONS);
            connectionOptions.setValue(lcOptionsParam == null ? null : lcOptionsParam.getValue());
        }
        
        private MgcpEndpoint findEndpoint(String localName) throws UnknownEndpointException {
            MgcpEndpoint[] endpoint = new MgcpEndpoint[1];
            transaction().find(new Text(localName), endpoint);
            return endpoint[0];
        } 

        private MgcpEndpoint findEndpoint(MgcpCall call, String endpointName) {
            MgcpEndpoint mgcpEndpoint;
            // Retrieve any endpoint of corresponding type from the pool
            if (endpointName.endsWith("*")) {
                String[] nameTokens = endpointName.split("/");
                EndpointType endpointType = EndpointType.fromCode(nameTokens[1]);
                Endpoint endpoint = EndpointPool.getInstance().poll(endpointType);
                try {
                    mgcpEndpoint = findEndpoint(endpointName);
                    mgcpEndpoint.setEndpoint(endpoint);
                    call.addEndpoint(mgcpEndpoint);
                } catch (UnknownEndpointException e) {
                    LOGGER.error("Could not create MGCP endpoint " + endpointName + " for call " + call.getId());
                    mgcpEndpoint = null;
                }
            } else {
                mgcpEndpoint = call.getMgcpEndpoint(endpointName);
            }
            return mgcpEndpoint;
        }

        private MgcpConnection createConnection(MgcpCall mgcpCall, MgcpEndpoint mgcpEndpoint, ConnectionType type,
                boolean isLocal) {
            try {
                MgcpConnection mgcpConnection = mgcpEndpoint.createConnection(mgcpCall, type, isLocal);
                mgcpConnection.setCallAgent(getEvent().getAddress());
                return mgcpConnection;
            } catch (TooManyConnectionsException | ResourceUnavailableException e) {
                LOGGER.warn("Couldnt create " + type + " connection in endpoint " + firstEndpointName[0] + ", call " + callId,
                        e);
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_CONNECTION_UNAVAILABLE);
            }
        }

        private void setupSingleRtpConnection(MgcpCall mgcpCall) {
            // Create on RTP connection
            firstConnection = createConnection(mgcpCall, firstEndpoint, ConnectionType.RTP, connectionOptions.getIsLocal());

            if (!sdp.isEmpty()) {
                // Set remote peer
                try {
                    firstConnection.getConnection().setOtherParty(sdp.getBytes());
                } catch (IOException e) {
                    LOGGER.warn("Could not set remote peer on endpoint " + firstEndpointName[0] + ", call " + callId, e);
                    throw new MgcpCommandException(MgcpResponseCode.MISSING_SDP_OFFER, SDP_NEGOTIATION_FAILED);
                }
            } else {
                // Generate SDP offer
                try {
                    firstConnection.getConnection().generateOffer(connectionOptions.isWebRTC());
                } catch (IOException e) {
                    LOGGER.warn("Could not generate SDP offer on endpoint " + firstEndpointName[0] + ", call " + callId, e);
                    throw new MgcpCommandException(MgcpResponseCode.INTERNAL_INCONSISTENCY_IN_LOCAL_SDP, ERROR_GENERATE_SDP);
                }
            }

            // Set connection mode
            try {
                ConnectionMode mode = ConnectionMode.valueOf(new Text(connectionMode));
                firstConnection.getConnection().setMode(mode);
            } catch (ModeNotSupportedException e) {
                LOGGER.warn("Unsupported mode " + connectionMode + "on endpoint " + firstEndpointName[0] + ", call " + callId,
                        e);
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, ERROR_UNSUPPORTED_MODE);
            }
        }

        private void setupTwoLocalConnections(MgcpCall mgcpCall) {
            // Create connection on primary endpoint
            firstConnection = createConnection(mgcpCall, firstEndpoint, ConnectionType.LOCAL, false);

            // Create connection on secondary endpoint
            secondConnection = createConnection(mgcpCall, secondEndpoint, ConnectionType.LOCAL, false);

            // Join connections
            try {
                firstConnection.getConnection().setOtherParty(secondConnection.getConnection());
            } catch (IOException e) {
                LOGGER.warn("Could not join connection " + firstConnection.getID() + " (endpoint=" + firstEndpointName[0]
                        + ") with connection " + secondConnection.getID() + " (endpoint=" + secondEndpointName[0]
                        + ") on call nr." + callId, e);
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_JOIN_CONNECTIONS);
            }

            // Update first connection mode
            try {
                ConnectionMode mode = ConnectionMode.valueOf(new Text(connectionMode));
                firstConnection.getConnection().setMode(mode);
            } catch (ModeNotSupportedException e) {
                LOGGER.warn("Unsupported mode " + connectionMode + "on endpoint " + firstEndpointName[0] + ", call " + callId,
                        e);
                throw new MgcpCommandException(MgcpResponseCode.INVALID_OR_UNSUPPORTED_MODE, ERROR_UNSUPPORTED_MODE);
            }
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            // Read and validate MGCP request parameters
            readRequestParameters((MgcpRequest) getEvent().getMessage());

            // Retrieve existing call or create a new one
            MgcpCall mgcpCall = transaction().getCall(callId, true);

            // Search for the primary endpoint
            firstEndpoint = findEndpoint(mgcpCall, firstEndpointName[0]);
            if (firstEndpoint == null) {
                LOGGER.warn("Could not find endpoint " + firstEndpointName[0] + " for call " + callId);
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILABLE);
            }

            // Search for the secondary endpoint IF requested
            if (!secondEndpointName[0].isEmpty()) {
                secondEndpoint = findEndpoint(mgcpCall, secondEndpointName[0]);
                if (secondEndpoint == null) {
                    LOGGER.warn("Could not find endpoint " + secondEndpointName[0] + " for call " + callId);
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, ERROR_ENDPOINT_UNAVAILABLE);
                }
            }

            // Setup calls according to request parameters
            if (secondEndpoint == null) {
                setupSingleRtpConnection(mgcpCall);
            } else {
                setupTwoLocalConnections(mgcpCall);
            }

            return 0L;
        }

    }

    private class Respond extends Task {

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            // Create MGCP Response
            MgcpEvent mgcpEvent = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) mgcpEvent.getMessage();

            response.setTxID(transaction().getId());
            response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
            response.setResponseString(SUCCESS);

            response.setParameter(Parameter.ENDPOINT_ID, firstEndpoint.getFullName());
            response.setParameter(Parameter.CONNECTION_ID, firstConnection.getTextualID());

            if (secondEndpoint == null) {
                response.setParameter(Parameter.SDP, new Text(firstConnection.getConnection().getLocalDescriptor()));
            } else {
                response.setParameter(Parameter.SECOND_ENDPOINT, secondEndpoint.getFullName());
            }

            if (secondConnection != null) {
                response.setParameter(Parameter.CONNECTION_ID2, secondConnection.getTextualID());
            }

            // Send response
            try {
                transaction().getProvider().send(mgcpEvent);
            } catch (IOException e) {
                LOGGER.error("Could not send response.", e);
                // XXX shouldnt we throw an exception for action to rollback?
            } finally {
                mgcpEvent.recycle();
            }
            return 0L;
        }

    }

    private class Rollback extends Task {

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        private void releaseEndpoint(MgcpEndpoint endpoint, MgcpConnection connection) {
            if (connection != null) {
                endpoint.deleteConnection(connection.getID());
            }
            endpoint.share();
        }

        @Override
        public long perform() {
            // Delete primary connection
            if (firstEndpoint != null) {
                releaseEndpoint(firstEndpoint, firstConnection);
            }

            // Delete secondary connection
            if (secondEndpoint != null) {
                releaseEndpoint(secondEndpoint, secondConnection);
            }

            // Create MGCP Response
            MgcpEvent event = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) event.getMessage();
            response.setResponseCode(((MgcpCommandException) transaction().getLastError()).getCode());
            response.setResponseString(((MgcpCommandException) transaction().getLastError()).getErrorMessage());
            response.setTxID(transaction().getId());

            // Send response
            try {
                transaction().getProvider().send(event);
            } catch (IOException e) {
                LOGGER.error("Could not send response.", e);
                // XXX shouldnt we throw an exception for action to rollback?
            } finally {
                event.recycle();
            }
            return 0L;
        }

    }

}
