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

import org.apache.log4j.Logger;
import org.mobicents.media.control.mgcp.command.param.NotifiedEntity;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.exception.MgcpParseException;
import org.mobicents.media.control.mgcp.message.MgcpParameterType;
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
import org.mobicents.media.control.mgcp.util.Parameters;

import com.google.common.base.Optional;

/**
 * The NotificationRequest command is used to request the gateway to send notifications upon the occurrence of specified events
 * in an endpoint.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RequestNotificationCommand extends AbstractMgcpCommand {

    private static final Logger log = Logger.getLogger(RequestNotificationCommand.class);

    // MGCP Components
    private final MgcpSignalProvider signalProvider;

    public RequestNotificationCommand(int transactionId, MgcpEndpointManager endpointManager, Parameters<MgcpParameterType> parameters, MgcpSignalProvider signalProvider) {
        super(transactionId, endpointManager, parameters);
        this.signalProvider = signalProvider;
    }

    private void validateParameters() throws MgcpCommandException, RuntimeException {
        // Endpoint Name
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        } else if (endpointId.get().contains(WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED.code(), MgcpResponseCode.WILDCARD_TOO_COMPLICATED.message());
        }
        
        // Request Identifier
        Optional<String> requestId = this.requestParameters.getString(MgcpParameterType.REQUEST_ID);
        if (!requestId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    private void executeCommand() throws MgcpCommandException {
        // Retrieve endpoint
        Optional<String> endpointId = this.requestParameters.getString(MgcpParameterType.ENDPOINT_ID);
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(endpointId.get());
        
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN.code(), MgcpResponseCode.ENDPOINT_UNKNOWN.message());
        }
        
        // TODO Check if connection is specified (to do later down in roadmap)
        
        // Retrieve signal requests (if any)
        Optional<String> requestedSignals = this.requestParameters.getString(MgcpParameterType.REQUESTED_SIGNALS);
        Optional<MgcpSignal[]> signals = Optional.absent();
        if (requestedSignals.isPresent()) {
            try {
                // Parse signals
                SignalRequest[] signalRequests = SignalsRequestParser.parse(requestedSignals.get());

                // Convert signal requests to actual signals
                signals.or(new MgcpSignal[signalRequests.length]);
                for (int i = 0; i < signalRequests.length; i++) {
                    SignalRequest signalRequest = signalRequests[i];
                    signals.get()[i] = this.signalProvider.provide(signalRequest.getPackageName(), signalRequest.getSignalType(), signalRequest.getParameters(), endpoint.getMediaGroup());
                }
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE.code(), MgcpResponseCode.UNKNOWN_PACKAGE.message());
            } catch (UnsupportedMgcpSignalException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.code(), MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL.message());
            }
        }
        
        // Requested Events
        Optional<String> events = this.requestParameters.getString(MgcpParameterType.REQUESTED_EVENTS);
        Optional<MgcpRequestedEvent[]> requestedEvents = Optional.absent();
        if (events.isPresent()) {
            try {
                requestedEvents.or(MgcpRequestedEventsParser.parse(events.get()));
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
        
        // Request Identifier
        Optional<String> requestIdentifier = this.requestParameters.getString(MgcpParameterType.REQUEST_ID);
        
        // Notified Entity
        Optional<String> callAgent = this.requestParameters.getString(MgcpParameterType.NOTIFIED_ENTITY);
        Optional<NotifiedEntity> notifiedEntity = Optional.absent();
        if (callAgent.isPresent()) {
            try {
                notifiedEntity.or(NotifiedEntityParser.parse(callAgent.get()));
            } catch (ParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
            }
        }
        
        // Submit notification request to endpoint
        NotificationRequest rqnt = new NotificationRequest(transactionId, requestIdentifier.get(), notifiedEntity.get(), requestedEvents.get(), signals.get());
        
        // TODO Make MGCP Controller observe state of the endpoint
        // Iterator<MgcpMessageObserver> iterator = this.observers.iterator();
        // while (iterator.hasNext()) {
        // MgcpMessageObserver observer = iterator.next();
        // this.endpoint.observe(observer);
        // }
        
        // Request notification to endpoint
        endpoint.requestNotification(rqnt);
    }

    @Override
    protected void execute() throws MgcpCommandException {
        try {
            validateParameters();
            executeCommand();
        } catch (MgcpCommandException e) {
            throw e;
        } catch (RuntimeException e) {
            log.error("Could not process MGCP Request.", e);
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR.code(), MgcpResponseCode.PROTOCOL_ERROR.message());
        }
    }

    @Override
    protected void rollback() {
        // Nothing to cleanup
    }

}
