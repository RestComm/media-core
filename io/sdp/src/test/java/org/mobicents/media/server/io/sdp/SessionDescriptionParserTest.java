package org.mobicents.media.server.io.sdp;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.SsrcAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.FingerprintAttribute;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;
import org.mobicents.media.server.io.sdp.fields.OriginField;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;
import org.mobicents.media.server.io.sdp.fields.TimingField;
import org.mobicents.media.server.io.sdp.fields.VersionField;
import org.mobicents.media.server.io.sdp.ice.attributes.CandidateAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpMuxAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionDescriptionParserTest {
	
	private static String webrtcDescription;
	
	@BeforeClass
	public static void setup() throws IOException, URISyntaxException {
		URL resource = SessionDescriptionParserTest.class.getResource("sdp-webrtc.txt");
		byte[] bytes = Files.readAllBytes(Paths.get(resource.toURI()));
		webrtcDescription = new String(bytes);
	}

	@Test
	public void testParse() throws SdpException {
		// given
		SessionDescription sdp;
		
		// when
		sdp = SessionDescriptionParser.parse(webrtcDescription);
		
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
		
		/* AUDIO */
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
		
		RtcpAttribute audioRtcp = audio.getRtcp();
		Assert.assertNotNull(audioRtcp);
		Assert.assertEquals(54278, audioRtcp.getPort());
		Assert.assertEquals("IN", audioRtcp.getNetworkType());
		Assert.assertEquals("IP4", audioRtcp.getAddressType());
		Assert.assertEquals("180.6.6.6", audioRtcp.getAddress());
		
		RtcpMuxAttribute audioRtcpMux = audio.getRtcpMux();
		Assert.assertNotNull(audioRtcpMux);

		Assert.assertNotNull(audio.getFormats());
		Assert.assertEquals(10, audio.getFormats().length);
		RtpMapAttribute audioOpus = audio.getFormat((short) 111);
		Assert.assertNotNull(audioOpus);
		Assert.assertNotNull(audioOpus.getParameters());
		Assert.assertNull(audioOpus.getPtime());
		Assert.assertNull(audioOpus.getMaxptime());
		RtpMapAttribute audioIsac16 = audio.getFormat((short) 103);
		Assert.assertNotNull(audioIsac16);
		Assert.assertNull(audioIsac16.getParameters());
		Assert.assertNull(audioIsac16.getPtime());
		Assert.assertNull(audioIsac16.getMaxptime());
		RtpMapAttribute audioIsac32 = audio.getFormat((short) 104);
		Assert.assertNotNull(audioIsac32);
		Assert.assertNull(audioIsac32.getParameters());
		Assert.assertNull(audioIsac32.getPtime());
		Assert.assertNull(audioIsac32.getMaxptime());
		RtpMapAttribute audioPcmu = audio.getFormat((short) 0);
		Assert.assertNotNull(audioPcmu);
		Assert.assertNull(audioPcmu.getParameters());
		Assert.assertNull(audioPcmu.getPtime());
		Assert.assertNull(audioPcmu.getMaxptime());
		RtpMapAttribute audioPcma = audio.getFormat((short) 8);
		Assert.assertNotNull(audioPcma);
		Assert.assertNull(audioPcma.getParameters());
		Assert.assertNull(audioPcma.getPtime());
		Assert.assertNull(audioPcma.getMaxptime());
		RtpMapAttribute audioCn48 = audio.getFormat((short) 107);
		Assert.assertNotNull(audioCn48);
		Assert.assertNull(audioCn48.getParameters());
		Assert.assertNull(audioCn48.getPtime());
		Assert.assertNull(audioCn48.getMaxptime());
		RtpMapAttribute audioCn32 = audio.getFormat((short) 106);
		Assert.assertNotNull(audioCn32);
		Assert.assertNull(audioCn32.getParameters());
		Assert.assertNull(audioCn32.getPtime());
		Assert.assertNull(audioCn32.getMaxptime());
		RtpMapAttribute audioCn16 = audio.getFormat((short) 105);
		Assert.assertNotNull(audioCn16);
		Assert.assertNull(audioCn16.getParameters());
		Assert.assertNull(audioCn16.getPtime());
		Assert.assertNull(audioCn16.getMaxptime());
		RtpMapAttribute audioCn8 = audio.getFormat((short) 13);
		Assert.assertNotNull(audioCn8);
		Assert.assertNull(audioCn8.getParameters());
		Assert.assertNull(audioCn8.getPtime());
		Assert.assertNull(audioCn8.getMaxptime());
		RtpMapAttribute audioDtmf = audio.getFormat((short) 126);
		Assert.assertNotNull(audioDtmf);
		Assert.assertNull(audioDtmf.getParameters());
		Assert.assertNull(audioDtmf.getPtime());
		Assert.assertNotNull(audioDtmf.getMaxptime());
		
		SetupAttribute audioSetup = audio.getSetup();
		Assert.assertNotNull(audioSetup);
		Assert.assertEquals("actpass", audioSetup.getValue());
		
		FingerprintAttribute audioFingerprint = audio.getFingerprint();
		Assert.assertNotNull(audioFingerprint);
		Assert.assertEquals("sha-256", audioFingerprint.getHashFunction());
		Assert.assertEquals("D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39", audioFingerprint.getFingerprint());
		
		SsrcAttribute audioSsrc = audio.getSsrc();
		Assert.assertNotNull(audioSsrc);
		Assert.assertEquals("189858836", audioSsrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", audioSsrc.getAttributeValue("cname"));
		
		IceUfragAttribute audioIceUfrag = audio.getIceUfrag();
		Assert.assertNotNull(audioIceUfrag);
		Assert.assertEquals("kwlYyWNjhC9JBe/V", audioIceUfrag.getUfrag());
		
		IcePwdAttribute audioIcePwd = audio.getIcePwd();
		Assert.assertNotNull(audioIcePwd);
		Assert.assertEquals("AU/SQPupllyS0SDG/eRWDCfA", audioIcePwd.getPassword());
		
		CandidateAttribute[] audioCandidates = audio.getCandidates();
		Assert.assertNotNull(audioCandidates);
		Assert.assertEquals(8, audioCandidates.length);

		/* VIDEO */
		MediaDescriptionField video = sdp.getMediaDescription("video");
		Assert.assertNotNull(video);
		Assert.assertEquals("video", video.getMedia());
		Assert.assertEquals(54279, video.getPort());
		Assert.assertEquals("RTP/SAVPF", video.getProtocol());
		Assert.assertTrue(video.containsFormat((short) 100));
		Assert.assertTrue(video.containsFormat((short) 116));
		Assert.assertTrue(video.containsFormat((short) 117));
		
		ConnectionField videoConnection = video.getConnection();
		Assert.assertNotNull(videoConnection);
		Assert.assertEquals("IN", videoConnection.getNetworkType());
		Assert.assertEquals("IP4", videoConnection.getAddressType());
		Assert.assertEquals("180.6.6.7", videoConnection.getAddress());
		
		ConnectionModeAttribute videoConnectionMode = video.getConnectionMode();
		Assert.assertNotNull(videoConnectionMode);
		Assert.assertEquals(ConnectionModeAttribute.SENDRECV, videoConnectionMode.getKey());
		
		RtcpAttribute videoRtcp = video.getRtcp();
		Assert.assertNotNull(videoRtcp);
		Assert.assertEquals(54279, videoRtcp.getPort());
		Assert.assertEquals("IN", videoRtcp.getNetworkType());
		Assert.assertEquals("IP4", videoRtcp.getAddressType());
		Assert.assertEquals("180.6.6.7", videoRtcp.getAddress());
		
		RtcpMuxAttribute videoRtcpMux = video.getRtcpMux();
		Assert.assertNotNull(videoRtcpMux);

		RtpMapAttribute videoVP8 = video.getFormat((short) 100);
		Assert.assertNotNull(videoVP8);
		Assert.assertNull(videoVP8.getParameters());
		Assert.assertNull(videoVP8.getPtime());
		Assert.assertNull(videoVP8.getMaxptime());
		RtpMapAttribute videoRed = video.getFormat((short) 116);
		Assert.assertNotNull(videoRed);
		Assert.assertNull(videoRed.getParameters());
		Assert.assertNull(videoRed.getPtime());
		Assert.assertNull(videoRed.getMaxptime());
		RtpMapAttribute videoUlpFec = video.getFormat((short) 117);
		Assert.assertNotNull(videoUlpFec);
		Assert.assertNull(videoUlpFec.getParameters());
		Assert.assertNull(videoUlpFec.getPtime());
		Assert.assertNull(videoUlpFec.getMaxptime());
		
		FingerprintAttribute videoFingerprint = video.getFingerprint();
		Assert.assertNotNull(videoFingerprint);
		Assert.assertEquals("sha-256", videoFingerprint.getHashFunction());
		Assert.assertEquals("D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39", videoFingerprint.getFingerprint());
		
		IceUfragAttribute videoIceUfrag = video.getIceUfrag();
		Assert.assertNotNull(videoIceUfrag);
		Assert.assertEquals("kwlYyWNjhC9JBe/V", videoIceUfrag.getUfrag());
		
		IcePwdAttribute videoIcePwd = video.getIcePwd();
		Assert.assertNotNull(videoIcePwd);
		Assert.assertEquals("AU/SQPupllyS0SDG/eRWDCfA", videoIcePwd.getPassword());
		
		CandidateAttribute[] videoCandidates = video.getCandidates();
		Assert.assertNotNull(videoCandidates);
		Assert.assertEquals(8, videoCandidates.length);
		
		SetupAttribute videoSetup = video.getSetup();
		Assert.assertNotNull(videoSetup);
		Assert.assertEquals("active", videoSetup.getValue());
		
		SsrcAttribute videoSsrc = video.getSsrc();
		Assert.assertNotNull(videoSsrc);
		Assert.assertEquals("1993357196", videoSsrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", videoSsrc.getAttributeValue("cname"));
		
		/* APPLICATION */
		MediaDescriptionField application = sdp.getMediaDescription("application");
		Assert.assertNull(application);
	}
	
	@Ignore
	@Test
	public void loadTest() throws SdpException {
		int runs = 500000;
		long start = System.nanoTime();
		for (int i = 0; i < runs; i++) {
			SessionDescriptionParser.parse(webrtcDescription);
		}
		long end = System.nanoTime();
		
		long runtime = end - start;
		long unitTime = runtime / runs;
		
		System.out.println("Load test took " + runtime + "ns (" + unitTime + "ns per SDP)");
	}

}
