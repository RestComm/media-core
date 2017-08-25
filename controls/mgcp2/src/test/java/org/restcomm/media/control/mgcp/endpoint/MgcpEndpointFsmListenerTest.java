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

package org.restcomm.media.control.mgcp.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;

import org.junit.Test;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointFsmListenerTest {
    
    @Test
    public void testNotifyStateChanged() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final MgcpEndpointObserver observer1 = mock(MgcpEndpointObserver.class);
        final MgcpEndpointObserver observer2 = mock(MgcpEndpointObserver.class);
        final Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        observers.add(observer1);
        observers.add(observer2);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        MgcpEndpointFsmListener listener = new MgcpEndpointFsmListener(endpoint, context);
        listener.transitionComplete(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTER_CONNECTION, txContext);

        // then
        verify(observer1).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
        verify(observer2).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
    }

    @Test
    public void testDontNotifyInitialState() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final MgcpEndpointObserver observer1 = mock(MgcpEndpointObserver.class);
        final MgcpEndpointObserver observer2 = mock(MgcpEndpointObserver.class);
        final Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        observers.add(observer1);
        observers.add(observer2);
        
        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        MgcpEndpointFsmListener listener = new MgcpEndpointFsmListener(endpoint, context);
        listener.transitionComplete(null, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTER_CONNECTION, txContext);

        // then
        verify(observer1, never()).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
        verify(observer2, never()).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
    }

    @Test
    public void testDontNotifyInternalTransitions() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final MgcpEndpointObserver observer1 = mock(MgcpEndpointObserver.class);
        final MgcpEndpointObserver observer2 = mock(MgcpEndpointObserver.class);
        final Set<MgcpEndpointObserver> observers = context.getEndpointObservers();
        observers.add(observer1);
        observers.add(observer2);
        
        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        MgcpEndpointFsmListener listener = new MgcpEndpointFsmListener(endpoint, context);
        listener.transitionComplete(MgcpEndpointState.IDLE, MgcpEndpointState.IDLE, MgcpEndpointEvent.REGISTER_CONNECTION, txContext);
        
        // then
        verify(observer1, never()).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);
        verify(observer2, never()).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);
    }

    @Test
    public void testTransitionDeclined() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/bridge/1", "127.0.0.1:2427");
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final MgcpEndpointContext context = new MgcpEndpointContext(endpointId, mediaGroup);
        final MgcpEndpoint endpoint = mock(MgcpEndpoint.class);
        
        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final FutureCallback<?> callback = mock(FutureCallback.class);
        
        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.CALLBACK, callback);
        
        MgcpEndpointFsmListener listener = new MgcpEndpointFsmListener(endpoint, context);
        listener.transitionDeclined(MgcpEndpointState.ACTIVE, MgcpEndpointState.TERMINATED, MgcpEndpointEvent.TERMINATE, txContext);
        
        // then
        verify(callback).onFailure(any(IllegalStateException.class));
    }

}
