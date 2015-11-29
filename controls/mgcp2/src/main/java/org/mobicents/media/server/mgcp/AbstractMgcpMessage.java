/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpMessage {

    protected String message;
    protected int transactionId;
    private final Map<MgcpParameterType, String> parameters;

    protected AbstractMgcpMessage() {
        this.message = "";
        this.parameters = new HashMap<MgcpParameterType, String>(15);
    }

    public int getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getParameter(MgcpParameterType type) {
        return this.parameters.get(type);
    }

    public void setParameter(MgcpParameterType type, String value) {
        this.parameters.put(type, value);
    }

    public void clearParameters() {
        this.parameters.clear();
    }

    public boolean hasSdp() {
        String sdp = this.parameters.get(MgcpParameterType.SDP);
        return sdp != null && !sdp.isEmpty();
    }

    public void reset() {
        this.transactionId = -1;
        this.message = "";
        clearParameters();
    }

}
