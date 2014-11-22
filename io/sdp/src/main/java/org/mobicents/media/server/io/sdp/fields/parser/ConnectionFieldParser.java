package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.ConnectionField;

/**
 * Parses SDP text to construct {@link ConnectionField} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionFieldParser implements SdpParser<ConnectionField> {

	// TODO use proper regex for IP address instead of [0-9\\.]+
	private static final String REGEX = "^c=\\w+\\s\\w+\\s[0-9\\.]+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if (sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public ConnectionField parse(String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(2).split(" ");
			String networkType = values[0];
			String addressType = values[1];
			String address = values[2];
			return new ConnectionField(networkType, addressType, address);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(ConnectionField field, String sdp) throws SdpException {
		try {
			String[] values = sdp.substring(2).split(" ");
			field.setNetworkType(values[0]);
			field.setAddressType(values[1]);
			field.setAddress(values[2]);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
