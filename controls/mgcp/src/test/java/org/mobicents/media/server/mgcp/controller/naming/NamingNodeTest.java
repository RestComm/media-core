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
package org.mobicents.media.server.mgcp.controller.naming;

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
public class NamingNodeTest {
    
    private NamingNode<String> root;
    
    public NamingNodeTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        root = new NamingNode(new Text("root"), null);
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getNode method, of class NamingNode.
     */
    @Test
    public void testGetName() {
        assertEquals("root", root.getName().toString());
    }

    /**
     * Test of createChild method, of class NamingNode.
     */
    @Test
    public void testCreateChild() {
        NamingNode child = root.createChild(new Text("child"));
        assertEquals("child", child.getName().toString());
        assertEquals(root, child.getParent());
    }
    
    @Test
    public void testAttachment() {
        root.attach("Attachment");
        assertEquals("Attachment", root.poll());
    }
    
    @Test
    public void testFind() {
        Text token1 = new Text("mobicents");
        Text token2 = new Text("media");
        Text token3 = new Text("endpoint");
        
        Text[] path = new Text[] {token1, token2, token3};
        
        NamingNode node1 = root.createChild(token1);
        NamingNode node2 = node1.createChild(token2);
        NamingNode node3 = node2.createChild(token3);
        
        NamingNode res = root.find(path, 3);
        assertEquals(node3, res);

        //checking duration
        long s = System.nanoTime();
        res = root.find(path, 3);
        long f = System.nanoTime();
        
        System.out.println("Duration=" + (f-s));
    }
}
