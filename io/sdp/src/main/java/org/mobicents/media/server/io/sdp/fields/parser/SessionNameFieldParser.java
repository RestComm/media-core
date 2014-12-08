package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.SessionNameField;

/**
 * Parses SDP text to construct {@link SessionNameField} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class SessionNameFieldParser implements SdpParser<SessionNameField> {

	private static final String REGEX = "^s=\\s|(\\S+\\s?)+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);
	
	@Override
	public boolean canParse(String sdp) {
		if(sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp.trim()).matches();
	}

	@Override
	public SessionNameField parse(String sdp) throws SdpException {
		try {
			return new SessionNameField(sdp.trim().substring(2));
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(SessionNameField field, String sdp) throws SdpException {
		try {
			field.setName(sdp.trim().substring(2));
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
