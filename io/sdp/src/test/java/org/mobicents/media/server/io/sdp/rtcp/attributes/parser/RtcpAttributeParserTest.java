package org.mobicents.media.server.io.sdp.rtcp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.rtcp.attributes.RtcpAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtcpAttributeParserTest {
	
	private static final RtcpAttributeParser parser = new RtcpAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtcp:65000";
		String sdp2 = "x=rtcp:65000";
		String sdp3 = "a=rtcp:xyz";
		String sdp4 = "a=rtcp:65000 xyz";
		
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
		String sdp1 = "a=rtcp:65000";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000";
		String sdp2 = "a=rtcp:50000";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(50000, obj.getPort());
	}

}
