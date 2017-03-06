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

package org.restcomm.media.sdp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.attributes.FormatParameterAttribute;
import org.restcomm.media.sdp.attributes.parser.FormatParameterAttributeParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FormatParameterAttributeParserTest {

	private final FormatParameterAttributeParser parser = new FormatParameterAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		String sdp2 = "a=fmtp:101 0-15\n\r";
		String sdp3 = "x=fmtp:101 0-15\n\r";
		String sdp4 = "a=fmtp:xyz 0-15\n\r";
		String sdp5 = "a=fmtp:101\n\r";

		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);

		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		
		// when
		FormatParameterAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(18, obj.getFormat());
		Assert.assertEquals("annexb=no", obj.getParams());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=fmtp:18 annexb=no\n\r";
		String sdp2 = "a=fmtp:101 0-15\n\r";
		
		// when
		FormatParameterAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(101, obj.getFormat());
		Assert.assertEquals("0-15", obj.getParams());
	}
	
}
