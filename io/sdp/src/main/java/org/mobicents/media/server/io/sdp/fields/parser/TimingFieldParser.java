package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.TimingField;

/**
 * Parses SDP text to construct {@link TimingField} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class TimingFieldParser implements SdpParser<TimingField> {

	private static final String REGEX = "^t=\\d+\\s\\d+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public TimingField parse(String sdp) throws SdpException {
		try {
			String[] values = sdp.trim().substring(2).split(" ");
			int startTime = Integer.parseInt(values[0]);
			int stopTime = Integer.parseInt(values[1]);
			return new TimingField(startTime, stopTime);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(TimingField field, String sdp) throws SdpException {
		try {
			String[] values = sdp.trim().substring(2).split(" ");
			int startTime = Integer.parseInt(values[0]);
			int stopTime = Integer.parseInt(values[1]);
			field.setStartTime(startTime);
			field.setStopTime(stopTime);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
