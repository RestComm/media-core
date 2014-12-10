package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;

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

}
