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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
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
import org.restcomm.media.rtp.session.RtpSessionCloseContext;
import org.restcomm.media.rtp.session.RtpSessionContext;
import org.restcomm.media.rtp.session.RtpSessionEvent;
import org.restcomm.media.rtp.session.RtpSessionFsm;
import org.restcomm.media.rtp.session.RtpSessionFsmBuilder;
import org.restcomm.media.rtp.session.RtpSessionFsmImpl;
import org.restcomm.media.rtp.session.RtpSessionNegotiateContext;
import org.restcomm.media.rtp.session.RtpSessionOpenContext;
import org.restcomm.media.rtp.session.RtpSessionState;
import org.restcomm.media.rtp.session.RtpSessionTransactionContext;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpSessionFsmImplTest {

    private RtpSessionFsm fsm;

    @After
    public void after() {
        if (fsm != null) {
            if (fsm.isStarted()) {
                fsm.terminate();
            }
            fsm = null;
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testAllocatingState() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);

        // when
        SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        RtpSessionOpenContext bindContext = new RtpSessionOpenContext(channel, address, null);
        fsm.enterBinding(RtpSessionState.OPENING, RtpSessionState.ALLOCATING, RtpSessionEvent.OPEN, bindContext);

        // then
        verify(channel).bind(eq(address), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testBindingState() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);

        // when
        SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        RtpSessionOpenContext bindContext = new RtpSessionOpenContext(channel, address, null);
        fsm.enterBinding(RtpSessionState.ALLOCATING, RtpSessionState.BINDING, RtpSessionEvent.ALLOCATED, bindContext);

        // then
        verify(channel).bind(eq(address), any(FutureCallback.class));
    }

    @Test
    public void testNegotiatingFormatsAction() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final RTPFormats offeredFormats = new RTPFormats(2);
        offeredFormats.add(AVProfile.audio.find(8));
        offeredFormats.add(AVProfile.audio.find(101));
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        final long remoteSsrc = 54321L;

        doNothing().when(fsm).fire(any(RtpSessionEvent.class), any(RtpSessionTransactionContext.class));
        
        // when
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress, remoteSsrc, null);
        fsm.enterNegotiatingFormats(RtpSessionState.NEGOTIATING, RtpSessionState.NEGOTIATING_FORMATS, RtpSessionEvent.NEGOTIATE, negotiateContext);
        
        // then
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertEquals(2, negotiatedFormats.size());
        assertNotNull(negotiatedFormats.getRTPFormat(8));
        assertNotNull(negotiatedFormats.getRTPFormat(101));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToInactive() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.INACTIVE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer).restart();
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).deactivate();
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToSendOnly() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer).restart();
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).activate();
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToReceiveOnly() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.RECV_ONLY;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput, never()).activate();
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToSendReceive() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToConference() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.CONFERENCE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithSameMode() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.CONFERENCE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);
        
        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);
        
        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);
        
        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput, never()).activate();
        verify(dtmfInput, never()).activate();
        verify(rtpOutput, never()).activate();
        verify(context, never()).setMode(mode);
    }

    @Test
    public void testIncomingRtpPacketAction() {
        // given
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final int payloadType = 0;
        final RtpPacket packet = mock(RtpPacket.class);
        final RTPFormat format = AVProfile.audio.find(payloadType);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        
        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);
        when(context.getStatistics()).thenReturn(statistics);
        
        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, jitterBuffer, dtmfInput);
        fsm.onIncomingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.INCOMING_RTP, txContext);
        
        // then
        verify(jitterBuffer).write(packet, format);
        verify(dtmfInput, never()).write(packet);
        verify(statistics).incomingRtp(packet);
    }

    @Test
    public void testIncomingDtmfPacketAction() {
        // given
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final int payloadType = 101;
        final RtpPacket packet = mock(RtpPacket.class);
        final RTPFormat format = AVProfile.audio.find(payloadType);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        
        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);
        when(context.getStatistics()).thenReturn(statistics);
        
        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, jitterBuffer, dtmfInput);
        fsm.onIncomingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.INCOMING_RTP, txContext);
        
        // then
        verify(jitterBuffer, never()).write(packet, format);
        verify(dtmfInput).write(packet);
        verify(statistics, never()).incomingRtp(packet);
    }
    
    @Test
    public void testIncomingRtpPacketActionWhenSessionIsInactive() {
        // given
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final int payloadType = 0;
        final RtpPacket packet = mock(RtpPacket.class);
        final RTPFormat format = AVProfile.audio.find(payloadType);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        
        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);
        when(context.getStatistics()).thenReturn(statistics);
        
        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, jitterBuffer, dtmfInput);
        fsm.onIncomingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.INCOMING_RTP, txContext);
        
        // then
        verify(jitterBuffer, never()).write(packet, format);
        verify(dtmfInput, never()).write(packet);
        verify(statistics, never()).incomingRtp(packet);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testOutgoingRtpPacketAction() {
        // given
        final long ssrc = 12345L;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final int payloadType = 0;
        final RtpPacket packet = mock(RtpPacket.class);
        final RtpChannel channel = mock(RtpChannel.class);
        
        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        when(context.getStatistics()).thenReturn(statistics);
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).send(eq(packet), any(FutureCallback.class));
        
        // when
        RtpSessionOutgoingRtpCallback callback = new RtpSessionOutgoingRtpCallback(ssrc, statistics, packet);
        RtpSessionOutgoingRtpContext txContext = new RtpSessionOutgoingRtpContext(packet, channel, callback);
        
        fsm.onOutgoingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.OUTGOING_RTP, txContext);
        
        // then
        verify(channel).send(packet, callback);
        verify(statistics).outgoingRtp(packet);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOutgoingRtpPacketActionWhenChannelIsInactive() {
        // given
        final long ssrc = 12345L;
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));
        
        final int payloadType = 0;
        final RtpPacket packet = mock(RtpPacket.class);
        final RtpChannel channel = mock(RtpChannel.class);
        
        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);
        when(context.getStatistics()).thenReturn(statistics);
        doAnswer(new Answer<Void>() {
            
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
            
        }).when(channel).send(eq(packet), any(FutureCallback.class));
        
        // when
        RtpSessionOutgoingRtpCallback callback = new RtpSessionOutgoingRtpCallback(ssrc, statistics, packet);
        RtpSessionOutgoingRtpContext txContext = new RtpSessionOutgoingRtpContext(packet, channel, callback);
        
        fsm.onOutgoingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.OUTGOING_RTP, txContext);
        
        // then
        verify(channel, never()).send(packet, callback);
        verify(statistics, never()).outgoingRtp(packet);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectingState() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);
        
        final RTPFormats offeredFormats = new RTPFormats(2);
        offeredFormats.add(AVProfile.audio.find(8));
        offeredFormats.add(AVProfile.audio.find(101));
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        final long remoteSsrc = 54321L;
        
        // when
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress, remoteSsrc, null);
        fsm.enterConnecting(RtpSessionState.NEGOTIATING_FORMATS, RtpSessionState.CONNECTING, RtpSessionEvent.NEGOTIATED_FORMATS, negotiateContext);
        
        // then
        verify(channel).connect(eq(remoteAddress), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testClosedState() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);
        
        // when
        RtpSessionCloseContext closeContext = new RtpSessionCloseContext(channel, null);
        fsm.enterClosed(RtpSessionState.ESTABLISHED, RtpSessionState.CLOSED, RtpSessionEvent.CLOSE, closeContext);
        
        // then
        verify(channel).close(any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInboundSessionFlow() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        this.fsm = RtpSessionFsmBuilder.INSTANCE.build(context);
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 6000);
        
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

        }).when(channel).bind(eq(localAddress), any(FutureCallback.class));

        // when
        fsm.start();
        RtpSessionOpenContext openContext = new RtpSessionOpenContext(channel, localAddress, mock(FutureCallback.class));
        fsm.fire(RtpSessionEvent.OPEN, openContext);

        // then
        verify(channel).open(any(FutureCallback.class));
        verify(channel).bind(eq(localAddress), any(FutureCallback.class));
        assertEquals(localAddress, context.getLocalAddress());
        assertEquals(RtpSessionState.OPEN, fsm.getCurrentState());
        
        // given
        final RTPFormats offeredFormats = new RTPFormats(2);
        offeredFormats.add(AVProfile.audio.find(8));
        offeredFormats.add(AVProfile.audio.find(101));
        final SocketAddress remoteAddress = new InetSocketAddress("127.0.0.1", 7000);
        final long remoteSsrc = 54321L;
        
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).connect(eq(remoteAddress), any(FutureCallback.class));
        
        // when
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress, remoteSsrc, null);
        fsm.fire(RtpSessionEvent.NEGOTIATE, negotiateContext);
        
        // then
        verify(channel).connect(eq(remoteAddress), any(FutureCallback.class));
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertEquals(2, negotiatedFormats.size());
        assertNotNull(negotiatedFormats.getRTPFormat(8));
        assertNotNull(negotiatedFormats.getRTPFormat(101));
        assertEquals(remoteAddress, context.getRemoteAddress());
        assertEquals(RtpSessionState.ESTABLISHED, fsm.getCurrentState());
        
        // when
        RtpSessionCloseContext closeContext = new RtpSessionCloseContext(channel, null);
        fsm.fire(RtpSessionEvent.CLOSE, closeContext);
        
        // then
        verify(channel).close(any(FutureCallback.class));
        assertEquals(RtpSessionState.CLOSED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeFlow() {
        // given
        final long ssrc = 12345L;
        final MediaType mediaType = MediaType.AUDIO;
        final WallClock clock = new WallClock();
        final RtpChannel channel = mock(RtpChannel.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        this.fsm = RtpSessionFsmBuilder.INSTANCE.build(context);
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 6000);
        
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        
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
            
        }).when(channel).bind(eq(localAddress), any(FutureCallback.class));
        
        // when
        fsm.start();
        RtpSessionOpenContext openContext = new RtpSessionOpenContext(channel, localAddress, mock(FutureCallback.class));
        fsm.fire(RtpSessionEvent.OPEN, openContext);
        
        RtpSessionUpdateModeContext modeContext = new RtpSessionUpdateModeContext(mode, jitterBuffer, dtmfInput, rtpInput, rtpOutput, mock(FutureCallback.class));
        fsm.fire(RtpSessionEvent.UPDATE_MODE, modeContext);
        
        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        assertEquals(mode, context.getMode());
    }

}
