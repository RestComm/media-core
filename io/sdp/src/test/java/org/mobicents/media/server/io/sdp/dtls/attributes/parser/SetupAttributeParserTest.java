package org.mobicents.media.server.io.sdp.dtls.attributes.parser;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.dtls.attributes.SetupAttribute;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SetupAttributeParserTest {

	private static final SetupAttributeParser PARSER = new SetupAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=setup:active";
		String sdp2 = "a=setup:passive";
		String sdp3 = "a=setup:actpass";
		String sdp4 = "a=setup:holdconn";
		String sdp5 = "x=setup:active";
		String sdp6 = "a=setup:xyz";
		String sdp7 = "a=setup:";
		
		// when
		boolean canParseSdp1 = PARSER.canParse(sdp1);
		boolean canParseSdp2 = PARSER.canParse(sdp2);
		boolean canParseSdp3 = PARSER.canParse(sdp3);
		boolean canParseSdp4 = PARSER.canParse(sdp4);
		boolean canParseSdp5 = PARSER.canParse(sdp5);
		boolean canParseSdp6 = PARSER.canParse(sdp6);
		boolean canParseSdp7 = PARSER.canParse(sdp7);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertTrue(canParseSdp3);
		Assert.assertTrue(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=setup:active";
		
		// when
		SetupAttribute obj = PARSER.parse(sdp1);
		
		// then
		Assert.assertNotNull(obj);
		Assert.assertEquals("active", obj.getValue());
	}

	@Test
	public void testParseOverwirte() throws SdpException {
		// given
		String sdp1 = "a=setup:active";
		String sdp2 = "a=setup:passive";
		
		// when
		SetupAttribute obj = PARSER.parse(sdp1);
		PARSER.parse(obj, sdp2);
		
		// then
		Assert.assertEquals("passive", obj.getValue());
	}
	
	@Test(expected = SdpException.class)
	public void testParseMissingRole() throws SdpException {
		// given
		String sdp1 = "a=setup:";

		// when
		PARSER.parse(sdp1);
	}

	@Test(expected = SdpException.class)
	public void testParseInvalidRole() throws SdpException {
		// given
		String sdp1 = "a=setup:other";
		
		// when
		PARSER.parse(sdp1);
	}
	
}
