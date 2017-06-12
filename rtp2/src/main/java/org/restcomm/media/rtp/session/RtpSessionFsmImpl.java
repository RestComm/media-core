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

import java.net.SocketAddress;

import org.apache.log4j.Logger;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.session.RtpSessionFsmImplTest;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionFsmImpl extends AbstractRtpSessionFsm {

    private static final Logger log = Logger.getLogger(RtpSessionFsmImplTest.class);

    private final RtpSessionContext globalContext;

    public RtpSessionFsmImpl(RtpSessionContext globalContext) {
        this.globalContext = globalContext;
    }

    /*
     * FSM
     */
    @Override
    public void enterAllocating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final RtpChannel channel = txContext.getChannel();

        // Open channel
        RtpSessionAllocateCallback callback = new RtpSessionAllocateCallback(this, txContext);
        channel.open(callback);
    }

    @Override
    public void enterBinding(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final SocketAddress address = txContext.getAddress();
        final RtpChannel channel = txContext.getChannel();

        // Ask RTP channel to bind to requested address
        RtpSessionBindCallback callback = new RtpSessionBindCallback(this, context);
        channel.bind(address, callback);
    }

    @Override
    public void enterOpened(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        // RTP channel was bound successfully
        final RtpSessionOpenContext txContext = (RtpSessionOpenContext) context;
        final SocketAddress localAddress = txContext.getAddress();

        // Update context with local address
        this.globalContext.setLocalAddress(localAddress);

        if (log.isDebugEnabled()) {
            long ssrc = this.globalContext.getSsrc();
            log.debug("RTP session " + ssrc + " is bound to " + localAddress.toString());
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
            // TODO close session
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
    public void enterConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        final RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        final SocketAddress address = txContext.getAddress();
        final RtpChannel channel = txContext.getChannel();

        RtpSessionConnectCallback callback = new RtpSessionConnectCallback(this, txContext);
        channel.connect(address, callback);
    }

    @Override
    public void enterNegotiated(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        final RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        final SocketAddress remoteAddress = txContext.getAddress();

        // Update context with remote peer address
        this.globalContext.setRemoteAddress(remoteAddress);

        if (log.isDebugEnabled()) {
            if (log.isDebugEnabled()) {
                long ssrc = this.globalContext.getSsrc();
                log.debug("RTP session " + ssrc + " is connected to " + remoteAddress.toString());
            }
        }

        // Move on to next state
        fire(RtpSessionEvent.NEGOTIATED);
    }

     @Override
     public void onUpdateMode(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
         RtpSessionFsmUpdateModeContext txContext = (RtpSessionFsmUpdateModeContext) context;
         
         ConnectionMode currentMode = this.globalContext.getMode();
         ConnectionMode newMode = txContext.getMode();
         
         if(!currentMode.equals(newMode)) {
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
             
             if(log.isDebugEnabled()) {
                 log.debug("RTP session " + this.globalContext.getSsrc() + " updated mode to " + newMode.name());
             }
         }
     }
    
    // @Override
    // public void onIncomingPacket(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
    // RtpSessionTransactionContext context) {
    // RtpSessionIncomingPacketContext txContext = (RtpSessionIncomingPacketContext) context;
    // RtpPacket packet = txContext.getPacket();
    // int payloadType = packet.getPayloadType();
    // ConnectionMode mode = this.globalContext.getMode();
    //
    // switch (mode) {
    // case RECV_ONLY:
    // case SEND_RECV:
    // case CONFERENCE:
    // case NETWORK_LOOPBACK:
    // // Confirm codec is supported
    // RTPFormat format = this.globalContext.getNegotiatedFormats().find(payloadType);
    //
    // if (format != null) {
    // // Deliver packet to jitter buffer
    // this.jitterBuffer.write(packet, format);
    // // Update statistics
    // this.globalContext.getStatistics().incomingRtp(packet);
    // } else {
    // if (log.isDebugEnabled()) {
    // long ssrc = this.globalContext.getSsrc();
    // log.debug("RTP Session " + ssrc + " dropped incoming packet because payload type " + payloadType
    // + " is not supported. Packet details: " + packet.toString());
    // }
    // // TODO update statistics.dropped
    // }
    //
    // // Send packet back to network if session is in LOOPBACK mode
    // if (ConnectionMode.NETWORK_LOOPBACK.equals(mode)) {
    // RtpSessionOutgoingPacketContext newTxContext = new RtpSessionOutgoingPacketContext(packet);
    // fire(RtpSessionEvent.OUTGOING_PACKET, newTxContext);
    // }
    // break;
    //
    // default:
    // if (log.isDebugEnabled()) {
    // long ssrc = this.globalContext.getSsrc();
    // log.debug("RTP Session " + ssrc + " dropped incoming packet because connection mode is " + mode + ". Packet details:"
    // + packet.toString());
    // }
    //
    // // TODO update statistics.dropped
    // break;
    // }
    // }
    //
    // @Override
    // public void onOutgoingPacket(RtpSessionState from, RtpSessionState to, RtpSessionEvent event,
    // RtpSessionTransactionContext context) {
    // RtpSessionOutgoingPacketContext txContext = (RtpSessionOutgoingPacketContext) context;
    // RtpPacket packet = txContext.getPacket();
    // ConnectionMode mode = this.globalContext.getMode();
    //
    // switch (mode) {
    // case SEND_ONLY:
    // case SEND_RECV:
    // case CONFERENCE:
    // case NETWORK_LOOPBACK:
    // RtpSessionOutgoingPacketCallback callback = new RtpSessionOutgoingPacketCallback(packet, this.globalContext);
    // this.channel.send(packet, callback);
    // break;
    //
    // default:
    // if (log.isDebugEnabled()) {
    // long ssrc = this.globalContext.getSsrc();
    // log.debug("RTP Session " + ssrc + " dropped outgoing packet because connection mode is " + mode + ". Packet details:"
    // + packet.toString());
    // }
    // break;
    // }
    // }

    @Override
    public void enterClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        RtpSessionCloseContext txContext = (RtpSessionCloseContext) context;
        RtpChannel channel = txContext.getChannel();
        RtpSessionCloseCallback callback = new RtpSessionCloseCallback();
        channel.close(callback);
    }

}
