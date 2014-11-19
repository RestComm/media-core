package org.mobicents.media.server.io.sdp.fields;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaDescriptionFieldTest {
	
	@Test
	public void testCustomMediaDescription() {
		// given
		String media = "audio";
		int port = 65535;
		String protocol = "RTP/AVP";
		int[] formats = new int[] { 0, 101 };
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.setMedia(media);
		md.setPort(port);
		md.setProtocol(protocol);
		md.addFormats(formats);
		
		// then
		Assert.assertEquals(media, md.getMedia());
		Assert.assertEquals(port, md.getPort());
		Assert.assertEquals(protocol, md.getProtocol());
		Assert.assertTrue(md.containsFormat(0));
		Assert.assertTrue(md.containsFormat(101));
		Assert.assertFalse(md.containsFormat(126));
	}
	
	@Test
	public void testValidParsing() throws SdpException {
		// given
		String line = "m=video 64534 RTP/AVPF 0 101 120";
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.parse(line);
		
		// then
		Assert.assertEquals("video", md.getMedia());
		Assert.assertEquals(64534, md.getPort());
		Assert.assertEquals("RTP/AVPF", md.getProtocol());
		Assert.assertTrue(md.containsFormat(0));
		Assert.assertTrue(md.containsFormat(101));
		Assert.assertTrue(md.containsFormat(120));
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsingMissingMedia() throws SdpException {
		// given
		String line = "m=64534 RTP/AVPF 0 101 120";
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsingPortFormat() throws SdpException {
		// given
		String line = "m=64534 xyz RTP/AVPF 0 101 120";
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsingMissingProtocol() throws SdpException {
		// given
		String line = "m=64534 63000 0 101 120";
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsingFormats() throws SdpException {
		// given
		String line = "m=64534 63000 RTP/AVP a b c";
		
		// when
		MediaDescriptionField md = new MediaDescriptionField();
		md.parse(line);
	}

}
