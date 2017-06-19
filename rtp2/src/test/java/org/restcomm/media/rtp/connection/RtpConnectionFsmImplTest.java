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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.RtpConnectionException;
import org.restcomm.media.rtp.session.exception.RtpSessionAllocateException;
import org.restcomm.media.rtp.session.exception.RtpSessionException;
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
        if(this.fsm != null) {
            if(this.fsm.isStarted()) {
                this.fsm.terminate();
            }
            this.fsm = null;
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testAllocateSessionAction() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final OpenContext txContext  = new OpenContext(originator, session, mode, address, remoteSession);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        // when
        fsm.enterAllocatingSession(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.OPEN, txContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSetSessionModeAction() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final OpenContext txContext  = new OpenContext(originator, session, mode, address, remoteSession);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        // when
        fsm.enterSettingSessionMode(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.SESSION_ALLOCATED, txContext);
        
        // then
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        assertEquals(mode, context.getMode());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testNegotiateSessionAction() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final OpenContext txContext  = new OpenContext(originator, session, mode, address, remoteSession);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        // when
        fsm.enterNegotiatingSession(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.SESSION_MODE_UPDATED, txContext);
        
        // then
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSessionAllocationFailure() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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
        
        OpenContext openContext = new OpenContext(originator, session, mode, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSessionSetModeFailure() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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
        
        OpenContext openContext = new OpenContext(originator, session, mode, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session, never()).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSessionNegotiationFailure() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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
        
        OpenContext openContext = new OpenContext(originator, session, mode, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(originator).onFailure(any(RtpConnectionException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenConnection() {
        // given
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);     
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        final FutureCallback<Void> openCallback = mock(FutureCallback.class);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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

        OpenContext openContext = new OpenContext(openCallback, session, mode, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(session).updateMode(eq(mode), any(FutureCallback.class));
        verify(session).negotiate(eq(remoteSession), any(FutureCallback.class));
        verify(openCallback).onSuccess(null);
        assertEquals(RtpConnectionState.OPEN, fsm.getCurrentState());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testUpdateConnectionMode() {
        // given
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);     
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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

        OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, remoteSession);
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
    public void testUpdateConnectionModeFailure() {
        // given
        final String cname = "mock";
        final RtpSession session = mock(RtpSession.class);
        final ConnectionMode mode = ConnectionMode.SEND_RECV;
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);     
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        this.fsm = RtpConnectionFsmBuilder.INSTANCE.build(context);
        
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
        
        OpenContext openContext = new OpenContext(mock(FutureCallback.class), session, mode, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        FutureCallback<Void> updateCallback = mock(FutureCallback.class);
        ConnectionMode newMode = ConnectionMode.SEND_ONLY;
        UpdateModeContext updateModeContext = new UpdateModeContext(updateCallback, newMode, session);
        this.fsm.fire(RtpConnectionEvent.UPDATE_MODE, updateModeContext);
        
        // then
        verify(session).updateMode(eq(newMode), any(FutureCallback.class));
        verify(updateCallback).onFailure(any(RtpConnectionException.class));;
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }

}
