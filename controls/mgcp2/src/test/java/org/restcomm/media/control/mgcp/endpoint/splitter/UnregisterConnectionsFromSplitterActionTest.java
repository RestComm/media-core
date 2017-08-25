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

package org.restcomm.media.control.mgcp.endpoint.splitter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointParameter;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class UnregisterConnectionsFromSplitterActionTest {

    @Test
    public void testUnregisterConnectionsFromSplitter() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        final AudioSplitter splitter = mock(AudioSplitter.class);
        final OOBSplitter oobSplitter = mock(OOBSplitter.class);
        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, connectionProvider, splitter, oobSplitter);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final AudioComponent component1 = mock(AudioComponent.class);
        final OOBComponent oobComponent1 = mock(OOBComponent.class);
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        when(connection1.getAudioComponent()).thenReturn(component1);
        when(connection1.getOutOfBandComponent()).thenReturn(oobComponent1);
        when(connection1.isLocal()).thenReturn(true);
        
        final AudioComponent component2 = mock(AudioComponent.class);
        final OOBComponent oobComponent2 = mock(OOBComponent.class);
        final MgcpConnection connection2 = mock(MgcpConnection.class);
        when(connection2.getAudioComponent()).thenReturn(component2);
        when(connection2.getOutOfBandComponent()).thenReturn(oobComponent2);
        when(connection2.isLocal()).thenReturn(true);
        
        final AudioComponent component3 = mock(AudioComponent.class);
        final OOBComponent oobComponent3 = mock(OOBComponent.class);
        final MgcpConnection connection3 = mock(MgcpConnection.class);
        when(connection3.getAudioComponent()).thenReturn(component3);
        when(connection3.getOutOfBandComponent()).thenReturn(oobComponent3);
        when(connection3.isLocal()).thenReturn(false);
        
        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        final MgcpConnection[] unregistered = new MgcpConnection[] {connection1, connection2, connection3};
        txContext.set(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, unregistered);

        final UnregisterConnectionsFromSplitterAction action = new UnregisterConnectionsFromSplitterAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext, fsm);

        // then
        verify(splitter, times(2)).releaseInsideComponent(any(AudioComponent.class));
        verify(splitter, times(1)).releaseOutsideComponent(any(AudioComponent.class));
        verify(oobSplitter, times(2)).releaseInsideComponent(any(OOBComponent.class));
        verify(oobSplitter, times(1)).releaseOutsideComponent(any(OOBComponent.class));
    }

}
