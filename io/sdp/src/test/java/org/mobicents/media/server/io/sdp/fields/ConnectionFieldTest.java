package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;

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
		Assert.assertEquals("127.0.0.1", field.getAddress());
		String expected = "c=IN IP4 127.0.0.1";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomConnection() {
		// given
		String networkType = "XYZ";
		String addressType = "IP6";
		String address = "0.0.0.0";

		// when
		ConnectionField field = new ConnectionField(networkType, addressType, address);

		// then
		Assert.assertEquals(networkType, field.getNetworkType());
		Assert.assertEquals(addressType, field.getAddressType());
		Assert.assertEquals(address, field.getAddress());
		String expected = "c=" + networkType + " " + addressType + " " + address;
		Assert.assertEquals(expected, field.toString());
	}
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "c=IN IP6 0.0.0.0";
		String invalidLine1 = "c=IN 127.0.0.1";
		String invalidLine2 = "x=IN IP6 0.0.0.0";
		
		// when
		ConnectionField field = new ConnectionField();
		
		// then
		Assert.assertTrue(field.canParse(validLine));
		Assert.assertFalse(field.canParse(invalidLine1));
		Assert.assertFalse(field.canParse(invalidLine2));
	}

	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "c=IN IP6 0.0.0.0";

		// when
		ConnectionField field = new ConnectionField();
		field.parse(line);

		// then
		Assert.assertEquals("IN", field.getNetworkType());
		Assert.assertEquals("IP6", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=IN 127.0.0.1";

		// when
		ConnectionField field = new ConnectionField();
		field.parse(line);
	}

}
