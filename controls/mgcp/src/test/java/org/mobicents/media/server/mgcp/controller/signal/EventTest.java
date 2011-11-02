/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.controller.signal;

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
public class EventTest {
    
    public EventTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of matches method, of class Event.
     */
    @Test
    public void testMatches() {
        Event oc = new Event(new Text("oc"));
        Text descriptor = new Text("oc(N)");
        
        assertFalse("Event should not match", oc.matches(descriptor));
        assertFalse("Event must be inactive", oc.isActive());
        
        oc.add(new DummyAction("N"));
        assertTrue("Event should match", oc.matches(descriptor));        
        assertTrue("Event must be active", oc.isActive());
    }
    
    private class DummyAction extends EventAction {

        public DummyAction(String name) {
            super(name);
        }
        
        @Override
        public void perform(Signal signal, Event event, Text options) {
        }
        
    }
}
