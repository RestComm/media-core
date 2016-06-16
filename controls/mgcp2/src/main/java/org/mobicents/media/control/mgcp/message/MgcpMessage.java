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

package org.mobicents.media.control.mgcp.message;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a generic MGCP message.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class MgcpMessage {

    protected int transactionId;
    protected final Map<MgcpParameterType, String> parameters;
    protected final ByteBuffer data;
    
    public MgcpMessage() {
        this.transactionId = -1;
        this.parameters = new HashMap<>(10);
        this.data = ByteBuffer.allocate(8192);
    }
    
    public int getTransactionId() {
        return transactionId;
    }
    
    public void setTransactionId(int transactionId) {
        this.transactionId = transactionId;
    }
    
    public boolean isSdpDetected() {
        return this.parameters.containsKey(MgcpParameterType.SDP);
    }
    
    public String getParameter(MgcpParameterType type) {
        return this.parameters.get(type);
    }
    
    public boolean hasParameter(MgcpParameterType type) {
        return this.parameters.containsKey(type);
    }
    
    public void addParameter(MgcpParameterType type, String value) {
        this.parameters.put(type, value);
    }
    
    public void removeParameter(MgcpParameterType type) {
        this.parameters.remove(type);
    }
    
    public void removeParameters() {
        this.parameters.clear();
    }
    
    public abstract boolean isRequest();
    
    @Override
    public abstract String toString();
    
}
