package org.mobicents.media.server.io.sdp.fields.parser;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.TimingField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TimingFieldParserTest {
	
	private final TimingFieldParser parser = new TimingFieldParser();
	
	@Test
	public void testCanParse() {
		// given
		String sdp1 = "t=123 456";
		String sdp2 = "t=xyz 456";
		String sdp3 = "t=123 xyz";
		String sdp4 = "x=123 456";
		
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
	public void testValidParse() throws SdpException {
		// given
		String line = "t=123 456";

		// when
		TimingField field = parser.parse(line);

		// then
		Assert.assertEquals(123, field.getStartTime());
		Assert.assertEquals(456, field.getStopTime());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "t=123";

		// when
		parser.parse(line);
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseNumberFormat() throws SdpException {
		// given
		String line = "t=123 xyz";
		
		// when
		parser.parse(line);
	}

}
