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

package org.restcomm.media.core.rtp.crypto;

import org.bouncycastle.crypto.tls.SRTPProtectionProfile;
import org.junit.Assert;
import org.junit.Test;
import org.restcomm.media.core.rtp.crypto.SRTPParameters;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@teletsax.com)
 *
 */
public class SrtpParametersTest {

	@Test
	public void test() {
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getProfile(), SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_80);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getCipherKeyLength(), 128/8);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_80.getCipherSaltLength(), 112/8);
		
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getProfile(), SRTPProtectionProfile.SRTP_AES128_CM_HMAC_SHA1_32);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getCipherKeyLength(), 128/8);
		Assert.assertEquals(SRTPParameters.SRTP_AES128_CM_HMAC_SHA1_32.getCipherSaltLength(), 112/8);
		
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getProfile(), SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_80);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getCipherKeyLength(), 0);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_80.getCipherSaltLength(), 0);
		
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getProfile(), SRTPProtectionProfile.SRTP_NULL_HMAC_SHA1_32);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getCipherKeyLength(), 0);
		Assert.assertEquals(SRTPParameters.SRTP_NULL_HMAC_SHA1_32.getCipherSaltLength(), 0);
	}

}
