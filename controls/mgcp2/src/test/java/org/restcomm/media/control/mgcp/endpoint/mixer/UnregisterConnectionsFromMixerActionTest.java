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

package org.restcomm.media.control.mgcp.endpoint.mixer;

import org.junit.Test;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.*;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 */
public class UnregisterConnectionsFromMixerActionTest {

    @Test
    public void testUnregisterConnectionsFromMixer() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MediaGroupImpl mediaGroup = mock(MediaGroupImpl.class);
        final AudioMixer mixer = mock(AudioMixer.class);
        final OOBMixer oobMixer = mock(OOBMixer.class);
        final NotificationCenter notificationCenter = mock(NotificationCenter.class);
        final MgcpMixerEndpointContext context = new MgcpMixerEndpointContext(endpointId, mediaGroup, notificationCenter, mixer, oobMixer);

        final MgcpEndpointFsm fsm = mock(MgcpEndpointFsm.class);
        when(fsm.getContext()).thenReturn(context);

        final AudioComponent component1 = mock(AudioComponent.class);
        final OOBComponent oobComponent1 = mock(OOBComponent.class);
        final MgcpConnection connection1 = mock(MgcpConnection.class);
        when(connection1.getAudioComponent()).thenReturn(component1);
        when(connection1.getOutOfBandComponent()).thenReturn(oobComponent1);

        final AudioComponent component2 = mock(AudioComponent.class);
        final OOBComponent oobComponent2 = mock(OOBComponent.class);
        final MgcpConnection connection2 = mock(MgcpConnection.class);
        when(connection2.getAudioComponent()).thenReturn(component2);
        when(connection2.getOutOfBandComponent()).thenReturn(oobComponent2);

        final AudioComponent component3 = mock(AudioComponent.class);
        final OOBComponent oobComponent3 = mock(OOBComponent.class);
        final MgcpConnection connection3 = mock(MgcpConnection.class);
        when(connection3.getAudioComponent()).thenReturn(component3);
        when(connection3.getOutOfBandComponent()).thenReturn(oobComponent3);

        // when
        final MgcpEndpointTransitionContext txContext = new MgcpEndpointTransitionContext();
        final MgcpConnection[] unregistered = new MgcpConnection[]{connection1, connection2, connection3};
        txContext.set(MgcpEndpointParameter.UNREGISTERED_CONNECTIONS, unregistered);

        final UnregisterConnectionsFromMixerAction action = new UnregisterConnectionsFromMixerAction();
        action.execute(MgcpEndpointState.ACTIVE, MgcpEndpointState.IDLE, MgcpEndpointEvent.UNREGISTERED_CONNECTION, txContext, fsm);

        // then
        verify(mixer, times(unregistered.length)).release(any(AudioComponent.class));
        verify(oobMixer, times(unregistered.length)).release(any(OOBComponent.class));
    }

}
