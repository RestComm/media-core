package org.mobicents.media.server.io.sdp.fields.attributes.rtcp;

import org.junit.Test;

import junit.framework.Assert;

public class RtcpMuxAttributeTest {

	@Test
	public void testCanParse() {
		// given
		String validLine = "a=rtcp-mux";
		String invalidLine1 = "x=rtcp-mux";
		String invalidLine2 = "a=rtcp";
		
		// when
		RtcpMuxAttribute attribute = new RtcpMuxAttribute();
		
		// then
		Assert.assertTrue(attribute.canParse(validLine));
		Assert.assertFalse(attribute.canParse(invalidLine1));
		Assert.assertFalse(attribute.canParse(invalidLine2));
	}
	
}
