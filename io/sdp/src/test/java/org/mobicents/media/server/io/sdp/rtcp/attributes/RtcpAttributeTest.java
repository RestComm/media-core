package org.mobicents.media.server.io.sdp.rtcp.attributes;

import org.junit.Test;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class RtcpAttributeTest {

	@Test
	public void testCustomRtcp() {
		// given
		int port = 61234;
		
		// when
		RtcpAttribute obj = new RtcpAttribute();
		obj.setPort(port);
		
		// then
		Assert.assertEquals("a=rtcp:61234", obj.toString());
	}
	
}
