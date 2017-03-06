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

package org.restcomm.media.sdp.attributes;

import org.junit.Test;
import org.restcomm.media.sdp.attributes.FormatParameterAttribute;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatAttributeTest {

	@Test
	public void testCustomFormat() {
		// given
		short format = 101;
		String params = "0-15";
		
		// when
		FormatParameterAttribute obj = new FormatParameterAttribute();
		obj.setFormat(format);
		obj.setParams(params);
		
		// given
		Assert.assertEquals(format, obj.getFormat());
		Assert.assertEquals(params, obj.getParams());
		Assert.assertEquals("a=fmtp:101 0-15", obj.toString());
	}

}
