package org.mobicents.media.server.io.sdp.dtls.attributes;

import org.junit.Test;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class FingerprintAttributeTest {
	
	@Test
	public void testCustomFingerprint() {
		// given
		String hashFunction = "sha-256";
		String fingerprint = "D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39";
		String sdp = "a=fingerprint:sha-256 D1:2C:BE:AD:C4:F6:64:5C:25:16:11:9C:AF:E7:0F:73:79:36:4E:9C:1E:15:54:39:0C:06:8B:ED:96:86:00:39";
		
		// when
		FingerprintAttribute obj = new FingerprintAttribute(hashFunction, fingerprint);
		
		// then
		Assert.assertEquals(hashFunction, obj.getHashFunction());
		Assert.assertEquals(fingerprint, obj.getFingerprint());
		Assert.assertEquals(sdp, obj.toString());
	}

}
