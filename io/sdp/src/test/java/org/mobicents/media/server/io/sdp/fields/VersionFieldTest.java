package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.exception.SdpException;
import org.mobicents.media.server.io.sdp.fields.VersionField;

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
	
	@Test
	public void testCanParse() {
		// given
		String validLine = "v=123";
		String invalidLine1 = "x=123";
		String invalidLine2 = "v=abc";
		String invalidLine3 = "v=123 213";
		
		// when
		VersionField field = new VersionField();
		
		// then
		Assert.assertTrue(field.canParse(validLine));
		Assert.assertFalse(field.canParse(invalidLine1));
		Assert.assertFalse(field.canParse(invalidLine2));
		Assert.assertFalse(field.canParse(invalidLine3));
	}
	
	@Test
	public void testValidParsing() throws SdpException {
		// given
		String vLine = "v=111";
		
		// when
		VersionField version = new VersionField();
		version.parse(vLine);
		
		// then
		Assert.assertEquals(111, version.getVersion());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParsing() throws SdpException {
		// given
		String vLine = "v=1 xyz";
		
		// when
		VersionField version = new VersionField();
		version.parse(vLine);
	}

}
