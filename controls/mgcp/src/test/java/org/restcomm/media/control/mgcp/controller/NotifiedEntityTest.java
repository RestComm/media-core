/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.controller;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.control.mgcp.controller.NotifiedEntity;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class NotifiedEntityTest {
    
    private NotifiedEntity entity = new NotifiedEntity();
    
    public NotifiedEntityTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        long s = System.nanoTime();
        entity.setValue(new Text("mscontrol@127.0.0.1:2727"));
        long f = System.nanoTime();
        
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getValue method, of class NotifiedEntity.
     */
    @Test
    public void testGetValue() {
        assertEquals("mscontrol@127.0.0.1:2727", entity.getValue().toString());
    }

    /**
     * Test of getLocalName method, of class NotifiedEntity.
     */
    @Test
    public void testGetLocalName() {
        assertEquals("mscontrol", entity.getLocalName().toString());
    }

    /**
     * Test of getDomainName method, of class NotifiedEntity.
     */
    @Test
    public void testGetHostName() {
        assertEquals("127.0.0.1", entity.getHostName().toString());
    }

    /**
     * Test of getPort method, of class NotifiedEntity.
     */
    @Test
    public void testGetPort() {
        assertEquals(2727, entity.getPort());
    }
}
