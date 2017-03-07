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
import org.restcomm.media.sdp.fields.VersionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class VersionFieldTest {

	@Test
	public void testDefaultVersion() {
		// given
		VersionField sdp;

		// when
		sdp = new VersionField();

		// then
		Assert.assertEquals(0, sdp.getVersion());
		Assert.assertEquals("v=0", sdp.toString());
	}

	@Test
	public void testCustomVersion() {
		// given
		VersionField sdp;
		short version = 5;

		// when
		sdp = new VersionField(version);

		// then
		Assert.assertEquals(version, sdp.getVersion());
		Assert.assertEquals("v=" + version, sdp.toString());
	}

}
