package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionFieldTest {

	@Test
	public void testDefaultConnection() {
		// given
		ConnectionField field;

		// when
		field = new ConnectionField();

		// then
		Assert.assertEquals("IN", field.getNetworkType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
		String expected = "c=IN IP4 0.0.0.0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomConnection() {
		// given
		String networkType = "XYZ";
		String addressType = "IP6";
		String address = "0.0.0.0";

		// when
		ConnectionField field = new ConnectionField(networkType, addressType,address);

		// then
		Assert.assertEquals(networkType, field.getNetworkType());
		Assert.assertEquals(addressType, field.getAddressType());
		Assert.assertEquals(address, field.getAddress());
		String expected = "c=" + networkType + " " + addressType + " " + address;
		Assert.assertEquals(expected, field.toString());
	}

}
