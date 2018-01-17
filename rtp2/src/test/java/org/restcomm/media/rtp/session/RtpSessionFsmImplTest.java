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
import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.rtp.*;
import org.restcomm.media.rtp.rfc2833.DtmfInput;
import org.restcomm.media.scheduler.WallClock;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.sdp.format.RTPFormat;
import org.restcomm.media.sdp.format.RTPFormats;
import org.restcomm.media.spi.ConnectionMode;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
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
        final RtpChannelInitializer channelInitializer = mock(RtpChannelInitializer.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);

        // when
        SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        RtpSessionOpenContext bindContext = new RtpSessionOpenContext(channel, channelInitializer, address, null);
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
        final RtpChannelInitializer channelInitializer = mock(RtpChannelInitializer.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        final RtpSessionFsmImpl fsm = new RtpSessionFsmImpl(context);

        // when
        SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        RtpSessionOpenContext bindContext = new RtpSessionOpenContext(channel, channelInitializer, address, null);
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
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress,
                remoteSsrc, null);
        fsm.enterNegotiatingFormats(RtpSessionState.NEGOTIATING, RtpSessionState.NEGOTIATING_FORMATS, RtpSessionEvent.NEGOTIATE,
                negotiateContext);

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

        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.INACTIVE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).deactivate();
        verify(audioComponent).updateMode(false, false);
        verify(context).setMode(mode);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateModeActionWithModeSetToSendOnly() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).activate();
        verify(audioComponent).updateMode(false, true);
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
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.RECV_ONLY;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.SEND_RECV);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput, never()).activate();
        verify(audioComponent).updateMode(true, false);
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
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        verify(audioComponent).updateMode(true, true);
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
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.CONFERENCE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        verify(audioComponent).updateMode(true, true);
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
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.CONFERENCE;
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);

        // when
        RtpSessionUpdateModeContext txContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, callback);
        fsm.onUpdateMode(RtpSessionState.OPEN, RtpSessionState.OPEN, RtpSessionEvent.UPDATE_MODE, txContext);

        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput, never()).activate();
        verify(dtmfInput, never()).activate();
        verify(rtpOutput, never()).activate();
        verify(audioComponent, never()).updateMode(anyBoolean(), anyBoolean());
        verify(context, never()).setMode(mode);
    }

    @Test
    public void testIncomingRtpPacketAction() {
        // given
        final RtpSessionStatistics statistics = mock(RtpSessionStatistics.class);
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final int payloadType = 0;
        final byte[] payload = "hello".getBytes();
        final RtpPacket packet = new RtpPacket(true, payloadType, 1, System.currentTimeMillis(), 12345L, payload);
        final RTPFormat format = AVProfile.audio.find(payloadType);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);

        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);
        when(context.getStatistics()).thenReturn(statistics);

        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, rtpInput, dtmfInput);
        fsm.onIncomingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.INCOMING_RTP, txContext);

        // then
        verify(rtpInput).write(packet, format);
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
        final byte[] payload = "hello".getBytes();
        final RtpPacket packet = new RtpPacket(true, payloadType, 1, System.currentTimeMillis(), 12345L, payload);
        final RTPFormat format = AVProfile.audio.find(payloadType);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);

        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.CONFERENCE);
        when(context.getStatistics()).thenReturn(statistics);

        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, rtpInput, dtmfInput);
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
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);

        when(packet.getPayloadType()).thenReturn(payloadType);
        when(context.getNegotiatedFormats()).thenReturn(AVProfile.audio);
        when(context.getMode()).thenReturn(ConnectionMode.INACTIVE);
        when(context.getStatistics()).thenReturn(statistics);

        // when
        RtpSessionIncomingRtpContext txContext = new RtpSessionIncomingRtpContext(packet, rtpInput, dtmfInput);
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
        RtpSessionOutgoingRtpCallback callback = new RtpSessionOutgoingRtpCallback(statistics, packet);
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
        RtpSessionOutgoingRtpCallback callback = new RtpSessionOutgoingRtpCallback(statistics, packet);
        RtpSessionOutgoingRtpContext txContext = new RtpSessionOutgoingRtpContext(packet, channel, callback);

        fsm.onOutgoingRtp(RtpSessionState.ESTABLISHED, RtpSessionState.ESTABLISHED, RtpSessionEvent.OUTGOING_RTP, txContext);

        // then
        verify(channel, never()).send(packet, callback);
        verify(statistics, never()).outgoingRtp(packet);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testConnectingAction() {
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
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress,
                remoteSsrc, null);
        fsm.enterConnecting(RtpSessionState.NEGOTIATING_FORMATS, RtpSessionState.CONNECTING, RtpSessionEvent.NEGOTIATED_FORMATS,
                negotiateContext);

        // then
        verify(channel).connect(eq(remoteAddress), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeactivatingAction() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final RtpChannel channel = mock(RtpChannel.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        doNothing().when(fsm).fire(any(RtpSessionEvent.class), any(RtpSessionTransactionContext.class));

        // when
        RtpSessionCloseContext txContext = new RtpSessionCloseContext(channel, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.enterDeactivating(RtpSessionState.CLOSING, RtpSessionState.DEACTIVATING, RtpSessionEvent.CLOSE, txContext);

        // then
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).deactivate();
        verify(context).setMode(ConnectionMode.INACTIVE);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testDeallocatingAction() {
        // given
        final RtpSessionContext context = mock(RtpSessionContext.class);
        final RtpSessionFsmImpl fsm = spy(new RtpSessionFsmImpl(context));

        final RtpChannel channel = mock(RtpChannel.class);
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        doNothing().when(fsm).fire(any(RtpSessionEvent.class), any(RtpSessionTransactionContext.class));

        // when
        RtpSessionCloseContext txContext = new RtpSessionCloseContext(channel, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.enterDeallocating(RtpSessionState.DEACTIVATING, RtpSessionState.DEALLOCATING, RtpSessionEvent.DEACTIVATED, txContext);

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
        final RtpChannelInitializer channelInitializer = mock(RtpChannelInitializer.class);
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
        RtpSessionOpenContext openContext = new RtpSessionOpenContext(channel, channelInitializer, localAddress, mock(FutureCallback.class));
        fsm.fire(RtpSessionEvent.OPEN, openContext);

        // then
        verify(channel).open(any(FutureCallback.class), eq(channelInitializer));
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
        RtpSessionNegotiateContext negotiateContext = new RtpSessionNegotiateContext(channel, offeredFormats, remoteAddress,
                remoteSsrc, null);
        fsm.fire(RtpSessionEvent.NEGOTIATE, negotiateContext);

        // then
        verify(channel).connect(eq(remoteAddress), any(FutureCallback.class));
        RTPFormats negotiatedFormats = context.getNegotiatedFormats();
        assertEquals(2, negotiatedFormats.size());
        assertNotNull(negotiatedFormats.getRTPFormat(8));
        assertNotNull(negotiatedFormats.getRTPFormat(101));
        assertEquals(remoteAddress, context.getRemoteAddress());
        assertEquals(RtpSessionState.ESTABLISHED, fsm.getCurrentState());

        // given
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).close(any(FutureCallback.class));

        // when
        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final FutureCallback<Void> callback = mock(FutureCallback.class);

        RtpSessionCloseContext closeContext = new RtpSessionCloseContext(channel, dtmfInput, rtpInput, rtpOutput, callback);
        fsm.fire(RtpSessionEvent.CLOSE, closeContext);

        // then
        verify(rtpInput).deactivate();
        verify(dtmfInput).deactivate();
        verify(rtpOutput).deactivate();
        assertEquals(ConnectionMode.INACTIVE, context.getMode());
        verify(jitterBuffer).restart();
        verify(jitterBuffer).forget(rtpInput);
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
        final RtpChannelInitializer channelInitializer = mock(RtpChannelInitializer.class);
        final RtpSessionStatistics statistics = new RtpSessionStatistics(clock, ssrc);
        final RTPFormats formats = AVProfile.audio;
        final RtpSessionContext context = new RtpSessionContext(ssrc, mediaType, statistics, formats);
        this.fsm = RtpSessionFsmBuilder.INSTANCE.build(context);
        final SocketAddress localAddress = new InetSocketAddress("127.0.0.1", 6000);

        final JitterBuffer jitterBuffer = mock(JitterBuffer.class);
        final RtpInput rtpInput = mock(RtpInput.class);
        final DtmfInput dtmfInput = mock(DtmfInput.class);
        final RtpOutput rtpOutput = mock(RtpOutput.class);
        final AudioComponent audioComponent = mock(AudioComponent.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }

        }).when(channel).open(any(FutureCallback.class), any(RtpChannelInitializer.class));

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
        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        RtpSessionOpenContext openContext = new RtpSessionOpenContext(channel, channelInitializer, localAddress, openCallback);
        fsm.fire(RtpSessionEvent.OPEN, openContext);

        verify(openCallback, timeout(100)).onSuccess(null);

        final FutureCallback<Void> updateCallback = mock(FutureCallback.class);
        RtpSessionUpdateModeContext modeContext = new RtpSessionUpdateModeContext(mode, dtmfInput, rtpInput, rtpOutput, audioComponent, updateCallback);
        fsm.fire(RtpSessionEvent.UPDATE_MODE, modeContext);

        verify(updateCallback, timeout(100)).onSuccess(null);

        // then
        verify(jitterBuffer, never()).restart();
        verify(rtpInput).activate();
        verify(dtmfInput).activate();
        verify(rtpOutput).activate();
        verify(audioComponent).updateMode(true, true);
        assertEquals(mode, context.getMode());
    }

}
