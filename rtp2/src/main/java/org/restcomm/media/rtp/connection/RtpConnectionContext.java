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

import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.spi.ConnectionMode;

/**
 * Runtime context of an RTP Connection.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
class RtpConnectionContext {
    
    // Dependencies
    private final RtpSessionFactory sessionFactory;
    private final PortManager portManager;
    
    // RTP Connection
    private final String cname;
    private final String bindAddress;
    private final String externalAddress;

    private ConnectionMode mode;
    private RtpSession rtpSession;
    private SessionDescription localDescription;
    private SessionDescription remoteDescription;
    
    private Throwable error;

    RtpConnectionContext(String cname, String bindAddress, String externalAddress, RtpSessionFactory sessionFactory, PortManager portManager) {
        // Dependencies
        this.sessionFactory = sessionFactory;
        this.portManager = portManager;
        
        // RTP Connection
        this.cname = cname;
        this.bindAddress = bindAddress;
        this.externalAddress = externalAddress;

        this.mode = ConnectionMode.INACTIVE;
    }
    
    /*
     * Dependencies
     */
    public RtpSessionFactory getSessionFactory() {
        return sessionFactory;
    }
    
    public PortManager getPortManager() {
        return portManager;
    }

    /*
     * RTP Connection
     */
    public Throwable getError() {
        return error;
    }
    
    public void setError(Throwable error) {
        this.error = error;
    }
    
    public ConnectionMode getMode() {
        return mode;
    }

    public void setMode(ConnectionMode mode) {
        this.mode = mode;
    }

    public RtpSession getRtpSession() {
        return rtpSession;
    }

    public void setRtpSession(RtpSession rtpSession) {
        this.rtpSession = rtpSession;
    }

    public SessionDescription getLocalDescription() {
        return localDescription;
    }

    public void setLocalDescription(SessionDescription localDescription) {
        this.localDescription = localDescription;
    }

    public SessionDescription getRemoteDescription() {
        return remoteDescription;
    }

    public void setRemoteDescription(SessionDescription remoteDescription) {
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
