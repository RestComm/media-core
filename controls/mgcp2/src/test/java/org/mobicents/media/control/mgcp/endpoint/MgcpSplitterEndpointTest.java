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

package org.mobicents.media.control.mgcp.endpoint;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionProvider;
import org.mobicents.media.control.mgcp.connection.MgcpLocalConnection;
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.control.mgcp.exception.MgcpException;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioSplitter;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBSplitter;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpSplitterEndpointTest {

    @Test
    public void testOpenCloseRemoteConnection()
            throws MgcpConnectionException, MgcpCallNotFoundException, MgcpConnectionNotFound {
        // given
        final MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        final AudioSplitter inbandMixer = mock(AudioSplitter.class);
        final OOBSplitter outbandMixer = mock(OOBSplitter.class);
        final MgcpConnectionProvider connections = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/mock/1", "127.0.0.1");
        final MgcpSplitterEndpoint endpoint = new MgcpSplitterEndpoint(endpointId, inbandMixer, outbandMixer, connections, mediaGroup);

        // when - half open connection
        when(connections.provideRemote()).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(1);
        when(connection.isLocal()).thenReturn(false);
        when(connection.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        endpoint.createConnection(1, false);

        // then
        verify(inbandMixer, times(1)).addOutsideComponent(any(AudioComponent.class));
        verify(outbandMixer, times(1)).addOutsideComponent(any(OOBComponent.class));

        // when - close connection
        endpoint.deleteConnection(1, connection.getIdentifier());

        // then
        verify(inbandMixer, times(1)).releaseOutsideComponent(any(AudioComponent.class));
        verify(outbandMixer, times(1)).releaseOutsideComponent(any(OOBComponent.class));
    }

    @Test
    public void testOpenCloseLocalConnection() throws MgcpException {
        // given
        final MgcpLocalConnection connection = mock(MgcpLocalConnection.class);
        final AudioSplitter inbandSplitter = mock(AudioSplitter.class);
        final OOBSplitter outbandSplitter = mock(OOBSplitter.class);
        final MgcpConnectionProvider connections = mock(MgcpConnectionProvider.class);
        final MediaGroup mediaGroup = mock(MediaGroup.class);
        final EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/mock/1", "127.0.0.1");
        final MgcpSplitterEndpoint endpoint = new MgcpSplitterEndpoint(endpointId, inbandSplitter, outbandSplitter, connections, mediaGroup);

        // when - open connection and join it to secondary endpoint
        when(connections.provideLocal()).thenReturn(connection);
        when(connection.getIdentifier()).thenReturn(1);
        when(connection.isLocal()).thenReturn(true);
        when(connection.getMode()).thenReturn(ConnectionMode.SEND_RECV);
        endpoint.createConnection(1, true);

        // then
        verify(inbandSplitter, times(1)).addInsideComponent(any(AudioComponent.class));
        verify(outbandSplitter, times(1)).addInsideComponent(any(OOBComponent.class));

        // when - close connection
        endpoint.deleteConnection(1, connection.getIdentifier());

        // then
        verify(inbandSplitter, times(1)).releaseInsideComponent(any(AudioComponent.class));
        verify(outbandSplitter, times(1)).releaseInsideComponent(any(OOBComponent.class));
    }

}
