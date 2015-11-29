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

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpRequest extends AbstractMgcpMessage {

    public static final String MGCP_VERSION = "MGCP 1.0";

    private MgcpActionType actionType;
    private String endpointId;
    private String sdp;

    public MgcpRequest() {
        super();
        this.endpointId = "";
        this.sdp = "";
    }

    public MgcpActionType getActionType() {
        return actionType;
    }

    public void setActionType(MgcpActionType actionType) {
        this.actionType = actionType;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(String endpointId) {
        this.endpointId = endpointId;
    }

    public String getSdp() {
        return sdp;
    }

    public void setSdp(String sdp) {
        this.sdp = sdp;
    }

    @Override
    public void reset() {
        super.reset();
        this.actionType = null;
        this.endpointId = "";
        this.sdp = "";
    }

    @Override
    public String toString() {
        return actionType + " " + transactionId + " " + " " + endpointId;
    }

}
