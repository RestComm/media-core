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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
import org.junit.Test;
import org.restcomm.media.core.spi.ConnectionMode;
import org.restcomm.media.rtp.RTPInput;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.statistics.RtpStatistics;
import org.restcomm.media.scheduler.Clock;
import org.restcomm.media.sdp.format.AVProfile;

import io.netty.channel.embedded.EmbeddedChannel;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpInboundHandlerTest {
    
    private RtpInboundHandler handler;
    
    @After
    public void after() {
        if(this.handler != null) {
            this.handler.deactivate();
            this.handler = null;
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
        this.handler = new RtpInboundHandler(context);
        
        // when
        handler.activate();
        
        // then
        verify(rtpInput, times(1)).activate();
        verify(dtmfInput, times(1)).activate();
        
        // then
        handler.deactivate();
        
        // then
        verify(rtpInput, times(1)).deactivate();
        verify(dtmfInput, times(1)).deactivate();
    }

    @Test
    public void testUpdateMode() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.RECV_ONLY);
        
        // then
        verify(context).setLoopable(false);
        verify(context).setReceivable(true);
    }

    @Test
    public void testReadAudioPacketWhenChannelIsReceivable() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        final EmbeddedChannel channel = new EmbeddedChannel(this.handler);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(0);
        when(packet.getLength()).thenReturn(256);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.RECV_ONLY);
        handler.setFormatMap(AVProfile.audio);
        channel.writeInbound(packet);
        
        // then
        verify(jitterBuffer).write(packet, AVProfile.audio.find(0));
        verify(dtmfInput, never()).write(packet);
        verify(statistics).onRtpReceive(packet);
    }

    @Test
    public void testReadDtmfPacketWhenChannelIsReceivable() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        final EmbeddedChannel channel = new EmbeddedChannel(this.handler);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(101);
        when(packet.getLength()).thenReturn(256);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.RECV_ONLY);
        handler.setFormatMap(AVProfile.audio);
        channel.writeInbound(packet);
        
        // then
        verify(dtmfInput).write(packet);
        verify(jitterBuffer, never()).write(packet, AVProfile.audio.find(101));
        verify(statistics).onRtpReceive(packet);
    }

    @Test
    public void testReadPacketWhenChannelIsInactive() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        final EmbeddedChannel channel = new EmbeddedChannel(this.handler);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(0);
        when(packet.getLength()).thenReturn(256);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.INACTIVE);
        handler.setFormatMap(AVProfile.audio);
        channel.writeInbound(packet);
        
        // then
        verify(dtmfInput, never()).write(packet);
        verify(jitterBuffer, never()).write(packet, AVProfile.audio.find(0));
        verify(statistics, never()).onRtpReceive(packet);
    }

    @Test
    public void testReadEmptyPacket() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        final EmbeddedChannel channel = new EmbeddedChannel(this.handler);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(RtpPacket.VERSION);
        when(packet.getPayloadType()).thenReturn(0);
        when(packet.getLength()).thenReturn(0);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.RECV_ONLY);
        handler.setFormatMap(AVProfile.audio);
        channel.writeInbound(packet);
        
        // then
        verify(dtmfInput, never()).write(packet);
        verify(jitterBuffer, never()).write(packet, AVProfile.audio.find(0));
        verify(statistics, never()).onRtpReceive(packet);
    }

    @Test
    public void testReadPacketWithOldVersion() {
        // given
        final Clock clock = mock(Clock.class);
        final RtpStatistics statistics = mock(RtpStatistics.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RTPInput rtpInput = mock(RTPInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInboundHandlerGlobalContext context = spy(new RtpInboundHandlerGlobalContext(clock, statistics, jitterBuffer, rtpInput, dtmfInput));
        this.handler = new RtpInboundHandler(context);
        final EmbeddedChannel channel = new EmbeddedChannel(this.handler);
        
        final RtpPacket packet = mock(RtpPacket.class);
        when(packet.getVersion()).thenReturn(0);
        when(packet.getPayloadType()).thenReturn(0);
        when(packet.getLength()).thenReturn(256);
        
        // when
        handler.activate();
        handler.updateMode(ConnectionMode.RECV_ONLY);
        handler.setFormatMap(AVProfile.audio);
        channel.writeInbound(packet);
        
        // then
        verify(dtmfInput, never()).write(packet);
        verify(jitterBuffer, never()).write(packet, AVProfile.audio.find(0));
        verify(statistics, never()).onRtpReceive(packet);
    }

}
