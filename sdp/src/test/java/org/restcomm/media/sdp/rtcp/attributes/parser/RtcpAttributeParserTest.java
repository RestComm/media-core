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

package org.restcomm.media.sdp.rtcp.attributes.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.rtcp.attributes.RtcpAttribute;
import org.restcomm.media.sdp.rtcp.attributes.parser.RtcpAttributeParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtcpAttributeParserTest {
	
	private static final RtcpAttributeParser parser = new RtcpAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=rtcp:65000\n\r";
		String sdp2 = "x=rtcp:65000\n\r";
		String sdp3 = "a=rtcp:xyz\n\r";
		String sdp4 = "a=rtcp:65000 xyz\n\r";
		
		String sdp5 = "a=rtcp:65000 IN IP4 127.0.0.1\n\r";
		String sdp6 = "a=rtcp:65000 IP4 127.0.0.1\n\r";
		String sdp7 = "a=rtcp:65000 IN 127.0.0.1\n\r";
		String sdp8 = "a=rtcp:65000 IN IP4 xyz\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		boolean canParseSdp7 = parser.canParse(sdp7);
		boolean canParseSdp8 = parser.canParse(sdp8);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertTrue(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
		Assert.assertFalse(canParseSdp8);
	}

	@Test
	public void testParseSimpleAttribute() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000\n\r";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertNull(obj.getNetworkType());
		Assert.assertNull(obj.getAddressType());
		Assert.assertNull(obj.getAddress());
	}

	@Test
	public void testParseComplexAttribute() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000 IN IP4 127.0.0.1\n\r";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertEquals("IN", obj.getNetworkType());
		Assert.assertEquals("IP4", obj.getAddressType());
		Assert.assertEquals("127.0.0.1", obj.getAddress());
	}

	@Test
	public void testParseSimpleToComplexOverwrite() throws SdpException {
		// given
		String sdp1 = "a=rtcp:65000\n\r";
		String sdp2 = "a=rtcp:64000 IN IP4 127.0.0.1\n\r";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(64000, obj.getPort());
		Assert.assertEquals("IN", obj.getNetworkType());
		Assert.assertEquals("IP4", obj.getAddressType());
		Assert.assertEquals("127.0.0.1", obj.getAddress());
	}

	@Test
	public void testParseComplexToSimpleOverwrite() throws SdpException {
		// given
		String sdp1 = "a=rtcp:64000 IN IP4 127.0.0.1\n\r";
		String sdp2 = "a=rtcp:65000\n\r";
		
		// when
		RtcpAttribute obj = parser.parse(sdp1);
		parser.parse(obj, sdp2);
		
		// then
		Assert.assertEquals(65000, obj.getPort());
		Assert.assertNull(obj.getNetworkType());
		Assert.assertNull(obj.getAddressType());
		Assert.assertNull(obj.getAddress());
	}

}
