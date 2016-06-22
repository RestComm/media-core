/**
 * 
 */
package org.mobicents.media.server.impl.rtp.sdp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mobicents.media.server.impl.rtp.ChannelsManager;
import org.mobicents.media.server.impl.rtp.channels.AudioChannel;
import org.mobicents.media.server.io.network.UdpManager;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SessionDescription;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.parser.MediaDescriptionFieldParser;
import org.mobicents.media.server.scheduler.Clock;
import org.mobicents.media.server.scheduler.PriorityQueueScheduler;
import org.mobicents.media.server.scheduler.Scheduler;
import org.mobicents.media.server.scheduler.ServiceScheduler;
import org.mobicents.media.server.scheduler.WallClock;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SdpFactoryTest {
    
    private final PriorityQueueScheduler mediaScheduler;
    private final Scheduler scheduler;
    private final UdpManager udpManager;
    private final ChannelsManager channelsManager;
    private final Clock wallClock;
    private final AudioChannel audioChannel;
    private final String localAddress = "127.0.0.1";
    private final String externalAddress = "external.address.com";
    
    public SdpFactoryTest() {
        this.wallClock = new WallClock();
        this.mediaScheduler = new PriorityQueueScheduler();
        this.mediaScheduler.setClock(this.wallClock);
        this.scheduler = new ServiceScheduler();
        this.udpManager = new UdpManager(scheduler);
        this.channelsManager = new ChannelsManager(udpManager);
        this.channelsManager.setScheduler(this.mediaScheduler);
        this.audioChannel = new AudioChannel(wallClock, channelsManager);
    }
    
    @Before
    public void before() throws InterruptedException {
        this.mediaScheduler.start();
        this.scheduler.start();
        this.udpManager.start();
    }
	
	@Test
	public void testRejectApplication() throws SdpException {
		// given
		String applicationSdp = "m=application 9 DTLS/SCTP 5000";
		MediaDescriptionFieldParser mediaFieldParser = new MediaDescriptionFieldParser();
		MediaDescriptionField applicationField = mediaFieldParser.parse(applicationSdp);
		
		SessionDescription answer = new SessionDescription();
		
		// when
		SdpFactory.rejectMediaField(answer, applicationField);
		
		// then
		MediaDescriptionField mediaDescription = answer.getMediaDescription("application");
		Assert.assertNotNull(mediaDescription);
		Assert.assertEquals(0, mediaDescription.getPort());
		Assert.assertEquals("DTLS/SCTP", mediaDescription.getProtocol());
		int[] payloadTypes = mediaDescription.getPayloadTypes();
		Assert.assertEquals(1, payloadTypes.length);
		Assert.assertEquals(5000, payloadTypes[0]);
	}
	
	@Test
	public void testExternalAddress() throws SdpException {
	    // create session description with valid externalAddress
	    SessionDescription sd = SdpFactory.buildSdp(true, localAddress, externalAddress, this.audioChannel);
	    Assert.assertTrue(sd.getConnection().getAddress().equals(externalAddress));
	    //Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(externalAddress));
	    
	    // create session description with empty externalAddress
	    sd = SdpFactory.buildSdp(true, localAddress, "", this.audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(localAddress));
        //Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(localAddress));
        
        // create session description with null externalAddress
        sd = SdpFactory.buildSdp(true, localAddress, null, this.audioChannel);
        Assert.assertTrue(sd.getConnection().getAddress().equals(localAddress));
        //Assert.assertTrue(sd.getMediaDescription("audio").getConnection().getAddress().equals(localAddress));
	}

}
