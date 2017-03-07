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
import org.restcomm.media.sdp.attributes.RtpMapAttribute;
import org.restcomm.media.sdp.attributes.parser.RtpMapAttributeParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMapAttributeParserTest {
	
	private final RtpMapAttributeParser parser = new RtpMapAttributeParser();
	

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtpmap:97 L16/8000\n\r";
		String sdp2 = "a=rtpmap:98 L16/11025/2\n\r";
		String sdp3 = "x=rtpmap:98 L16/11025/2\n\r";
		String sdp4 = "a=xyz:98 L16/11025/2\n\r";
		String sdp5 = "a=rtpmap:xyz L16/11025/2\n\r";
		String sdp6 = "a=rtpmap:98 L16/xyz/2\n\r";
		String sdp7 = "a=rtpmap:98 L16/11025/xyz\n\r";

		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		boolean canParseSdp7 = parser.canParse(sdp7);

		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertTrue(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}

	@Test
	public void testSimpleParse() throws SdpException {
		// given
		String line = "a=rtpmap:97 L16/8000\n\r";

		// when
		RtpMapAttribute attr = parser.parse(line);

		// then
		Assert.assertEquals(97, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(8000, attr.getClockRate());
		Assert.assertEquals(1, attr.getCodecParams());
	}

	@Test
	public void testComplexParse() throws SdpException {
		// given
		String line = "a=rtpmap:98 L16/11025/2\n\r";

		// when
		RtpMapAttribute attr = parser.parse(line);

		// then
		Assert.assertEquals(98, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(11025, attr.getClockRate());
		Assert.assertEquals(2, attr.getCodecParams());
	}

	@Test
	public void testSimpleToComplexParseOverwrite() throws SdpException {
		// given
		String line1 = "a=rtpmap:97 L16/8000\n\r";
		String line2 = "a=rtpmap:98 L16/11025/2\n\r";
		
		// when
		RtpMapAttribute attr = parser.parse(line1);
		parser.parse(attr, line2);
		
		// then
		Assert.assertEquals(98, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(11025, attr.getClockRate());
		Assert.assertEquals(2, attr.getCodecParams());
	}

	@Test
	public void testComplexToSimpleParseOverwrite() throws SdpException {
		// given
		String line1 = "a=rtpmap:98 L16/11025/2\n\r";
		String line2 = "a=rtpmap:97 L16/8000\n\r";
		
		// when
		RtpMapAttribute attr = parser.parse(line1);
		parser.parse(attr, line2);
		
		// then
		Assert.assertEquals(97, attr.getPayloadType());
		Assert.assertEquals("L16", attr.getCodec());
		Assert.assertEquals(8000, attr.getClockRate());
		Assert.assertEquals(1, attr.getCodecParams());
	}

}
