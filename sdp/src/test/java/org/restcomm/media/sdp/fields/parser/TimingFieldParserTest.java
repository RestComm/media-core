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

package org.restcomm.media.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.fields.TimingField;
import org.restcomm.media.sdp.fields.parser.TimingFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TimingFieldParserTest {
	
	private final TimingFieldParser parser = new TimingFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "t=123 456\n\r";
		String sdp2 = "t=xyz 456\n\r";
		String sdp3 = "t=123 xyz\n\r";
		String sdp4 = "x=123 456\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
	}

	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "t=123 456\n\r";

		// when
		TimingField field = parser.parse(line);

		// then
		Assert.assertEquals(123, field.getStartTime());
		Assert.assertEquals(456, field.getStopTime());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "t=123\n\r";

		// when
		parser.parse(line);
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseNumberFormat() throws SdpException {
		// given
		String line = "t=123 xyz\n\r";
		
		// when
		parser.parse(line);
	}

}
