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
import org.restcomm.media.sdp.fields.TimingField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TimingFieldTest {

	@Test
	public void testDefaultTiming() {
		// given
		TimingField field;

		// when
		field = new TimingField();

		// then
		Assert.assertEquals(0, field.getStartTime());
		Assert.assertEquals(0, field.getStopTime());
		String expected = "t=0 0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomConnection() {
		// given
		int startTime = 123;
		int stopTime = 456;

		// when
		TimingField field = new TimingField();
		field.setStartTime(startTime);
		field.setStopTime(stopTime);

		// then
		Assert.assertEquals(startTime, field.getStartTime());
		Assert.assertEquals(stopTime, field.getStopTime());
		String expected = "t=" + startTime + " " + stopTime;
		Assert.assertEquals(expected, field.toString());
	}
	
}
