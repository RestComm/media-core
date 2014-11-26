package org.mobicents.media.server.io.sdp.attributes;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.attributes.FormatParameterAttribute;

import junit.framework.Assert;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class FormatAttributeTest {

	@Test
	public void testCustomFormat() {
		// given
		short format = 101;
		String params = "0-15";
		
		// when
		FormatParameterAttribute obj = new FormatParameterAttribute();
		obj.setFormat(format);
		obj.setParams(params);
		
		// given
		Assert.assertEquals(format, obj.getFormat());
		Assert.assertEquals(params, obj.getParams());
		Assert.assertEquals("a=fmtp:101 0-15", obj.toString());
	}

}
