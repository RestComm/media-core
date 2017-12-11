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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.restcomm.media.component.audio.AudioComponent;
import org.restcomm.media.component.audio.AudioMixer;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBMixer;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MediaGroupImpl;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointObserver;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;

import com.google.common.util.concurrent.FutureCallback;
import org.restcomm.media.control.mgcp.endpoint.notification.NotificationCenter;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpointTest {
    
    private final MgcpMixerEndpointFsmBuilder fsmBuilder = new MgcpMixerEndpointFsmBuilder();
    
    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final MediaGroupImpl mediaGroup = mock(MediaGroupImpl.class);
        final AudioComponent mediaGroupComponent = mock(AudioComponent.class);
        final OOBComponent mediaGroupOobComponent = mock(OOBComponent.class);
        when(mediaGroup.getAudioComponent()).thenReturn(mediaGroupComponent);
        when(mediaGroup.getOobComponent()).thenReturn(mediaGroupOobComponent);
        
        final AudioMixer mixer = mock(AudioMixer.class);
        final OOBMixer oobMixer = mock(OOBMixer.class);
        final NotificationCenter notificationCenter = mock(NotificationCenter.class);
        final MgcpMixerEndpointContext context = new MgcpMixerEndpointContext(endpointId, mediaGroup, notificationCenter, mixer, oobMixer);
        final MgcpEndpointFsm fsm = this.fsmBuilder.build(context);
        final MgcpEndpointObserver observer = mock(MgcpEndpointObserver.class);

        // when
        final MgcpEndpoint endpoint = new MgcpMixerEndpoint(context, fsm);
        endpoint.observe(observer);
        
        // then
        verify(mixer, times(0)).start();
        verify(oobMixer, times(0)).start();
        verify(observer, times(0)).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);
        
        // when
        final AudioComponent connectionComponent = mock(AudioComponent.class);
        final OOBComponent connectionOobComponent = mock(OOBComponent.class);
        final MgcpConnection connection = mock(MgcpConnection.class);
        when(connection.getAudioComponent()).thenReturn(connectionComponent);
        when(connection.getOutOfBandComponent()).thenReturn(connectionOobComponent);
        final FutureCallback<Void> registerCallback = mock(FutureCallback.class);
        
        endpoint.registerConnection(connection, registerCallback);
        
        // then
        verify(registerCallback, timeout(100)).onSuccess(null);
        verify(mixer).start();
        verify(mixer).addComponent(connectionComponent);
        verify(mixer).addComponent(mediaGroupComponent);
        verify(oobMixer).start();
        verify(oobMixer).addComponent(connectionOobComponent);
        verify(oobMixer).addComponent(mediaGroupOobComponent);
        verify(observer).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
        
        // when
        final FutureCallback<MgcpConnection[]> unregisterCallback = mock(FutureCallback.class);
        
        endpoint.unregisterConnections(unregisterCallback);
        
        // then
        ArgumentCaptor<MgcpConnection[]> unregisterCaptor = ArgumentCaptor.forClass(MgcpConnection[].class);
        verify(unregisterCallback, timeout(100)).onSuccess(unregisterCaptor.capture());
        assertNotNull(unregisterCaptor.getValue());
        assertEquals(1, unregisterCaptor.getValue().length);
        verify(mixer, times(2)).stop(); // times(2) because IDLE is entered as initial state
        verify(mixer, times(2)).release(mediaGroupComponent);
        verify(mixer, times(1)).release(connectionComponent);
        verify(oobMixer, times(2)).stop(); // times(2) because IDLE is entered as initial state
        verify(oobMixer, times(2)).release(mediaGroupOobComponent);
        verify(oobMixer, times(1)).release(connectionOobComponent);
        verify(observer, times(1)).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);
    }

}
