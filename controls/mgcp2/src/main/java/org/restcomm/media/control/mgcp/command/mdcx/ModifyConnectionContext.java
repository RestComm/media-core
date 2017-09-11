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

package org.restcomm.media.control.mgcp.command.mdcx;

import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ModifyConnectionContext {

    // Dependencies
    private final MgcpEndpointManager endpointManager;

    // Command Status
    private final int transactionId;
    private final Parameters<MgcpParameterType> parameters;

    private int callId;
    private String endpointId;
    private MgcpEndpoint endpoint;
    private int connectionId;
    private MgcpConnection connection;
    private ConnectionMode mode;
    private String remoteDescription;
    private String localDescription;

    private FutureCallback<MgcpCommandResult> callback;
    private Throwable error;

    public ModifyConnectionContext(int transactionId, Parameters<MgcpParameterType> parameters, MgcpEndpointManager endpointManager) {
        // Dependencies
        this.endpointManager = endpointManager;

        // Command Status
        this.transactionId = transactionId;
        this.parameters = parameters;

        this.callId = -1;
        this.endpointId = "";
        this.endpoint = null;
        this.connectionId = -1;
        this.connection = null;
        this.remoteDescription = "";
        this.localDescription = "";
        this.mode = null;
    }

    public MgcpEndpointManager getEndpointManager() {
        return this.endpointManager;
    }

    protected int getTransactionId() {
        return transactionId;
    }

    protected Parameters<MgcpParameterType> getParameters() {
        return parameters;
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

    public MgcpEndpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(MgcpEndpoint endpoint) {
        this.endpoint = endpoint;
    }

    protected int getConnectionId() {
        return connectionId;
    }

    protected void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public MgcpConnection getConnection() {
        return connection;
    }

    public void setConnection(MgcpConnection connection) {
        this.connection = connection;
    }

    protected ConnectionMode getMode() {
        return mode;
    }

    protected void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    protected String getRemoteDescription() {
        return remoteDescription;
    }

    protected void setRemoteDescription(String remoteDescription) {
        this.remoteDescription = remoteDescription;
    }

    protected String getLocalDescription() {
        return localDescription;
    }

    protected void setLocalDescription(String localDescription) {
        this.localDescription = localDescription;
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

}
