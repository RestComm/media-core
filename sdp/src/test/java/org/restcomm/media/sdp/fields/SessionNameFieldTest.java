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
import org.restcomm.media.sdp.fields.SessionNameField;

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
