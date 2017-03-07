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

import org.junit.Test;
import org.restcomm.media.sdp.ice.attributes.parser.IceLiteAttributeParser;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class IceLiteAttributeParserTest {
	
	private final IceLiteAttributeParser parser = new IceLiteAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ice-lite\n\r";
		String sdp2 = "x=ice-lite\n\r";
		String sdp3 = "a=ice-pwd\n\r";
		String sdp4 = "a=ice-lite:xyz\n\r";
		
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

}
