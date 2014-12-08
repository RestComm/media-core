package org.mobicents.media.server.io.sdp.rtcp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtcpMuxAttributeParserTest {

	private final RtcpMuxAttributeParser parser = new RtcpMuxAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtcp-mux\n\r";
		String sdp2 = "x=rtcp-mux\n\r";
		String sdp3 = "a=rtcp\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
	}
	
}
