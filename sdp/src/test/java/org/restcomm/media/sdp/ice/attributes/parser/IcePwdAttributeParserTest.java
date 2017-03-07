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

package org.restcomm.media.sdp.ice.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.ice.attributes.IcePwdAttribute;
import org.restcomm.media.sdp.ice.attributes.parser.IcePwdAttributeParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IcePwdAttributeParserTest {
	
	private final IcePwdAttributeParser parser = new IcePwdAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		String sdp2 = "a=ice-pass: \n\r";
		String sdp3 = "x=ice-pwd:P4ssW0rD\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		
		// when
		IcePwdAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals("P4ssW0rD", obj.getPassword());
	}

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:P4ssW0rD\n\r";
		String sdp2 = "a=ice-pwd:N3wP4sS\n\r";
		
		// when
		IcePwdAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals("N3wP4sS", obj.getPassword());
	}

	@Test(expected=SdpException.class)
	public void testParseMissingPassword() throws SdpException {
		// given
		String sdp1 = "a=ice-pwd:\n\r";
		
		// when
		parser.parse(sdp1);
	}

}
