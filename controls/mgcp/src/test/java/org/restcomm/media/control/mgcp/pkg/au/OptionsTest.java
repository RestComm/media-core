/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.pkg.au;

import java.util.Collection;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.control.mgcp.pkg.au.Options;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class OptionsTest {
    
    public OptionsTest() {
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
     * Test of getSegments method, of class Options.
     */
    @Test
    public void testGetSegments() {
        Text params = new Text("an=null");
        Options options = Options.allocate(params);
        
        Collection<Text> segments = options.getSegments();
        assertTrue(segments != null);
        
        Options.recycle(options);
    }

    /**
     * Test of getSegments method, of class Options.
     */
    @Test
    public void testGetPositionKeys() {
    	Text params = new Text("dp=0| cb=true psk=1,fst psk=4,prv psk=2,cur psk=6,nxt psk=3,lst");
        Options options = Options.allocate(params);
        
        assertTrue(options.prevKeyValid());
        assertTrue(options.firstKeyValid());
        assertTrue(options.currKeyValid());
        assertTrue(options.nextKeyValid());
        assertTrue(options.lastKeyValid());
        
        Options.recycle(options);
    }
}
