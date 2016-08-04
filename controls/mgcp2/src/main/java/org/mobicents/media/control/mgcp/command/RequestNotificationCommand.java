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

import java.text.ParseException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.message.MgcpMessageObserver;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
import org.mobicents.media.control.mgcp.message.MgcpRequest;
import org.mobicents.media.control.mgcp.message.MgcpResponse;
import org.mobicents.media.control.mgcp.message.MgcpResponseCode;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.mobicents.media.control.mgcp.pkg.MgcpRequestedEventsParser;
import org.mobicents.media.control.mgcp.pkg.MgcpSignal;
import org.mobicents.media.control.mgcp.pkg.MgcpSignalProvider;
import org.mobicents.media.control.mgcp.pkg.NotifiedEntityParser;
import org.mobicents.media.control.mgcp.pkg.SignalRequest;
import org.mobicents.media.control.mgcp.pkg.SignalsRequestParser;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpActionException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpEventException;
import org.mobicents.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.mobicents.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;

/**
 * The NotificationRequest command is used to request the gateway to send notifications upon the occurrence of specified events
 * in an endpoint.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(RequestNotificationCommand.class);
    
    private final MgcpSignalProvider signalProvider;

    // MGCP Command Execution
    private int transactionId = 0;
    private String endpointId;
    private MgcpEndpoint endpoint;
    private NotifiedEntity notifiedEntity;
    private String requestIdentifier;
    private MgcpRequestedEvent[] requestedEvents;
    private SignalRequest[] signalRequests;

    public RequestNotificationCommand(MgcpEndpointManager endpointManager, MgcpSignalProvider signalProvider) {
        super(endpointManager);
        this.signalProvider = signalProvider;
    }

    private void validateEndpointId(String endpointId) throws MgcpCommandException {
        if (endpointId.indexOf(WILDCARD_ANY) != -1) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(),
                    MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
    }

    private void validateRequest(MgcpRequest request) throws MgcpCommandException, RuntimeException {
        this.transactionId = request.getTransactionId();

        // Endpoint Name
        this.endpointId = request.getEndpointId().substring(0, request.getEndpointId().indexOf(ENDPOINT_ID_SEPARATOR));
        validateEndpointId(this.endpointId);

        // Notified Entity
        String callAgent = request.getParameter(MgcpParameterType.NOTIFIED_ENTITY);
        if (callAgent != null) {
            try {
                this.notifiedEntity = NotifiedEntityParser.parse(callAgent);
            } catch (ParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(),
                        MgcpResponseCode.PROTOCOL_ERROR.message());
            }
        }

        // Request Identifier
        this.requestIdentifier = request.getParameter(MgcpParameterType.REQUEST_ID);
        if (this.requestIdentifier == null || this.requestIdentifier.isEmpty()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }

        // Requested Events
        String events = request.getParameter(MgcpParameterType.REQUESTED_EVENTS);
        if (events != null) {
            try {
                this.requestedEvents = MgcpRequestedEventsParser.parse(events);
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE.code(), MgcpResponseCode.UNKNOWN_PACKAGE.message());
            } catch (UnrecognizedMgcpEventException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.message());
            } catch (UnrecognizedMgcpActionException e) {
                throw new MgcpCommandException(MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR.code(), MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR.message());
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
            }
        }

        // Requested Signals
        String signals = request.getParameter(MgcpParameterType.REQUESTED_SIGNALS);
        if (signals != null) {
            try {
                this.signalRequests = SignalsRequestParser.parse(signals);
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
            }
        }
    }

    private MgcpResponse buildResponse() {
        MgcpResponse response = new MgcpResponse();
        response.setCode(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code());
        response.setMessage(MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message());
        response.setTransactionId(this.transactionId);
        return response;
    }

    private void executeCommand() throws MgcpCommandException {
        // Retrieve endpoint
        this.endpoint = this.endpointManager.getEndpoint(this.endpointId);
        if (this.endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }

        // TODO Check if connection is specified (to do later down in roadmap)

        // Retrieve signal requests (if any)
        MgcpSignal[] signals = null;
        if (this.signalRequests != null) {
            signals = new MgcpSignal[this.signalRequests.length];
            for (int i = 0; i < this.signalRequests.length; i++) {
                try {
                    SignalRequest signalRequest = this.signalRequests[i];
                    signals[i] = this.signalProvider.provide(signalRequest.getPackageName(), signalRequest.getSignalType(), signalRequest.getParameters(), this.endpoint.getMediaGroup());
                } catch (UnrecognizedMgcpPackageException e) {
                    throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE.code(),
                            MgcpResponseCode.UNKNOWN_PACKAGE.message());
                } catch (UnsupportedMgcpSignalException e) {
                    throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.message());
                }
            }
        }

        NotificationRequest rqnt = new NotificationRequest(transactionId, requestIdentifier, notifiedEntity, requestedEvents, signals);
        
        // Make MGCP Controller observe state of the endpoint
        Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        while (iterator.hasNext()) {
            MgcpMessageObserver observer = iterator.next();
            this.endpoint.observe(observer);
        }
        
        // Request notification to endpoint
        this.endpoint.requestNotification(rqnt);
    }

    @Override
    protected MgcpResponse executeRequest(MgcpRequest request) throws MgcpCommandException {
        try {
            validateRequest(request);
            executeCommand();
            return buildResponse();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    @Override
    protected MgcpResponse rollback(int transactionId, int code, String message) {
        MgcpResponse response = new MgcpResponse();
        response.setCode(code);
        response.setMessage(message);
        response.setTransactionId(transactionId);
        return response;
    }

    @Override
    protected void reset() {
        this.transactionId = 0;
        this.endpointId = null;
        this.endpoint = null;
        this.notifiedEntity = null;
        this.requestIdentifier = null;
        this.requestedEvents = null;
        this.signalRequests = null;
    }

}
