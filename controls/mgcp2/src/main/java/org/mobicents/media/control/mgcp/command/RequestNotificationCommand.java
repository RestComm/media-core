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
import org.mobicents.media.control.mgcp.pkg.MgcpPackageManager;
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
import org.mobicents.media.control.mgcp.util.collections.Parameters;

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
    private final MgcpPackageManager packageManager;

    public RequestNotificationCommand(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager, MgcpPackageManager packageManager, MgcpSignalProvider signalProvider) {
        super(transactionId, parameters, endpointManager);
        this.packageManager = packageManager;
        this.signalProvider = signalProvider;
    }

    private void validateParameters(Parameters<MgcpParameterType> parameters, RqntContext context)
            throws MgcpCommandException, RuntimeException {
        // Endpoint Name
        Optional<String> endpointId = parameters.getString(MgcpParameterType.ENDPOINT_ID);
        if (!endpointId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        } else if (endpointId.get().contains(WILDCARD_ANY)) {
            throw new MgcpCommandException(MgcpResponseCode.WILDCARD_TOO_COMPLICATED);
        } else {
            context.endpointId = endpointId.get();
        }

        // Request Identifier
        Optional<String> requestId = this.requestParameters.getString(MgcpParameterType.REQUEST_ID);
        if (!requestId.isPresent()) {
            throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
        } else {
            context.requestId = requestId.get();
        }

        // Signal Requests (optional)
        Optional<String> requestedSignals = this.requestParameters.getString(MgcpParameterType.REQUESTED_SIGNALS);
        if (requestedSignals.isPresent()) {
            try {
                // Parse signals
                SignalRequest[] signalRequests = SignalsRequestParser.parse(requestedSignals.get());
                context.signalRequests = signalRequests;
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        }

        // Requested Events (optional)
        Optional<String> events = this.requestParameters.getString(MgcpParameterType.REQUESTED_EVENTS);
        if (events.isPresent()) {
            try {
                MgcpRequestedEvent[] requestedEvents = MgcpRequestedEventsParser.parse(events.get());
                context.requestedEvents = requestedEvents;
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE);
            } catch (UnrecognizedMgcpEventException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL);
            } catch (UnrecognizedMgcpActionException e) {
                throw new MgcpCommandException(MgcpResponseCode.EVENT_OR_SIGNAL_PARAMETER_ERROR);
            } catch (MgcpParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        }

        // Notified Entity (optional)
        Optional<String> callAgent = this.requestParameters.getString(MgcpParameterType.NOTIFIED_ENTITY);
        if (callAgent.isPresent()) {
            try {
                NotifiedEntity notifiedEntity = NotifiedEntityParser.parse(callAgent.get());
                context.notifiedEntity = notifiedEntity;
            } catch (ParseException e) {
                throw new MgcpCommandException(MgcpResponseCode.PROTOCOL_ERROR);
            }
        }
    }

    private void executeCommand(RqntContext context) throws MgcpCommandException {
        // Retrieve endpoint
        MgcpEndpoint endpoint = this.endpointManager.getEndpoint(context.endpointId);
        if (endpoint == null) {
            throw new MgcpCommandException(MgcpResponseCode.ENDPOINT_UNKNOWN);
        }

        // Build signals (if any)
        SignalRequest[] signalRequests = context.signalRequests;
        MgcpSignal[] signals = new MgcpSignal[0];
        if (signalRequests.length > 0) {
            try {
                // Convert signal requests to actual signals
                signals = new MgcpSignal[signalRequests.length];
                for (int i = 0; i < signalRequests.length; i++) {
                    SignalRequest signalRequest = signalRequests[i];
                    signals[i] = this.signalProvider.provide(signalRequest.getPackageName(), signalRequest.getSignalType(), Integer.parseInt(context.requestId, 16), context.notifiedEntity, signalRequest.getParameters(), endpoint);
                }
            } catch (UnrecognizedMgcpPackageException e) {
                throw new MgcpCommandException(MgcpResponseCode.UNKNOWN_PACKAGE);
            } catch (UnsupportedMgcpSignalException e) {
                throw new MgcpCommandException(MgcpResponseCode.NO_SUCH_EVENT_OR_SIGNAL);
            }
        }

        // Submit notification request to endpoint
        NotificationRequest rqnt = new NotificationRequest(transactionId, context.requestId, context.notifiedEntity, context.requestedEvents, signals);

        // Request notification to endpoint
        endpoint.requestNotification(rqnt);
    }

    private MgcpCommandResult respond(RqntContext context) {
        Parameters<MgcpParameterType> parameters = new Parameters<>();
        MgcpCommandResult result = new MgcpCommandResult(this.transactionId, context.code, context.message, parameters);
        return result;
    }

    @Override
    public MgcpCommandResult call() {
        // Initialize empty context
        RqntContext context = new RqntContext();
        try {
            // Validate Parameters
            validateParameters(this.requestParameters, context);
            // Execute Command
            executeCommand(context);
            context.code = MgcpResponseCode.TRANSACTION_WAS_EXECUTED.code();
            context.message = MgcpResponseCode.TRANSACTION_WAS_EXECUTED.message();
        } catch (RuntimeException e) {
            log.warn("Unexpected error occurred during tx=" + this.transactionId + " execution. Reason:", e);
            context.code = MgcpResponseCode.PROTOCOL_ERROR.code();
            context.message = MgcpResponseCode.PROTOCOL_ERROR.message();
        } catch (MgcpCommandException e) {
            log.warn("Protocol error occurred during tx=" + this.transactionId + " execution. Reason:", e);
            context.code = e.getCode();
            context.message = e.getMessage();
        }
        return respond(context);
    }

    private class RqntContext {

        private String endpointId;
        private String requestId;
        private SignalRequest[] signalRequests;
        private MgcpRequestedEvent[] requestedEvents;
        private NotifiedEntity notifiedEntity;

        private int code;
        private String message;

        public RqntContext() {
            this.endpointId = "";
            this.requestId = "";
            this.signalRequests = new SignalRequest[0];
            this.requestedEvents = new MgcpRequestedEvent[0];
            this.notifiedEntity = null;

            this.code = MgcpResponseCode.ABORTED.code();
            this.message = MgcpResponseCode.ABORTED.message();
        }
    }

}
