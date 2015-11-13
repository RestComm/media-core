/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2015, Telestax Inc and individual contributors
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

package org.mobicents.media.server.mgcp.controller;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.spi.Endpoint;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MgcpCallTest {

    @Test
    public void testAddEndpoint() {
        // Given
        CallManager callManager = new CallManager();
        MgcpCall mgcpCall = callManager.createCall(1);

        Endpoint endpoint1 = new BaseEndpointMock("/mobicents/bridge/1");
        Endpoint endpoint2 = new BaseEndpointMock("/mobicents/bridge/2");
        Endpoint endpoint3 = new BaseEndpointMock("/mobicents/bridge/3");

        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());
        MgcpEndpoint mgcpEndpoint2 = new MgcpEndpoint(endpoint2, null, "localhost", 2727, new ArrayList<MgcpPackage>());
        MgcpEndpoint mgcpEndpoint3 = new MgcpEndpoint(endpoint3, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        mgcpCall.addEndpoint(mgcpEndpoint1);
        mgcpCall.addEndpoint(mgcpEndpoint2);
        mgcpCall.addEndpoint(mgcpEndpoint3);

        // Then
        Assert.assertEquals(3, mgcpCall.countEndpoints());
        Assert.assertEquals(mgcpEndpoint1, mgcpCall.getMgcpEndpoint(mgcpEndpoint1.getName()));
        Assert.assertEquals(mgcpEndpoint2, mgcpCall.getMgcpEndpoint(mgcpEndpoint2.getName()));
        Assert.assertEquals(mgcpEndpoint3, mgcpCall.getMgcpEndpoint(mgcpEndpoint3.getName()));
    }

    @Test
    public void testDeleteEndpoint() {
        // Given
        CallManager callManager = new CallManager();
        MgcpCall mgcpCall = callManager.createCall(1);

        Endpoint endpoint1 = new BaseEndpointMock("/mobicents/bridge/1");
        Endpoint endpoint2 = new BaseEndpointMock("/mobicents/bridge/2");

        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());
        MgcpEndpoint mgcpEndpoint2 = new MgcpEndpoint(endpoint2, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        mgcpCall.addEndpoint(mgcpEndpoint1);
        mgcpCall.addEndpoint(mgcpEndpoint2);
        mgcpCall.removeEndpoint(mgcpEndpoint1.getName());

        // Then
        Assert.assertEquals(1, mgcpCall.countEndpoints());
        Assert.assertNull(mgcpCall.getMgcpEndpoint(mgcpEndpoint1.getName()));
        Assert.assertEquals(mgcpEndpoint2, mgcpCall.getMgcpEndpoint(mgcpEndpoint2.getName()));
        Assert.assertNotNull(callManager.getCall(mgcpCall.getId()));
    }

    @Test
    public void testDeleteUnknownEndpoint() {
        // Given
        CallManager callManager = new CallManager();
        MgcpCall mgcpCall = callManager.createCall(1);
        Endpoint endpoint1 = new BaseEndpointMock("/mobicents/bridge/1");
        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        mgcpCall.addEndpoint(mgcpEndpoint1);
        MgcpEndpoint removedEndpoint = mgcpCall.removeEndpoint("/mobicents/bridge/2");

        // Then
        Assert.assertEquals(1, mgcpCall.countEndpoints());
        Assert.assertNull(removedEndpoint);
    }

    @Test
    public void testDeleteEndpoints() {
        // Given
        CallManager callManager = new CallManager();
        MgcpCall mgcpCall = callManager.createCall(1);

        Endpoint endpoint1 = new BaseEndpointMock("/mobicents/bridge/1");
        Endpoint endpoint2 = new BaseEndpointMock("/mobicents/bridge/2");
        Endpoint endpoint3 = new BaseEndpointMock("/mobicents/bridge/3");

        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());
        MgcpEndpoint mgcpEndpoint2 = new MgcpEndpoint(endpoint2, null, "localhost", 2727, new ArrayList<MgcpPackage>());
        MgcpEndpoint mgcpEndpoint3 = new MgcpEndpoint(endpoint3, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        mgcpCall.addEndpoint(mgcpEndpoint1);
        mgcpCall.addEndpoint(mgcpEndpoint2);
        mgcpCall.addEndpoint(mgcpEndpoint3);
        mgcpCall.removeEndpoints();

        // Then
        Assert.assertEquals(0, mgcpCall.countEndpoints());
        Assert.assertNull(callManager.getCall(mgcpCall.getId()));
    }

    @Test
    public void testDeleteCascadeConnection() {
        // Given
        CallManager callManager = new CallManager();
        MgcpCall mgcpCall = callManager.createCall(1);
        Endpoint endpoint1 = new BaseEndpointMock("/mobicents/bridge/1");
        ((BaseEndpointImpl) endpoint1).addConnection(new BaseConnectionMock(1));
        ((BaseEndpointImpl) endpoint1).addConnection(new BaseConnectionMock(2));
        ((BaseEndpointImpl) endpoint1).addConnection(new BaseConnectionMock(3));
        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        mgcpCall.addEndpoint(mgcpEndpoint1);
        mgcpCall.removeEndpoint(mgcpEndpoint1.getName());

        // Then
        Assert.assertEquals(0, mgcpCall.countEndpoints());
        Assert.assertEquals(0, endpoint1.getActiveConnectionsCount());
    }

}
