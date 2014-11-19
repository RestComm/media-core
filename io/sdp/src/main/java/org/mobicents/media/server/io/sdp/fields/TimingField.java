package org.mobicents.media.server.io.sdp.fields;

import org.mobicents.media.server.io.sdp.Field;
import org.mobicents.media.server.io.sdp.exception.SdpException;

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
	private static final String BEGIN = String.valueOf(TYPE) + FIELD_SEPARATOR;
	private static final String FORMAT = BEGIN + "%d %d";

	// error messages
	private static final String EMPTY_TEXT = "Could not parse Timing Field because text is empty";
	private static final String INVALID_TEXT = "Could not parse Timing Field text: %s";
	
	// default values
	private static final int DEFAULT_START = 0;
	private static final int DEFAULT_STOP = 0;
	
	private int startTime;
	private int stopTime;
	
	public TimingField() {
		this(DEFAULT_START, DEFAULT_STOP);
	}
	
	public TimingField(int startTime, int stopTime) {
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
	public char getType() {
		return TYPE;
	}

	@Override
	public void parse(String text) throws SdpException {
		if(text == null || text.isEmpty()) {
			throw new SdpException(EMPTY_TEXT);
		}
		
		if(text.startsWith(BEGIN)) {
			try {
				String[] values = text.substring(2).split(" ");
				this.startTime = Integer.valueOf(values[0]);
				this.stopTime = Integer.valueOf(values[1]);
			} catch (IndexOutOfBoundsException | NumberFormatException e) {
				throw new SdpException(String.format(INVALID_TEXT, text), e);
			}
		} else {
			throw new SdpException(String.format(INVALID_TEXT, text));
		}
	}
	
	@Override
	public String toString() {
		return String.format(FORMAT, this.startTime, this.stopTime);
	}

}
