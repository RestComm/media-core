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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointEvent;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointParameter;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointTransitionContext;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RegisterConnectionInSplitterActionTest {

    @Test
    public void testRegisterLocalConnectionInSplitter() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final AudioSplitter splitter = mock(AudioSplitter.class);
        final OOBSplitter oobSplitter = mock(OOBSplitter.class);
        final NotificationCenter notificationCenter = mock(NotificationCenter.class);
        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, notificationCenter, splitter, oobSplitter);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final AudioComponent component = mock(AudioComponent.class);
        final OOBComponent oobComponent = mock(OOBComponent.class);
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.isLocal()).thenReturn(true);
        when(connection.getAudioComponent()).thenReturn(component);
        when(connection.getOutOfBandComponent()).thenReturn(oobComponent);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.REGISTERED_CONNECTION, connection);

        final RegisterConnectionInSplitterAction action = new RegisterConnectionInSplitterAction();
        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTERED_CONNECTION, txContext, fsm);

        // then
        verify(splitter).addInsideComponent(component);
        verify(splitter, never()).addOutsideComponent(component);
        verify(oobSplitter).addInsideComponent(oobComponent);
        verify(oobSplitter, never()).addOutsideComponent(oobComponent);
    }

    @Test
    public void testRegisterRemoteConnectionInSplitter() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final AudioSplitter splitter = mock(AudioSplitter.class);
        final OOBSplitter oobSplitter = mock(OOBSplitter.class);
        final NotificationCenter notificationCenter = mock(NotificationCenter.class);
        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, notificationCenter, splitter, oobSplitter);
        
        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);
        
        final AudioComponent component = mock(AudioComponent.class);
        final OOBComponent oobComponent = mock(OOBComponent.class);
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.isLocal()).thenReturn(false);
        when(connection.getAudioComponent()).thenReturn(component);
        when(connection.getOutOfBandComponent()).thenReturn(oobComponent);
        
        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        txContext.set(MgcpEndpointParameter.REGISTERED_CONNECTION, connection);
        
        final RegisterConnectionInSplitterAction action = new RegisterConnectionInSplitterAction();
        action.execute(MgcpEndpointState.IDLE, MgcpEndpointState.ACTIVE, MgcpEndpointEvent.REGISTERED_CONNECTION, txContext, fsm);
        
        // then
        verify(splitter, never()).addInsideComponent(component);
        verify(splitter).addOutsideComponent(component);
        verify(oobSplitter, never()).addInsideComponent(oobComponent);
        verify(oobSplitter).addOutsideComponent(oobComponent);
    }

}
