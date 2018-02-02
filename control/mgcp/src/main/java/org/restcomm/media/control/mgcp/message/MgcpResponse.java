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

package org.restcomm.media.control.mgcp.message;

import java.util.Iterator;

import com.google.common.base.Optional;

/**
 * Represents an MGCP response.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpResponse extends MgcpMessage {

    private final StringBuilder builder;
    private String message;
    private int code;

    public MgcpResponse() {
        super();
        this.builder = new StringBuilder();
        this.message = "";
        this.code = 0;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    /**
     * Indicates whether response code is successful.
     * 
     * @return True is code lesser than 300; false otherwise.
     * @see <a href="https://tools.ietf.org/html/rfc3435#section-2.4">RFC3435 - Section 2.4</a>
     */
    public boolean isSuccessful() {
        return this.code <= 299;
    }

    @Override
    public String toString() {
        // Reset builder
        this.builder.setLength(0);

        // Build message
        this.builder.append(this.code).append(" ").append(getTransactionId()).append(" ").append(this.message);

        // Append Parameters
        Iterator<MgcpParameterType> iterator = this.parameters.keySet().iterator();
        while (iterator.hasNext()) {
            MgcpParameterType key = iterator.next();
            Optional<String> value = parameters.getString(key);
            if(value.isPresent() && !key.equals(MgcpParameterType.SDP)) {
                builder.append(System.lineSeparator()).append(key.getCode()).append(":").append(value.get());
            }
        }

        // Append SDP last (if available)
        Optional<String> sdp = this.parameters.getString(MgcpParameterType.SDP);
        if (sdp.isPresent()) {
            builder.append(System.lineSeparator()).append(System.lineSeparator()).append(sdp.get());
        }

        return this.builder.toString();
    }

}
