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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.connection.exception.SessionAllocationException;
import org.restcomm.media.rtp.session.exception.RtpSessionAllocateException;
import org.restcomm.media.sdp.fields.MediaDescriptionField;

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
        final SocketAddress address = new InetSocketAddress("127.0.0.1", 6000);
        final MediaDescriptionField remoteSession = mock(MediaDescriptionField.class);
        final RtpConnectionOpenContext txContext  = new RtpConnectionOpenContext(originator, session, address, remoteSession);
        final RtpConnectionContext context = new RtpConnectionContext(cname);
        final RtpConnectionFsmImpl fsm = new RtpConnectionFsmImpl(context);
        
        // when
        fsm.enterAllocatingSession(RtpConnectionState.OPENING, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.OPEN, txContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testSessionAllocationFailure() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
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
        
        RtpConnectionOpenContext openContext = new RtpConnectionOpenContext(originator, session, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(originator).onFailure(any(SessionAllocationException.class));
        assertEquals(RtpConnectionState.CORRUPTED, fsm.getCurrentState());
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void testOpenConnection() {
        // given
        final String cname = "mock";
        final FutureCallback<Void> originator = mock(FutureCallback.class);
        final RtpSession session = mock(RtpSession.class);
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
        
        // when
        this.fsm.start();
        
        RtpConnectionOpenContext openContext = new RtpConnectionOpenContext(originator, session, address, remoteSession);
        this.fsm.fire(RtpConnectionEvent.OPEN, openContext);
        
        // then
        verify(session).open(eq(address), any(FutureCallback.class));
        verify(originator).onSuccess(null);
    }

}
