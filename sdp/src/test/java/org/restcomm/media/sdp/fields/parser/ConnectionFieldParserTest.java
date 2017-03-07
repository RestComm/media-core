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
import org.restcomm.media.sdp.fields.ConnectionField;
import org.restcomm.media.sdp.fields.parser.ConnectionFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class ConnectionFieldParserTest {
	
	private final ConnectionFieldParser parser = new ConnectionFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "c=IN IP6 0.0.0.0\n\r";
		String invalidLine1 = "c=IN 127.0.0.1\n\r";
		String invalidLine2 = "x=IN IP6 0.0.0.0\n\r";
		
		// when
		boolean canParseValidLine = parser.canParse(validLine);
		boolean canParseInvalidLine1 = parser.canParse(invalidLine1);
		boolean canParseInvalidLine2 = parser.canParse(invalidLine2);
		
		// then
		Assert.assertTrue(canParseValidLine);
		Assert.assertFalse(canParseInvalidLine1);
		Assert.assertFalse(canParseInvalidLine2);
	}

	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "c=IN IP6 0.0.0.0\n\r";

		// when
		ConnectionField connection = parser.parse(line);

		// then
		Assert.assertEquals("IN", connection.getNetworkType());
		Assert.assertEquals("IP6", connection.getAddressType());
		Assert.assertEquals("0.0.0.0", connection.getAddress());
	}
	
	@Test
	public void testValidParseOverwrite() throws SdpException {
		// given
		String line1 = "c=IN IP6 0.0.0.0\n\r";
		String line2 = "c=IN IP4 127.0.0.1\n\r";

		// when
		ConnectionField connection = parser.parse(line1);
		parser.parse(connection, line2);

		// then
		Assert.assertEquals("IN", connection.getNetworkType());
		Assert.assertEquals("IP4", connection.getAddressType());
		Assert.assertEquals("127.0.0.1", connection.getAddress());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "o=IN 127.0.0.1\n\r";

		// when
		parser.parse(line);
	}
	
}
