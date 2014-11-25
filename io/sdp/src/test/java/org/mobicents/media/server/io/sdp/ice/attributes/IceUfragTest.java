package org.mobicents.media.server.io.sdp.ice.attributes;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceUfragTest {
	
	@Test
	public void testCustomIceUfrag() {
		// given
		String ufrag = "uFr4g";
		
		// when
		IceUfragAttribute obj = new IceUfragAttribute();
		obj.setUfrag(ufrag);
		
		// then
		Assert.assertEquals(ufrag, obj.getUfrag());
		Assert.assertEquals("a=ice-ufrag:uFr4g", obj.toString());
	}

}
