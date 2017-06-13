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

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpChannel;
import org.restcomm.media.rtp.RtpInput;
import org.restcomm.media.rtp.RtpOutput;
import org.restcomm.media.rtp.RtpPacket;
import org.restcomm.media.rtp.jitter.JitterBuffer;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.rtp.session.exception.RtpSessionException;
import org.restcomm.media.rtp.session.exception.RtpSessionNegotiationException;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.attributes.SsrcAttribute;
import org.restcomm.media.sdp.fields.ConnectionField;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionImplTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testOpen() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        // when
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        FutureCallback<Void> callback = mock(FutureCallback.class);
        session.open(address, callback);

        // then
        verify(callback, timeout(10)).onSuccess(null);
        verify(channel).open(any(FutureCallback.class));
        verify(channel).bind(eq(address), any(FutureCallback.class));
        assertEquals(ConnectionMode.INACTIVE, context.getMode());
        assertEquals(address, context.getLocalAddress());
        assertNull(context.getRemoteAddress());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateMode() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        // when
        InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        session.open(address, mock(FutureCallback.class));

        FutureCallback<Void> modeCallback = mock(FutureCallback.class);
        ConnectionMode mode = ConnectionMode.SEND_RECV;
        session.updateMode(mode, modeCallback);

        // then
        verify(modeCallback, timeout(10)).onSuccess(null);
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        assertEquals(mode, context.getMode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testUpdateModeWhenSessionIsIdle() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        // when
        FutureCallback<Void> modeCallback = mock(FutureCallback.class);
        ConnectionMode mode = ConnectionMode.SEND_RECV;
        session.updateMode(mode, modeCallback);

        // then
        verify(modeCallback, timeout(10)).onFailure(any(Throwable.class));
        verify(rtpInput, never()).activate();
        verify(dtmfInput, never()).activate();
        verify(rtpOutput, never()).activate();
        assertEquals(ConnectionMode.INACTIVE, context.getMode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNegotiateSession() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));

        FutureCallback<Void> callback = mock(FutureCallback.class);
        session.negotiate(remoteSdp, callback);

        // then
        verify(callback, timeout(10)).onSuccess(null);
        verify(channel).connect(eq(remoteAddress), any(FutureCallback.class));
        assertEquals(remoteAddress, context.getRemoteAddress());
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertEquals(2, negotiatedFormats.size());
        assertNotNull(negotiatedFormats.getRTPFormat(pcmu.getPayloadType()));
        assertNotNull(negotiatedFormats.getRTPFormat(telephoneEvent.getPayloadType()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNegotiateSessionWhenSessionIsIdle() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        // when
        FutureCallback<Void> callback = mock(FutureCallback.class);
        session.negotiate(remoteSdp, callback);

        // then
        verify(callback, timeout(10)).onFailure(any(RtpSessionException.class));
        verify(channel, never()).connect(eq(remoteAddress), any(FutureCallback.class));
        assertNull(context.getRemoteAddress());
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertTrue(negotiatedFormats.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testNegotiateSessionWithUnsupportedFormats() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute unknownFormat = new RtpMapAttribute();
        unknownFormat.setClockRate(8000);
        unknownFormat.setCodec("xyz");
        unknownFormat.setPayloadType(999);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { unknownFormat };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));

        FutureCallback<Void> callback = mock(FutureCallback.class);
        session.negotiate(remoteSdp, callback);

        // then
        verify(callback, timeout(10)).onFailure(any(RtpSessionNegotiationException.class));
        verify(channel, never()).connect(eq(remoteAddress), any(FutureCallback.class));
        assertNull(context.getRemoteAddress());
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertTrue(negotiatedFormats.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIncomingRtp() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));
        session.negotiate(remoteSdp, mock(FutureCallback.class));

        RtpPacket packet = new RtpPacket(true, pcmu.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.incomingRtp(packet);

        // then
        verify(jitterBuffer).write(eq(packet), any(RTPFormat.class));
        verify(dtmfInput, never()).write(packet);
        verify(statistics).incomingRtp(packet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIncomingDtmf() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));
        session.negotiate(remoteSdp, mock(FutureCallback.class));

        RtpPacket packet = new RtpPacket(true, telephoneEvent.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.incomingRtp(packet);

        // then
        verify(jitterBuffer, never()).write(eq(packet), any(RTPFormat.class));
        verify(dtmfInput).write(packet);
        verify(statistics, never()).incomingRtp(packet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testIncomingRtpWhenSessionNotNegotiated() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);
        
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };
        
        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).open(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));
        
        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));
        
        RtpPacket packet = new RtpPacket(true, telephoneEvent.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.incomingRtp(packet);
        
        // then
        verify(jitterBuffer, never()).write(eq(packet), any(RTPFormat.class));
        verify(dtmfInput, never()).write(packet);
        verify(statistics, never()).incomingRtp(packet);
        // TODO check dropped packets
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testIncomingRtpWhenSessionModeIsSendOnly() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_ONLY, mock(FutureCallback.class));
        session.negotiate(remoteSdp, mock(FutureCallback.class));

        RtpPacket packet = new RtpPacket(true, pcmu.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.incomingRtp(packet);

        // then
        verify(jitterBuffer, never()).write(eq(packet), any(RTPFormat.class));
        verify(dtmfInput, never()).write(packet);
        verify(statistics, never()).incomingRtp(packet);
        // TODO check dropped packets
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOutgoingRtp() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).send(any(RtpPacket.class), any(FutureCallback.class));
        

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));
        session.negotiate(remoteSdp, mock(FutureCallback.class));

        RtpPacket packet = new RtpPacket(true, pcmu.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.outgoingRtp(packet);

        // then
        verify(channel).send(eq(packet), any(FutureCallback.class));
        verify(statistics).outgoingRtp(packet);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testOutgoingRtpWhenSessionIsNotEstablished() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);
        
        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };
        
        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).open(any(FutureCallback.class));
        
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));
        
        
        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.SEND_RECV, mock(FutureCallback.class));
        
        RtpPacket packet = new RtpPacket(true, pcmu.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.outgoingRtp(packet);
        
        // then
        verify(channel, never()).send(eq(packet), any(FutureCallback.class));
        verify(statistics, never()).outgoingRtp(packet);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testOutgoingRtpWhenSessionModeIsReceiveOnly() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RTPFormats formats = AVProfile.audio;
        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionImpl session = new RtpSessionImpl(channel, context, jitterBuffer, rtpInput, dtmfInput, rtpOutput);

        InetSocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        MediaDescriptionField remoteSdp = mock(MediaDescriptionField.class);
        ConnectionField remoteConnection = mock(ConnectionField.class);
        SsrcAttribute ssrcAttribute = new SsrcAttribute("54321");
        RtpMapAttribute pcmu = new RtpMapAttribute();
        pcmu.setClockRate(8000);
        pcmu.setCodec("pcmu");
        pcmu.setPayloadType(0);
        RtpMapAttribute telephoneEvent = new RtpMapAttribute();
        telephoneEvent.setCodec("telephone-event");
        telephoneEvent.setPayloadType(101);
        RtpMapAttribute[] offeredFormats = new RtpMapAttribute[] { pcmu, telephoneEvent };

        when(remoteSdp.getSsrc()).thenReturn(ssrcAttribute);
        when(remoteSdp.getPort()).thenReturn(remoteAddress.getPort());
        when(remoteSdp.getConnection()).thenReturn(remoteConnection);
        when(remoteConnection.getAddress()).thenReturn(remoteAddress.getHostString());
        when(remoteSdp.getFormats()).thenReturn(offeredFormats);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).bind(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).send(any(RtpPacket.class), any(FutureCallback.class));
        

        // when
        session.open(new InetSocketAddress("127.0.0.1", 6000), mock(FutureCallback.class));
        session.updateMode(ConnectionMode.RECV_ONLY, mock(FutureCallback.class));
        session.negotiate(remoteSdp, mock(FutureCallback.class));

        RtpPacket packet = new RtpPacket(true, pcmu.getPayloadType(), 100, 160 * 1, ssrc, new byte[160]);
        session.outgoingRtp(packet);

        // then
        verify(channel, never()).send(eq(packet), any(FutureCallback.class));
        verify(statistics, never()).outgoingRtp(packet);
    }
    
}
