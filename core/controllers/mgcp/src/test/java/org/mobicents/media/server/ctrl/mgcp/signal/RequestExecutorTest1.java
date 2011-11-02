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

package org.mobicents.media.server.ctrl.mgcp.signal;

import jain.protocol.ip.mgcp.message.parms.EventName;
import jain.protocol.ip.mgcp.message.parms.RequestedEvent;
import jain.protocol.ip.mgcp.pkg.MgcpEvent;
import jain.protocol.ip.mgcp.pkg.PackageName;
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.ctrl.mgcp.UnknownActivityException;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 */
public class RequestExecutorTest1 implements Dispatcher {
    private RequestExecutors executors;
    
    private RequestExecutor executor1;
    private RequestExecutor executor2;
    
    private DispatcherImpl dispatcher1;
    private DispatcherImpl dispatcher2;
    
    private RequestedEvent request1;
    private RequestedEvent request2;
    private RequestedEvent request3;
    
    private int count;
    
    private Logger logger = Logger.getLogger(RequestExecutorTest1.class);
    
    public RequestExecutorTest1() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        count = 0;
        executors = new RequestExecutors(3);
        
        executor1 = executors.poll();
        executor2 = executors.poll();
        
        dispatcher1 = new DispatcherImpl(new EventName(PackageName.factory("T"), MgcpEvent.factory("test1")));
        dispatcher2 = new DispatcherImpl(new EventName(PackageName.factory("T"), MgcpEvent.factory("test1")));

        executor1.setDispatcher(dispatcher1);
        executor2.setDispatcher(dispatcher2);
        
        request1 = new RequestedEvent(new EventName(PackageName.factory("T"), MgcpEvent.factory("test1")));
        request2 = new RequestedEvent(new EventName(PackageName.factory("T"), MgcpEvent.factory("test2")));
        request3 = new RequestedEvent(new EventName(PackageName.factory("T"), MgcpEvent.factory("test3")));
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setDispatcher method, of class RequestExecutor.
     */
    @Test
    public void testDispatcher() throws Exception {
        executor1.accept(new RequestedEvent[]{request1}, null);
        executor1.execute();

        executor2.accept(new RequestedEvent[]{request1}, null);
        executor2.execute();
        
        Thread.currentThread().sleep(3000);   
        
        assertTrue("Event not detected", dispatcher1.isSuccess());
        assertTrue("Event not detected", dispatcher2.isSuccess());
    }

    public void onEvent(EventName event) {
        logger.info("Receive " + event);
        count++;
    }

    public void completed() {
    }

    public Endpoint getEndpoint() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Connection getConnection(String ID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class DispatcherImpl implements Dispatcher {
        
        private EventName expected;
        private boolean success = false;
        
        public DispatcherImpl(EventName expected) {
            this.expected = expected;
        }
        
        public boolean isSuccess() {
            return this.success;
        }
        
        public void onEvent(EventName event) {
            System.out.println("Receive event: " + event);
            if (event.getEventIdentifier().getName().equals(expected.getEventIdentifier().getName())) {
                success = true;
            }
        }

        public void completed() {
        }

        public Endpoint getEndpoint() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Connection getConnection(String ID) throws UnknownActivityException {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }
}