package org.mobicents.media.server.io.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.FormatAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FormatAttributeParserTest {

	private final FormatAttributeParser parser = new FormatAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=fmtp:18 annexb=no";
		String sdp2 = "a=fmtp:101 0-15";
		String sdp3 = "x=fmtp:101 0-15";
		String sdp4 = "a=fmtp:xyz 0-15";
		String sdp5 = "a=fmtp:101";

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
		String sdp1 = "a=fmtp:18 annexb=no";
		
		// when
		FormatAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(18, obj.getFormat());
		Assert.assertEquals("annexb=no", obj.getParams());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=fmtp:18 annexb=no";
		String sdp2 = "a=fmtp:101 0-15";
		
		// when
		FormatAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(101, obj.getFormat());
		Assert.assertEquals("0-15", obj.getParams());
	}
	
}
