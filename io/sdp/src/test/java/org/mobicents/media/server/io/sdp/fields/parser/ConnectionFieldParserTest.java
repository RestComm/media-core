package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ConnectionFieldParserTest {
	
	private final ConnectionFieldParser parser = new ConnectionFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "c=IN IP6 0.0.0.0";
		String invalidLine1 = "c=IN 127.0.0.1";
		String invalidLine2 = "x=IN IP6 0.0.0.0";
		
		// when
		boolean canParseValidLine = parser.canParse(validLine);
		boolean canParseInvalidLine1 = parser.canParse(invalidLine1);
		boolean canParseInvalidLine2 = parser.canParse(invalidLine2);
		
		// then
		Assert.assertTrue(canParseValidLine);
		Assert.assertFalse(canParseInvalidLine1);
		Assert.assertFalse(canParseInvalidLine2);
	}

	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "c=IN IP6 0.0.0.0";

		// when
		ConnectionField connection = parser.parse(line);

		// then
		Assert.assertEquals("IN", connection.getNetworkType());
		Assert.assertEquals("IP6", connection.getAddressType());
		Assert.assertEquals("0.0.0.0", connection.getAddress());
	}
	
	@Test
	public void testValidParseOverwrite() throws SdpException {
		// given
		String line1 = "c=IN IP6 0.0.0.0";
		String line2 = "c=IN IP4 127.0.0.1";

		// when
		ConnectionField connection = parser.parse(line1);
		parser.parse(connection, line2);

		// then
		Assert.assertEquals("IN", connection.getNetworkType());
		Assert.assertEquals("IP4", connection.getAddressType());
		Assert.assertEquals("127.0.0.1", connection.getAddress());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=IN 127.0.0.1";

		// when
		parser.parse(line);
	}
	
}
