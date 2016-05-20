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
import org.mobicents.media.control.mgcp.connection.MgcpRemoteConnection;
import org.mobicents.media.control.mgcp.exception.MgcpCallNotFoundException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionException;
import org.mobicents.media.control.mgcp.exception.MgcpConnectionNotFound;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.audio.AudioMixer;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.component.oob.OOBMixer;
import org.mobicents.media.server.spi.ConnectionMode;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpMixerEndpointTest {

    @Test
    public void testOpenCloseConnection() throws MgcpConnectionException, MgcpCallNotFoundException, MgcpConnectionNotFound {
        // given
        MgcpConnectionProvider connectionProvider = mock(MgcpConnectionProvider.class);
        MgcpRemoteConnection connection = mock(MgcpRemoteConnection.class);
        AudioMixer inbandMixer = mock(AudioMixer.class);
        OOBMixer outbandMixer = mock(OOBMixer.class);
        MgcpMixerEndpoint endpoint = new MgcpMixerEndpoint("restcomm/mock/1", connectionProvider, inbandMixer, outbandMixer);

        // when - half open connection
        when(connection.getIdentifier()).thenReturn(1);
        when(connectionProvider.provideRemote()).thenReturn(connection);
        endpoint.createConnection(1, ConnectionMode.SEND_RECV);

        // then
        verify(inbandMixer, times(1)).addComponent(any(AudioComponent.class));
        verify(outbandMixer, times(1)).addComponent(any(OOBComponent.class));

        // when - close connection
        endpoint.deleteConnection(1, connection.getIdentifier());

        // then
        verify(inbandMixer, times(1)).release(any(AudioComponent.class));
        verify(outbandMixer, times(1)).release(any(OOBComponent.class));
    }

}
