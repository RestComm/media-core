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

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.spi.ConnectionMode;

import java.net.InetSocketAddress;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpConnectionImpl implements RtpConnection {

    // Dependencies
    private final RtpSessionFactory sessionFactory;
    private final PortManager portManager;
    private final SessionDescriptionParser sdpParser;
    private final SdpBuilder sdpBuilder;

    // RTP Connection
    private final RtpConnectionContext context;
    private final RtpConnectionFsm fsm;

    public RtpConnectionImpl(RtpSessionFactory sessionFactory, PortManager portManager, SessionDescriptionParser sdpParser, SdpBuilder sdpBuilder, RtpConnectionContext context, RtpConnectionFsmBuilder fsmBuilder) {
        this.sessionFactory = sessionFactory;
        this.portManager = portManager;
        this.sdpParser = sdpParser;
        this.sdpBuilder = sdpBuilder;
        this.context = context;
        this.fsm = fsmBuilder.build(context);
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        // Build transitional context
        RtpConnectionTransitionContext txContext = new RtpConnectionTransitionContext();
        txContext.set(RtpConnectionTransitionParameter.MODE, mode);
        txContext.set(RtpConnectionTransitionParameter.RTP_SESSION, this.context.getRtpSession());
        txContext.set(RtpConnectionTransitionParameter.CALLBACK, callback);

        // Request connection to update mode
        this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, txContext);
    }

    @Override
    public void open(String remoteDescription, FutureCallback<String> callback) {
        // Reserve address for connection
        String localAddress = this.context.getBindAddress();
        int port = this.portManager.next();

        // Build transitional context
        RtpConnectionTransitionContext txContext = new RtpConnectionTransitionContext();
        txContext.set(RtpConnectionTransitionParameter.SDP_PARSER, this.sdpParser);
        txContext.set(RtpConnectionTransitionParameter.SDP_BUILDER, this.sdpBuilder);
        txContext.set(RtpConnectionTransitionParameter.RTP_SESSION_FACTORY, this.sessionFactory);
        txContext.set(RtpConnectionTransitionParameter.CNAME, this.context.getCname());
        txContext.set(RtpConnectionTransitionParameter.BIND_ADDRESS, new InetSocketAddress(localAddress, port));
        txContext.set(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, this.context.getExternalAddress());
        txContext.set(RtpConnectionTransitionParameter.REMOTE_SDP_STRING, remoteDescription);
        txContext.set(RtpConnectionTransitionParameter.CALLBACK, callback);

        // Request connection to open
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);
    }

    @Override
    public void halfOpen(FutureCallback<String> callback) {
        // Reserve address to connection
        String localAddress = this.context.getBindAddress();
        int port = this.portManager.next();

        // Build transitional context
        RtpConnectionTransitionContext txContext = new RtpConnectionTransitionContext();
        txContext.set(RtpConnectionTransitionParameter.SDP_BUILDER, this.sdpBuilder);
        txContext.set(RtpConnectionTransitionParameter.RTP_SESSION_FACTORY, this.sessionFactory);
        txContext.set(RtpConnectionTransitionParameter.CNAME, this.context.getCname());
        txContext.set(RtpConnectionTransitionParameter.BIND_ADDRESS, new InetSocketAddress(localAddress, port));
        txContext.set(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, this.context.getExternalAddress());
        txContext.set(RtpConnectionTransitionParameter.CALLBACK, callback);

        // Request connection to open
        this.fsm.fire(RtpConnectionEvent.HALF_OPEN, txContext);
    }

    @Override
    public void modify(String remoteDescription, FutureCallback<String> callback) {
        // Build transitional context
        RtpConnectionTransitionContext txContext = new RtpConnectionTransitionContext();
        txContext.set(RtpConnectionTransitionParameter.SDP_PARSER, this.sdpParser);
        txContext.set(RtpConnectionTransitionParameter.SDP_BUILDER, this.sdpBuilder);
        txContext.set(RtpConnectionTransitionParameter.RTP_SESSION, this.context.getRtpSession());
        txContext.set(RtpConnectionTransitionParameter.REMOTE_SDP_STRING, remoteDescription);
        txContext.set(RtpConnectionTransitionParameter.CNAME, this.context.getCname());
        txContext.set(RtpConnectionTransitionParameter.INBOUND, this.context.isInbound());
        txContext.set(RtpConnectionTransitionParameter.BIND_ADDRESS, this.context.getRtpSession().getRtpAddress());
        txContext.set(RtpConnectionTransitionParameter.EXTERNAL_ADDRESS, this.context.getExternalAddress());
        txContext.set(RtpConnectionTransitionParameter.CALLBACK, callback);

        // Request connection to open
        this.fsm.fire(RtpConnectionEvent.MODIFY, txContext);
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // Build transitional context
        RtpConnectionTransitionContext txContext = new RtpConnectionTransitionContext();
        txContext.set(RtpConnectionTransitionParameter.RTP_SESSION, this.context.getRtpSession());
        txContext.set(RtpConnectionTransitionParameter.CALLBACK, callback);

        // Request connection to close
        this.fsm.fire(RtpConnectionEvent.CLOSE, txContext);
    }

    @Override
    public AudioComponent getAudioComponent() {
        RtpSession rtpSession = this.context.getRtpSession();
        return rtpSession == null ? null : rtpSession.getAudioComponent();
    }

    @Override
    public OOBComponent getOOBComponent() {
        RtpSession rtpSession = this.context.getRtpSession();
        return rtpSession == null ? null : rtpSession.getOOBComponent();
    }

}
