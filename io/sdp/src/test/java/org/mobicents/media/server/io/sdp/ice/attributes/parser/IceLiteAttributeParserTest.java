package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import org.junit.Test;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceLiteAttributeParserTest {
	
	private final IceLiteAttributeParser parser = new IceLiteAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ice-lite";
		String sdp2 = "x=ice-lite";
		String sdp3 = "a=ice-pwd";
		String sdp4 = "a=ice-lite:xyz";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
	}

}
