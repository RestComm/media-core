package org.mobicents.media.server.io.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.attributes.MaxPacketTimeAttribute;
import org.mobicents.media.server.io.sdp.attributes.parser.MaxPacketTimeAttributeParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MaxPacketTimeAttributeParserTest {

	private final MaxPacketTimeAttributeParser parser = new MaxPacketTimeAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=maxptime:100";
		String sdp2 = "x=maxptime:100";
		String sdp3 = "a=ptime:100";
		String sdp4 = "a=ptime:xyz";
		String sdp5 = "a=ptime: ";
		
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
		String sdp1 = "a=maxptime:100";
		
		// when
		MaxPacketTimeAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals("maxptime", obj.getKey());
		Assert.assertEquals(100, obj.getTime());
		Assert.assertEquals(sdp1, obj.toString());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=maxptime:100";
		String sdp2 = "a=maxptime:123";
		
		// when
		MaxPacketTimeAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals("maxptime", obj.getKey());
		Assert.assertEquals(123, obj.getTime());
		Assert.assertEquals(sdp2, obj.toString());
	}
	
	@Test(expected = SdpException.class)
	public void testParseInvalidTimeFormat() throws SdpException {
		// given
		String sdp1 = "a=maxptime:abc";
		
		// when
		parser.parse(sdp1);
	}

	@Test(expected = SdpException.class)
	public void testParseInexistentValue() throws SdpException {
		// given
		String sdp1 = "a=maxptime: ";
		
		// when
		parser.parse(sdp1);
	}
	
}
