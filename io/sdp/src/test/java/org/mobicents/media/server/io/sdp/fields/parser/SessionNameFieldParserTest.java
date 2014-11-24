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
		String validLine1 = "s=xyz";
		String validLine2 = "s= ";
		String validLine3 = "s=";
		String invalidLine1 = "s=  ";
		
		// when
		boolean canParseValidLine1 = parser.canParse(validLine1);
		boolean canParseValidLine2 = parser.canParse(validLine2);
		boolean canParseValidLine3 = parser.canParse(validLine3);
		boolean canParseInvalidLine1 = parser.canParse(invalidLine1);
		
		// then
		Assert.assertTrue(canParseValidLine1);
		Assert.assertTrue(canParseValidLine2);
		Assert.assertTrue(canParseValidLine3);
		Assert.assertFalse(canParseInvalidLine1);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "s=xyz";
		
		// when
		SessionNameField field = parser.parse(line);
		
		// then
		Assert.assertEquals("xyz", field.getName());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "s=xyz";
		String line2 = "s=abc";
		
		// when
		SessionNameField field = parser.parse(line1);
		parser.parse(field, line2);
		
		// then
		Assert.assertEquals("abc", field.getName());
	}

}
