package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;
import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.fields.TimingField;

/**
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 *
 */
public class TimingFieldTest {


	@Test
	public void testDefaultTiming() {
		// given
		TimingField field;

		// when
		field = new TimingField();

		// then
		Assert.assertEquals(0, field.getStartTime());
		Assert.assertEquals(0, field.getStopTime());
		String expected = "t=0 0";
		Assert.assertEquals(expected, field.toString());
	}

	@Test
	public void testCustomConnection() {
		// given
		int startTime = 123;
		int stopTime = 456;

		// when
		TimingField field = new TimingField();
		field.setStartTime(startTime);
		field.setStopTime(stopTime);

		// then
		Assert.assertEquals(startTime, field.getStartTime());
		Assert.assertEquals(stopTime, field.getStopTime());
		String expected = "t=" + startTime + " " + stopTime;
		Assert.assertEquals(expected, field.toString());
	}
	
	@Test
	public void testCanParse() {
		// given
		String validLine1 = "t=123 456";
		String invalidLine1 = "t=xyz 456";
		String invalidLine2 = "t=123 xyz";
		String invalidLine3 = "x=123 456";
		
		// when
		TimingField field = new TimingField();
		
		// then
		Assert.assertTrue(field.canParse(validLine1));
		Assert.assertFalse(field.canParse(invalidLine1));
		Assert.assertFalse(field.canParse(invalidLine2));
		Assert.assertFalse(field.canParse(invalidLine3));
	}

	@Test
	public void testValidParse() throws SdpException {
		// given
		String line = "t=123 456";

		// when
		TimingField field = new TimingField();
		field.parse(line);

		// then
		Assert.assertEquals(123, field.getStartTime());
		Assert.assertEquals(456, field.getStopTime());
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseMissingElement() throws SdpException {
		// given
		String line = "t=123";

		// when
		TimingField field = new TimingField();
		field.parse(line);
	}

	@Test(expected = SdpException.class)
	public void testInvalidParseNumberFormat() throws SdpException {
		// given
		String line = "t=123 xyz";
		
		// when
		TimingField field = new TimingField();
		field.parse(line);
	}
	
}
