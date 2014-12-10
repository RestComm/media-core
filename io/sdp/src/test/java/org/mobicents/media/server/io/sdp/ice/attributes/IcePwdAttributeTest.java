package org.mobicents.media.server.io.sdp.ice.attributes;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IcePwdAttributeTest {
	
	@Test
	public void testCustomIcePwd() {
		// given
		String password = "P4sSw0Rd";
		
		// when
		IcePwdAttribute obj = new IcePwdAttribute();
		obj.setPassword(password);
		
		// then
		Assert.assertEquals(password, obj.getPassword());
		Assert.assertEquals("a=ice-pwd:P4sSw0Rd", obj.toString());
	}

}