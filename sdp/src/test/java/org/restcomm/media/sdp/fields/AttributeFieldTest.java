/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */

package org.restcomm.media.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.fields.AttributeField;

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
