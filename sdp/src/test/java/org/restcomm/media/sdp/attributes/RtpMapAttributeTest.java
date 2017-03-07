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

package org.restcomm.media.sdp.attributes;

import junit.framework.Assert;

import org.junit.Test;
import org.restcomm.media.sdp.attributes.RtpMapAttribute;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class RtpMapAttributeTest {

	@Test
	public void testSimpleRtpMap() {
		// given
		RtpMapAttribute obj = new RtpMapAttribute();
		short payloadType = 97;
		String codec = "L16";
		short clockRate = 8000;
		
		// when
		obj.setPayloadType(payloadType);
		obj.setCodec(codec);
		obj.setClockRate(clockRate);

		// then
		Assert.assertEquals(payloadType, obj.getPayloadType());
		Assert.assertEquals(codec, obj.getCodec());
		Assert.assertEquals(clockRate, obj.getClockRate());
		Assert.assertEquals(RtpMapAttribute.DEFAULT_CODEC_PARAMS, obj.getCodecParams());
		Assert.assertEquals("a=rtpmap:97 L16/8000", obj.toString());
	}

	@Test
	public void testComplexRtpMap() {
		// given
		RtpMapAttribute obj = new RtpMapAttribute();
		short payloadType = 97;
		String codec = "L16";
		short clockRate = 8000;
		short codecParams = 20;
		
		// when
		obj.setPayloadType(payloadType);
		obj.setCodec(codec);
		obj.setClockRate(clockRate);
		obj.setCodecParams(codecParams);
		
		// then
		Assert.assertEquals(payloadType, obj.getPayloadType());
		Assert.assertEquals(codec, obj.getCodec());
		Assert.assertEquals(clockRate, obj.getClockRate());
		Assert.assertEquals(codecParams, obj.getCodecParams());
		Assert.assertEquals("a=rtpmap:97 L16/8000/20", obj.toString());
	}

}
