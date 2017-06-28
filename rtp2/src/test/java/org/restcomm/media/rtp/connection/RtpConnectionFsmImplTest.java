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

package org.restcomm.media.rtp.connection;

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
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.rtp.session.exception.RtpSessionAllocateException;
import org.restcomm.media.rtp.session.exception.RtpSessionException;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SdpParser;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionFsmImplTest {

    private RtpConnectionFsm fsm;

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
    @SuppressWarnings("unchecked")
    public void testParseRemoteSessionDescriptionAction() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());

        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        final RtpConnectionFsmImpl fsm = spy(new RtpConnectionFsmImpl(context));

        final SessionDescription remoteDescription = mock(SessionDescription.class);
        final String remoteSdp = sdpBuffer.toString();

        when(sdpParser.parse(remoteSdp)).thenReturn(remoteDescription);

        doNothing().when(fsm).fire(any(RtpConnectionEvent.class), any(RtpConnectionTransitionContext.class));

        // when
        final OpenContext txContext = new OpenContext(originator, session, mode, address, "", sdpBuffer.toString(), sdpParser, sdpBuilder);
        fsm.enterParsingRemoteDescription(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.OPEN, txContext);

        // then
        verify(sdpParser).parse(remoteSdp);
        assertEquals(remoteDescription, context.getRemoteDescription());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testAllocateSessionAction() {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());

        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();

        // when
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        fsm.enterAllocatingSession(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.OPEN, txContext);

        // then
        verify(session).open(eq(address), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSetSessionModeAction() {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();

        // when
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        fsm.enterSettingSessionMode(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.SESSION_ALLOCATED, txContext);

        // then
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        assertEquals(mode, context.getMode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNegotiateSessionAction() {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        context.setRemoteDescription(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);

        // when
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        fsm.enterNegotiatingSession(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.SESSION_MODE_UPDATED, txContext);

        // then
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateSessionModeAction() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final UpdateModeContext txContext = new UpdateModeContext(originator, mode, session);
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        // when
        fsm.enterUpdatingSessionMode(RtpConnectionState.UPDATING_MODE, RtpConnectionState.UPDATING_SESSION_MODE, RtpConnectionEvent.UPDATE_MODE, txContext);

        // then
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParseRemoteSessionDescription() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        
        // when
        this.fsm.start();
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);
        
        // then
        verify(sdpParser).parse(remoteSdp);
        assertEquals(remoteSessionDescription, context.getRemoteDescription());
        assertEquals(RtpConnectionState.ALLOCATING_SESSION, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testParseRemoteSessionDescriptionFailure() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        when(sdpParser.parse(remoteSdp)).thenThrow(SdpException.class);
        
        // when
        this.fsm.start();
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);
        
        // then
        verify(sdpParser).parse(remoteSdp);
        assertNull(context.getRemoteDescription());
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSessionAllocationFailure() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());

        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new RtpSessionAllocateException("Testing purposes!"));
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        // when
        this.fsm.start();

        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);

        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSessionSetModeFailure() throws SdpException {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = "remote";
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new RtpSessionException("Testing purposes!"));
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        // when
        this.fsm.start();

        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);

        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session, never()).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSessionNegotiationFailure() throws SdpException {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = "remote";
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onFailure(new RtpSessionException("Testing purposes!"));
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        // when
        this.fsm.start();
        final OpenContext txContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, txContext);

        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOpenConnection() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());

        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription localSessionDescription = mock(SessionDescription.class);
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenReturn(localSessionDescription);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        // when
        this.fsm.start();

        final OpenContext openContext = new OpenContext(originator, session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);

        // then
        verify(sdpParser).parse(remoteSdp);
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(sdpBuilder).buildSessionDescription(false, cname, address.getHostString(), externalAddress, session);
        verify(originator).onSuccess(null);
        assertEquals(remoteSessionDescription, context.getRemoteDescription());
        assertEquals(localSessionDescription, context.getLocalDescription());
        assertEquals(RtpConnectionState.OPEN, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateConnectionMode() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription localSessionDescription = mock(SessionDescription.class);
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenReturn(localSessionDescription);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        // when
        this.fsm.start();

        final OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);

        FutureCallback<Void> updateCallback = mock(FutureCallback.class);
        ConnectionMode newMode = ConnectionMode.SEND_ONLY;
        UpdateModeContext updateModeContext = new UpdateModeContext(updateCallback, newMode, session);
        this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, updateModeContext);

        // then
        verify(session).updateMode(eq(newMode), any(FutureCallback.class));
        verify(updateCallback).onSuccess(null);
        assertEquals(RtpConnectionState.OPEN, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateConnectionModeFailure() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription localSessionDescription = mock(SessionDescription.class);
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenReturn(localSessionDescription);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ConnectionMode connectionMode = invocation.getArgumentAt(0, ConnectionMode.class);
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);

                if (mode.equals(connectionMode)) {
                    callback.onSuccess(null);
                } else {
                    callback.onFailure(new RtpConnectionException("Testing purposes!"));
                }
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        // when
        this.fsm.start();

        final OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);

        FutureCallback<Void> updateCallback = mock(FutureCallback.class);
        ConnectionMode newMode = ConnectionMode.SEND_ONLY;
        UpdateModeContext updateModeContext = new UpdateModeContext(updateCallback, newMode, session);
        this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, updateModeContext);

        // then
        verify(session).updateMode(eq(newMode), any(FutureCallback.class));
        verify(updateCallback).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCloseIdleConnection() {
        // given
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).close(any(FutureCallback.class));

        // when
        this.fsm.start();

        FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        CloseContext closeContext = new CloseContext(closeCallback, session);
        this.fsm.fire(RtpConnectionEvent.CLOSE, closeContext);

        // then
        verify(session, never()).close(any(FutureCallback.class));
        verify(closeCallback).onSuccess(null);
        assertEquals(RtpConnectionState.CLOSED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCloseOpeningConnection() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription localSessionDescription = mock(SessionDescription.class);
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenReturn(localSessionDescription);

        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                CloseContext closeContext = new CloseContext(closeCallback, session);
                fsm.fire(RtpConnectionEvent.CLOSE, closeContext);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).close(any(FutureCallback.class));

        // when
        this.fsm.start();

        OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);

        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session).close(any(FutureCallback.class));
        verify(closeCallback).onSuccess(null);
        assertEquals(RtpConnectionState.CLOSED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCloseConnectionWhileUpdatingMode() throws SdpException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription localSessionDescription = mock(SessionDescription.class);
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenReturn(localSessionDescription);

        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                ConnectionMode connectionMode = invocation.getArgumentAt(0, ConnectionMode.class);
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);

                if (mode.equals(connectionMode)) {
                    callback.onSuccess(null);
                } else {
                    CloseContext closeContext = new CloseContext(closeCallback, session);
                    fsm.fire(RtpConnectionEvent.CLOSE, closeContext);
                }
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).close(any(FutureCallback.class));

        // when
        this.fsm.start();

        OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);

        ConnectionMode newMode = ConnectionMode.SEND_ONLY;
        UpdateModeContext updateModeContext = new UpdateModeContext(mock(FutureCallback.class), newMode, session);
        this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, updateModeContext);

        // then
        verify(session).close(any(FutureCallback.class));
        verify(closeCallback).onSuccess(null);
        assertEquals(RtpConnectionState.CLOSED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCloseConnectionWhenCorrupted() throws SdpException, InterruptedException {
        // given
        final StringBuilder sdpBuffer = new StringBuilder("v=0").append(System.lineSeparator());
        sdpBuffer.append("o=- 326911306 0 IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("s=-").append(System.lineSeparator());
        sdpBuffer.append("c=IN IP4 10.20.0.200").append(System.lineSeparator());
        sdpBuffer.append("t=0 0").append(System.lineSeparator());
        sdpBuffer.append("m=audio 13842 RTP/AVP 8 101").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        sdpBuffer.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        sdpBuffer.append("a=fmtp:101 0-15").append(System.lineSeparator());
        sdpBuffer.append("a=sendrecv").append(System.lineSeparator());
        sdpBuffer.append("a=ptime:20").append(System.lineSeparator());
        
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final InetSocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, address.getHostString(), externalAddress);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);

        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final String remoteSdp = sdpBuffer.toString();
        final SessionDescription remoteSessionDescription = mock(SessionDescription.class);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        
        when(sdpParser.parse(remoteSdp)).thenReturn(remoteSessionDescription);
        when(remoteSessionDescription.getMediaDescription(MediaType.AUDIO.name().toLowerCase())).thenReturn(remoteSession);
        
        when(sdpBuilder.buildSessionDescription(false, cname, address.getHostString(), externalAddress, session)).thenThrow(SdpException.class);

        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).updateMode(any(ConnectionMode.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<Void> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).close(any(FutureCallback.class));

        // when
        this.fsm.start();

        OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, externalAddress, remoteSdp, sdpParser, sdpBuilder);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
        
        Thread.sleep(100);
        
        CloseContext closeContext = new CloseContext(closeCallback, session);
        fsm.fire(RtpConnectionEvent.CLOSE, closeContext);

        // then
        verify(session).close(any(FutureCallback.class));
        verify(closeCallback).onSuccess(null);
        assertEquals(RtpConnectionState.CLOSED, fsm.getCurrentState());
    }

}
