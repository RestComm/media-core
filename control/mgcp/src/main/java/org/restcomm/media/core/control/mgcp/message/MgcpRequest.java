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

package org.restcomm.media.core.control.mgcp.message;

import java.util.Iterator;

import com.google.common.base.Optional;

/**
 * Represents an MGCP request.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequest extends MgcpMessage {

    public static final String VERSION = "MGCP 1.0";

    private MgcpRequestType requestType;
    private LocalConnectionOptions lcOptions;
    private final StringBuilder builder;

    public MgcpRequest() {
        super();
        this.lcOptions = new LocalConnectionOptions();
        this.builder = new StringBuilder();
    }

    public MgcpRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(MgcpRequestType requestType) {
        this.requestType = requestType;
    }

    public String getEndpointId() {
        return this.parameters.getString(MgcpParameterType.ENDPOINT_ID).orNull();
    }

    public void setEndpointId(String endpointId) {
        this.parameters.put(MgcpParameterType.ENDPOINT_ID, endpointId);
    }

    public LocalConnectionOptions getLocalConnectionOptions() {
        return this.lcOptions;
    }

    public void setLocalConnectionOptions(LocalConnectionOptions lcOptions) {
        this.lcOptions = lcOptions;
        this.parameters.put(MgcpParameterType.LOCAL_CONNECTION_OPTIONS, lcOptions.toString());
    }

    @Override
    public boolean isRequest() {
        return true;
    }

    @Override
    public String toString() {
        // Reset builder
        this.builder.setLength(0);

        // Build header
        this.builder.append(this.requestType.name()).append(" ").append(this.transactionId).append(" ").append(getEndpointId())
                .append(" ").append(VERSION).append(System.lineSeparator());

        // Build parameters
        Iterator<MgcpParameterType> keys = this.parameters.keySet().iterator();
        while (keys.hasNext()) {
            MgcpParameterType key = (MgcpParameterType) keys.next();
            if (!MgcpParameterType.ENDPOINT_ID.equals(key) && !MgcpParameterType.SDP.equals(key)) {
                Optional<String> value = this.parameters.getString(key);
                if (value.isPresent()) {
                    builder.append(key.getCode()).append(":").append(value.get()).append(System.lineSeparator());
                }
            }
        }

        // Print SDP (if any)
        Optional<String> sdp = this.parameters.getString(MgcpParameterType.SDP);
        if (sdp.isPresent()) {
            builder.append(System.lineSeparator()).append(sdp.get());
        }
        return builder.toString();
    }

}
