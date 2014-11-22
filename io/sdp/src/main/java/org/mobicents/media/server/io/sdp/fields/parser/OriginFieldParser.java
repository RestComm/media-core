package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.OriginField;

/**
 * Parses SDP text to construct {@link OriginField} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class OriginFieldParser implements SdpParser<OriginField> {
	
	// TODO use proper regex for IP address instead of [0-9\\.]+
	private static final String REGEX = "^o=\\S+\\s\\d+\\s\\d+\\s\\w+\\s\\w+\\s[0-9\\.]+";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public OriginField parse(String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(2).split(" ");
			String username = values[0];
			int sessionId = Integer.parseInt(values[1]);
			int sessionVersion = Integer.parseInt(values[2]);
			String netType = values[3];
			String addressType = values[4];
			String address = values[5];
			return new OriginField(username, sessionId, sessionVersion, netType, addressType, address);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(OriginField field, String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(2).split(" ");
			field.setUsername(values[0]);
			field.setSessionId(Integer.parseInt(values[1]));
			field.setSessionVersion(Integer.parseInt(values[2]));
			field.setNetType(values[3]);
			field.setAddressType(values[4]);
			field.setAddress(values[5]);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
