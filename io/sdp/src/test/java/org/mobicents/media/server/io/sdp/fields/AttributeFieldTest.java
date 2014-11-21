package org.mobicents.media.server.io.sdp.fields;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.AttributeField;
import org.mobicents.media.server.io.sdp.SdpException;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class AttributeFieldTest {

	@Test
	public void testSimpleAttribute() {
		// given
		SimpleAttributeField field = new SimpleAttributeField();
		String key = "ice-lite";

		// when
		field.setKey(key);

		// then
		Assert.assertEquals(key, field.getKey());
		Assert.assertNull(field.getValue());
		Assert.assertEquals("a=" + key, field.toString());
	}

	@Test
	public void testSimpleAttributeValidParsing() throws SdpException {
		// given
		String line = "a=ice-lite";

		// when
		SimpleAttributeField field = new SimpleAttributeField();
		field.parse(line);

		// then
		Assert.assertEquals("ice-lite", field.getKey());
		Assert.assertNull(field.getValue());
	}

	@Test(expected = SdpException.class)
	public void testSimpleAttributeInvalidParsing() throws SdpException {
		// given
		String line = "a=:";

		// when
		SimpleAttributeField field = new SimpleAttributeField();
		field.parse(line);
	}

	@Test
	public void testComplexAttribute() {
		// given
		ComplexAttributeField field = new ComplexAttributeField();
		String key = "ice-ufrag";
		String value = "xmgFsdf";

		// when
		field.setKey(key);
		field.setValue(value);

		// then
		Assert.assertEquals(key, field.getKey());
		Assert.assertEquals(value, field.getValue());
		Assert.assertEquals("a=" + key + ":" + value, field.toString());
	}

	@Test
	public void testComplexAttributeValidParsing() throws SdpException {
		// given
		String line = "a=ice-ufrag:xmgFsdf";

		// when
		ComplexAttributeField field = new ComplexAttributeField();
		field.parse(line);

		// then
		Assert.assertEquals("ice-ufrag", field.getKey());
		Assert.assertEquals("xmgFsdf", field.getValue());
	}

	@Test(expected = SdpException.class)
	public void testComplexAttributeInvalidParsing() throws SdpException {
		// given
		String line = "a=ice-lite";

		// when
		ComplexAttributeField field = new ComplexAttributeField();
		field.parse(line);
	}

	private class SimpleAttributeField extends AttributeField {

		@Override
		protected boolean isComplex() {
			return false;
		}

		public void setKey(String key) {
			this.key = key;
		}

		@Override
		public boolean canParse(String text) {
			return false;
		}

	}

	private class ComplexAttributeField extends AttributeField {

		@Override
		protected boolean isComplex() {
			return true;
		}

		public void setKey(String key) {
			this.key = key;
		}

		public void setValue(String value) {
			this.value = value;
		}

		@Override
		public boolean canParse(String text) {
			return false;
		}

	}
}
