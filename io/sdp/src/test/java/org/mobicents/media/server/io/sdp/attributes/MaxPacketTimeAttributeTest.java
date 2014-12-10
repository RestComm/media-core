package org.mobicents.media.server.io.sdp.attributes;

import org.junit.Test;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MaxPacketTimeAttributeTest {
	
	@Test
	public void testCustomMaxPacketTime() {
		// when
		int maxTime = 123;
		
		// given
		MaxPacketTimeAttribute obj = new MaxPacketTimeAttribute();
		obj.setTime(maxTime);
		
		// then
		Assert.assertEquals(maxTime, obj.getTime());
		Assert.assertEquals("a=maxptime:123", obj.toString());
	}

}
