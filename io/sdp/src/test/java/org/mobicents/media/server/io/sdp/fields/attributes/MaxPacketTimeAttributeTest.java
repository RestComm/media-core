package org.mobicents.media.server.io.sdp.fields.attributes;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MaxPacketTimeAttributeTest {
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "a=maxptime:123";
		String invalidLine1 = "x=maxptime:123";
		String invalidLine2 = "a=maxptime:xyz";
		String invalidLine3 = "a=maxptime:";
		String invalidLine4 = "a=xyz:123";
		
		// when
		MaxPacketTimeAttribute attr = new MaxPacketTimeAttribute();
		
		// then
		Assert.assertTrue(attr.canParse(validLine));
		Assert.assertFalse(attr.canParse(invalidLine1));
		Assert.assertFalse(attr.canParse(invalidLine2));
		Assert.assertFalse(attr.canParse(invalidLine3));
		Assert.assertFalse(attr.canParse(invalidLine4));
	}

	@Test
	public void testParse() throws SdpException {
		// given
		String validLine = "a=maxptime:123";
		
		// when
		MaxPacketTimeAttribute attr = new MaxPacketTimeAttribute();
		attr.parse(validLine);
		
		// then
		Assert.assertEquals("123", attr.getValue());
	}

}
