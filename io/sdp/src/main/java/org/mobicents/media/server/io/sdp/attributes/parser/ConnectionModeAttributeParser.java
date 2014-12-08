package org.mobicents.media.server.io.sdp.attributes.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.attributes.ConnectionModeAttribute;

/**
 * Parses SDP text to construct {@link ConnectionModeAttribute} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class ConnectionModeAttributeParser implements SdpParser<ConnectionModeAttribute> {

	private static final String REGEX = "^a=(sendonly|recvonly|sendrecv|inactive)$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	private boolean isTypeValid(String type) {
		return ConnectionModeAttribute.SENDONLY.equals(type) ||
				ConnectionModeAttribute.RECVONLY.equals(type) ||
				ConnectionModeAttribute.SENDRECV.equals(type) ||
				ConnectionModeAttribute.INACTIVE.equals(type);
	}
	
	@Override
	public boolean canParse(String sdp) {
		if (sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public ConnectionModeAttribute parse(String sdp) throws SdpException {
		try {
			String mode = sdp.trim().substring(2);
			if (!isTypeValid(mode)) {
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
			String mode = sdp.trim().substring(2);
			if (!isTypeValid(mode)) {
				throw new IllegalArgumentException("Unknown connection mode");
			}
			field.setMode(mode);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
