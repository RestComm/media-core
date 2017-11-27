/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
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

package org.restcomm.media.control.mgcp.command.rqnt;

import com.google.common.base.Optional;
import org.apache.log4j.Logger;
import org.restcomm.media.control.mgcp.command.MgcpCommand;
import org.restcomm.media.control.mgcp.command.MgcpCommandException;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.exception.MgcpParseException;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.pkg.*;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpActionException;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpEventException;
import org.restcomm.media.control.mgcp.pkg.exception.UnrecognizedMgcpPackageException;
import org.restcomm.media.control.mgcp.pkg.exception.UnsupportedMgcpSignalException;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import java.text.ParseException;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
class ValidateParametersAction extends RequestNotificationAction {

    private static final Logger log = Logger.getLogger(ValidateParametersAction.class);

    static final ValidateParametersAction INSTANCE = new ValidateParametersAction();

    ValidateParametersAction() {
        super();
    }

    private String loadEndpointId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (endpointId.isPresent() && !endpointId.get().isEmpty()) {
            if (endpointId.get().contains(MgcpCommand.WILDCARD_ALL) || endpointId.get().contains(MgcpCommand.WILDCARD_ANY)) {
                throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
            } else {
                return endpointId.get();
            }
        } else {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }
    }

    private MgcpEndpoint loadEndpoint(String endpointId, MgcpEndpointManager endpointManager) throws MgcpCommandException {
        // Retrieve endpoint
        MgcpEndpoint endpoint = endpointManager.getEndpoint(endpointId);
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        } else {
            return endpoint;
        }
    }

    private String loadRequestId(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        // Request Identifier
        Optional<String> requestId = parameters.getString(MgcpParameterType.REQUEST_ID);
        if (!requestId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
        } else {
            return requestId.get();
        }
    }

    private SignalRequest[] loadRequestedSignals(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        Optional<String> requestedSignals = parameters.getString(MgcpParameterType.REQUESTED_SIGNALS);
        if (requestedSignals.isPresent()) {
            try {
                // Parse signals
                return SignalsRequestParser.parse(requestedSignals.get());
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        } else {
            return RequestNotificationContext.EMPTY_REQUESTED_SIGNALS;
        }
    }

    private MgcpSignal[] loadSignals(String requestId, MgcpEndpoint endpoint, SignalRequest[] requestedSignals, MgcpSignalProvider signalProvider) throws MgcpCommandException {
        if (requestedSignals.length > 0) {
            try {
                // Convert signal requests to actual signals
                final int count = requestedSignals.length;
                final MgcpSignal[] signals = new MgcpSignal[count];

                for (int i = 0; i < count; i++) {
                    SignalRequest signalRequest = requestedSignals[i];
                    signals[i] = signalProvider.provide(signalRequest.getPackageName(), signalRequest.getSignalType(), requestId, signalRequest.getParameters(), endpoint);
                }
                return signals;
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE);
            } catch (UnsupportedMgcpSignalException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL);
            }
        } else {
            return RequestNotificationContext.EMPTY_SIGNALS;
        }
    }

    private NotifiedEntity loadNotifiedEntity(Parameters<MgcpParameterType> parameters) throws MgcpCommandException {
        // Notified Entity (optional)
        Optional<String> callAgent = parameters.getString(MgcpParameterType.NOTIFIED_ENTITY);
        if (callAgent.isPresent()) {
            try {
                return NotifiedEntityParser.parse(callAgent.get());
            } catch (ParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        } else {
            return null;
        }
    }

    private MgcpRequestedEvent[] loadRequestedEvents(Parameters<MgcpParameterType> parameters, MgcpPackageManager packageManager) throws MgcpCommandException {
        // Requested Events (optional)
        Optional<String> events = parameters.getString(MgcpParameterType.REQUESTED_EVENTS);
        if (events.isPresent()) {
            try {
                final String requestId = parameters.getString(MgcpParameterType.REQUEST_ID).or("");
                return MgcpRequestedEventsParser.parse(requestId, events.get(), packageManager);
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE);
            } catch (UnrecognizedMgcpEventException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL);
            } catch (UnrecognizedMgcpActionException e) {
                throw new MgcpCommandException(MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR);
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        } else {
            return RequestNotificationContext.EMPTY_EVENTS;
        }
    }


    @Override
    public void execute(RequestNotificationState from, RequestNotificationState to, RequestNotificationEvent event, RequestNotificationContext context, RequestNotificationFsm stateMachine) {
        final Parameters<MgcpParameterType> parameters = context.getParameters();

        try {
            // Read parameters from MGCP command
            final String endpointId = loadEndpointId(parameters);
            final String requestId = loadRequestId(parameters);
            final SignalRequest[] requestedSignals = loadRequestedSignals(parameters);
            final MgcpRequestedEvent[] requestedEvents = loadRequestedEvents(parameters, context.getPackageManager());
            final NotifiedEntity notifiedEntity = loadNotifiedEntity(parameters);

            // Verify endpoint exists
            final MgcpEndpoint endpoint = loadEndpoint(endpointId, context.getEndpointManager());

            // Parse signals
            final MgcpSignal[] signals = loadSignals(requestId, endpoint, requestedSignals, context.getSignalProvider());

            // Update context
            context.setEndpointId(endpointId);
            context.setEndpoint(endpoint);
            context.setRequestId(requestId);
            context.setSignalRequests(requestedSignals);
            context.setSignals(signals);
            context.setRequestedEvents(requestedEvents);
            context.setNotifiedEntity(notifiedEntity);

            // Parameters loaded successfully. Move to next state.
            stateMachine.fire(RequestNotificationEvent.VALIDATED_PARAMETERS, context);
        } catch (Exception e) {
            // Save error in context and move to FAILED state
            context.setError(e);
            stateMachine.fireImmediate(RequestNotificationEvent.FAILURE, context);
        }

    }

}
