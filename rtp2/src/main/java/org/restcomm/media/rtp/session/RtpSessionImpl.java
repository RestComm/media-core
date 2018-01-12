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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.rtp.*;
import org.restcomm.media.rtp.handler.RtpDemultiplexer;
import org.restcomm.media.rtp.handler.RtpInboundHandler;
import org.restcomm.media.rtp.handler.RtpPacketFilter;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.attributes.SsrcAttribute;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class RtpSessionImpl implements RtpSession {

    private static final Logger log = LogManager.getLogger(RtpSessionImpl.class);

    // RTP Session
    private final RtpSessionContext context;
    private final RtpSessionFsm fsm;
    private final RtpChannelInitializer channelInitializer;

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
        this.channelInitializer = new RtpChannelInitializer(new RtpDemultiplexer(), new RtpPacketFilter(), new RtpInboundHandler(this));

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
    public SocketAddress getRtpAddress() {
        return this.context.getLocalAddress();
    }

    @Override
    public RTPFormats getFormats() {
        return (this.context.getRemoteAddress() == null) ? this.context.getSupportedFormats() : this.context.getNegotiatedFormats();
    }

    @Override
    public ConnectionMode getMode() {
        return this.context.getMode();
    }

    @Override
    public void open(SocketAddress address, FutureCallback<Void> callback) {
        // Register FSM listener for operation feedback
        RtpSessionOpenListener openListener = new RtpSessionOpenListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(openListener);

        // Fire event
        RtpSessionOpenContext txContext = new RtpSessionOpenContext(this.channel, this.channelInitializer, address, callback);
        this.fsm.fire(RtpSessionEvent.OPEN, txContext);
    }

    @Override
    public void negotiate(MediaDescriptionField sdp, FutureCallback<Void> callback) {
        // Gather remote session information
        final String[] payloadTypes = sdp.getPayloadTypes();
        final RTPFormats offeredFormats = new RTPFormats();

        for (String payloadType : payloadTypes) {
            RTPFormat format;
            try {
                final int payloadTypeInt = Integer.parseInt(payloadType);

                if(payloadTypeInt < AVProfile.DYNAMIC_PT_MIN || payloadTypeInt > AVProfile.DYNAMIC_PT_MAX) {
                    // static payload type
                    format = AVProfile.getFormat(payloadTypeInt, AVProfile.AUDIO);
                } else {
                    // dynamic payload type
                    final RtpMapAttribute codecSdp = sdp.getFormat(payloadTypeInt);
                    final String codecName = codecSdp.getCodec();
                    final RTPFormat staticFormat = AVProfile.getFormat(codecName);

                    // Check if code is supported
                    final boolean supported = staticFormat != null && staticFormat.getClockRate() == codecSdp.getClockRate();
                    if(supported) {
                        format = new RTPFormat(payloadTypeInt, staticFormat.getFormat(), staticFormat.getClockRate());
                    } else {
                        format = null;
                    }
                }
            } catch (NumberFormatException e) {
                format = null;
            }

            if(format != null) {
                offeredFormats.add(format);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(this.context.getMediaType() + " channel " + this.context.getSsrc() + " dropped unsupported RTP payload type " + payloadType);
                }
            }
        }

        SocketAddress address = new InetSocketAddress(sdp.getConnection().getAddress(), sdp.getPort());
        final SsrcAttribute ssrcAttribute = sdp.getSsrc();
        long ssrc = (ssrcAttribute == null || ssrcAttribute.getSsrcId().isEmpty()) ? 0 : Long.parseLong(ssrcAttribute.getSsrcId());

        // Register FSM listener for operation feedback
        RtpSessionNegotiateListener listener = new RtpSessionNegotiateListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(listener);

        // Fire event
        RtpSessionNegotiateContext txContext = new RtpSessionNegotiateContext(this.channel, offeredFormats, address, ssrc, callback);
        this.fsm.fire(RtpSessionEvent.NEGOTIATE, txContext);
    }

    @Override
    public void close(FutureCallback<Void> callback) {
        // Register FSM listener for operation feedback
        RtpSessionCloseListener listener = new RtpSessionCloseListener(this.fsm, callback);
        this.fsm.addDeclarativeListener(listener);

        // Fire event
        RtpSessionCloseContext txContext = new RtpSessionCloseContext(this.channel, this.jitterBuffer, this.dtmfInput, this.rtpInput, this.rtpOutput, callback);
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
        RtpSessionOutgoingRtpCallback callback = new RtpSessionOutgoingRtpCallback(this.context.getStatistics(), packet);
        RtpSessionOutgoingRtpContext txContext = new RtpSessionOutgoingRtpContext(packet, this.channel, callback);
        this.fsm.fire(RtpSessionEvent.OUTGOING_RTP, txContext);
    }

    @Override
    public boolean isActive() {
        RtpSessionState state = this.fsm.getCurrentState();
        switch (state) {
            case IDLE:
            case CLOSING:
            case DEACTIVATING:
            case DEALLOCATING:
            case DEALLOCATED:
            case CLOSED:
                return true;

            default:
                return false;
        }
    }

    @Override
    public AudioComponent getAudioComponent() {
        // TODO RtpSession.getAudioComponent
        return null;
    }

    @Override
    public OOBComponent getOOBComponent() {
        // TODO RtpSession.getOOBComponent
        return null;
    }
}
