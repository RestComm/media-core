package org.mobicents.media.server.io.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FormatParameterAttributeParserTest {

	private final FormatParameterAttributeParser parser = new FormatParameterAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		String sdp2 = "a=fmtp:101 0-15\n\r";
		String sdp3 = "x=fmtp:101 0-15\n\r";
		String sdp4 = "a=fmtp:xyz 0-15\n\r";
		String sdp5 = "a=fmtp:101\n\r";

		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);

		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		
		// when
		FormatParameterAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(18, obj.getFormat());
		Assert.assertEquals("annexb=no", obj.getParams());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		String sdp2 = "a=fmtp:101 0-15\n\r";
		
		// when
		FormatParameterAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(101, obj.getFormat());
		Assert.assertEquals("0-15", obj.getParams());
	}
	
}
