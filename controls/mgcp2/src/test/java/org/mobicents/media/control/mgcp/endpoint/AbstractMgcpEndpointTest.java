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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mobicents.media.control.mgcp.connection.MgcpConnection;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionMode;
import org.mobicents.media.control.mgcp.connection.MgcpConnectionState;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class AbstractMgcpEndpointTest {

    private static String basicSdp;

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        URL resource = AbstractMgcpEndpointTest.class.getResource("basic-sdp.txt");
        byte[] data = Files.readAllBytes(Paths.get(resource.toURI()));
        basicSdp = new String(data);
    }

    @Ignore
    @Test
    public void testHalfOpenRemoteConnection() {
        // given
        AbstractMgcpEndpoint endpoint = new MgcpEndpointMock("ms/mock/1");

        // when - creating connection
        MgcpConnection connection = endpoint.createConnection(1, MgcpConnectionMode.SEND_RECV);

        // then
        assertEquals("ms/mock/1", endpoint.getEndpointId());
        assertNotNull(connection);
        assertEquals(MgcpConnectionMode.SEND_RECV, connection.getMode());
        assertEquals(MgcpConnectionState.HALF_OPEN, connection.getState());
        assertTrue(endpoint.isActive());
    }

    @Ignore
    @Test
    public void testOpenRemoteConnection() {
        // given
        AbstractMgcpEndpoint endpoint = new MgcpEndpointMock("ms/mock/1");

        // when - creating connection
        MgcpConnection connection = endpoint.createConnection(1, MgcpConnectionMode.SEND_ONLY, basicSdp);

        // then
        assertEquals("ms/mock/1", endpoint.getEndpointId());
        assertNotNull(connection);
        assertEquals(MgcpConnectionMode.SEND_ONLY, connection.getMode());
        assertEquals(MgcpConnectionState.OPEN, connection.getState());
        assertTrue(endpoint.isActive());
    }
    
    // TODO finish writing tests

    private static final class MgcpEndpointMock extends AbstractMgcpEndpoint {

        private int onConnectionCreated = 0;
        private int onConnectionDeleted = 0;
        private int onActivated = 0;
        private int onDeactivated = 0;

        public MgcpEndpointMock(String endpointId) {
            super(endpointId);
        }

        @Override
        protected void onConnectionCreated(MgcpConnection connection) {
            this.onConnectionCreated++;
        }

        @Override
        protected void onConnectionDeleted(MgcpConnection connection) {
            this.onConnectionDeleted++;
        }

        @Override
        protected void onActivated() {
            this.onActivated++;
        }

        @Override
        protected void onDeactivated() {
            this.onDeactivated++;
        }

    }

}
