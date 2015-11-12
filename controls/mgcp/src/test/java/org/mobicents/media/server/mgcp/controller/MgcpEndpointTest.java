/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.core.connections.BaseConnection;
import org.mobicents.media.server.component.audio.AudioComponent;
import org.mobicents.media.server.component.oob.OOBComponent;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
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
        
        mgcpEndpoint = new MgcpEndpoint(endpoint, null, "localhost", 2727, new ArrayList<MgcpPackage>());        
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
    	MgcpCall call = callManager.getCall(1, true);
    	MockConnection connection = new MockConnection(1);
        for (int i = 0; i < 1000; i++) {
            MgcpConnection mgcpConnection = mgcpEndpoint.poll(call);  
            mgcpConnection.connection = connection;
            assertEquals(1, call.size());
            
            mgcpEndpoint.offer(mgcpConnection);
            assertEquals(0, call.size());            
        }    	
    }
    
    private class MockConnection extends BaseConnection {
        
        public MockConnection(int id) {
            super(id, null);
        }

        @Override
        public ConnectionType getType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void generateOffer(boolean webrtc) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setOtherParty(Connection other) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setOtherParty(byte[] descriptor) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setOtherParty(Text descriptor) throws IOException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public long getPacketsReceived() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getBytesReceived() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getPacketsTransmitted() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public long getBytesTransmitted() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public double getJitter() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public boolean isAvailable() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void generateCname() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public String getCname() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public AudioComponent getAudioComponent() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public OOBComponent getOOBComponent() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void onClosed() {
            // TODO Auto-generated method stub
            
        }

        @Override
        protected void onFailed() {
            // TODO Auto-generated method stub
            
        }
        
    }

}
