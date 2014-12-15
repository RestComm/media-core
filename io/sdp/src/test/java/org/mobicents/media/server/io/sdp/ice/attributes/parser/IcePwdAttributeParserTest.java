package org.mobicents.media.server.io.sdp.ice.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.ice.attributes.IcePwdAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IcePwdAttributeParserTest {
	
	private final IcePwdAttributeParser parser = new IcePwdAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		String sdp2 = "a=ice-pass: \n\r";
		String sdp3 = "x=ice-pwd:P4ssW0rD\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		
		// when
		IcePwdAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals("P4ssW0rD", obj.getPassword());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		String sdp2 = "a=ice-pwd:N3wP4sS\n\r";
		
		// when
		IcePwdAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals("N3wP4sS", obj.getPassword());
	}

	@Test(expected=SdpException.class)
	public void testParseMissingPassword() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:\n\r";
		
		// when
		parser.parse(sdp1);
	}

}
