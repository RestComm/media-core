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

package org.restcomm.media.control.mgcp.endpoint;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.net.InetSocketAddress;

import org.junit.Test;
import org.restcomm.media.control.mgcp.endpoint.EndpointIdentifier;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpoint;
import org.restcomm.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.restcomm.media.control.mgcp.endpoint.provider.AbstractMgcpEndpointProvider;
import org.restcomm.media.control.mgcp.exception.MgcpEndpointNotFoundException;
import org.restcomm.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;
import org.restcomm.media.control.mgcp.message.MessageDirection;
import org.restcomm.media.control.mgcp.message.MgcpMessage;
import org.restcomm.media.control.mgcp.message.MgcpMessageObserver;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpEndpointManagerTest {

    private static final String NAMESPACE_BRIDGE = "mobicents/bridge/";
    private static final String NAMESPACE_IVR = "mobicents/ivr/";
    private static final String NAMESPACE_CNF = "mobicents/cnf/";

    @Test
    public void testInstallProvider() {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        AbstractMgcpEndpointProvider<?> bridgeProvider = mock(AbstractMgcpEndpointProvider.class);
        AbstractMgcpEndpointProvider<?> ivrProvider = mock(AbstractMgcpEndpointProvider.class);

        // when
        when(bridgeProvider.getNamespace()).thenReturn(NAMESPACE_BRIDGE);
        when(ivrProvider.getNamespace()).thenReturn(NAMESPACE_IVR);

        endpointManager.installProvider(bridgeProvider);
        endpointManager.installProvider(ivrProvider);

        // then
        assertTrue(endpointManager.supportsNamespace(NAMESPACE_BRIDGE));
        assertTrue(endpointManager.supportsNamespace(NAMESPACE_IVR));
        assertFalse(endpointManager.supportsNamespace(NAMESPACE_CNF));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInstallDuplicateProvider() {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        AbstractMgcpEndpointProvider<?> bridgeProvider1 = mock(AbstractMgcpEndpointProvider.class);
        AbstractMgcpEndpointProvider<?> bridgeProvider2 = mock(AbstractMgcpEndpointProvider.class);

        // when
        when(bridgeProvider1.getNamespace()).thenReturn(NAMESPACE_BRIDGE);
        when(bridgeProvider2.getNamespace()).thenReturn(NAMESPACE_BRIDGE);

        endpointManager.installProvider(bridgeProvider1);
        endpointManager.installProvider(bridgeProvider2);
    }

    @Test
    public void testUninstallProvider() {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        AbstractMgcpEndpointProvider<?> bridgeProvider = mock(AbstractMgcpEndpointProvider.class);
        AbstractMgcpEndpointProvider<?> ivrProvider = mock(AbstractMgcpEndpointProvider.class);

        // when
        when(bridgeProvider.getNamespace()).thenReturn(NAMESPACE_BRIDGE);
        when(ivrProvider.getNamespace()).thenReturn(NAMESPACE_IVR);

        endpointManager.installProvider(bridgeProvider);
        endpointManager.installProvider(ivrProvider);
        endpointManager.uninstallProvider(NAMESPACE_IVR);

        // then
        assertTrue(endpointManager.supportsNamespace(NAMESPACE_BRIDGE));
        assertFalse(endpointManager.supportsNamespace(NAMESPACE_IVR));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testEndpointRegistration() throws UnrecognizedMgcpNamespaceException, MgcpEndpointNotFoundException {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        EndpointIdentifier endpointId = new EndpointIdentifier("mobicents/bridge/1", "127.0.0.1:2427");
        AbstractMgcpEndpointProvider<MgcpEndpoint> bridgeProvider = mock(AbstractMgcpEndpointProvider.class);

        // when
        when(bridgeProvider.getNamespace()).thenReturn(NAMESPACE_BRIDGE);
        when(bridgeProvider.getDomain()).thenReturn("127.0.0.1:2427");
        when(bridgeProvider.provide()).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getEndpointId()).thenReturn(endpointId);

        endpointManager.installProvider(bridgeProvider);
        MgcpEndpoint endpoint = endpointManager.registerEndpoint(NAMESPACE_BRIDGE);

        // then
        assertEquals(bridgeEndpoint, endpoint);
        assertEquals(bridgeEndpoint, endpointManager.getEndpoint(bridgeEndpoint.getEndpointId().toString()));
        // TODO Fix me!!
//        verify(bridgeEndpoint, times(1)).observe(endpointManager);

        // when
        endpointManager.unregisterEndpoint(bridgeEndpoint.getEndpointId().toString());

        // then
        assertNull(endpointManager.getEndpoint(bridgeEndpoint.getEndpointId().toString()));
        // TODO Fix me!!
//        verify(bridgeEndpoint, times(1)).forget(endpointManager);
    }

    @Test(expected = UnrecognizedMgcpNamespaceException.class)
    public void testRegisterUnknownEndpoint() throws UnrecognizedMgcpNamespaceException {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();

        // when
        endpointManager.registerEndpoint(NAMESPACE_BRIDGE);
    }

    @Test
    public void testMessagePropagation() {
        // given
        final InetSocketAddress from = new InetSocketAddress("127.0.0.1", 2727);
        final InetSocketAddress to = new InetSocketAddress("127.0.0.1", 2427);
        final MgcpMessageObserver observer = mock(MgcpMessageObserver.class);
        final MgcpMessage message = mock(MgcpMessage.class);
        final MgcpEndpointManager endpointManager = new MgcpEndpointManager();

        // when
        endpointManager.observe(observer);
        endpointManager.onMessage(from, to, message, MessageDirection.OUTGOING);

        // then
        verify(observer, times(1)).onMessage(from, to, message, MessageDirection.OUTGOING);

    }
}
