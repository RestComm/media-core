/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mobicents.media.server.announcement;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.mobicents.media.ComponentType;
import org.mobicents.media.server.component.DspFactoryImpl;
import org.mobicents.media.server.impl.rtp.RTPManager;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.DefaultClock;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.spi.Connection;
import org.mobicents.media.server.spi.ConnectionType;
import org.mobicents.media.server.spi.MediaType;
import org.mobicents.media.server.spi.ResourceUnavailableException;
import org.mobicents.media.server.spi.player.Player;

/**
 *
 * @author yulian oifa
 */
public class AnnouncementEndpointTest {
    
    private AnnouncementEndpoint aap;
    
    private Clock clock;
    private Scheduler scheduler;
    private RTPManager rtpManager;
    private UdpManager udpManager;
    private DspFactoryImpl dspFactory = new DspFactoryImpl();
            
    public AnnouncementEndpointTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() throws ResourceUnavailableException, IOException {
    	try
    	{
        clock = new DefaultClock();
        
        scheduler = new Scheduler();
        scheduler.setClock(clock);
        
        udpManager = new UdpManager(scheduler);
        udpManager.setBindAddress("localhost");
        udpManager.start();
        
        rtpManager = new RTPManager(udpManager);
        rtpManager.setBindAddress("127.0.0.1");
        rtpManager.setScheduler(scheduler);
        rtpManager.start();
        
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Encoder");
        dspFactory.addCodec("org.mobicents.media.server.impl.dsp.audio.g711.ulaw.Decoder");
        
        aap = new AnnouncementEndpoint("test");
        aap.setScheduler(scheduler);
        aap.setRtpManager(rtpManager);
        aap.setDspFactory(dspFactory);
        aap.start();
    	}
    	catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }
    
    @After
    public void tearDown() {
        aap.stop();
        
        rtpManager.stop();
        udpManager.stop();
        
        scheduler.stop();
    }

    /**
     * Test of getSink method, of class AnnouncementEndpoint.
     */
    @Test
    public void testRTPConnection() throws Exception {
    	try
    	{
        Connection connection = aap.createConnection(ConnectionType.RTP,false);
        assertTrue("Connection should not be null", connection != null);
        
        System.out.println(connection.getDescriptor());
    	}
        catch(Exception e)
    	{
    		e.printStackTrace();
    	}
    }

    @Test
    public void testGetResource() {
    	Player p = (Player) aap.getResource(MediaType.AUDIO, ComponentType.PLAYER);
        assertTrue(p != null);    	
    }
}
