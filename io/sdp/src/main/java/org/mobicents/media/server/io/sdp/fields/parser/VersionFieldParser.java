package org.mobicents.media.server.io.sdp.fields.parser;

import java.util.regex.Pattern;

import org.mobicents.media.server.io.sdp.SdpException;
import org.mobicents.media.server.io.sdp.SdpParser;
import org.mobicents.media.server.io.sdp.fields.VersionField;

/**
 * Parses SDP text to construct {@link VersionField} objects.
 * 
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * 
 */
public class VersionFieldParser implements SdpParser<VersionField> {

	private static final String REGEX = "^v=\\d+$";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	@Override
	public boolean canParse(String sdp) {
		if (sdp == null || sdp.isEmpty()) {
			return false;
		}
		return PATTERN.matcher(sdp).matches();
	}

	@Override
	public VersionField parse(String sdp) throws SdpException {
		try {
			short version = Short.parseShort(sdp.substring(2));
			return new VersionField(version);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

	@Override
	public void parse(VersionField field, String sdp) throws SdpException {
		try {
			short version = Short.parseShort(sdp.substring(2));
			field.setVersion(version);
		} catch (Exception e) {
			throw new SdpException(PARSE_ERROR + sdp, e);
		}
	}

}
