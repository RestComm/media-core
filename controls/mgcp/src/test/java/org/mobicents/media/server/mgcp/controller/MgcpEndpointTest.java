/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.controller;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.core.endpoints.BaseEndpointImpl;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 */
public class MgcpEndpointTest {
    
    private MyTestEndpoint endpoint;
    private MgcpEndpoint mgcpEndpoint;
    
    @Before
    public void setUp() {
        endpoint = new MyTestEndpoint("test");
        mgcpEndpoint = new MgcpEndpoint(endpoint, null, "localhost", 2727, new ArrayList<MgcpPackage>());        
    }

    /**
     * Test of getName method, of class MgcpEndpoint.
     */
    @Test
    public void testGetName() {
        assertEquals("test", mgcpEndpoint.getName());
    }

    /**
     * Test of getFullName method, of class MgcpEndpoint.
     */
    @Test
    public void testGetFullName() {
        assertEquals("test@localhost:2727", mgcpEndpoint.getFullName().toString());
    }

    /**
     * Test of getEndpoint method, of class MgcpEndpoint.
     */
    @Test
    public void testGetEndpoint() {
        assertEquals(endpoint, mgcpEndpoint.getEndpoint());
    }

    /**
     * Test of lock method, of class MgcpEndpoint.
     */
    @Test
    public void testLock() {
        assertEquals(mgcpEndpoint.getState(), MgcpEndpoint.STATE_FREE);
        mgcpEndpoint.lock();
        assertEquals(mgcpEndpoint.getState(), MgcpEndpoint.STATE_LOCKED);
    }

    /**
     * Test of share method, of class MgcpEndpoint.
     */
    @Test
    public void testShare() {
        assertEquals(mgcpEndpoint.getState(), MgcpEndpoint.STATE_FREE);
        mgcpEndpoint.lock();
        mgcpEndpoint.share();
        assertEquals(mgcpEndpoint.getState(), MgcpEndpoint.STATE_FREE);
    }

    /**
     * Test of poll method, of class MgcpEndpoint.
     */
    @Test
    public void testPollAndOffer() {
        // Given
        Endpoint endpoint1 = new BaseEndpointMock("mobicents/bridge/1");
        MgcpEndpoint mgcpEndpoint1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());

        // When
        BaseConnectionMock connection1 = new BaseConnectionMock(1);
        ((BaseEndpointImpl) endpoint1).addConnection(connection1);
        MgcpConnection mgcpConnection = mgcpEndpoint1.poll();
        mgcpConnection.wrap(mgcpEndpoint1, connection1);

        // Then
        Assert.assertEquals(1, mgcpEndpoint1.countConnections());
        Assert.assertEquals(1, endpoint1.getActiveConnectionsCount());
        Assert.assertNotNull(endpoint1.getConnection(connection1.getId()));
        
        // When
        mgcpEndpoint1.deleteConnection(mgcpConnection.getID());
        
        // Then
        assertEquals(0, mgcpEndpoint.countConnections());
        Assert.assertEquals(0, endpoint1.getActiveConnectionsCount());
        Assert.assertNull(endpoint1.getConnection(connection1.getId()));
    }

}
