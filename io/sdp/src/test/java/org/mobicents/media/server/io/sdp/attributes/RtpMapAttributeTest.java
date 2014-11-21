package org.mobicents.media.server.io.sdp.attributes;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;

import junit.framework.Assert;

public class RtpMapAttributeTest {

	@Test
	public void testCanParse() {
		// given
		String validLine1 = "a=rtpmap:97 L16/8000";
		String validLine2 = "a=rtpmap:98 L16/11025/2";
		String invalidLine1 = "x=rtpmap:98 L16/11025/2";
		String invalidLine2 = "a=xyz:98 L16/11025/2";
		String invalidLine3 = "a=rtpmap:xyz L16/11025/2";
		String invalidLine4 = "a=rtpmap:98 L16/xyz/2";
		String invalidLine5 = "a=rtpmap:98 L16/11025/xyz";

		// when
		RtpMapAttribute attr = new RtpMapAttribute();

		// then
		Assert.assertTrue(attr.canParse(validLine1));
		Assert.assertTrue(attr.canParse(validLine2));
		Assert.assertFalse(attr.canParse(invalidLine1));
		Assert.assertFalse(attr.canParse(invalidLine2));
		Assert.assertFalse(attr.canParse(invalidLine3));
		Assert.assertFalse(attr.canParse(invalidLine4));
		Assert.assertFalse(attr.canParse(invalidLine5));
	}

	@Test
	public void testSimpleParse() throws SdpException {
		// given
		String line = "a=rtpmap:97 L16/8000";

		// when
		RtpMapAttribute attr = new RtpMapAttribute();
		attr.parse(line);

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
		RtpMapAttribute attr = new RtpMapAttribute();
		attr.parse(line);

		// then
		Assert.assertEquals(98, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(11025, attr.getClockRate());
		Assert.assertEquals(2, attr.getCodecParams());
	}

}
