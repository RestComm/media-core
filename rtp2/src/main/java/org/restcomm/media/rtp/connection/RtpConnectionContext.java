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

package org.restcomm.media.rtp.connection;

import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.spi.ConnectionMode;

/**
 * Runtime context of an RTP Connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RtpConnectionContext {

    private final String cname;
    private final String bindAddress;
    private final String externalAddress;

    private boolean inbound;
    private ConnectionMode mode;
    private RtpSession rtpSession;
    private SessionDescription localDescription;
    private SessionDescription remoteDescription;

    private Throwable error;

    RtpConnectionContext(String cname, String bindAddress, String externalAddress) {
        this.cname = cname;
        this.bindAddress = bindAddress;
        this.externalAddress = externalAddress;
        this.mode = ConnectionMode.INACTIVE;
    }

    /*
     * RTP Connection
     */
    public Throwable getError() {
        return error;
    }

    void setError(Throwable error) {
        this.error = error;
    }
    
    public boolean isInbound() {
        return inbound;
    }
    
    void setInbound(boolean inbound) {
        this.inbound = inbound;
    }

    public ConnectionMode getMode() {
        return mode;
    }

    void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    public RtpSession getRtpSession() {
        return rtpSession;
    }

    void setRtpSession(RtpSession rtpSession) {
        this.rtpSession = rtpSession;
    }

    public SessionDescription getLocalDescription() {
        return localDescription;
    }

    void setLocalDescription(SessionDescription localDescription) {
        this.localDescription = localDescription;
    }

    public SessionDescription getRemoteDescription() {
        return remoteDescription;
    }

    void setRemoteDescription(SessionDescription remoteDescription) {
        this.remoteDescription = remoteDescription;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public String getExternalAddress() {
        return externalAddress;
    }

    public String getCname() {
        return cname;
    }

}
