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

package org.restcomm.media.control.mgcp.command.crcx;

import org.restcomm.media.control.mgcp.command.MgcpCommandResult;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.message.MgcpParameterType;
import org.restcomm.media.control.mgcp.util.collections.Parameters;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CreateConnectionContext {

    // Dependencies
    private final MgcpConnectionProvider connectionProvider;
    private final MgcpEndpointManager endpointManager;

    // MGCP Command
    private final int transactionId;
    private final Parameters<MgcpParameterType> parameters;

    private int callId;
    private String primaryEndpointId;
    private MgcpEndpoint primaryEndpoint;
    private String secondaryEndpointId;
    private MgcpEndpoint secondaryEndpoint;
    private MgcpConnection primaryConnection;
    private boolean primaryConnectionOpen;
    private MgcpConnection secondaryConnection;
    private boolean secondaryConnectionOpen;
    private ConnectionMode connectionMode;
    private String remoteDescription;
    private String localDescription;
    private LocalConnectionOptions localConnectionOptions;

    private FutureCallback<MgcpCommandResult> callback;
    private Throwable error;

    public CreateConnectionContext(MgcpConnectionProvider connectionProvider, MgcpEndpointManager endpointManager, int transactionId, Parameters<MgcpParameterType> parameters) {
        // Dependencies
        this.connectionProvider = connectionProvider;
        this.endpointManager = endpointManager;
        
        // MGCP Command
        this.transactionId = transactionId;
        this.parameters = parameters;
        
        this.callId = 0;
        this.primaryEndpointId = "";
        this.primaryEndpoint = null;
        this.secondaryEndpointId = "";
        this.secondaryEndpoint = null;
        this.primaryConnection = null;
        this.primaryConnectionOpen = false;
        this.secondaryConnection = null;
        this.secondaryConnectionOpen = false;
        this.connectionMode = ConnectionMode.INACTIVE;
        this.remoteDescription = "";
        this.localDescription = "";
        this.localConnectionOptions = new LocalConnectionOptions();
        
        this.callback = null;
        this.error = null;
    }

    protected int getCallId() {
        return callId;
    }

    protected void setCallId(int callId) {
        this.callId = callId;
    }

    protected String getPrimaryEndpointId() {
        return primaryEndpointId;
    }

    protected void setPrimaryEndpointId(String primaryEndpointId) {
        this.primaryEndpointId = primaryEndpointId;
    }
    
    protected MgcpEndpoint getPrimaryEndpoint() {
        return primaryEndpoint;
    }
    
    protected void setPrimaryEndpoint(MgcpEndpoint primaryEndpoint) {
        this.primaryEndpoint = primaryEndpoint;
        if(this.primaryEndpoint != null) {
            setPrimaryEndpointId(this.primaryEndpoint.getEndpointId().toString());
        }
    }

    protected String getSecondaryEndpointId() {
        return secondaryEndpointId;
    }

    protected void setSecondaryEndpointId(String secondaryEndpointId) {
        this.secondaryEndpointId = secondaryEndpointId;
    }
    
    protected MgcpEndpoint getSecondaryEndpoint() {
        return secondaryEndpoint;
    }
    
    protected void setSecondaryEndpoint(MgcpEndpoint secondaryEndpoint) {
        this.secondaryEndpoint = secondaryEndpoint;
        if(this.secondaryEndpoint != null) {
            setSecondaryEndpointId(this.secondaryEndpoint.getEndpointId().toString());
        }
    }

    protected MgcpConnection getPrimaryConnection() {
        return primaryConnection;
    }

    protected void setPrimaryConnection(MgcpConnection primaryConnection) {
        this.primaryConnection = primaryConnection;
    }
    
    protected void setPrimaryConnectionOpen(boolean primaryConnectionOpen) {
        this.primaryConnectionOpen = primaryConnectionOpen;
    }
    
    protected boolean isPrimaryConnectionOpen() {
        return primaryConnectionOpen;
    }

    protected MgcpConnection getSecondaryConnection() {
        return secondaryConnection;
    }

    protected void setSecondaryConnection(MgcpConnection secondaryConnection) {
        this.secondaryConnection = secondaryConnection;
    }
    
    protected void setSecondaryConnectionOpen(boolean secondaryConnectionOpen) {
        this.secondaryConnectionOpen = secondaryConnectionOpen;
    }
    
    protected boolean isSecondaryConnectionOpen() {
        return secondaryConnectionOpen;
    }

    protected ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    protected void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
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
        this.localDescription = localDescription == null? "" : localDescription;
    }

    protected LocalConnectionOptions getLocalConnectionOptions() {
        return localConnectionOptions;
    }

    protected void setLocalConnectionOptions(LocalConnectionOptions localConnectionOptions) {
        this.localConnectionOptions = localConnectionOptions;
    }

    protected MgcpConnectionProvider getConnectionProvider() {
        return connectionProvider;
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
