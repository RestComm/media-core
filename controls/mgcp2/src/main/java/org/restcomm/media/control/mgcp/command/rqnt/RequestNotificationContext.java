/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2017, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General  License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General  License for more details.
 *
 * You should have received a copy of the GNU Lesser General 
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.restcomm.media.control.mgcp.command.rqnt;

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.command.param.NotifiedEntity;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.pkg.MgcpPackageManager;
import org.restcomm.media.control.mgcp.pkg.MgcpRequestedEvent;
import org.restcomm.media.control.mgcp.pkg.MgcpSignalProvider;
import org.restcomm.media.control.mgcp.pkg.SignalRequest;
import org.restcomm.media.control.mgcp.signal.MgcpSignal;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com) on 23/11/2017
 */
class RequestNotificationContext {

    static final SignalRequest[] EMPTY_REQUESTED_SIGNALS = new SignalRequest[0];
    static final MgcpSignal[] EMPTY_SIGNALS = new MgcpSignal[0];
    static final MgcpRequestedEvent[] EMPTY_EVENTS = new MgcpRequestedEvent[0];

    // MGCP Components
    private final MgcpEndpointManager endpointManager;
    private final MgcpSignalProvider signalProvider;
    private final MgcpPackageManager packageManager;

    // RQNT Context
    private final int transactionId;
    private final Parameters<MgcpParameterType> parameters;

    private String endpointId;
    private MgcpEndpoint endpoint;
    private String requestIdHex;
    private SignalRequest[] signalRequests;
    private MgcpSignal[] signals;
    private MgcpRequestedEvent[] requestedEvents;
    private NotifiedEntity notifiedEntity;

    // RQNT Result
    private Throwable error;
    private FutureCallback<MgcpCommandResult> callback;

    RequestNotificationContext(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager, MgcpSignalProvider signalProvider, MgcpPackageManager packageManager) {
        // MGCP Components
        this.endpointManager = endpointManager;
        this.signalProvider = signalProvider;
        this.packageManager = packageManager;

        // RQNT Context
        this.transactionId = transactionId;
        this.parameters = parameters;

        this.endpointId = "";
        this.endpoint = null;
        this.requestIdHex = "";
        this.signalRequests = EMPTY_REQUESTED_SIGNALS;
        this.requestedEvents = EMPTY_EVENTS;
        this.notifiedEntity = null;
    }

    MgcpEndpointManager getEndpointManager() {
        return endpointManager;
    }

    MgcpSignalProvider getSignalProvider() {
        return signalProvider;
    }

    MgcpPackageManager getPackageManager() {
        return packageManager;
    }

    int getTransactionId() {
        return transactionId;
    }

    Parameters<MgcpParameterType> getParameters() {
        return parameters;
    }

    String getEndpointId() {
        return endpointId;
    }

    void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    MgcpEndpoint getEndpoint() {
        return endpoint;
    }

    void setEndpoint(MgcpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    String getRequestId() {
        return requestIdHex;
    }

    void setRequestId(String requestId) {
        this.requestIdHex = requestId;
    }

    SignalRequest[] getSignalRequests() {
        return signalRequests;
    }

    void setSignalRequests(SignalRequest[] signalRequests) {
        this.signalRequests = signalRequests;
    }

    MgcpSignal[] getSignals() {
        return signals;
    }

    void setSignals(MgcpSignal[] signals) {
        this.signals = signals;
    }

    MgcpRequestedEvent[] getRequestedEvents() {
        return requestedEvents;
    }

    void setRequestedEvents(MgcpRequestedEvent[] requestedEvents) {
        this.requestedEvents = requestedEvents;
    }

    NotifiedEntity getNotifiedEntity() {
        return notifiedEntity;
    }

    void setNotifiedEntity(NotifiedEntity notifiedEntity) {
        this.notifiedEntity = notifiedEntity;
    }

    Throwable getError() {
        return error;
    }

    void setError(Throwable error) {
        this.error = error;
    }

    FutureCallback<MgcpCommandResult> getCallback() {
        return callback;
    }

    void setCallback(FutureCallback<MgcpCommandResult> callback) {
        this.callback = callback;
    }

}
