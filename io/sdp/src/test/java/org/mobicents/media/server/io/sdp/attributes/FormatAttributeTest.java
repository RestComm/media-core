package org.mobicents.media.server.io.sdp.attributes;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.attributes.FormatAttribute;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatAttributeTest {

	@Test
	public void testCanParse() {
		// given
		String validLine1 = "a=fmtp:18 annexb=no";
		String validLine2 = "a=fmtp:101 0-15";
		String invalidLine1 = "x=fmtp:101 0-15";
		String invalidLine2 = "a=fmtp:xyz 0-15";
		String invalidLine3 = "a=fmtp:101";

		// when
		FormatAttribute attr = new FormatAttribute();

		// then
		Assert.assertTrue(attr.canParse(validLine1));
		Assert.assertTrue(attr.canParse(validLine2));
		Assert.assertFalse(attr.canParse(invalidLine1));
		Assert.assertFalse(attr.canParse(invalidLine2));
		Assert.assertFalse(attr.canParse(invalidLine3));
	}

}
