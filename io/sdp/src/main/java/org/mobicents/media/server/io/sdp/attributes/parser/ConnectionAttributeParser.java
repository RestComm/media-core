package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.AbstractConnectionModeAttribute.ConnectionMode;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;

/**
 * Parses SDP text to construct {@link ConnectionModeAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionAttributeParser implements SdpParser<ConnectionModeAttribute> {

	private static final String REGEX = "^a=\\w+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if (sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches()
				&& ConnectionMode.containsMode(sdp.substring(2));
	}

	@Override
	public ConnectionModeAttribute parse(String sdp) throws SdpException {
		try {
			ConnectionMode mode = ConnectionMode.fromMode(sdp.substring(2));
			if (mode == null) {
				throw new IllegalArgumentException("Unknown connection mode");
			}
			return new ConnectionModeAttribute(mode);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(ConnectionModeAttribute field, String sdp) throws SdpException {
		try {
			ConnectionMode mode = ConnectionMode.fromMode(sdp.substring(2));
			if (mode == null) {
				throw new IllegalArgumentException("Unknown connection mode");
			}
			field.setMode(mode);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
