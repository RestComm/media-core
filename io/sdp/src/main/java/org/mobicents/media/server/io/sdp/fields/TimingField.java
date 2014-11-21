package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.SdpException;

/**
 * t=<start-time> <stop-time>
 * <p>
 * The "t=" lines specify the start and stop times for a session. <br>
 * Multiple "t=" lines MAY be used if a session is active at multiple
 * irregularly spaced times; each additional "t=" line specifies an additional
 * period of time for which the session will be active. If the session is active
 * at regular times, an "r=" line (see below) should be used in addition to, and
 * following, a "t=" line -- in which case the "t=" line specifies the start and
 * stop times of the repeat sequence.
 * </p>
 * <p>
 * The first and second sub-fields give the start and stop times, respectively,
 * for the session. These values are the decimal representation of Network Time
 * Protocol (NTP) time values in seconds since 1900. To convert these
 * values to UNIX time, subtract decimal 2208988800.
 * </p>
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class TimingField implements Field {

	// text parsing
	private static final char TYPE = 't';
	private static final String BEGIN = TYPE + FIELD_SEPARATOR;
	private static final int BEGIN_LEN = BEGIN.length();
	private static final String REGEX = "^" + BEGIN + "\\d+\\s\\d+$";

	// default values
	private static final int DEFAULT_START = 0;
	private static final int DEFAULT_STOP = 0;
	
	private final StringBuilder builder;
	
	private int startTime;
	private int stopTime;
	
	public TimingField() {
		this(DEFAULT_START, DEFAULT_STOP);
	}
	
	public TimingField(int startTime, int stopTime) {
		this.builder = new StringBuilder(BEGIN);
		this.startTime = startTime;
		this.stopTime = stopTime;
	}
	
	public int getStartTime() {
		return startTime;
	}

	public void setStartTime(int startTime) {
		this.startTime = startTime;
	}

	public int getStopTime() {
		return stopTime;
	}

	public void setStopTime(int stopTime) {
		this.stopTime = stopTime;
	}

	@Override
	public char getFieldType() {
		return TYPE;
	}
	
	@Override
	public boolean canParse(String text) {
		if(text == null || text.isEmpty()) {
			return false;
		}
		return text.matches(REGEX);
	}

	@Override
	public void parse(String text) throws SdpException {
		try {
			String[] values = text.substring(2).split(" ");
			this.startTime = Integer.valueOf(values[0]);
			this.stopTime = Integer.valueOf(values[1]);
		} catch (Exception e) {
			throw new SdpException(String.format(PARSE_ERROR, text), e);
		}
	}
	
	@Override
	public String toString() {
		// clear builder
		this.builder.setLength(BEGIN_LEN);
		this.builder.append(this.startTime).append(" ").append(this.stopTime);
		return this.builder.toString();
	}

}
