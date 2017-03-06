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
package org.restcomm.media.control.mgcp.controller.naming;

import java.util.ArrayList;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.control.mgcp.controller.MgcpEndpoint;
import org.restcomm.media.control.mgcp.controller.MyTestEndpoint;
import org.restcomm.media.control.mgcp.controller.naming.EndpointQueue;
import org.restcomm.media.control.mgcp.controller.signal.MgcpPackage;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class EndpointQueueTest {
    
    private MyTestEndpoint endpoint1 = new MyTestEndpoint("mobicents/aap/1");
    private MyTestEndpoint endpoint2 = new MyTestEndpoint("mobicents/aap/2");
    private MyTestEndpoint endpoint3 = new MyTestEndpoint("mobicents/aap/3");
    
    private MgcpEndpoint ac1 = new MgcpEndpoint(endpoint1, null, "localhost", 2727, new ArrayList<MgcpPackage>());
    private MgcpEndpoint ac2 = new MgcpEndpoint(endpoint2, null, "localhost", 2727, new ArrayList<MgcpPackage>());
    private MgcpEndpoint ac3 = new MgcpEndpoint(endpoint3, null, "localhost", 2727, new ArrayList<MgcpPackage>());
   
    private EndpointQueue queue = new EndpointQueue();
    private MgcpEndpoint[] res = new MgcpEndpoint[10];
    
    public EndpointQueueTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    	queue.add(ac1);
        queue.add(ac2);
        queue.add(ac3);
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test for find all
     */
    @Test
    public void testFindAll() {
        int n = queue.find(new Text("*"), res);
        assertEquals(3, n);
    }

    /**
     * Test of find any
     */
    @Test
    public void testFindAny() {
        int n = queue.find(new Text("$"), res);
        assertEquals(1, n);
    }

    /**
     * Test of find any
     */
    @Test
    public void testFindAndLock() {
        //during this test queue returned endpoint will remain locked
        //so after three calls queue will contain only locked endpoints
        
        //(1)
        int n = queue.find(new Text("$"), res);
        assertEquals(1, n);
        
        //(2)
        n = queue.find(new Text("$"), res);
        assertEquals(1, n);
        
        //(3)
        n = queue.find(new Text("$"), res);
        assertEquals(1, n);
        
        //(4) expecting that all locked already
        n = queue.find(new Text("$"), res);
        assertEquals(0, n);
        
        //unlock any one endpoit and try search again
        ac3.share();
        n = queue.find(new Text("$"), res);
        assertEquals(1, n);
    }
 
    @Test
    public void testFindExactMatch() {
        int n = queue.find(new Text("1"), res);
        assertEquals(1, n);
    }
    
    @Test
    public void testWrongName() {
        int n = queue.find(new Text("100"), res);
        assertEquals(0, n);
    }
}
