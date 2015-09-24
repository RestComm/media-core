/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat, Inc. and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp;

import org.mobicents.media.server.mgcp.message.MgcpResponse;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Clock;
import java.net.InetSocketAddress;
import org.mobicents.media.server.spi.listener.TooManyListenersException;
import org.mobicents.media.server.utils.Text;
import org.mobicents.media.server.mgcp.message.MgcpRequest;
import java.io.IOException;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Scheduler;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.mgcp.message.Parameter;
import static org.junit.Assert.*;

/**
 *
 * @author oifa yulian
 */
public class MgcpProviderTest {
    
    private Clock clock = new DefaultClock();
    
    private Scheduler scheduler;
    private UdpManager udpInterface;

    private MgcpProvider provider1, provider2, provider3, provider4;
    
    private RequestTester reqTester;
    private ResponseTester respTester;
    
    InetSocketAddress destination,destination2;
    
    public MgcpProviderTest() {    	
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws IOException, TooManyListenersException {
    	scheduler = new Scheduler();
        scheduler.setClock(clock);
        scheduler.start();
        
        udpInterface = new UdpManager();
        udpInterface.setLocalBindAddress("127.0.0.1");
        udpInterface.setBindAddress("127.0.0.1");
        udpInterface.start();
        
        destination = new InetSocketAddress("127.0.0.1", 1029);
        
        provider1 = new MgcpProvider(udpInterface, 1027, scheduler);
        provider2 = new MgcpProvider(udpInterface, 1029, scheduler);
        
        provider1.activate();
        provider2.activate();
        
        destination2 = new InetSocketAddress("127.0.0.1", 1033);
        
        reqTester = new RequestTester();
        respTester = new ResponseTester();
    }
    
    @After
    public void tearDown() {
        if (provider1 != null) provider1.shutdown();
        if (provider2 != null) provider2.shutdown();        
        
        udpInterface.stop();
        scheduler.stop();
    }

    /**
     * Test of createEvent method, of class MgcpProvider.
     */
    @Test
    public void testRequest() throws Exception {
        provider2.addListener(reqTester);
        
        MgcpEvent evt = provider1.createEvent(MgcpEvent.REQUEST, destination);
        MgcpRequest req = (MgcpRequest) evt.getMessage();
        
        req.setCommand(new Text("CRCX"));
        req.setTxID(1);
        req.setEndpoint(new Text("test@127.0.0.1"));
        req.setParameter(new Text("c"), new Text("abcd"));
        
        provider1.send(evt, destination);
        
        Thread.sleep(100);        
        assertTrue("Problems", reqTester.success);
    }

    public void doSendReceive() throws Exception {
        MgcpEvent evt = provider1.createEvent(MgcpEvent.REQUEST, destination);
        MgcpRequest req = (MgcpRequest) evt.getMessage();
        
        req.setCommand(new Text("CRCX"));
        req.setTxID(1);
        req.setEndpoint(new Text("test@127.0.0.1"));
        req.setParameter(new Text("c"), new Text("abcd"));
        
        provider1.send(evt);
        evt.recycle();
        
        Thread.sleep(100);
    }
    
    @Test
    public void testRequest1() throws Exception {
        provider2.addListener(reqTester);
        for (int i = 0; i < 20; i++) {
            System.out.println("Test #" + i);
            doSendReceive();
        }
            doSendReceive();
    }
    
    @Test
    public void testResponse() throws Exception {
    	provider3 = new MgcpProvider(udpInterface, 1031, scheduler);
        provider4 = new MgcpProvider(udpInterface, 1033, scheduler);
        
        provider3.activate();
        provider4.activate();
        
        provider4.addListener(respTester);
        
        MgcpEvent evt = provider3.createEvent(MgcpEvent.RESPONSE, destination2);
        MgcpResponse resp = (MgcpResponse) evt.getMessage();
        
        resp.setResponseCode(200);
        resp.setTxID(1);
        resp.setResponseString(new Text("Success"));
        
        provider3.send(evt, destination2);
        
        Thread.sleep(100);
        
        assertTrue("Problems", respTester.success);
        
        if (provider3 != null) provider3.shutdown();
        if (provider4 != null) provider4.shutdown();
        
    }
    
    private class RequestTester implements MgcpListener {

        protected boolean success = false;
        
        public void process(MgcpEvent event) {
            try {
                success = event.getEventID() == MgcpEvent.REQUEST;
                
                MgcpRequest req = (MgcpRequest) event.getMessage();
                
                success &= req.getCommand().toString().equalsIgnoreCase("crcx");
                success &= req.getTxID() == 1;
                success &= req.getEndpoint().toString().equalsIgnoreCase("test@127.0.0.1");
                success &= req.getParameter(Parameter.CALL_ID).getValue().toString().equals("abcd");
                
            } finally {
                event.recycle();
            }
        }
        
    }
    
    private class ResponseTester implements MgcpListener {

        protected boolean success = false;
        
        public void process(MgcpEvent event) {
            try {
            	success = event.getEventID() == MgcpEvent.RESPONSE;
                MgcpResponse resp = (MgcpResponse) event.getMessage();
                success &= resp.getResponseCode() == 200;
                success &= resp.getTxID() == 1;                
            }
            
            finally {
                event.recycle();
            }
        }
        
    }
}
