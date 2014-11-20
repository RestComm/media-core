package org.mobicents.media.server.io.sdp.fields.attributes.ice;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.fields.attributes.ice.IceLiteAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceLiteAttributeTest {

	@Test
	public void testCanParse() {
		// given
		String validLine = "a=ice-lite";
		String invalidLine1 = "x=ice-lite";
		String invalidLine2 = "x=ice-ligth";
		
		// when
		IceLiteAttribute attribute = new IceLiteAttribute();
		
		// then
		Assert.assertTrue(attribute.canParse(validLine));
		Assert.assertFalse(attribute.canParse(invalidLine1));
		Assert.assertFalse(attribute.canParse(invalidLine2));
	}
	
}
