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

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.BeforeClass;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public abstract class AbstractMgcpEndpointTestTemplate {

    private static String basicSdp;

    @BeforeClass
    public static void setup() throws IOException, URISyntaxException {
        ClassLoader loader = AbstractMgcpEndpointTestTemplate.class.getClassLoader();
        URL resource = loader.getResource("sdp/basic-sdp.txt");
        byte[] data = Files.readAllBytes(Paths.get(resource.toURI()));
        basicSdp = new String(data);
    }

    // protected void testHalfOpenRemoteConnection(AbstractMgcpEndpoint endpoint) throws MgcpConnectionException {
    // // when - creating connection
    // MgcpConnection connection = endpoint.createConnection(1, ConnectionMode.SEND_RECV);
    //
    // // then
    // assertEquals("restcomm/mock/1", endpoint.getEndpointId());
    // assertNotNull(connection);
    // assertEquals(ConnectionMode.SEND_RECV, connection.getMode());
    // assertEquals(MgcpConnectionState.HALF_OPEN, connection.getState());
    // assertTrue(endpoint.isActive());
    // }
    //
    // protected void testOpenRemoteConnection(AbstractMgcpEndpoint endpoint) throws MgcpConnectionException {
    // // when - creating connection
    // MgcpConnection connection = endpoint.createConnection(1, ConnectionMode.SEND_ONLY, basicSdp);
    //
    // // then
    // assertEquals("restcomm/mock/1", endpoint.getEndpointId());
    // assertNotNull(connection);
    // assertEquals(ConnectionMode.SEND_ONLY, connection.getMode());
    // assertEquals(MgcpConnectionState.OPEN, connection.getState());
    // assertTrue(endpoint.isActive());
    // }

    // TODO finish writing tests
}
