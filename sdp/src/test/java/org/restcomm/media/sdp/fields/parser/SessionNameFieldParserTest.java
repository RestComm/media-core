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
import org.restcomm.media.sdp.fields.SessionNameField;
import org.restcomm.media.sdp.fields.parser.SessionNameFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SessionNameFieldParserTest {
	
	private final SessionNameFieldParser parser = new SessionNameFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String validLine1 = "s=xyz\n\r";
		String validLine2 = "s= \n\r";
		String validLine3 = "s=\n\r";
		String validLine4 = "s=  \n\r";
		
		// when
		boolean canParseValidLine1 = parser.canParse(validLine1);
		boolean canParseValidLine2 = parser.canParse(validLine2);
		boolean canParseValidLine3 = parser.canParse(validLine3);
		boolean canParseValidLine4 = parser.canParse(validLine4);
		
		// then
		Assert.assertTrue(canParseValidLine1);
		Assert.assertTrue(canParseValidLine2);
		Assert.assertTrue(canParseValidLine3);
		Assert.assertTrue(canParseValidLine4);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "s=xyz\n\r";
		
		// when
		SessionNameField field = parser.parse(line);
		
		// then
		Assert.assertEquals("xyz", field.getName());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "s=xyz\n\r";
		String line2 = "s=abc\n\r";
		
		// when
		SessionNameField field = parser.parse(line1);
		parser.parse(field, line2);
		
		// then
		Assert.assertEquals("abc", field.getName());
	}

}
