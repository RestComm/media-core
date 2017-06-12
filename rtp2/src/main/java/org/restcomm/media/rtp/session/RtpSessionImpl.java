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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;
import org.restcomm.media.spi.format.EncodingName;
import org.restcomm.media.spi.format.Format;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionImpl implements RtpSession {

    // RTP Session
    private final RtpSessionContext context;
    private final RtpSessionFsm fsm;

    // RTP Components
    private final RtpChannel channel;
    private final JitterBuffer jitterBuffer;
    private final DtmfInput dtmfInput;
    private final RtpInput rtpInput;
    private final RtpOutput rtpOutput;

    public RtpSessionImpl(RtpChannel channel, RtpSessionContext context, JitterBuffer jitterBuffer, RtpInput rtpInput, DtmfInput dtmfInput, RtpOutput rtpOutput) {
        // RTP Session
        this.context = context;
        this.fsm = RtpSessionFsmBuilder.INSTANCE.build(this.context);
        
        // RTP Components
        this.channel = channel;
        this.jitterBuffer = jitterBuffer;
        this.dtmfInput = dtmfInput;
        this.rtpInput = rtpInput;
        this.rtpOutput = rtpOutput;
    }

    @Override
    public long getSsrc() {
        return this.context.getSsrc();
    }

    @Override
    public MediaType getMediaType() {
        return this.context.getMediaType();
    }

    @Override
    public void open(SocketAddress address, FutureCallback<Void> callback) {
        // Register FSM listener for operation feedback
        RtpSessionOpenListener openListener = new RtpSessionOpenListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(openListener);

        // Fire event
        RtpSessionOpenContext txContext = new RtpSessionOpenContext(this.channel, address, callback);
        this.fsm.fire(RtpSessionEvent.OPEN, txContext);
    }

    @Override
    public void negotiate(MediaDescriptionField sdp, FutureCallback<Void> callback) {
        // Gather remote session information
        RtpMapAttribute[] formats = sdp.getFormats();
        SocketAddress address = new InetSocketAddress(sdp.getConnection().getAddress(), sdp.getPort());
        String ssrcId = sdp.getSsrc().getSsrcId();
        long ssrc = ssrcId.isEmpty() ? 0 : Long.parseLong(ssrcId);

        // Register FSM listener for operation feedback
        RtpSessionNegotiateListener listener = new RtpSessionNegotiateListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(listener);

        // Fire event
        RtpSessionNegotiateContext txContext = new RtpSessionNegotiateContext(this.channel, getFormats(formats), address, ssrc, callback);
        this.fsm.fire(RtpSessionEvent.NEGOTIATE, txContext);
    }

    private RTPFormats getFormats(RtpMapAttribute[] map) {
        RTPFormats offeredFormats = new RTPFormats(map.length);
        for (RtpMapAttribute format : map) {
            String codec = format.getCodec();
            int clockRate = format.getClockRate();
            int payloadType = format.getPayloadType();
            RTPFormat rtpFormat = new RTPFormat(payloadType, new Format(new EncodingName(codec)), clockRate);
            offeredFormats.add(rtpFormat);
        }
        return offeredFormats;
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // Register FSM listener for operation feedback
        RtpSessionCloseListener listener = new RtpSessionCloseListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(listener);
        
        // Fire event
        RtpSessionCloseContext txContext = new RtpSessionCloseContext(this.channel, callback);
        this.fsm.fire(RtpSessionEvent.CLOSE, txContext);
    }

    @Override
    public void updateMode(ConnectionMode mode, FutureCallback<Void> callback) {
        // Register FSM listener for operation feedback
        RtpSessionUpdateModeListener listener = new RtpSessionUpdateModeListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(listener);

        // Fire event
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, this.jitterBuffer, this.dtmfInput, this.rtpInput, this.rtpOutput, callback);
        this.fsm.fire(RtpSessionEvent.UPDATE_MODE, txContext);
    }

    @Override
    public void incomingRtp(RtpPacket packet) {
        // Fire event
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, this.jitterBuffer, this.dtmfInput);
        this.fsm.fire(RtpSessionEvent.INCOMING_RTP, txContext);
    }

    @Override
    public void outgoingRtp(RtpPacket packet) {
        // Fire event
        RtpSessionOutgoingRtpContext txContext = new RtpSessionOutgoingRtpContext(packet, this.channel);
        this.fsm.fire(RtpSessionEvent.OUTGOING_RTP, txContext);
    }

}
