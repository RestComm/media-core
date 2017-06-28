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

import java.net.InetSocketAddress;

import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionImpl implements RtpConnection {

    // Dependencies
    private final SessionDescriptionParser sdpParser;
    private final SdpBuilder sdpBuilder;
    private final RtpSessionFactory sessionFactory;
    private final PortManager portManager;

    // RTP Connection
    private final RtpConnectionContext context;
    private final RtpConnectionFsm fsm;
    private RtpSession session;

    public RtpConnectionImpl(RtpConnectionContext context, SessionDescriptionParser sdpParser, SdpBuilder sdpBuilder, RtpSessionFactory sessionFactory, PortManager portManager) {
        // Dependencies
        this.sdpParser = sdpParser;
        this.sdpBuilder = sdpBuilder;
        this.sessionFactory = sessionFactory;
        this.portManager = portManager;

        // RTP Connection
        this.context = context;
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(this.context);
        this.session = null;
    }
    
    @Override
    public String getLocalDescription() {
        SessionDescription sdp = this.context.getLocalDescription();
        return sdp == null ? "" : sdp.toString();
    }
    
    @Override
    public String getRemoteDescription() {
        SessionDescription sdp = this.context.getRemoteDescription();
        return sdp == null ? "" : sdp.toString();
    }
    
    @Override
    public boolean isOpen() {
        RtpConnectionState state = this.fsm.getCurrentState();
        switch (state) {
            case OPEN:
            case UPDATING_MODE:
                return true;

            default:
                return false;
        }
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        if (this.fsm.canAccept(RtpConnectionEvent.UPDATE_MODE)) {
            // Fire event in FSM to update the connection mode
            UpdateModeContext txContext = new UpdateModeContext(callback, mode, this.session);
            this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, txContext);
        } else {
            // Reject operation
            denyOperation(callback, RtpConnectionEvent.UPDATE_MODE);
        }
    }

    @Override
    public void open(ConnectionMode mode, String sdp, FutureCallback<Void> callback) {
        if (this.fsm.canAccept(RtpConnectionEvent.OPEN)) {
            this.session = this.sessionFactory.build();
            final String localAddress = this.context.getLocalAddress();
            final String externalAddress = this.context.getExternalAddress();
            final int port = this.portManager.next();

            // Fire event in FSM to open the connection
            InetSocketAddress bindAddress = new InetSocketAddress(localAddress, port);
            OpenContext txContext = new OpenContext(callback, this.session, mode, bindAddress, externalAddress, sdp, this.sdpParser, this.sdpBuilder);
            this.fsm.fire(RtpConnectionEvent.OPEN, txContext);
        } else {
            // Reject operation
            denyOperation(callback, RtpConnectionEvent.OPEN);
        }
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        if (this.fsm.canAccept(RtpConnectionEvent.CLOSE)) {
            // Fire event in FSM to close the connection
            CloseContext txContext = new CloseContext(callback, this.session);
            this.fsm.fire(RtpConnectionEvent.CLOSE, txContext);
        } else {
            // Reject operation
            denyOperation(callback, RtpConnectionEvent.CLOSE);
        }
    }

    private void denyOperation(FutureCallback<Void> callback, RtpConnectionEvent event) {
        String cname = this.context.getCname();
        String eventName = RtpConnectionEvent.CLOSE.name();
        RtpConnectionException exception = new RtpConnectionException("RTP connection " + cname + " denied " + eventName + " operation");
        callback.onFailure(exception);
    }

}
