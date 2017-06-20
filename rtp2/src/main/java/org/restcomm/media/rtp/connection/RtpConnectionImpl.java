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

import java.net.SocketAddress;

import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionImpl implements RtpConnection {

    // Dependencies
    private final SessionDescriptionParser sdpParser;
    private final RtpSessionFactory sessionFactory;

    // RTP Connection
    private final RtpConnectionContext context;
    private final RtpConnectionFsm fsm;
    private RtpSession session;

    public RtpConnectionImpl(RtpConnectionContext context, SessionDescriptionParser sdpParser, RtpSessionFactory sessionFactory) {
        // Dependencies
        this.sdpParser = sdpParser;
        this.sessionFactory = sessionFactory;

        // RTP Connection
        this.context = context;
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(this.context);
        this.session = null;
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        if(this.fsm.canAccept(RtpConnectionEvent.UPDATE_MODE)) {
            
        } else {
            
        }

    }

    @Override
    public void open(SocketAddress localAddress, ConnectionMode mode, String sdp, FutureCallback<Void> callback) {
        if(this.fsm.canAccept(RtpConnectionEvent.OPEN)) {
            try {
                // Parse remote description
                final SessionDescription remoteDescription = SessionDescriptionParser.parse(sdp);
                final MediaDescriptionField remoteAudioSession = remoteDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase());
                final RtpSession rtpSession = this.sessionFactory.build();
                
                // Fire event in FSM to open the connection
                OpenContext txContext = new OpenContext(callback, rtpSession, mode, localAddress, remoteAudioSession);
                this.fsm.fire(RtpConnectionEvent.OPEN, txContext);
            } catch (SdpException e) {
                String cname = this.context.getCname();
                RtpConnectionException exception = new RtpConnectionException("RTP connection " + cname + " could not parse remote description.", e);
                callback.onFailure(exception);
            }
        } else {
            // Reject operation
            String cname = this.context.getCname();
            String event = RtpConnectionEvent.OPEN.name();
            RtpConnectionException exception = new RtpConnectionException("RTP connection " + cname + " denied " + event +" operation");
            callback.onFailure(exception);
        }
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // TODO Auto-generated method stub

    }

}
