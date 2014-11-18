package org.mobicents.media.server.io.sdp.fields;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.OriginField;

import junit.framework.Assert;

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
		Assert.assertEquals("127.0.0.1", field.getAddress());
		String expected = "o=- 0 1 IN IP4 127.0.0.1";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomOrigin() {
		// given
		int sessionId = 123;
		String address = "0.0.0.0";

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
	
	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "o=- 30 1 IN IP4 127.0.0.1";
		
		// when
		OriginField field = new OriginField();
		field.parse(line);
		
		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals(30, field.getSessionId());
		Assert.assertEquals(1, field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("127.0.0.1", field.getAddress());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=- 30 1 IN 127.0.0.1";
		
		// when
		OriginField field = new OriginField();
		field.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testInvalidParseNumberFormat() throws SdpException {
		// given
		String line = "o=- xyz 1 IN IP4 127.0.0.1";
		
		// when
		OriginField field = new OriginField();
		field.parse(line);
	}

}
