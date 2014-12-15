package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SessionNameFieldParserTest {
	
	private final SessionNameFieldParser parser = new SessionNameFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String validLine1 = "s=xyz\n\r";
		String validLine2 = "s= \n\r";
		String validLine3 = "s=\n\r";
		String validLine4 = "s=  \n\r";
		
		// when
		boolean canParseValidLine1 = parser.canParse(validLine1);
		boolean canParseValidLine2 = parser.canParse(validLine2);
		boolean canParseValidLine3 = parser.canParse(validLine3);
		boolean canParseValidLine4 = parser.canParse(validLine4);
		
		// then
		Assert.assertTrue(canParseValidLine1);
		Assert.assertTrue(canParseValidLine2);
		Assert.assertTrue(canParseValidLine3);
		Assert.assertTrue(canParseValidLine4);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "s=xyz\n\r";
		
		// when
		SessionNameField field = parser.parse(line);
		
		// then
		Assert.assertEquals("xyz", field.getName());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "s=xyz\n\r";
		String line2 = "s=abc\n\r";
		
		// when
		SessionNameField field = parser.parse(line1);
		parser.parse(field, line2);
		
		// then
		Assert.assertEquals("abc", field.getName());
	}

}
