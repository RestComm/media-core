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

package org.restcomm.media.rtp.session;

import com.google.common.util.concurrent.FutureCallback;
import org.apache.log4j.Logger;
import org.restcomm.media.rtp.*;
import org.restcomm.media.rtp.format.DtmfFormat;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.session.exception.RtpSessionUnwritableException;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import java.net.SocketAddress;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionFsmImpl extends AbstractRtpSessionFsm {

    private static final Logger log = Logger.getLogger(RtpSessionFsmImpl.class);

    private final RtpSessionContext globalContext;

    public RtpSessionFsmImpl(RtpSessionContext globalContext) {
        this.globalContext = globalContext;
    }

    /*
     * FSM
     */
    @Override
    public void enterAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final RtpChannel channel = txContext.getChannel();
        final RtpChannelInitializer channelInitializer = txContext.getChannelInitializer();

        // Open channel
        RtpSessionAllocateCallback callback = new RtpSessionAllocateCallback(this, txContext);
        channel.open(callback, channelInitializer);
    }

    @Override
    public void enterBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final SocketAddress address = txContext.getAddress();
        final RtpChannel channel = txContext.getChannel();

        // Ask RTP channel to bind to requested address
        RtpSessionBindCallback callback = new RtpSessionBindCallback(this, context);
        channel.bind(address, callback);
    }

    @Override
    public void enterOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        // RTP channel was bound successfully
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final SocketAddress localAddress = txContext.getAddress();

        // Update context with local address
        this.globalContext.setLocalAddress(localAddress);

        if (log.isDebugEnabled()) {
            long ssrc = this.globalContext.getSsrc();
            log.debug("RTP session " + ssrc + " bound to " + localAddress.toString());
        }

        // Move on to OPEN state
        fire(RtpSessionEvent.OPENED);
    }

    @Override
    public void enterNegotiatingFormats(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        final RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        final RTPFormats supported = this.globalContext.getSupportedFormats();
        final RTPFormats offered = txContext.getFormats();
        final RTPFormats negotiated = supported.intersection(offered);

        if (negotiated.isEmpty()) {
            RtpSessionUnsupportedFormatsContext unsupportedContext = new RtpSessionUnsupportedFormatsContext(supported, offered,
                    txContext.getCallback());
            fire(RtpSessionEvent.UNSUPPORTED_FORMATS, unsupportedContext);
        } else {
            // Update context with negotiated formats
            this.globalContext.setNegotiatedFormats(negotiated);

            if (log.isDebugEnabled()) {
                long ssrc = this.globalContext.getSsrc();
                log.debug("RTP session " + ssrc + " negotiated the formats " + negotiated.toString());
            }

            // Move on to next state
            fire(RtpSessionEvent.NEGOTIATED_FORMATS, context);
        }
    }

    @Override
    public void enterConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        final RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        final SocketAddress address = txContext.getAddress();
        final RtpChannel channel = txContext.getChannel();

        RtpSessionConnectCallback callback = new RtpSessionConnectCallback(this, txContext);
        channel.connect(address, callback);
    }

    @Override
    public void enterNegotiationFailed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        switch (event) {
            case UNSUPPORTED_FORMATS:
                if (log.isDebugEnabled()) {
                    RtpSessionUnsupportedFormatsContext txContext = (RtpSessionUnsupportedFormatsContext) context;
                    long ssrc = this.globalContext.getSsrc();
                    log.debug("RTP session " + ssrc + " failed to negotiate because it does not support any offered format "
                            + txContext.getOfferedFormats().toString());
                }
                break;

            case CONNECT_FAILURE:
                if (log.isDebugEnabled()) {
                    RtpSessionConnectFailureContext txContext = (RtpSessionConnectFailureContext) context;
                    long ssrc = this.globalContext.getSsrc();
                    SocketAddress address = txContext.getAddress();
                    log.debug("RTP session " + ssrc + " failed to connect to remote peer " + address.toString());
                }
                break;

            default:
                if (log.isDebugEnabled()) {
                    long ssrc = this.globalContext.getSsrc();
                    log.debug("RTP session " + ssrc + " failed to negotiate with remote peer");
                }
                break;
        }
    }

    @Override
    public void enterNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        final RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        final SocketAddress remoteAddress = txContext.getAddress();

        // Update context with remote peer address
        this.globalContext.setRemoteAddress(remoteAddress);

        if (log.isDebugEnabled()) {
            if (log.isDebugEnabled()) {
                long ssrc = this.globalContext.getSsrc();
                log.debug("RTP session " + ssrc + " connected to " + remoteAddress.toString());
            }
        }

        // Move on to next state
        fire(RtpSessionEvent.NEGOTIATED);
    }

    @Override
    public void onUpdateMode(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        RtpSessionUpdateModeContext txContext = (RtpSessionUpdateModeContext) context;

        ConnectionMode currentMode = this.globalContext.getMode();
        ConnectionMode newMode = txContext.getMode();

        if (!currentMode.equals(newMode)) {
            JitterBuffer jitterBuffer = txContext.getJitterBuffer();
            DtmfInput dtmfInput = txContext.getDtmfInput();
            RtpInput rtpInput = txContext.getRtpInput();
            RtpOutput rtpOutput = txContext.getRtpOutput();

            // Update mode of RTP components
            switch (newMode) {
                case RECV_ONLY:
                case NETWORK_LOOPBACK:
                    dtmfInput.activate();
                    rtpInput.activate();
                    rtpOutput.deactivate();
                    break;

                case SEND_ONLY:
                    dtmfInput.deactivate();
                    rtpInput.deactivate();
                    rtpOutput.activate();
                    jitterBuffer.restart();
                    break;

                case SEND_RECV:
                case CONFERENCE:
                    dtmfInput.activate();
                    rtpInput.activate();
                    rtpOutput.activate();
                    break;

                default:
                    dtmfInput.deactivate();
                    rtpInput.deactivate();
                    rtpOutput.deactivate();
                    jitterBuffer.restart();
                    break;
            }

            // Set new mode in global context
            this.globalContext.setMode(newMode);

            if (log.isDebugEnabled()) {
                log.debug("RTP session " + this.globalContext.getSsrc() + " updated mode to " + newMode.name());
            }
        }
    }

    @Override
    public void onIncomingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        RtpSessionIncomingRtpContext txContext = (RtpSessionIncomingRtpContext) context;
        RtpSessionStatistics statistics = this.globalContext.getStatistics();
        RtpPacket packet = txContext.getPacket();
        
        // RTP v0 packets are used in some applications. Discarded since we do not handle them.
        int version = packet.getVersion();
        if (version == 0) {

            // TODO update statistics.dropped
            if (log.isDebugEnabled()) {
                log.debug("RTP Channel " + statistics.getSsrc() + " dropped RTP v0 packet.");
            }
            return;
        }
        
        // Check if packet is not empty
        boolean hasData = (packet.getLength() > 0);
        if (!hasData) {
            // TODO update statistics.dropped
            if (log.isDebugEnabled()) {
                log.debug("RTP Channel " + statistics.getSsrc() + " dropped packet because payload was empty.");
            }
            return;
        }

        // Packets are only processed if session is in "receiver" mode
        ConnectionMode mode = this.globalContext.getMode();
        switch (mode) {
            case RECV_ONLY:
            case SEND_RECV:
            case CONFERENCE:
            case NETWORK_LOOPBACK:
                // Confirm codec is supported
                int payloadType = packet.getPayloadType();
                RTPFormat format = this.globalContext.getNegotiatedFormats().getRTPFormat(payloadType);

                if (format == null) {
                    // TODO update statistics.dropped
                    if (log.isDebugEnabled()) {
                        long ssrc = this.globalContext.getSsrc();
                        log.debug("RTP session " + ssrc + " dropped incoming packet because payload type " + payloadType + " is not supported.");
                    }
                } else {
                    // Consume packet
                    if (DtmfFormat.FORMAT.matches(format.getFormat())) {
                        txContext.getDtmfInput().write(packet);
                    } else {
                        txContext.getJitterBuffer().write(packet, format);
                        // Update statistics
                        this.globalContext.getStatistics().incomingRtp(packet);
                    }
                }

                if (ConnectionMode.NETWORK_LOOPBACK.equals(mode)) {
                    // TODO Loop packet back to remote peer
                }
                break;

            default:
                if (log.isTraceEnabled()) {
                    long ssrc = this.globalContext.getSsrc();
                    log.trace("RTP session " + ssrc + " dropped incoming packet because connection mode is " + mode);
                }

                // TODO update statistics.dropped
                break;
        }

    }

    @Override
    public void onOutgoingRtp(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        RtpSessionOutgoingRtpContext txContext = (RtpSessionOutgoingRtpContext) context;
        FutureCallback<Void> callback = txContext.getCallback();

        // Packets are only processed if session is in "sender" mode
        ConnectionMode mode = this.globalContext.getMode();

        switch (mode) {
            case SEND_ONLY:
            case SEND_RECV:
            case CONFERENCE:
            case NETWORK_LOOPBACK:
                // Send packet to remote peer
                RtpChannel channel = txContext.getChannel();
                RtpPacket packet = txContext.getPacket();
                channel.send(packet, callback);
                break;

            default:
                // Session mode does not allow to send packets
                long ssrc = this.globalContext.getSsrc();
                RtpSessionUnwritableException exception = new RtpSessionUnwritableException(
                        "RTP session " + ssrc + " cannot send packet because is operating in " + mode.name() + " mode");
                callback.onFailure(exception);
                break;
        }
    }

    @Override
    public void enterDeactivating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        RtpSessionCloseContext txContext = (RtpSessionCloseContext) context;

        // Deactivate RTP components
        txContext.getRtpInput().deactivate();
        txContext.getDtmfInput().deactivate();
        txContext.getRtpOutput().deactivate();
        txContext.getJitterBuffer().restart();
        txContext.getJitterBuffer().forget(txContext.getRtpInput());

        // Update mode to inactive
        this.globalContext.setMode(ConnectionMode.INACTIVE);

        // Move to next state
        fire(RtpSessionEvent.DEACTIVATED, context);
    }

    @Override
    public void enterDeallocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        RtpSessionCloseContext txContext = (RtpSessionCloseContext) context;

        // Close channel
        long ssrc = this.globalContext.getSsrc();
        RtpChannel channel = txContext.getChannel();
        RtpSessionCloseCallback closeCallback = new RtpSessionCloseCallback(ssrc, this, txContext);

        channel.close(closeCallback);
    }

    @Override
    public void enterDeallocated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        // move to final state
        fire(RtpSessionEvent.CLOSED, context);
    }

    @Override
    public void enterClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
            RtpSessionTransactionContext context) {
        if (log.isDebugEnabled()) {
            long ssrc = this.globalContext.getSsrc();
            log.debug("RTP session " + ssrc + " is closed");
        }
    }

}
