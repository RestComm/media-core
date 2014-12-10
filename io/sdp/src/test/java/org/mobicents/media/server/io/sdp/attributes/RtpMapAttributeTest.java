package org.mobicents.media.server.io.sdp.attributes;

import junit.framework.Assert;

import org.junit.Test;

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
