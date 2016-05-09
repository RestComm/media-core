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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpoint;
import org.mobicents.media.control.mgcp.endpoint.MgcpEndpointManager;
import org.mobicents.media.control.mgcp.endpoint.AbstractMgcpEndpointProvider;
import org.mobicents.media.control.mgcp.exception.UnrecognizedMgcpNamespaceException;

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

    @Test(expected=IllegalArgumentException.class)
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
    public void testRegisterEndpoint() throws UnrecognizedMgcpNamespaceException {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        MgcpEndpoint bridgeEndpoint = mock(MgcpEndpoint.class);
        AbstractMgcpEndpointProvider<MgcpEndpoint> bridgeProvider = mock(AbstractMgcpEndpointProvider.class);

        // when
        when(bridgeProvider.getNamespace()).thenReturn(NAMESPACE_BRIDGE);
        when(bridgeProvider.provide()).thenReturn(bridgeEndpoint);
        when(bridgeEndpoint.getEndpointId()).thenReturn(NAMESPACE_BRIDGE + "1");
        endpointManager.installProvider(bridgeProvider);
        MgcpEndpoint endpoint = endpointManager.registerEndpoint(NAMESPACE_BRIDGE);

        // then
        assertEquals(bridgeEndpoint, endpoint);
        assertEquals(bridgeEndpoint, endpointManager.getEndpoint(bridgeEndpoint.getEndpointId()));
    }

    @Test(expected=UnrecognizedMgcpNamespaceException.class)
    public void testRegisterUnknownEndpoint() throws UnrecognizedMgcpNamespaceException {
        // given
        MgcpEndpointManager endpointManager = new MgcpEndpointManager();
        
        // when
        endpointManager.registerEndpoint(NAMESPACE_BRIDGE);
    }

}
