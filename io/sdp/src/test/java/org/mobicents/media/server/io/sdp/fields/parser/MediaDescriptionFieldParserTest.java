package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.MediaDescriptionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaDescriptionFieldParserTest {
	
	private final MediaDescriptionFieldParser parser = new MediaDescriptionFieldParser();

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		String sdp2 = "x=video 64534 RTP/AVPF 0 101 120\n\r";
		String sdp3 = "m=video xyz RTP/AVPF 0 101 120\n\r";
		String sdp4 = "m=video 64534 0 101 120\n\r";
		String sdp5 = "m=video 64534 RTP/AVPF 0 xyz 120\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		
		// when
		MediaDescriptionField md = parser.parse(line);
		
		// then
		Assert.assertEquals("video", md.getMedia());
		Assert.assertEquals(64534, md.getPort());
		Assert.assertEquals("RTP/AVPF", md.getProtocol());
		Assert.assertTrue(md.containsPayloadType((short) 0));
		Assert.assertTrue(md.containsPayloadType((short) 101));
		Assert.assertTrue(md.containsPayloadType((short) 120));
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		String line2 = "m=audio 65000 RTP/SAVPF 0 101 180\n\r";
		
		// when
		MediaDescriptionField md = parser.parse(line1);
		parser.parse(md, line2);
		
		// then
		Assert.assertEquals("audio", md.getMedia());
		Assert.assertEquals(65000, md.getPort());
		Assert.assertEquals("RTP/SAVPF", md.getProtocol());
		Assert.assertTrue(md.containsPayloadType((short) 0));
		Assert.assertTrue(md.containsPayloadType((short) 101));
		Assert.assertTrue(md.containsPayloadType((short) 180));
		Assert.assertFalse(md.containsPayloadType((short) 120));
	}

	@Test(expected=SdpException.class)
	public void testParseMissingMedia() throws SdpException {
		// given
		String line = "m=64534 RTP/AVPF 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testParseInvalidPortFormat() throws SdpException {
		// given
		String line = "m=64534 xyz RTP/AVPF 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testParseMissingProtocol() throws SdpException {
		// given
		String line = "m=64534 63000 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testParseInvalidFormats() throws SdpException {
		// given
		String line = "m=64534 63000 RTP/AVP a b c\n\r";
		
		// when
		parser.parse(line);
	}
	
}
