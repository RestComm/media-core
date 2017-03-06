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

package org.restcomm.media.sdp.ice.attributes;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.ice.attributes.IcePwdAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IcePwdAttributeTest {
	
	@Test
	public void testCustomIcePwd() {
		// given
		String password = "P4sSw0Rd";
		
		// when
		IcePwdAttribute obj = new IcePwdAttribute();
		obj.setPassword(password);
		
		// then
		Assert.assertEquals(password, obj.getPassword());
		Assert.assertEquals("a=ice-pwd:P4sSw0Rd", obj.toString());
	}

}