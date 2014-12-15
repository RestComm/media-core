package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.VersionField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class VersionFieldParserTest {
	
	private final VersionFieldParser parser = new VersionFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "v=123\n\r";
		String sdp2 = "x=123\n\r";
		String sdp3 = "v=abc\n\r";
		String sdp4 = "v=123 213\n\r";
		
		// when
		boolean canParseSdp1 = parser.canParse(sdp1);
		boolean canParseSdp2 = parser.canParse(sdp2);
		boolean canParseSdp3 = parser.canParse(sdp3);
		boolean canParseSdp4 = parser.canParse(sdp4);
		
		// then
		Assert.assertTrue(canParseSdp1);
		Assert.assertFalse(canParseSdp2);
		Assert.assertFalse(canParseSdp3);
		Assert.assertFalse(canParseSdp4);
	}
	
	@Test
	public void testParse() throws SdpException {
		// given
		String line = "v=111\n\r";
		
		// when
		VersionField field = parser.parse(line);
		
		// then
		Assert.assertEquals(111, field.getVersion());
	}

	@Test(expected=SdpException.class)
	public void testInvalidParse() throws SdpException {
		// given
		String line = "v=1 xyz\n\r";
		
		// when
		parser.parse(line);
	}

}
