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
import org.restcomm.media.sdp.fields.ConnectionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionFieldTest {

	@Test
	public void testDefaultConnection() {
		// given
		ConnectionField field;

		// when
		field = new ConnectionField();

		// then
		Assert.assertEquals("IN", field.getNetworkType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
		String expected = "c=IN IP4 0.0.0.0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomConnection() {
		// given
		String networkType = "XYZ";
		String addressType = "IP6";
		String address = "0.0.0.0";

		// when
		ConnectionField field = new ConnectionField(networkType, addressType,address);

		// then
		Assert.assertEquals(networkType, field.getNetworkType());
		Assert.assertEquals(addressType, field.getAddressType());
		Assert.assertEquals(address, field.getAddress());
		String expected = "c=" + networkType + " " + addressType + " " + address;
		Assert.assertEquals(expected, field.toString());
	}

}
