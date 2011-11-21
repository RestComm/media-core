package org.mobicents.media.server.mgcp;

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


import java.net.SocketAddress;
import java.util.concurrent.ConcurrentHashMap;
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
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class MgcpProviderLoadTest {
    
    private Clock clock = new DefaultClock();
    
    private Scheduler scheduler;
    private UdpManager udpInterface;

    private MgcpProvider provider1, provider2;
    
    private Server server;
    
    private ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap();
    private ClientListener demux;
    
    private volatile int errorCount;
    private volatile int txID = 1;
    
    public MgcpProviderLoadTest() {
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
        
        udpInterface = new UdpManager(scheduler);
        udpInterface.setBindAddress("localhost");
        udpInterface.start();
        
        provider1 = new MgcpProvider("provider1", udpInterface, 1024, scheduler);
        provider2 = new MgcpProvider("provider2", udpInterface, 1025, scheduler);
        
        provider1.activate();
        provider2.activate();
        
        server = new Server(provider1);
        
        demux = new ClientListener();
        provider2.addListener(demux);    	
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
    public void testProcess() throws Exception {
    	for (int i = 0; i < 30; i++) {
            new Thread(new Client(provider2)).start();
            Thread.sleep(10);
        }
        
        Thread.sleep(5000);
        assertEquals(0, errorCount);        
    }

    private class Server implements MgcpListener {
        
        private MgcpProvider mgcpProvider;
        
        public Server(MgcpProvider mgcpProvider) {
            this.mgcpProvider = mgcpProvider;
            try {
                this.mgcpProvider.addListener(this);
            } catch (Exception e) {
            }
        }
        
        public void process(MgcpEvent event) {
            //CRCX request expected
            MgcpEvent evt = null;
            try {
                MgcpRequest request = (MgcpRequest) event.getMessage();
                if (!request.getCommand().equals(new Text("CRCX"))) {
                    errorCount++;
                }
                SocketAddress destination = event.getAddress();
                evt = provider1.createEvent(MgcpEvent.RESPONSE, destination);
                MgcpResponse resp = (MgcpResponse) evt.getMessage();

                resp.setResponseCode(200);
                resp.setTxID(request.getTxID());
                resp.setResponseString(new Text("Success"));

                try {
                    mgcpProvider.send(evt);
                } catch (Exception e) {                	
                }
            } finally {
                event.recycle();
                evt.recycle();
            }
        }
        
    }
    
    private class Client implements Runnable {
        private MgcpProvider mgcpProvider;
        private volatile boolean passed = false;
        
        public Client(MgcpProvider mgcpProvider) {
            this.mgcpProvider = mgcpProvider;
        }
        
        public void run() {
            MgcpEvent evt = null;

            try {
                InetSocketAddress destination = new InetSocketAddress("localhost", 1024);

                evt = provider1.createEvent(MgcpEvent.REQUEST, destination);
                MgcpRequest req = (MgcpRequest) evt.getMessage();

                req.setCommand(new Text("CRCX"));
                req.setTxID(txID++);
                req.setEndpoint(new Text("test@localhost"));
                req.setParameter(new Text("c"), new Text("abcd"));

                try {
                    mgcpProvider.send(evt);
                } catch (Exception e) {                	
                }

                clients.put(req.getTxID(), this);

                synchronized (this) {
                    try {
                        wait(5000);
                    } catch (InterruptedException e) {
                    }
                }

                if (!passed) {
                    errorCount++;
                }
            } finally {
                evt.recycle();
            }
        }
        
        public void terminate() {
            this.passed = true;
            synchronized(this) {
                notify();
            }
        }
    }
    
    private class ClientListener implements MgcpListener {

        public void process(MgcpEvent event) {
            MgcpResponse response = (MgcpResponse) event.getMessage();
            try {
                int txID = response.getTxID();

                Client client = clients.remove(txID);
                if (client != null) {
                    client.terminate();
                } else {
                    errorCount++;
                }
            } finally {
                event.recycle();
            }
        }
    
    }
}
