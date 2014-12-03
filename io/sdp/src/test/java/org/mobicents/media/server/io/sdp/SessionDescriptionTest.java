package org.mobicents.media.server.io.sdp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Test;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionDescriptionTest {
	
	private static String webrtcDescription;
	
	@BeforeClass
	public static void setup() throws IOException, URISyntaxException {
		URL resource = SessionDescriptionTest.class.getResource("sdp-webrtc.txt");
		byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
		webrtcDescription = new String(bytes);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		SessionDescription sdp = new SessionDescription();
		
		// when
		sdp.parse(webrtcDescription);
		
		// then
		VersionField version = sdp.getVersion();
		Assert.assertNotNull(version);
		Assert.assertEquals(0, version.getVersion());
		
		OriginField origin = sdp.getOrigin();
		Assert.assertNotNull(origin);
		Assert.assertEquals("-", origin.getUsername());
		Assert.assertEquals(680121471469462884L, origin.getSessionId());
		Assert.assertEquals(2, origin.getSessionVersion());
		Assert.assertEquals("IN", origin.getNetType());
		Assert.assertEquals("IP4", origin.getAddressType());
		Assert.assertEquals("127.0.0.1", origin.getAddress());
		
		SessionNameField sessionName = sdp.getSessionName();
		Assert.assertNotNull(sessionName);
		Assert.assertEquals("-", sessionName.getName());
		
		TimingField timing = sdp.getTiming();
		Assert.assertNotNull(timing);
		Assert.assertEquals(0, timing.getStartTime());
		Assert.assertEquals(0, timing.getStopTime());
		
		MediaDescriptionField audio = sdp.getMediaDescription("audio");
		Assert.assertNotNull(audio);
		Assert.assertEquals("audio", audio.getMedia());
		Assert.assertEquals(54278, audio.getPort());
		Assert.assertEquals("RTP/SAVPF", audio.getProtocol());
		Assert.assertTrue(audio.containsFormat(111));
		Assert.assertTrue(audio.containsFormat(103));
		Assert.assertTrue(audio.containsFormat(104));
		Assert.assertTrue(audio.containsFormat(0));
		Assert.assertTrue(audio.containsFormat(8));
		Assert.assertTrue(audio.containsFormat(106));
		Assert.assertTrue(audio.containsFormat(105));
		Assert.assertTrue(audio.containsFormat(13));
		Assert.assertTrue(audio.containsFormat(126));
		ConnectionField audioConnection = audio.getConnection();
		Assert.assertNotNull(audioConnection);
		Assert.assertEquals("IN", audioConnection.getNetworkType());
		Assert.assertEquals("IP4", audioConnection.getAddressType());
		Assert.assertEquals("180.6.6.6", audioConnection.getAddress());
		ConnectionModeAttribute audioConnectionMode = audio.getConnectionMode();
		Assert.assertNotNull(audioConnectionMode);
		Assert.assertEquals(ConnectionModeAttribute.SENDRECV, audioConnectionMode.getKey());

		MediaDescriptionField video = sdp.getMediaDescription("video");
		Assert.assertNotNull(video);
		Assert.assertEquals("video", video.getMedia());
		Assert.assertEquals(54279, video.getPort());
		Assert.assertEquals("RTP/SAVPF", video.getProtocol());
		Assert.assertTrue(video.containsFormat(100));
		Assert.assertTrue(video.containsFormat(116));
		Assert.assertTrue(video.containsFormat(117));
		ConnectionField videoConnection = video.getConnection();
		Assert.assertNotNull(videoConnection);
		Assert.assertEquals("IN", videoConnection.getNetworkType());
		Assert.assertEquals("IP4", videoConnection.getAddressType());
		Assert.assertEquals("180.6.6.7", videoConnection.getAddress());
		ConnectionModeAttribute videoConnectionMode = video.getConnectionMode();
		Assert.assertNotNull(videoConnectionMode);
		Assert.assertEquals(ConnectionModeAttribute.SENDRECV, videoConnectionMode.getKey());

		MediaDescriptionField application = sdp.getMediaDescription("application");
		Assert.assertNull(application);
	}

}
