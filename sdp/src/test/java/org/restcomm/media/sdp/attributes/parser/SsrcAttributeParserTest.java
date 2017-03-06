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

import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.attributes.SsrcAttribute;
import org.restcomm.media.sdp.attributes.parser.SsrcAttributeParser;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class SsrcAttributeParserTest {
	
	private final SsrcAttributeParser parser = new SsrcAttributeParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1993357196 xyz\n\r";
		String sdp3 = "x=ssrc:1993357196 xyz\n\r";
		String sdp4 = "a=ssrc:1993357196\n\r";
		String sdp5 = "a=ssrc:1993357196 cname:\n\r";
		
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
	public void testParseSimpleAttribute() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 xyz\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertNull(ssrc.getAttributeValue("xyz"));
	}

	@Test
	public void testParseComplexAttribute() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);

		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", ssrc.getAttributeValue("cname"));
	}
	
	@Test
	public void testParseOverwriteSameSsrc() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1993357196 label:753ad6b1-31a6-4fea-8070-269c6df6ff0e\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		parser.parse(ssrc, sdp2);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1993357196", ssrc.getSsrcId());
		Assert.assertEquals("rfQMs6/rvuHHWkM4", ssrc.getAttributeValue("cname"));
		Assert.assertEquals("753ad6b1-31a6-4fea-8070-269c6df6ff0e", ssrc.getAttributeValue("label"));
	}

	@Test
	public void testParseOverwriteNewSsrc() throws SdpException {
		// given
		String sdp1 = "a=ssrc:1993357196 cname:rfQMs6/rvuHHWkM4\n\r";
		String sdp2 = "a=ssrc:1111111111 label:753ad6b1-31a6-4fea-8070-269c6df6ff0e\n\r";
		
		// when
		SsrcAttribute ssrc = parser.parse(sdp1);
		parser.parse(ssrc, sdp2);
		
		// then
		Assert.assertNotNull(ssrc);
		Assert.assertEquals("1111111111", ssrc.getSsrcId());
		Assert.assertNull(ssrc.getAttributeValue("cname"));
		Assert.assertEquals("753ad6b1-31a6-4fea-8070-269c6df6ff0e", ssrc.getAttributeValue("label"));
	}

}
