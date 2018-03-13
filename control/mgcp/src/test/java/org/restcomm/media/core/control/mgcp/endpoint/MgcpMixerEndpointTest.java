/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2016, Telestax Inc and individual contributors
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

package org.restcomm.media.core.control.mgcp.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.restcomm.media.core.component.audio.AudioComponent;
import org.restcomm.media.core.component.audio.AudioMixer;
import org.restcomm.media.core.component.oob.OOBComponent;
import org.restcomm.media.core.component.oob.OOBMixer;
import org.restcomm.media.core.control.mgcp.connection.MgcpConnectionProvider;
import org.restcomm.media.core.control.mgcp.connection.MgcpRemoteConnection;
import org.restcomm.media.core.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.core.control.mgcp.endpoint.MediaGroup;
import org.restcomm.media.core.control.mgcp.endpoint.MediaGroupImpl;
import org.restcomm.media.core.control.mgcp.endpoint.MgcpMixerEndpoint;
import org.restcomm.media.core.control.mgcp.exception.MgcpCallNotFoundException;
import org.restcomm.media.core.control.mgcp.exception.MgcpConnectionException;
import org.restcomm.media.core.control.mgcp.exception.MgcpConnectionNotFoundException;
import org.restcomm.media.core.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpointTest {

    @Test
    public void testOpenCloseConnection() throws MgcpConnectionException, MgcpCallNotFoundException, MgcpConnectionNotFoundException {
        // given
        final int callId = 1;
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final AudioMixer inbandMixer = mock(AudioMixer.class);
        final OOBMixer outbandMixer = mock(OOBMixer.class);
        final MgcpConnectionProvider connections = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroupImpl.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/mock/1", "127.0.0.1:2427");
        final MgcpMixerEndpoint endpoint = new MgcpMixerEndpoint(endpointId, inbandMixer, outbandMixer, connections, mediaGroup);

        // when - half open connection
        when(connections.provideRemote(callId)).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(1);
        when(connection.getCallIdentifier()).thenReturn(callId);
        when(connection.getMode()).thenReturn(ConnectionMode.SEND_RECV);

        endpoint.createConnection(callId, false);

        // then
        // 2 components are registered: one for the connection, another for the media group of the endpoint upon activation
        verify(inbandMixer, times(2)).addComponent(any(AudioComponent.class));
        verify(outbandMixer, times(2)).addComponent(any(OOBComponent.class));

        // when - close connection
        endpoint.deleteConnection(callId, connection.getIdentifier());

        // then
        verify(inbandMixer, times(2)).release(any(AudioComponent.class));
        verify(outbandMixer, times(2)).release(any(OOBComponent.class));
    }

}
