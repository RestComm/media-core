package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class VersionFieldTest {

	@Test
	public void testDefaultVersion() {
		// given
		VersionField sdp;

		// when
		sdp = new VersionField();

		// then
		Assert.assertEquals(0, sdp.getVersion());
		Assert.assertEquals("v=0", sdp.toString());
	}

	@Test
	public void testCustomVersion() {
		// given
		VersionField sdp;
		short version = 5;

		// when
		sdp = new VersionField(version);

		// then
		Assert.assertEquals(version, sdp.getVersion());
		Assert.assertEquals("v=" + version, sdp.toString());
	}

}
