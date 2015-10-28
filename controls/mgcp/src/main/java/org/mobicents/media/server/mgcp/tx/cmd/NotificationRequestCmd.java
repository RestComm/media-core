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
import java.util.Collection;

import org.apache.log4j.Logger;
import org.mobicents.media.server.mgcp.MgcpEvent;
import org.mobicents.media.server.mgcp.controller.MgcpEndpoint;
import org.mobicents.media.server.mgcp.controller.UnknownEventException;
import org.mobicents.media.server.mgcp.controller.UnknownPackageException;
import org.mobicents.media.server.mgcp.controller.UnknownSignalException;
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

/**
 *
 * @author yulian oifa
 */
public class NotificationRequestCmd extends Action {

    private final static Logger logger = Logger.getLogger(NotificationRequestCmd.class);

    // Messages
    private final static Text SUCCESS = new Text("Success");
    private final static Text eventsSplit = new Text("),");

    // Task handler
    private final TaskChain handler;

    // Temporary Data
    private MgcpRequest request;
    private MgcpEndpoint[] endpoints = new MgcpEndpoint[1];

    public NotificationRequestCmd(PriorityQueueScheduler scheduler) {
        this.handler = new TaskChain(3, scheduler);
        this.handler.add(new Request());
        this.handler.add(new Executor());
        this.handler.add(new Responder());
        setActionHandler(handler);
        setRollbackHandler(new ErrorHandler());
    }

    @Override
    protected void reset() {
        this.request = null;
        this.endpoints[0] = null;
    }

    private class Request extends Task {

        public Request() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            request = (MgcpRequest) getEvent().getMessage();
            // getting the endpoint name
            Text localName = new Text();
            Text domainName = new Text();

            // read endpoint name from request and break into parts
            Text[] endpointName = new Text[] { localName, domainName };
            request.getEndpoint().divide('@', endpointName);

            // checking the local name:
            if (localName.contains('*')) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED, new Text(
                        "Wildcard all is not allowed here"));
            }

            try {
                int n = transaction().find(localName, endpoints);
                if (n == 0) {
                    throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Unknown endpoint"));
                }
            } catch (UnknownEndpointException e) {
                throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN, new Text("Endpoint not available"));
            }

            endpoints[0].getRequest().cancel();

            Parameter reqID = request.getParameter(Parameter.REQUEST_ID);
            if (reqID == null) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR, new Text("Request identifier is missing"));
            }

            Text callAgent = null;
            Parameter p = request.getParameter(Parameter.NOTIFIED_ENTITY);
            if (p != null) {
                callAgent = p.getValue();
            }

            Parameter pe = request.getParameter(Parameter.REQUESTED_EVENTS);
            Parameter ps = request.getParameter(Parameter.REQUESTED_SIGNALS);

            Collection<Text> events = null;
            Collection<Text> signals = null;

            if (pe != null) {
                events = pe.getValue().split(eventsSplit);
            }

            if (ps != null) {
                signals = ps.getValue().split(eventsSplit);
            }

            if (pe != null || ps != null) {
                try {
                    endpoints[0].getRequest().accept(reqID.getValue(), callAgent, events, signals);
                } catch (UnknownEventException e1) {
                    throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_DETECT_EVENT, new Text(e1.getMessage()));
                } catch (UnknownSignalException e2) {
                    throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_GENERATE_SIGNAL, new Text(e2.getMessage()));
                } catch (UnknownPackageException e) {
                    throw new MgcpCommandException(MgcpResponseCode.CAN_NOT_DETECT_EVENT, new Text(e.getMessage()));
                } catch (Exception e) {
                    logger.error("Could not process RQNT", e);
                    throw new MgcpCommandException(MgcpResponseCode.TRANSIENT_ERROR, new Text(e.getMessage()));
                }

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

    private class Executor extends Task {

        public Executor() {
            super();
        }

        @Override
        public int getQueueNumber() {
            return PriorityQueueScheduler.MANAGEMENT_QUEUE;
        }

        @Override
        public long perform() {
            endpoints[0].getRequest().execute();
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
            MgcpEvent evt = null;
            try {
                evt = transaction().getProvider().createEvent(MgcpEvent.RESPONSE, getEvent().getAddress());
                MgcpResponse response = (MgcpResponse) evt.getMessage();
                response.setResponseCode(((MgcpCommandException) transaction().getLastError()).getCode());
                response.setResponseString(((MgcpCommandException) transaction().getLastError()).getErrorMessage());
                response.setTxID(transaction().getId());

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
