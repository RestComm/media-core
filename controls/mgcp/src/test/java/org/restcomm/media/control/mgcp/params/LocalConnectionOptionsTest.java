/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.restcomm.media.control.mgcp.params;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

import org.restcomm.media.control.mgcp.params.LocalConnectionOptions;
import org.restcomm.media.spi.utils.Text;

/**
 *
 * @author kulikov
 */
public class LocalConnectionOptionsTest {
    
    private LocalConnectionOptions lcOptions = new LocalConnectionOptions();
    
    public LocalConnectionOptionsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
        lcOptions.setValue(new Text("gc:10, a:PCMU"));
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of getGain method, of class LocalConnectionOptions.
     */
    @Test
    public void testGetGain() {
        assertEquals(10, lcOptions.getGain());
    }
}
