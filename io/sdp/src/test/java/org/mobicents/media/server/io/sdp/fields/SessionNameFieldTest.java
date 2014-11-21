package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SessionNameFieldTest {
	
	@Test
	public void testDefaultSessionName() {
		// given
		SessionNameField field;

		// when
		field = new SessionNameField();

		// then
		Assert.assertEquals(" ", field.getName());
		Assert.assertEquals("s= ", field.toString());
	}

	@Test
	public void testCustomVersion() {
		// given
		SessionNameField field;
		String name = "xyz";

		// when
		field = new SessionNameField();
		field.setName(name);

		// then
		Assert.assertEquals(name, field.getName());
		Assert.assertEquals("s=" + name, field.toString());
	}
	
	@Test
	public void testCanParse() {
		// given
		String validLine1 = "s=xyz";
		String validLine2 = "s= ";
		String validLine3 = "s=";
		String invalidLine2 = "s=  ";
		
		// when
		SessionNameField field = new SessionNameField();
		
		// then
		Assert.assertTrue(field.canParse(validLine1));
		Assert.assertTrue(field.canParse(validLine2));
		Assert.assertTrue(field.canParse(validLine3));
		Assert.assertFalse(field.canParse(invalidLine2));
	}
	
	@Test
	public void testParsing() throws SdpException {
		// given
		String line = "s=xyz";
		
		// when
		SessionNameField field = new SessionNameField();
		field.parse(line);
		
		// then
		Assert.assertEquals("xyz", field.getName());
	}

}
