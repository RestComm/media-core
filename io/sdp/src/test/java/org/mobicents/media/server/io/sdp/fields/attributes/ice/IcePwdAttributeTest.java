package org.mobicents.media.server.io.sdp.fields.attributes.ice;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.attributes.ice.IcePwdAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IcePwdAttributeTest {
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "a=ice-pwd:P4ssW0rD";
		String invalidLine1 = "a=ice-pass: ";
		String invalidLine2 = "x=ice-pwd:P4ssW0rD";
		
		// when
		IcePwdAttribute attribute = new IcePwdAttribute();
		
		// then
		Assert.assertTrue(attribute.canParse(validLine));
		Assert.assertFalse(attribute.canParse(invalidLine1));
		Assert.assertFalse(attribute.canParse(invalidLine2));
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String validLine = "a=ice-pwd:P4ssW0rD";
		
		// when
		IcePwdAttribute attribute = new IcePwdAttribute();
		attribute.parse(validLine);
		
		// then
		Assert.assertEquals("P4ssW0rD", attribute.getValue());
	}

}
