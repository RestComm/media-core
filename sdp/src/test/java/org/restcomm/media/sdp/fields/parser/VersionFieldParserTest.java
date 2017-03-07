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
import org.restcomm.media.sdp.fields.VersionField;
import org.restcomm.media.sdp.fields.parser.VersionFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class VersionFieldParserTest {
	
	private final VersionFieldParser parser = new VersionFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "v=123\n\r";
		String sdp2 = "x=123\n\r";
		String sdp3 = "v=abc\n\r";
		String sdp4 = "v=123 213\n\r";
		
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
	public void testParse() throws SdpException {
		// given
		String line = "v=111\n\r";
		
		// when
		VersionField field = parser.parse(line);
		
		// then
		Assert.assertEquals(111, field.getVersion());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParse() throws SdpException {
		// given
		String line = "v=1 xyz\n\r";
		
		// when
		parser.parse(line);
	}

}
