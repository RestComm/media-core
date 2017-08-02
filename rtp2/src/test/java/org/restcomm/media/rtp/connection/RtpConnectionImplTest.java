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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.rtp.MediaType;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.format.AVProfile;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionImplTest {

    private RtpConnection connection;

    @After
    public void after() {
        if (this.connection != null) {
            this.connection.close(new FutureCallback<Void>() {

                @Override
                public void onSuccess(Void result) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onFailure(Throwable t) {
                    // TODO Auto-generated method stub

                }
            });
            this.connection = null;
        }
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testSuccessfulInboundFlow() {
        // given
        final StringBuilder remoteSdpString = new StringBuilder("v=0").append(System.lineSeparator());
        remoteSdpString.append("o=- 1450021652720 1450021652720 IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdpString.append("s=remote peer").append(System.lineSeparator());
        remoteSdpString.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdpString.append("t=0 0").append(System.lineSeparator());
        remoteSdpString.append("m=audio 6000 RTP/AVP 0 8 101").append(System.lineSeparator());
        remoteSdpString.append("a=ptime:20").append(System.lineSeparator());

        final SessionDescriptionParser sdpParser = new SessionDescriptionParser();
        final SdpBuilder sdpBuilder = new SdpBuilder();

        final String cname = "cname";
        final String localAddress = "127.0.0.1";
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, localAddress, externalAddress);
        final RtpConnectionFsmBuilder fsmBuilder = new RtpConnectionFsmBuilder();

        final PortManager portManager = mock(PortManager.class);
        when(portManager.next()).thenReturn(65000);

        final RtpSessionFactory sessionFactory = mock(RtpSessionFactory.class);
        final RtpSession session = mock(RtpSession.class);

        when(sessionFactory.build()).thenReturn(session);
        when(session.getMediaType()).thenReturn(MediaType.AUDIO);
        when(session.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        when(session.getRtpAddress()).thenReturn(new InetSocketAddress(localAddress, 65000));
        when(session.getSsrc()).thenReturn(12345L);
        when(session.getSupportedFormats()).thenReturn(AVProfile.audio);

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).open(any(SocketAddress.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(1, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).negotiate(any(MediaDescriptionField.class), any(FutureCallback.class));

        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                FutureCallback<?> callback = invocation.getArgumentAt(0, FutureCallback.class);
                callback.onSuccess(null);
                return null;
            }
        }).when(session).close(any(FutureCallback.class));

        // when
        final FutureCallback<String> openCallback = mock(FutureCallback.class);
        this.connection = new RtpConnectionImpl(sessionFactory, portManager, sdpParser, sdpBuilder, context, fsmBuilder);
        this.connection.open(remoteSdpString.toString(), openCallback);

        // then
        ArgumentCaptor<String> openCaptor = ArgumentCaptor.forClass(String.class);
        verify(openCallback, timeout(100)).onSuccess(openCaptor.capture());
        assertNotNull(openCaptor.getValue());
        assertNotNull(context.getRemoteDescription());
        assertNotNull(context.getLocalDescription());
        assertNotNull(context.getRtpSession());

        // when
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        this.connection.close(closeCallback);

        // then
        verify(closeCallback, timeout(100)).onSuccess(null);
        assertNull(context.getRemoteDescription());
        assertNull(context.getLocalDescription());
        assertNull(context.getRtpSession());
        assertEquals(ConnectionMode.INACTIVE, context.getMode());
    }

    @Test
    @SuppressWarnings({ "unchecked" })
    public void testCorruptedInboundFlowWhenParsingRemoteDescription() throws SdpException {
        // given
        final StringBuilder remoteSdpString = new StringBuilder("v=0").append(System.lineSeparator());
        remoteSdpString.append("o=- 1450021652720 1450021652720 IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdpString.append("s=remote peer").append(System.lineSeparator());
        remoteSdpString.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdpString.append("t=0 0").append(System.lineSeparator());
        remoteSdpString.append("m=audio 6000 RTP/AVP 0 8 101").append(System.lineSeparator());
        remoteSdpString.append("a=ptime:20").append(System.lineSeparator());

        final String cname = "cname";
        final String localAddress = "127.0.0.1";
        final String externalAddress = "";
        final RtpConnectionContext context = new RtpConnectionContext(cname, localAddress, externalAddress);
        final RtpConnectionFsmBuilder fsmBuilder = new RtpConnectionFsmBuilder();

        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final SessionDescriptionParser sdpParser = mock(SessionDescriptionParser.class);
        final SdpException error = new SdpException("testing purposes");
        when(sdpParser.parse(remoteSdpString.toString())).thenThrow(error);

        final PortManager portManager = mock(PortManager.class);
        when(portManager.next()).thenReturn(65000);

        final RtpSessionFactory sessionFactory = mock(RtpSessionFactory.class);

        // when
        final FutureCallback<String> openCallback = mock(FutureCallback.class);
        this.connection = new RtpConnectionImpl(sessionFactory, portManager, sdpParser, sdpBuilder, context, fsmBuilder);
        this.connection.open(remoteSdpString.toString(), openCallback);

        // then
        verify(openCallback, timeout(100)).onFailure(error);
        verify(sessionFactory, never()).build();
        verify(sdpBuilder, never()).buildSessionDescription(any(Boolean.class), any(String.class), any(String.class), any(String.class), any(RtpSession[].class));
        assertNull(context.getRemoteDescription());
        assertNull(context.getLocalDescription());
        assertNull(context.getRtpSession());

        // when
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        this.connection.close(closeCallback);

        // then
        verify(closeCallback, timeout(100)).onSuccess(null);
        assertNull(context.getRemoteDescription());
        assertNull(context.getLocalDescription());
        assertNull(context.getRtpSession());
        assertEquals(ConnectionMode.INACTIVE, context.getMode());
    }

}
