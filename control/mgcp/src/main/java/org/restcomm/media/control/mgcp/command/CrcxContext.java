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

package org.restcomm.media.control.mgcp.command;

import org.restcomm.media.control.mgcp.message.LocalConnectionOptions;
import org.restcomm.media.control.mgcp.message.MgcpResponseCode;
import org.restcomm.media.core.spi.ConnectionMode;

/**
 * Contains contextual data for an MGCP CRCX operation.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class CrcxContext {

    private int callId;
    private String endpointId;
    private String secondEndpointId;
    private int connectionId;
    private int secondConnectionId;
    private String remoteDescription;
    private String localDescription;
    private ConnectionMode connectionMode;
    private LocalConnectionOptions lcOptions;

    private int code;
    private String message;

    public CrcxContext() {
        super();
        this.callId = 0;
        this.endpointId = "";
        this.secondEndpointId = "";
        this.connectionId = 0;
        this.secondConnectionId = 0;
        this.remoteDescription = "";
        this.localDescription = "";
        this.connectionMode = null;
        this.lcOptions = new LocalConnectionOptions();

        this.code = MgcpResponseCode.ABORTED.code();
        this.message = MgcpResponseCode.ABORTED.message();
    }

    public int getCallId() {
        return callId;
    }

    public void setCallId(int callId) {
        this.callId = callId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getSecondEndpointId() {
        return secondEndpointId;
    }

    public void setSecondEndpointId(String secondEndpointId) {
        this.secondEndpointId = secondEndpointId;
    }

    public int getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(int connectionId) {
        this.connectionId = connectionId;
    }

    public int getSecondConnectionId() {
        return secondConnectionId;
    }

    public void setSecondConnectionId(int secondConnectionId) {
        this.secondConnectionId = secondConnectionId;
    }

    public String getRemoteDescription() {
        return remoteDescription;
    }

    public void setRemoteDescription(String remoteDescription) {
        this.remoteDescription = remoteDescription;
    }

    public String getLocalDescription() {
        return localDescription;
    }

    public void setLocalDescription(String localDescription) {
        this.localDescription = localDescription;
    }

    public ConnectionMode getConnectionMode() {
        return connectionMode;
    }

    public void setConnectionMode(ConnectionMode connectionMode) {
        this.connectionMode = connectionMode;
    }

    public LocalConnectionOptions getLocalConnectionOptions() {
        return lcOptions;
    }

    public void setLocalConnectionOptions(LocalConnectionOptions lcOptions) {
        this.lcOptions = lcOptions;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

}
