package org.mobicents.media.server.io.sdp.attributes;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class PacketTimeAttributeTest {
	
	@Test
	public void testDefaultPacketTime() {
		// given
		
		// when
		PacketTimeAttribute obj = new PacketTimeAttribute();
		
		// then
		Assert.assertEquals(0, obj.getTime());
		Assert.assertEquals("a=ptime:0", obj.toString());
	}
	
	@Test
	public void testCustomPacketTime() {
		// given
		short time = 123;
		
		// when
		PacketTimeAttribute obj = new PacketTimeAttribute();
		obj.setTime(123);
		
		// then
		Assert.assertEquals(time, obj.getTime());
		Assert.assertEquals("a=ptime:123", obj.toString());
	}

}
