package org.mobicents.media.server.io.sdp.fields.attributes.rtcp;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.attributes.rtcp.RtcpAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtcpAttributeTest {
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "a=rtcp:65000";
		String invalidLine1 = "x=rtcp:65000";
		String invalidLine2 = "a=rtcp:xyz";
		String invalidLine3 = "a=rtcp:65000 xyz";
		
		// when
		RtcpAttribute attribute = new RtcpAttribute();
		
		// then
		Assert.assertTrue(attribute.canParse(validLine));
		Assert.assertFalse(attribute.canParse(invalidLine1));
		Assert.assertFalse(attribute.canParse(invalidLine2));
		Assert.assertFalse(attribute.canParse(invalidLine3));
	}

	@Test
	public void testParse() throws SdpException {
		// given
		String validLine = "a=rtcp:65000";
		
		// when
		RtcpAttribute attribute = new RtcpAttribute();
		attribute.parse(validLine);
		
		// then
		Assert.assertEquals("65000", attribute.getValue());
	}
	
}
