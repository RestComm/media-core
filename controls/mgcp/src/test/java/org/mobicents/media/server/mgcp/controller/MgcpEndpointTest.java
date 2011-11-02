/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.controller;

import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.utils.Text;

/**
 *
 * @author kulikov
 */
public class MgcpEndpointTest {
    
    private MyTestEndpoint endpoint;
    private CallManager callManager;
    private MgcpEndpoint mgcpEndpoint;
    
    public MgcpEndpointTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        endpoint = new MyTestEndpoint("test");
        mgcpEndpoint = new MgcpEndpoint(endpoint, null, "localhost", 2727, new ArrayList());
        
        callManager = new CallManager();
    }
    
    @After
    public void tearDown() {
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
        MgcpCall call = callManager.getCall(new Text("1"), true);
        for (int i = 0; i < 1000; i++) {
            MgcpConnection mgcpConnection = mgcpEndpoint.poll(call);            
            assertEquals(1, call.size());
            
            mgcpEndpoint.offer(mgcpConnection);
            assertEquals(0, call.size());            
        }
    }

}
