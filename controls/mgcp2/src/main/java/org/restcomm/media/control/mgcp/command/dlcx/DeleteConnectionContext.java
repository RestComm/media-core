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

package org.restcomm.media.control.mgcp.command.dlcx;

import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.control.mgcp.util.collections.Parameters;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class DeleteConnectionContext {

    static final int NO_CALL_ID = -1;
    static final int NO_CONNECTION_ID = -1;
    static final MgcpConnection[] EMPTY_CONNECTIONS = new MgcpConnection[0];

    // Dependencies
    private final MgcpEndpointManager endpointManager;

    // Command Info
    private final int transactionId;
    private final Parameters<MgcpParameterType> parameters;

    // Input parameters
    private int callId;
    private String endpointId;
    private int connectionId;

    // Resources
    private MgcpEndpoint endpoint;
    private MgcpConnection[] connections;

    // Result
    private MgcpResponseCode response;
    private String connectionParams;

    // Callback
    private FutureCallback<MgcpCommandResult> callback;
    private Throwable error;

    public DeleteConnectionContext(int transactionId, Parameters<MgcpParameterType> parameters,
            MgcpEndpointManager endpointManager) {
        // Dependencies
        this.endpointManager = endpointManager;

        // Command Info
        this.transactionId = transactionId;
        this.parameters = parameters;

        // Input parameters
        this.callId = NO_CALL_ID;
        this.endpointId = "";
        this.connectionId = NO_CONNECTION_ID;

        // Resources
        this.endpoint = null;
        this.connections = EMPTY_CONNECTIONS;

        // Result
        this.response = MgcpResponseCode.ABORTED;
        this.connectionParams = "";
    }

    protected int getCallId() {
        return callId;
    }

    protected void setCallId(int callId) {
        this.callId = callId;
    }

    protected String getEndpointId() {
        return endpointId;
    }

    protected void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    protected int getConnectionId() {
        return connectionId;
    }

    protected void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    protected MgcpEndpoint getEndpoint() {
        return endpoint;
    }

    protected void setEndpoint(MgcpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected MgcpConnection[] getConnections() {
        return connections;
    }

    protected void setConnections(MgcpConnection[] connections) {
        this.connections = connections;
    }

    protected MgcpResponseCode getResponse() {
        return response;
    }

    protected void setResponse(MgcpResponseCode response) {
        this.response = response;
    }

    protected String getConnectionParams() {
        return connectionParams;
    }

    protected void setConnectionParams(String connectionParams) {
        this.connectionParams = connectionParams;
    }

    protected FutureCallback<MgcpCommandResult> getCallback() {
        return callback;
    }

    protected void setCallback(FutureCallback<MgcpCommandResult> callback) {
        this.callback = callback;
    }

    protected Throwable getError() {
        return error;
    }

    protected void setError(Throwable error) {
        this.error = error;
    }

    protected MgcpEndpointManager getEndpointManager() {
        return endpointManager;
    }

    protected int getTransactionId() {
        return transactionId;
    }

    protected Parameters<MgcpParameterType> getParameters() {
        return parameters;
    }

}
