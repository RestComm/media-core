package org.mobicents.media.server.io.sdp.fields.attributes;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PacketTimeAttributeTest {
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "a=ptime:123";
		String invalidLine1 = "x=ptime:123";
		String invalidLine2 = "a=ptime:xyz";
		String invalidLine3 = "a=ptime:";
		String invalidLine4 = "a=xyz:123";
		
		// when
		PacketTimeAttribute attr = new PacketTimeAttribute();
		
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
		String validLine = "a=ptime:123";
		
		// when
		PacketTimeAttribute attr = new PacketTimeAttribute();
		attr.parse(validLine);
		
		// then
		Assert.assertEquals("123", attr.getValue());
	}

}
