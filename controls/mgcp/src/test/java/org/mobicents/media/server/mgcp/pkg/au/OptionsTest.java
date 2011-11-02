/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.mgcp.pkg.au;

import java.util.Collection;
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
        Options options = new Options(params);
        
        Collection<Text> segments = options.getSegments();
        assertTrue(segments != null);
    }

}
