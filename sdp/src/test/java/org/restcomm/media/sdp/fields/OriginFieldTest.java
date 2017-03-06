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
import org.restcomm.media.sdp.fields.OriginField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class OriginFieldTest {

	@Test
	public void testDefaultOrigin() {
		// given
		OriginField field;

		// when
		field = new OriginField();

		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals("0", field.getSessionId());
		Assert.assertEquals("1", field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
		String expected = "o=- 0 1 IN IP4 0.0.0.0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomOrigin() {
		// given
		String sessionId = String.valueOf(System.currentTimeMillis());
		String address = "127.0.0.1";

		// when
		OriginField field = new OriginField(sessionId, address);

		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals(sessionId, field.getSessionId());
		Assert.assertEquals("1", field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals(address, field.getAddress());
		String expected = "o=- " + sessionId + " 1 IN IP4 " + address;
		Assert.assertEquals(expected, field.toString());
	}

}
