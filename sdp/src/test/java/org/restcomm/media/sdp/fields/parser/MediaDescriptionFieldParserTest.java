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

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.restcomm.media.sdp.SdpException;
import org.restcomm.media.sdp.fields.MediaDescriptionField;
import org.restcomm.media.sdp.fields.parser.MediaDescriptionFieldParser;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class MediaDescriptionFieldParserTest {
	
	private final MediaDescriptionFieldParser parser = new MediaDescriptionFieldParser();

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		String sdp2 = "x=video 64534 RTP/AVPF 0 101 120\n\r";
		String sdp3 = "m=video xyz RTP/AVPF 0 101 120\n\r";
		String sdp4 = "m=video 64534 0 101 120\n\r";
		String sdp5 = "m=video 64534 RTP/AVPF 0 xyz 120\n\r";
		String sdp6 = "m=image 52550 udptl t38\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		boolean canParseSdp5 = parser.canParse(sdp5);
		boolean canParseSdp6 = parser.canParse(sdp6);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertTrue(canParseSdp5);
		Assert.assertTrue(canParseSdp6);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		
		// when
		MediaDescriptionField md = parser.parse(line);
		
		// then
		Assert.assertEquals("video", md.getMedia());
		Assert.assertEquals(64534, md.getPort());
		Assert.assertEquals("RTP/AVPF", md.getProtocol());
		Assert.assertTrue(md.containsPayloadType("0"));
		Assert.assertTrue(md.containsPayloadType("101"));
		Assert.assertTrue(md.containsPayloadType("120"));
	}
	
    @Test
    public void testParseImageType() throws SdpException {
        // given
        String line = "m=image 52550 udptl t38\n\r";

        // when
        MediaDescriptionField md = parser.parse(line);

        // then
        Assert.assertEquals("image", md.getMedia());
        Assert.assertEquals(52550, md.getPort());
        Assert.assertEquals("udptl", md.getProtocol());
        Assert.assertTrue(md.containsPayloadType("t38"));
    }

	@Test
	public void testParseOverwrite() throws SdpException {
		// given
		String line1 = "m=video 64534 RTP/AVPF 0 101 120\n\r";
		String line2 = "m=audio 65000 RTP/SAVPF 0 101 180\n\r";
		
		// when
		MediaDescriptionField md = parser.parse(line1);
		parser.parse(md, line2);
		
		// then
		Assert.assertEquals("audio", md.getMedia());
		Assert.assertEquals(65000, md.getPort());
		Assert.assertEquals("RTP/SAVPF", md.getProtocol());
		Assert.assertTrue(md.containsPayloadType("0"));
		Assert.assertTrue(md.containsPayloadType("101"));
		Assert.assertTrue(md.containsPayloadType("180"));
		Assert.assertFalse(md.containsPayloadType("120"));
	}

	@Test(expected=SdpException.class)
	public void testParseMissingMedia() throws SdpException {
		// given
		String line = "m=64534 RTP/AVPF 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

	@Test(expected=SdpException.class)
	public void testParseInvalidPortFormat() throws SdpException {
		// given
		String line = "m=64534 xyz RTP/AVPF 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

	@Ignore
	@Test(expected=SdpException.class)
	public void testParseMissingProtocol() throws SdpException {
		// given
		String line = "m=64534 63000 0 101 120\n\r";
		
		// when
		parser.parse(line);
	}

}
