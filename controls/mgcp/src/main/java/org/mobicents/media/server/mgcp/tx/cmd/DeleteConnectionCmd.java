/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.mobicents.media.server.mgcp.tx.cmd;

import java.io.IOException;

import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.MgcpCall;
import org.mobicents.media.server.mgcp.controller.MgcpConnection;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.naming.UnknownEndpointException;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.mgcp.message.MgcpResponseCode;
import org.mobicents.media.server.mgcp.message.Parameter;
import org.mobicents.media.server.mgcp.tx.Action;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Task;
import org.mobicents.media.server.scheduler.TaskChain;
import org.mobicents.media.server.utils.Text;
import org.apache.log4j.Logger;

/**
 * Modify connection command.
 * 
 * @author Oifa Yulian
 */
public class DeleteConnectionCmd extends Action {

    private final static Logger logger = Logger.getLogger(DeleteConnectionCmd.class);

    // response strings
    private final static Text CALLID_MISSING = new Text("Missing call identifier");
    private final static Text UNKNOWN_CALL_IDENTIFIER = new Text("Could not find this call with specified identifier");
    private final static Text CONNECTIONID_EXPECTED = new Text("Connection identifier was not specified");
    private final static Text SUCCESS = new Text("Success");

    // Temporary Data
    private MgcpRequest request;
    private Parameter connectionID;
    int rx, tx = 0;

    private Text localName = new Text();
    private Text domainName = new Text();
    private Text[] endpointName = new Text[] { localName, domainName };

    private MgcpEndpoint[] endpoints = new MgcpEndpoint[1];

    // Task Executor
    private final TaskChain handler;

    public DeleteConnectionCmd(PriorityQueueScheduler scheduler) {
        this.handler = new TaskChain(2, scheduler);
        this.handler.add(new Delete());
        this.handler.add(new Responder());
        setActionHandler(handler);
        setRollbackHandler(new ErrorHandler());
    }

    @Override
    protected void reset() {
        this.request = null;
        this.connectionID = null;
        this.rx = 0;
        this.tx = 0;
        this.localName = new Text();
        this.domainName = new Text();
        this.endpointName = new Text[] { localName, domainName };
        this.endpoints[0] = null;
    }

    private class Delete extends Task {

        public Delete() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        private void deleteForEndpoint(MgcpRequest request) {
            // getting endpoint name
            request.getEndpoint().divide('@', endpointName);
            // searching endpoint
            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
                }
            } catch (UnknownEndpointException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Endpoint not available"));
            }

            endpoints[0].deleteAllConnections();
        }

        private void deleteForCall(Parameter callID, MgcpRequest request) {
            // getting call
            MgcpCall call = transaction().getCall(callID.getValue().hexToInteger(), false);
            if (call == null) {
                throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID, UNKNOWN_CALL_IDENTIFIER);
            }

            call.deleteConnections();
        }

        @Override
        public long perform() {
            request = (MgcpRequest) getEvent().getMessage();

            Parameter callID = request.getParameter(Parameter.CALL_ID);
            connectionID = request.getParameter(Parameter.CONNECTION_ID);

            if (callID == null && connectionID == null) {
                this.deleteForEndpoint(request);
                return 0;
            }

            if (callID != null && connectionID == null) {
                this.deleteForCall(callID, request);
                return 0;
            }

            if (callID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CALLID_MISSING);
            }

            // getting call
            MgcpCall call = transaction().getCall(callID.getValue().hexToInteger(), false);
            if (call == null) {
                throw new MgcpCommandException(MgcpResponseCode.INCORRECT_CALL_ID, UNKNOWN_CALL_IDENTIFIER);
            }

            if (connectionID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, CONNECTIONID_EXPECTED);
            }

            // getting endpoint name
            request.getEndpoint().divide('@', endpointName);
            // searching endpoint
            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
                }
            } catch (UnknownEndpointException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Endpoint not available"));
            }

            MgcpConnection connection = endpoints[0].getConnection(connectionID.getValue().hexToInteger());

            if (connection != null) {
                rx = (int) connection.getPacketsReceived();
                tx = (int) connection.getPacketsTransmitted();

                endpoints[0].deleteConnection(connectionID.getValue().hexToInteger());
            }
            return 0;
        }

    }

    /**
     * Searches endpoint specified in message.
     * 
     * The result will be stored into variable endpoint.
     */
    private class EndpointLocator extends Task {

        public EndpointLocator() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            try {
                // searching endpoint
                int n = transaction().find(localName, endpoints);

                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
                }

                // checking endpoint's state
                if (endpoints[0].getState() == MgcpEndpoint.STATE_BUSY) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
                }
            } catch (Exception e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_NOT_AVAILABLE, new Text("Endpoint not available"));
            }
            return 0;
        }

    }

    private class Responder extends Task {

        public Responder() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) evt.getMessage();
            response.setResponseCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED);
            response.setResponseString(SUCCESS);
            response.setTxID(transaction().getId());

            if (connectionID != null) {
                response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
            }
            response.setParameter(Parameter.CONNECTION_PARAMETERS, new Text("PS=" + tx + ", PR=" + rx));

            try {
                transaction().getProvider().send(evt);
            } catch (IOException e) {
                logger.error(e);
            } finally {
                evt.recycle();
            }
            return 0;
        }

    }

    private class ErrorHandler extends Task {

        public ErrorHandler() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            MgcpEvent evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
            MgcpResponse response = (MgcpResponse) evt.getMessage();
            response.setResponseCode(((MgcpCommandException) transaction().getLastError()).getCode());
            response.setResponseString(((MgcpCommandException) transaction().getLastError()).getErrorMessage());
            response.setTxID(transaction().getId());

            if (connectionID != null) {
                response.setParameter(Parameter.CONNECTION_ID, connectionID.getValue());
            }

            try {
                transaction().getProvider().send(evt);
            } catch (IOException e) {
                logger.error(e);
            } finally {
                evt.recycle();
            }
            return 0;
        }

    }

}
