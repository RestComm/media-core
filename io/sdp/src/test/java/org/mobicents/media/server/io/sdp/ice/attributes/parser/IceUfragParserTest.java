package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.ice.attributes.IceUfragAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceUfragParserTest {
	
	private final IceUfragAttributeParser parser = new IceUfragAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ice-ufrag:Ufr4G";
		String sdp2 = "x=ice-ufrag:Ufr4G";
		String sdp3 = "a=ice-ufrag: ";
		String sdp4 = "a=ice-pwd:Ufr4G";
		
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

	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=ice-ufrag:Ufr4G";
		
		// when
		IceUfragAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals("Ufr4G",obj.getUfrag());
	}

	@Test(expected=SdpException.class)
	public void testParseMissingValue() throws SdpException {
		// given
		String sdp1 = "a=ice-ufrag:";
		
		// when
		parser.parse(sdp1);
	}

}
