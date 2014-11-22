package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.OriginField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class OriginFieldParserTest {

	private final OriginFieldParser parser = new OriginFieldParser();
	

	@Test
	public void testCanParse() {
		// given
		String validLine = "o=- 30 1 IN IP4 127.0.0.1";
		String invalidLine1 = "o=- 30 1 IN 127.0.0.1";
		String invalidLine2 = "o=- xyz 1 IN 127.0.0.1";
		String invalidLine3 = "a=- 30 1 IN 127.0.0.1";
		
		// when
		boolean canParseValidLine = parser.canParse(validLine);
		boolean canParseInvalidLine1 = parser.canParse(invalidLine1);
		boolean canParseInvalidLine2 = parser.canParse(invalidLine2);
		boolean canParseInvalidLine3 = parser.canParse(invalidLine3);
		
		// then
		Assert.assertTrue(canParseValidLine);
		Assert.assertFalse(canParseInvalidLine1);
		Assert.assertFalse(canParseInvalidLine2);
		Assert.assertFalse(canParseInvalidLine3);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "o=- 30 1 IN IP4 127.0.0.1";
		
		// when
		OriginField field = parser.parse(line);
		
		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals(30, field.getSessionId());
		Assert.assertEquals(1, field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("127.0.0.1", field.getAddress());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "o=- 30 1 IN IP4 127.0.0.1";
		String line2 = "o=xyz 40 2 IN IP6 0.0.0.0";
		
		// when
		OriginField field = parser.parse(line1);
		parser.parse(field, line2);
		
		// then
		Assert.assertEquals("xyz", field.getUsername());
		Assert.assertEquals(40, field.getSessionId());
		Assert.assertEquals(2, field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP6", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=- 30 1 IN 127.0.0.1";
		
		// when
		parser.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testInvalidParseNumberFormat() throws SdpException {
		// given
		String line = "o=- xyz 1 IN IP4 127.0.0.1";
		
		// when
		parser.parse(line);
	}
	
}
