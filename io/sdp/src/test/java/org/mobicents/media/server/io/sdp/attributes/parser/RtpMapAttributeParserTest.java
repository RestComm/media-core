package org.mobicents.media.server.io.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMapAttributeParserTest {
	
	private final RtpMapAttributeParser parser = new RtpMapAttributeParser();
	

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtpmap:97 L16/8000";
		String sdp2 = "a=rtpmap:98 L16/11025/2";
		String sdp3 = "x=rtpmap:98 L16/11025/2";
		String sdp4 = "a=xyz:98 L16/11025/2";
		String sdp5 = "a=rtpmap:xyz L16/11025/2";
		String sdp6 = "a=rtpmap:98 L16/xyz/2";
		String sdp7 = "a=rtpmap:98 L16/11025/xyz";

		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		boolean canParseSdp7 = parser.canParse(sdp7);

		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}

	@Test
	public void testSimpleParse() throws SdpException {
		// given
		String line = "a=rtpmap:97 L16/8000";

		// when
		RtpMapAttribute attr = parser.parse(line);

		// then
		Assert.assertEquals(97, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(8000, attr.getClockRate());
		Assert.assertEquals(-1, attr.getCodecParams());
	}

	@Test
	public void testComplexParse() throws SdpException {
		// given
		String line = "a=rtpmap:98 L16/11025/2";

		// when
		RtpMapAttribute attr = parser.parse(line);

		// then
		Assert.assertEquals(98, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(11025, attr.getClockRate());
		Assert.assertEquals(2, attr.getCodecParams());
	}

	@Test
	public void testSimpleToComplexParseOverwrite() throws SdpException {
		// given
		String line1 = "a=rtpmap:97 L16/8000";
		String line2 = "a=rtpmap:98 L16/11025/2";
		
		// when
		RtpMapAttribute attr = parser.parse(line1);
		parser.parse(attr, line2);
		
		// then
		Assert.assertEquals(98, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(11025, attr.getClockRate());
		Assert.assertEquals(2, attr.getCodecParams());
	}

	@Test
	public void testComplexToSimpleParseOverwrite() throws SdpException {
		// given
		String line1 = "a=rtpmap:98 L16/11025/2";
		String line2 = "a=rtpmap:97 L16/8000";
		
		// when
		RtpMapAttribute attr = parser.parse(line1);
		parser.parse(attr, line2);
		
		// then
		Assert.assertEquals(97, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(8000, attr.getClockRate());
		Assert.assertEquals(-1, attr.getCodecParams());
	}

}
