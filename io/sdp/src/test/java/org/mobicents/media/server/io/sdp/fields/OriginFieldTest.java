package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class OriginFieldTest {

	@Test
	public void testDefaultOrigin() {
		// given
		OriginField field;

		// when
		field = new OriginField();

		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals(0, field.getSessionId());
		Assert.assertEquals(1, field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
		String expected = "o=- 0 1 IN IP4 0.0.0.0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomOrigin() {
		// given
		int sessionId = 123;
		String address = "127.0.0.1";

		// when
		OriginField field = new OriginField(sessionId, address);

		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals(sessionId, field.getSessionId());
		Assert.assertEquals(1, field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals(address, field.getAddress());
		String expected = "o=- " + sessionId + " 1 IN IP4 " + address;
		Assert.assertEquals(expected, field.toString());
	}

}
