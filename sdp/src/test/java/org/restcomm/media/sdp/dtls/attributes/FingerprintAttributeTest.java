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

package org.restcomm.media.sdp.dtls.attributes;

import org.junit.Test;
import org.restcomm.media.sdp.dtls.attributes.FingerprintAttribute;

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
