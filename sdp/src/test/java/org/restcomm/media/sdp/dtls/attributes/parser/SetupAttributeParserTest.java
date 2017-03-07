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

package org.restcomm.media.sdp.dtls.attributes.parser;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.dtls.attributes.SetupAttribute;
import org.restcomm.media.sdp.dtls.attributes.parser.SetupAttributeParser;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SetupAttributeParserTest {

	private static final SetupAttributeParser PARSER = new SetupAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=setup:active\n\r";
		String sdp2 = "a=setup:passive\n\r";
		String sdp3 = "a=setup:actpass\n\r";
		String sdp4 = "a=setup:holdconn\n\r";
		String sdp5 = "x=setup:active\n\r";
		String sdp6 = "a=setup:xyz\n\r";
		String sdp7 = "a=setup:\n\r";
		
		// when
		boolean canParseSdp1 = PARSER.canParse(sdp1);
		boolean canParseSdp2 = PARSER.canParse(sdp2);
		boolean canParseSdp3 = PARSER.canParse(sdp3);
		boolean canParseSdp4 = PARSER.canParse(sdp4);
		boolean canParseSdp5 = PARSER.canParse(sdp5);
		boolean canParseSdp6 = PARSER.canParse(sdp6);
		boolean canParseSdp7 = PARSER.canParse(sdp7);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertTrue(canParseSdp3);
		Assert.assertTrue(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=setup:active\n\r";
		
		// when
		SetupAttribute obj = PARSER.parse(sdp1);
		
		// then
		Assert.assertNotNull(obj);
		Assert.assertEquals("active", obj.getValue());
	}

	@Test
	public void testParseOverwirte() throws SdpException {
		// given
		String sdp1 = "a=setup:active\n\r";
		String sdp2 = "a=setup:passive\n\r";
		
		// when
		SetupAttribute obj = PARSER.parse(sdp1);
		PARSER.parse(obj, sdp2);
		
		// then
		Assert.assertEquals("passive", obj.getValue());
	}
	
	@Test(expected = SdpException.class)
	public void testParseMissingRole() throws SdpException {
		// given
		String sdp1 = "a=setup:\n\r";

		// when
		PARSER.parse(sdp1);
	}

	@Test(expected = SdpException.class)
	public void testParseInvalidRole() throws SdpException {
		// given
		String sdp1 = "a=setup:other";
		
		// when
		PARSER.parse(sdp1);
	}
	
}
