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
import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.Endpoint;

/**
 *
 * @author kulikov
 */
public class RequestExecutorsTest implements Dispatcher {
    private RequestExecutors executors;
    private RequestExecutor executor;
    
    private int count;    
    private Logger logger = Logger.getLogger(RequestExecutorsTest.class);
    
    public RequestExecutorsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        logger.info("Preloading executors....");
        count = 0;
        executors = new RequestExecutors(3);        
        logger.info("Done");
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of setDispatcher method, of class RequestExecutor.
     */
    @Test
    public void testPollAndRecycle() throws Exception {
        logger.info("Polling executors and finally recycle executor");
        assertEquals(3, executors.remainder());
        
        //poll and check remainder
        executor = executors.poll();
        assertEquals(2, executors.remainder());
        
        executor.setDispatcher(this);
        executor.execute();
        assertEquals(1, count);
        
        //recyle and check remainder
        executor.recycle();
        assertEquals(3, executors.remainder());

        //do it once again
        executor = executors.poll();
        assertEquals(2, executors.remainder());
        
        executor.execute();
        assertEquals(1, count);
        
        executor.recycle();
        assertEquals(3, executors.remainder());
        
    }


    
    public void onEvent(EventName event) {
        logger.info("Receive " + event);
        count++;
    }

    public void completed() {
        count++;
    }

    public Endpoint getEndpoint() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public Connection getConnection(String ID) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


}