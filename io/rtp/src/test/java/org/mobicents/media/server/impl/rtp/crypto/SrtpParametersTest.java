package org.mobicents.media.server.impl.rtp.crypto;

import org.bouncycastle.crypto.tls.SRTPProtectionProfile;
import org.junit.Assert;
import org.junit.Test;

public class SrtpParametersTest {

	@Test
	public void test() {
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getProfile(), SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getCipherKeyLength(), 128);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getCipherSaltLength(), 112);
		
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getProfile(), SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getCipherKeyLength(), 128);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getCipherSaltLength(), 112);
		
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getProfile(), SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getCipherKeyLength(), 0);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getCipherSaltLength(), 0);
		
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getProfile(), SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getCipherKeyLength(), 0);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getCipherSaltLength(), 0);
		
	}

}
