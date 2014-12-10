package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.fields.AttributeField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class AttributeFieldTest {

	@Test
	public void testSimpleAttribute() {
		// given
		AttributeField field = new AttributeField();
		String key = "ice-lite";

		// when
		field.key = key;

		// then
		Assert.assertEquals(key, field.getKey());
		Assert.assertNull(field.getValue());
		Assert.assertEquals("a=" + key, field.toString());
	}

	@Test
	public void testComplexAttribute() {
		// given
		AttributeField field = new AttributeField();
		String key = "ice-ufrag";
		String value = "xmgFsdf";

		// when
		field.key = key;
		field.value = value;

		// then
		Assert.assertEquals(key, field.getKey());
		Assert.assertEquals(value, field.getValue());
		Assert.assertEquals("a=" + key + ":" + value, field.toString());
	}
	
}
