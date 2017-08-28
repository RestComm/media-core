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
import org.restcomm.media.component.audio.AudioSplitter;
import org.restcomm.media.component.oob.OOBComponent;
import org.restcomm.media.component.oob.OOBSplitter;
import org.restcomm.media.control.mgcp.connection.MgcpConnection;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointFsm;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointObserver;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointState;

import com.google.common.util.concurrent.FutureCallback;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSplitterEndpointTest {

    private final MgcpSplitterEndpointFsmBuilder fsmBuilder = new MgcpSplitterEndpointFsmBuilder();

    @Test
    @SuppressWarnings("unchecked")
    public void testLifecycle() {
        // given
        final EndpointIdentifier endpointId = new EndpointIdentifier("restcomm/mock", "127.0.0.1:2427");
        final AudioSplitter splitter = mock(AudioSplitter.class);
        final OOBSplitter oobSplitter = mock(OOBSplitter.class);
        final MgcpSplitterEndpointContext context = new MgcpSplitterEndpointContext(endpointId, splitter, oobSplitter);
        final MgcpEndpointFsm fsm = this.fsmBuilder.build(context);
        final MgcpEndpointObserver observer = mock(MgcpEndpointObserver.class);

        // when
        final MgcpEndpoint endpoint = new MgcpSplitterEndpoint(context, fsm);
        endpoint.observe(observer);

        // then
        verify(splitter, times(0)).start();
        verify(oobSplitter, times(0)).start();
        verify(observer, times(0)).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);

        // when
        final int callId = 1;
        final int localConnectionId = 1;
        final AudioComponent localConnectionComponent = mock(AudioComponent.class);
        final OOBComponent localConnectionOobComponent = mock(OOBComponent.class);
        final MgcpConnection localConnection = mock(MgcpConnection.class);
        when(localConnection.getAudioComponent()).thenReturn(localConnectionComponent);
        when(localConnection.getOutOfBandComponent()).thenReturn(localConnectionOobComponent);
        when(localConnection.isLocal()).thenReturn(true);
        when(localConnection.getCallIdentifier()).thenReturn(callId);
        when(localConnection.getIdentifier()).thenReturn(localConnectionId);
        final FutureCallback<Void> registerCallback = mock(FutureCallback.class);

        endpoint.registerConnection(localConnection, registerCallback);

        // then
        verify(registerCallback, timeout(100)).onSuccess(null);
        verify(splitter).start();
        verify(splitter).addInsideComponent(localConnectionComponent);
        verify(oobSplitter).start();
        verify(oobSplitter).addInsideComponent(localConnectionOobComponent);
        verify(observer).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);
        
        // when
        final int remoteConnectionId = 2;
        final AudioComponent remoteConnectionComponent = mock(AudioComponent.class);
        final OOBComponent remoteConnectionOobComponent = mock(OOBComponent.class);
        final MgcpConnection remoteConnection = mock(MgcpConnection.class);
        when(remoteConnection.getAudioComponent()).thenReturn(remoteConnectionComponent);
        when(remoteConnection.getOutOfBandComponent()).thenReturn(remoteConnectionOobComponent);
        when(remoteConnection.isLocal()).thenReturn(false);
        when(remoteConnection.getCallIdentifier()).thenReturn(callId);
        when(remoteConnection.getIdentifier()).thenReturn(remoteConnectionId);
        final FutureCallback<Void> registerCallback2 = mock(FutureCallback.class);

        endpoint.registerConnection(remoteConnection, registerCallback2);
        
        // then
        verify(registerCallback2, timeout(100)).onSuccess(null);
        verify(splitter).start();
        verify(splitter).addOutsideComponent(remoteConnectionComponent);
        verify(oobSplitter).start();
        verify(oobSplitter).addOutsideComponent(remoteConnectionOobComponent);
        verify(observer).onEndpointStateChanged(endpoint, MgcpEndpointState.ACTIVE);

        // when
        final FutureCallback<MgcpConnection[]> unregisterCallback = mock(FutureCallback.class);

        endpoint.unregisterConnections(unregisterCallback);

        // then
        ArgumentCaptor<MgcpConnection[]> unregisterCaptor = ArgumentCaptor.forClass(MgcpConnection[].class);
        verify(unregisterCallback, timeout(100)).onSuccess(unregisterCaptor.capture());
        assertNotNull(unregisterCaptor.getValue());
        assertEquals(2, unregisterCaptor.getValue().length);
        verify(splitter, times(2)).stop(); // times(2) because IDLE is entered as initial state
        verify(splitter, times(1)).releaseInsideComponent(localConnectionComponent);
        verify(splitter, times(1)).releaseOutsideComponent(remoteConnectionComponent);
        verify(oobSplitter, times(2)).stop(); // times(2) because IDLE is entered as initial state
        verify(oobSplitter, times(1)).releaseInsideComponent(localConnectionOobComponent);
        verify(oobSplitter, times(1)).releaseOutsideComponent(remoteConnectionOobComponent);
        verify(observer, times(1)).onEndpointStateChanged(endpoint, MgcpEndpointState.IDLE);
    }

}
