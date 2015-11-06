/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.controller;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.mgcp.controller.signal.MgcpPackage;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionFailureListener;
import org.mobicents.media.server.spi.ConnectionListener;
import org.mobicents.media.server.spi.ConnectionMode;
import org.mobicents.media.server.spi.ConnectionState;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.Endpoint;
import org.mobicents.media.server.utils.Text;

/**
 * @author kulikov
 */
public class MgcpEndpointTest {
    
    private MyTestEndpoint endpoint;
    private CallManager callManager;
    private MgcpEndpoint mgcpEndpoint;
    
    @Before
    public void setUp() {
        endpoint = new MyTestEndpoint("test");
        mgcpEndpoint = new MgcpEndpoint(endpoint, null, "localhost", 2727, new ArrayList<MgcpPackage>());        
        callManager = new CallManager();
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
        for (int i = 0; i < 1000; i++) {
            MgcpConnection mgcpConnection = mgcpEndpoint.poll(call);
            mgcpConnection.wrap(call, null, new ConnectionMock(1));
            assertEquals(1, call.size());
            
            mgcpEndpoint.offer(mgcpConnection);
            assertEquals(0, call.size());            
        }    	
    }
    
    private class ConnectionMock implements Connection {
        
        int id;
        
        public ConnectionMock(int id) {
            this.id = id;
        }

        @Override
        public void checkOut() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void checkIn() {
            // TODO Auto-generated method stub
            
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String getTextualId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ConnectionType getType() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean getIsLocal() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        public void setIsLocal(boolean isLocal) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public ConnectionState getState() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ConnectionMode getMode() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public void setMode(ConnectionMode mode) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setEndpoint(Endpoint endpoint) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public Endpoint getEndpoint() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getLocalDescriptor() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public String getRemoteDescriptor() {
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
        public void addListener(ConnectionListener listener) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void setConnectionFailureListener(ConnectionFailureListener connectionFailureListener) {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void removeListener(ConnectionListener listener) {
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
        public void bind() throws IllegalStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void open() throws IllegalStateException {
            // TODO Auto-generated method stub
            
        }

        @Override
        public void close() throws IllegalStateException {
            // TODO Auto-generated method stub
            
        }}
    

}
