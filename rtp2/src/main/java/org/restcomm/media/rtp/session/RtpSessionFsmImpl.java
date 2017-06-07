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
import org.restcomm.media.rtp.RtpSessionContext;

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
        
        if(log.isDebugEnabled()) {
            long ssrc = this.globalContext.getSsrc();
            log.debug("RTP Channel " + ssrc + " is bound to " + localAddress);
        }
        
        // Move on to OPEN state 
        fire(RtpSessionEvent.OPENED);
    }

    @Override
    public void enterConnecting(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        RtpSessionConnectContext txContext = (RtpSessionConnectContext) context;
        SocketAddress address = txContext.getAddress();
        RtpChannel channel = txContext.getChannel();
        RtpSessionConnectCallback callback = new RtpSessionConnectCallback(this);
        channel.connect(address, callback);
    }

//    @Override
//    public void onModeUpdate(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
//        RtpSessionModeUpdateContext txContext = (RtpSessionModeUpdateContext) context;
//        ConnectionMode mode = txContext.getMode();
//        this.globalContext.setMode(mode);
//    }
//
//    @Override
//    public void onIncomingPacket(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
//        RtpSessionIncomingPacketContext txContext = (RtpSessionIncomingPacketContext) context;
//        RtpPacket packet = txContext.getPacket();
//        int payloadType = packet.getPayloadType();
//        ConnectionMode mode = this.globalContext.getMode();
//
//        switch (mode) {
//            case RECV_ONLY:
//            case SEND_RECV:
//            case CONFERENCE:
//            case NETWORK_LOOPBACK:
//                // Confirm codec is supported
//                RTPFormat format = this.globalContext.getNegotiatedFormats().find(payloadType);
//
//                if (format != null) {
//                    // Deliver packet to jitter buffer
//                    this.jitterBuffer.write(packet, format);
//                    // Update statistics
//                    this.globalContext.getStatistics().incomingRtp(packet);
//                } else {
//                    if (log.isDebugEnabled()) {
//                        long ssrc = this.globalContext.getSsrc();
//                        log.debug("RTP Session " + ssrc + " dropped incoming packet because payload type " + payloadType
//                                + " is not supported. Packet details: " + packet.toString());
//                    }
//                    // TODO update statistics.dropped
//                }
//
//                // Send packet back to network if session is in LOOPBACK mode
//                if (ConnectionMode.NETWORK_LOOPBACK.equals(mode)) {
//                    RtpSessionOutgoingPacketContext newTxContext = new RtpSessionOutgoingPacketContext(packet);
//                    fire(RtpSessionEvent.OUTGOING_PACKET, newTxContext);
//                }
//                break;
//
//            default:
//                if (log.isDebugEnabled()) {
//                    long ssrc = this.globalContext.getSsrc();
//                    log.debug("RTP Session " + ssrc + " dropped incoming packet because connection mode is " + mode + ". Packet details:"
//                            + packet.toString());
//                }
//                
//                // TODO update statistics.dropped
//                break;
//        }
//    }
//
//    @Override
//    public void onOutgoingPacket(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
//        RtpSessionOutgoingPacketContext txContext = (RtpSessionOutgoingPacketContext) context;
//        RtpPacket packet = txContext.getPacket();
//        ConnectionMode mode = this.globalContext.getMode();
//
//        switch (mode) {
//            case SEND_ONLY:
//            case SEND_RECV:
//            case CONFERENCE:
//            case NETWORK_LOOPBACK:
//                RtpSessionOutgoingPacketCallback callback = new RtpSessionOutgoingPacketCallback(packet, this.globalContext);
//                this.channel.send(packet, callback);
//                break;
//
//            default:
//                if (log.isDebugEnabled()) {
//                    long ssrc = this.globalContext.getSsrc();
//                    log.debug("RTP Session " + ssrc + " dropped outgoing packet because connection mode is " + mode + ". Packet details:"
//                            + packet.toString());
//                }
//                break;
//        }
//    }

    @Override
    public void enterNegotiating(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        RtpSessionNegotiateContext txContext = (RtpSessionNegotiateContext) context;
        // TODO negotiate session
    }

    @Override
    public void enterClosed(RtpSessionState from, RtpSessionState to, RtpSessionEvent event, RtpSessionTransactionContext context) {
        RtpSessionCloseContext txContext = (RtpSessionCloseContext) context;
        RtpChannel channel = txContext.getChannel();
        RtpSessionCloseCallback callback = new RtpSessionCloseCallback();
        channel.close(callback);
    }

}
