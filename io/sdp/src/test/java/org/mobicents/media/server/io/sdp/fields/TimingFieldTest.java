package org.mobicents.media.server.io.sdp.fields;

import junit.framework.Assert;

import org.junit.Test;

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
	
}
