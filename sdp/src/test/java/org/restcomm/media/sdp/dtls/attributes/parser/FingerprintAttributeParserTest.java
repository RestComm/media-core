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
import org.restcomm.media.sdp.dtls.attributes.FingerprintAttribute;
import org.restcomm.media.sdp.dtls.attributes.parser.FingerprintAttributeParser;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FingerprintAttributeParserTest {

	private final FingerprintAttributeParser parser = new FingerprintAttributeParser();

	@Test
	public void testCanParse() {
		// given
		String sdp1 = "a=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";
		String sdp2 = "x=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";
		String sdp3 = "a=finger:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";
		String sdp4 = "a=fingerprint:D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";
		String sdp5 = "a=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39:\n\r";
		String sdp6 = "a=fingerprint:sha-256 D1:2C:BE: AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39:\n\r";
		String sdp7 = "a=fingerprint:sha-256\n\r";
		
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
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
		Assert.assertFalse(canParseSdp5);
		Assert.assertFalse(canParseSdp6);
		Assert.assertFalse(canParseSdp7);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String sdp1 = "a=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";

		// when
		FingerprintAttribute obj = parser.parse(sdp1);

		// then
		Assert.assertEquals("sha-256", obj.getHashFunction());
		Assert.assertEquals("D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39", obj.getFingerprint());
	}

	@Test(expected=SdpException.class)
	public void testParseMissingHashFunction() throws SdpException {
		// given
		String sdp1 = "a=fingerprint:D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39\n\r";
		
		// when
		parser.parse(sdp1);
	}

	@Test(expected=SdpException.class)
	public void testParseMissingFingerprint() throws SdpException {
		// given
		String sdp1 = "a=fingerprint:sha-256";
		
		// when
		parser.parse(sdp1);
	}
	
}
