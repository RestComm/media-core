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
import static org.mockito.Mockito.*;

import java.net.SocketAddress;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.network.deprecated.PortManager;
import org.restcomm.media.network.deprecated.RtpPortManager;
import org.restcomm.media.rtp.RtpConnection;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;
import org.restcomm.media.rtp.sdp.SdpBuilder;
import org.restcomm.media.sdp.SessionDescription;
import org.restcomm.media.sdp.SessionDescriptionParser;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.spi.ConnectionMode;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpConnectionImplTest {

    private RtpConnection connection;

    @After
    @SuppressWarnings("unchecked")
    public void after() {
        if (this.connection != null) {
            this.connection.close(mock(FutureCallback.class));
            this.connection = null;
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testInboundCallFlow() {
        // given
        final String cname = "mock";
        final String localAddress = "127.0.0.1";
        final String externalAddress = "";
        final PortManager portManager = new RtpPortManager(64000, 65000);
        final RtpSessionFactory sessionFactory = mock(RtpSessionFactory.class);
        final RtpSession session = mock(RtpSession.class);
        final SdpBuilder sdpBuilder = mock(SdpBuilder.class);
        final SessionDescriptionParser sdpParser = new SessionDescriptionParser();
        final SessionDescription localSdp = mock(SessionDescription.class);
        final RtpConnectionContext context = new RtpConnectionContext(cname, localAddress, externalAddress);
        this.connection = new RtpConnectionImpl(context, sdpParser, sdpBuilder, sessionFactory, portManager);
        
        when(sessionFactory.build()).thenReturn(session);
        when(sdpBuilder.buildSessionDescription(eq(false), eq(cname), eq(localAddress), eq(externalAddress), eq(session))).thenReturn(localSdp);

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


        // when - open connection
        final StringBuilder remoteSdp = new StringBuilder("v=0").append(System.lineSeparator());
        remoteSdp.append("o=- 326911306 0 IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdp.append("s=-").append(System.lineSeparator());
        remoteSdp.append("c=IN IP4 127.0.0.1").append(System.lineSeparator());
        remoteSdp.append("t=0 0").append(System.lineSeparator());
        remoteSdp.append("m=audio 6000 RTP/AVP 8 101").append(System.lineSeparator());
        remoteSdp.append("a=rtpmap:8 PCMA/8000").append(System.lineSeparator());
        remoteSdp.append("a=rtpmap:101 telephone-event/8000").append(System.lineSeparator());
        remoteSdp.append("a=fmtp:101 0-15").append(System.lineSeparator());
        remoteSdp.append("a=sendrecv").append(System.lineSeparator());
        remoteSdp.append("a=ptime:20").append(System.lineSeparator());
        final ConnectionMode mode = ConnectionMode.SEND_ONLY;
        final FutureCallback<Void> openCallback = mock(FutureCallback.class);

        connection.open(mode, remoteSdp.toString(), openCallback);

        // then
        verify(openCallback, timeout(100)).onSuccess(null);
        assertNotNull(connection.getLocalDescription());
        assertTrue(connection.getLocalDescription().length() > 0);
        assertNotNull(connection.getRemoteDescription());
        assertTrue(connection.getRemoteDescription().length() > 0);
        assertTrue(connection.isOpen());

        // when - close connection
        final FutureCallback<Void> closeCallback = mock(FutureCallback.class);
        connection.close(closeCallback);

        // then
        verify(closeCallback, timeout(100)).onSuccess(null);
        assertTrue(connection.getLocalDescription().isEmpty());
        assertTrue(connection.getRemoteDescription().isEmpty());
        assertFalse(connection.isOpen());
    }

}
