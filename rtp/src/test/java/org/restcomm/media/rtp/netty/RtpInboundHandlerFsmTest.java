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

package org.restcomm.media.rtp.netty;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Test;
import org.restcomm.media.core.sdp.format.AVProfile;
import org.restcomm.media.core.sdp.format.RTPFormat;
import org.restcomm.media.rtp.RTPInput;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandlerFsmTest {

    private RtpInboundHandlerFsm fsm;

    @After
    public void after() {
        if (this.fsm != null) {
            if (this.fsm.isStarted()) {
                this.fsm.terminate();
            }
            this.fsm = null;
        }
    }

    @Test
    public void testActivateThenDeactivate() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput);
        this.fsm = RtpInboundHandlerFsmBuilder.INSTANCE.build(context);

        // when
        fsm.start();

        // then
        verify(rtpInput).activate();
        verify(dtmfInput).activate();

        // when
        fsm.fire(RtpInboundHandlerEvent.DEACTIVATE);

        // then
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
    }
    
    @Test
    public void testProcessIncomingAudioPacket() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput);
        this.fsm = RtpInboundHandlerFsmBuilder.INSTANCE.build(context);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(0);

        // when
        fsm.start();
        context.setFormats(AVProfile.audio);
        context.setReceivable(true);
        context.setLoopable(false);
        fsm.fireImmediate(RtpInboundHandlerEvent.PACKET_RECEIVED, new RtpInboundHandlerPacketReceivedContext(packet));

        // then
        verify(statistics).onRtpReceive(packet);
        verify(jitterBuffer).write(packet, AVProfile.audio.find(0));
    }

    @Test
    public void testProcessIncomingDtmfPacket() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput);
        this.fsm = RtpInboundHandlerFsmBuilder.INSTANCE.build(context);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(101);
        
        // when
        fsm.start();
        context.setFormats(AVProfile.audio);
        context.setReceivable(true);
        context.setLoopable(false);
        fsm.fireImmediate(RtpInboundHandlerEvent.PACKET_RECEIVED, new RtpInboundHandlerPacketReceivedContext(packet));
        
        // then
        verify(statistics).onRtpReceive(packet);
        verify(dtmfInput).write(packet);
    }

    @Test
    public void testDropIncomingPacketWithUnknowPayload() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput);
        this.fsm = RtpInboundHandlerFsmBuilder.INSTANCE.build(context);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(999);
        
        // when
        fsm.start();
        context.setFormats(AVProfile.audio);
        context.setReceivable(true);
        context.setLoopable(false);
        fsm.fireImmediate(RtpInboundHandlerEvent.PACKET_RECEIVED, new RtpInboundHandlerPacketReceivedContext(packet));
        
        // then
        verify(statistics, never()).onRtpReceive(packet);
        verify(jitterBuffer, never()).write(eq(packet), any(RTPFormat.class));
    }

}
