package org.mobicents.media.server.io.sdp.fields.attributes.ice;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.attributes.ice.IceUfragAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceUfragTest {
	
	@Test
	public void testCanParse() {
		// given
		String validString = "a=ice-ufrag:Ufr4G";
		String invalidString1 = "x=ice-ufrag:Ufr4G";
		String invalidString2 = "a=ice-ufrag: ";
		String invalidString3 = "a=ice-pwd:Ufr4G";
		
		// when
		IceUfragAttribute attribute = new IceUfragAttribute();
		
		// then
		Assert.assertTrue(attribute.canParse(validString));
		Assert.assertFalse(attribute.canParse(invalidString1));
		Assert.assertFalse(attribute.canParse(invalidString2));
		Assert.assertFalse(attribute.canParse(invalidString3));
	}

	@Test
	public void testParse() throws SdpException {
		// given
		String validString = "a=ice-ufrag:Ufr4G";
		
		// when
		IceUfragAttribute attribute = new IceUfragAttribute();
		attribute.parse(validString);
		
		// then
		Assert.assertEquals("Ufr4G",attribute.getValue());
	}

}
