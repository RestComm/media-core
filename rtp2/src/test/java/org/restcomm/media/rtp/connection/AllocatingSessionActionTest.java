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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.rtp.RtpSession;
import org.restcomm.media.rtp.RtpSessionFactory;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AllocatingSessionActionTest {
    
    @Test
    @SuppressWarnings("unchecked")
    public void testAllocateSessionInboundFlow() {
        // given
        final InetSocketAddress bindAddress = new InetSocketAddress("127.0.0.1", 6002);
        final RtpSession session = mock(RtpSession.class);
        final RtpSessionFactory sessionFactory = mock(RtpSessionFactory.class);
        final RtpConnectionTransitionContext context = new RtpConnectionTransitionContext();
        final RtpConnectionFsm fsm = mock(RtpConnectionFsm.class);
        final AllocatingSessionAction action = new AllocatingSessionAction();
        
        context.set(RtpConnectionTransitionParameter.BIND_ADDRESS, bindAddress);
        context.set(RtpConnectionTransitionParameter.RTP_SESSION_FACTORY, sessionFactory);
        
        when(sessionFactory.build()).thenReturn(session);
        
        // when
        action.execute(RtpConnectionState.PARSING_REMOTE_SDP, RtpConnectionState.ALLOCATING_SESSION, RtpConnectionEvent.PARSED_REMOTE_SDP, context, fsm);
        
        // then
        ArgumentCaptor<InetSocketAddress> addressCaptor = ArgumentCaptor.forClass(InetSocketAddress.class);
        verify(sessionFactory, times(1)).build();
        verify(session, times(1)).open(addressCaptor.capture(), any(FutureCallback.class));
        assertEquals(bindAddress, addressCaptor.getValue());
        assertEquals(session, context.get(RtpConnectionTransitionParameter.RTP_SESSION, RtpSession.class));
    }

}
