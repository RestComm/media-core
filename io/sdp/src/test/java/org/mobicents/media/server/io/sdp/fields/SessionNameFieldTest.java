package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
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
	public void testValidParsing() throws SdpException {
		// given
		String line = "s=xyz";
		
		// when
		SessionNameField field = new SessionNameField();
		field.parse(line);
		
		// then
		Assert.assertEquals("xyz", field.getName());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsing() throws SdpException {
		// given
		String line = "v=xyz";
		
		// when
		SessionNameField field = new SessionNameField();
		field.parse(line);
	}

}
