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
import org.restcomm.media.sdp.fields.OriginField;
import org.restcomm.media.sdp.fields.parser.OriginFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class OriginFieldParserTest {

	private final OriginFieldParser parser = new OriginFieldParser();
	

	@Test
	public void testCanParse() {
		// given
		String validLine = "o=- 30 1 IN IP4 127.0.0.1\n\r";
		String invalidLine1 = "o=- 30 1 IN 127.0.0.1\n\r";
		String invalidLine2 = "o=- xyz 1 IN 127.0.0.1\n\r";
		String invalidLine3 = "a=- 30 1 IN 127.0.0.1\n\r";
		
		// when
		boolean canParseValidLine = parser.canParse(validLine);
		boolean canParseInvalidLine1 = parser.canParse(invalidLine1);
		boolean canParseInvalidLine2 = parser.canParse(invalidLine2);
		boolean canParseInvalidLine3 = parser.canParse(invalidLine3);
		
		// then
		Assert.assertTrue(canParseValidLine);
		Assert.assertFalse(canParseInvalidLine1);
		Assert.assertFalse(canParseInvalidLine2);
		Assert.assertFalse(canParseInvalidLine3);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "o=- 30 1 IN IP4 127.0.0.1\n\r";
		
		// when
		OriginField field = parser.parse(line);
		
		// then
		Assert.assertEquals("-", field.getUsername());
		Assert.assertEquals("30", field.getSessionId());
		Assert.assertEquals("1", field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP4", field.getAddressType());
		Assert.assertEquals("127.0.0.1", field.getAddress());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "o=- 30 1 IN IP4 127.0.0.1\n\r";
		String line2 = "o=xyz 40 2 IN IP6 0.0.0.0\n\r";
		
		// when
		OriginField field = parser.parse(line1);
		parser.parse(field, line2);
		
		// then
		Assert.assertEquals("xyz", field.getUsername());
		Assert.assertEquals("40", field.getSessionId());
		Assert.assertEquals("2", field.getSessionVersion());
		Assert.assertEquals("IN", field.getNetType());
		Assert.assertEquals("IP6", field.getAddressType());
		Assert.assertEquals("0.0.0.0", field.getAddress());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=- 30 1 IN 127.0.0.1\n\r";
		
		// when
		parser.parse(line);
	}
	
}
